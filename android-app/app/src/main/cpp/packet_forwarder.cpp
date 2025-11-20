#include "packet_forwarder.h"
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>  // Add this for TCP_NODELAY
#include <arpa/inet.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <android/log.h>
#include <sys/select.h>
#include <poll.h>

#define LOG_TAG "mph_native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// Feature flag: disable legacy TCP response synthesis by default (must be declared before use)
static bool g_enableLegacyTcpResponseSynthesis = false;
void setLegacyTcpResponseSynthesisEnabled(bool enabled) { g_enableLegacyTcpResponseSynthesis = enabled; }

// Forward declarations
static uint16_t calculateIpChecksum(const uint8_t* header, size_t len);
static uint16_t calculateTransportChecksum(uint32_t srcIp, uint32_t dstIp,
                                           uint8_t protocol, const uint8_t* data, size_t len);
static void writeUdpResponseToTun(int tunFd, uint32_t srcIp, uint32_t dstIp,
                                   uint16_t srcPort, uint16_t dstPort,
                                   const uint8_t* data, size_t dataLen);
static void writeTcpResponseToTun(int tunFd, uint32_t srcIp, uint32_t dstIp,
                                   uint16_t srcPort, uint16_t dstPort,
                                   const uint8_t* data, size_t dataLen);

PacketForwarder::PacketForwarder() {
    m_stats = {};
    m_lastStatsLog = std::chrono::steady_clock::now();
}

PacketForwarder::~PacketForwarder() {
    stop();
}

bool PacketForwarder::start(int tunFd, 
                            std::function<bool(int)> protectCallback,
                            std::function<void(const uint8_t*, size_t)> packetCallback) {
    if (m_running.load()) {
        LOGE("Forwarder already running");
        return false;
    }
    
    if (tunFd < 0) {
        LOGE("Invalid TUN file descriptor: %d", tunFd);
        return false;
    }
    
    m_tunFd = tunFd;
    m_protectCallback = protectCallback;
    m_packetCallback = packetCallback;
    m_running.store(true);
    m_lastStatsLog = std::chrono::steady_clock::now();
    
    // Start TUN read loop
    m_tunThread = std::thread(&PacketForwarder::tunReadLoop, this);
    
    // Start TCP response handler
    m_tcpResponseThread = std::thread(&PacketForwarder::tcpResponseLoop, this);
    
    // Start UDP response handler
    m_udpResponseThread = std::thread(&PacketForwarder::udpResponseLoop, this);
    
    // Start stats monitor thread (logs every 5 seconds)
    m_statsMonitorThread = std::thread(&PacketForwarder::statsMonitorLoop, this);
    
    LOGI("✅ Packet forwarder started with TUN FD: %d", tunFd);
    return true;
}

void PacketForwarder::stop() {
    if (!m_running.load()) {
        return;
    }
    
    LOGI("Stopping packet forwarder...");
    m_running.store(false);
    
    if (m_tunThread.joinable()) {
        m_tunThread.join();
    }
    
    if (m_tcpResponseThread.joinable()) {
        m_tcpResponseThread.join();
    }
    
    if (m_udpResponseThread.joinable()) {
        m_udpResponseThread.join();
    }
    
    if (m_statsMonitorThread.joinable()) {
        m_statsMonitorThread.join();
    }
    
    cleanupConnections();
    
    LOGI("✅ Packet forwarder stopped");
}

bool PacketForwarder::isRunning() const {
    return m_running.load();
}

PacketForwarder::Stats PacketForwarder::getStats() const {
    std::lock_guard<std::mutex> lock(m_statsMutex);
    return m_stats;
}

void PacketForwarder::tunReadLoop() {
    const size_t BUFFER_SIZE = 32767;
    uint8_t buffer[BUFFER_SIZE];
    
    LOGI("TUN read loop started");
    
    while (m_running.load()) {
        ssize_t len = read(m_tunFd, buffer, BUFFER_SIZE);
        
        if (len > 0) {
            {
                std::lock_guard<std::mutex> lock(m_statsMutex);
                m_stats.packetsRead++;
                m_stats.bytesRead += len;
            }
            
            // Call packet callback for processing (if provided)
            if (m_packetCallback) {
                m_packetCallback(buffer, static_cast<size_t>(len));
            }
            
            processPacket(buffer, static_cast<size_t>(len));
        } else if (len == 0) {
            // No data available
            usleep(1000); // 1ms
        } else {
            if (errno != EAGAIN && errno != EWOULDBLOCK) {
                {
                    std::lock_guard<std::mutex> lock(m_statsMutex);
                    m_stats.errors++;
                }
                LOGE("TUN read error: %s", strerror(errno));
            }
            usleep(1000);
        }
    }
    
    LOGI("TUN read loop ended");
}

void PacketForwarder::processPacket(const uint8_t* data, size_t len) {
    if (len < 20) { // Minimum IP header size
        return;
    }
    
    uint32_t srcIp, dstIp;
    uint8_t protocol;
    
    if (!parseIpHeader(data, len, &srcIp, &dstIp, &protocol)) {
        return;
    }
    
    // Handle TCP
    if (protocol == 6) {
        handleTcpPacket(data, len);
    }
    // Handle UDP
    else if (protocol == 17) {
        handleUdpPacket(data, len);
    }
    // Handle ICMP (ping responses)
    else if (protocol == 1) {
        // For ICMP, we can forward directly or handle specially
        // For now, just log it
        LOGD("ICMP packet: %s -> %s", 
             inet_ntoa(*(struct in_addr*)&srcIp),
             inet_ntoa(*(struct in_addr*)&dstIp));
    }
}

uint16_t PacketForwarder::parseIpHeader(const uint8_t* data, size_t len,
                                          uint32_t* srcIp, uint32_t* dstIp,
                                          uint8_t* protocol) {
    if (len < 20) {
        return 0;
    }
    
    uint8_t version = (data[0] >> 4) & 0x0F;
    if (version != 4) {
        return 0; // Not IPv4
    }
    
    uint8_t ihl = (data[0] & 0x0F) * 4;
    if (ihl < 20 || ihl > len) {
        return 0;
    }
    
    *protocol = data[9];
    memcpy(srcIp, &data[12], 4);
    memcpy(dstIp, &data[16], 4);
    
    uint16_t totalLength = (data[2] << 8) | data[3];
    return totalLength;
}

void PacketForwarder::handleTcpPacket(const uint8_t* ipHeader, size_t ipLen) {
    uint32_t srcIp, dstIp;
    uint8_t protocol;
    uint16_t ipTotalLen = parseIpHeader(ipHeader, ipLen, &srcIp, &dstIp, &protocol);
    
    if (ipTotalLen < 40 || ipLen < ipTotalLen) { // IP header + TCP header
        return;
    }
    
    uint8_t ihl = (ipHeader[0] & 0x0F) * 4;
    const uint8_t* tcpHeader = ipHeader + ihl;
    uint16_t srcPort = (tcpHeader[0] << 8) | tcpHeader[1];
    uint16_t dstPort = (tcpHeader[2] << 8) | tcpHeader[3];
    
    // Skip packets to localhost - they should not be forwarded
    if (dstIp == inet_addr("127.0.0.1") || srcIp == inet_addr("127.0.0.1")) {
        LOGD("Skipping localhost packet: %s:%d -> %s:%d", 
             inet_ntoa(*(struct in_addr*)&srcIp), srcPort,
             inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
        return;
    }
    
    // PCAPdroid-style connection key for better lookup performance
    ConnKey key = {srcIp, dstIp, srcPort, dstPort};
    
    std::lock_guard<std::mutex> lock(m_connectionsMutex);
    
    // Check connection map for faster lookup
    auto it = m_tcpConnMap.find(key);
    TcpConnection* conn = nullptr;
    
    if (it != m_tcpConnMap.end()) {
        // Found existing connection
        size_t index = it->second;
        if (index < m_tcpConnections.size()) {
            conn = &m_tcpConnections[index];
        }
    }
    
    if (!conn) {
        // Create new connection - PCAPdroid approach with better error handling
        TcpConnection newConn;
        newConn.srcIp = srcIp;
        newConn.dstIp = dstIp;
        newConn.srcPort = srcPort;
        newConn.dstPort = dstPort;
        newConn.socketFd = createTcpSocket(dstIp, dstPort);
        newConn.connected = false;
        newConn.seqNum = 0;
        newConn.ackNum = 0;
        newConn.lastActivity = std::chrono::steady_clock::now();
        
        if (newConn.socketFd >= 0) {
            // CRITICAL: Protect socket BEFORE connecting to prevent routing loops
            // This is the key fix from PCAPdroid - proper socket protection timing
            if (m_protectCallback && !m_protectCallback(newConn.socketFd)) {
                LOGE("❌ Failed to protect TCP socket %d - closing", newConn.socketFd);
                close(newConn.socketFd);
                return;
            }
            
            // Now connect after protection - PCAPdroid approach
            if (connectTcpSocket(newConn.socketFd, dstIp, dstPort)) {
                newConn.connected = true;
                
                // Add to connection vector and map for faster lookup
                size_t index = m_tcpConnections.size();
                m_tcpConnections.push_back(newConn);
                m_tcpConnMap[key] = index;
                conn = &m_tcpConnections.back();
                
                {
                    std::lock_guard<std::mutex> statsLock(m_statsMutex);
                    m_stats.tcpConnections++;
                }
                LOGD("✅ New TCP connection established: %s:%d -> %s:%d", 
                     inet_ntoa(*(struct in_addr*)&srcIp), srcPort,
                     inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
            } else {
                LOGE("❌ Failed to connect TCP socket %d to %s:%d", 
                     newConn.socketFd,
                     inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
                close(newConn.socketFd);
                return;
            }
        } else {
            return;
        }
    }
    
    if (conn && conn->connected && conn->socketFd >= 0) {
        conn->lastActivity = std::chrono::steady_clock::now();
        
        const uint8_t* tcpData = tcpHeader + ((tcpHeader[12] >> 4) * 4); // TCP data offset
        size_t tcpDataLen = ipTotalLen - ihl - ((tcpHeader[12] >> 4) * 4);
        
        if (tcpDataLen > 0) {
            // PCAPdroid-style send with proper error handling
            ssize_t sent = send(conn->socketFd, tcpData, tcpDataLen, MSG_NOSIGNAL);
            if (sent > 0) {
                std::lock_guard<std::mutex> statsLock(m_statsMutex);
                m_stats.packetsForwarded++;
                m_stats.bytesForwarded += sent;
                LOGD("➡️ TCP sent %zd bytes to %s:%d", sent, 
                     inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
            } else if (sent < 0) {
                if (errno != EAGAIN && errno != EWOULDBLOCK) {
                    LOGE("❌ TCP send failed: %s", strerror(errno));
                    // Mark connection as disconnected
                    conn->connected = false;
                }
            }
        }
        
        // PCAPdroid-style response handling with proper polling
        // Use poll instead of recv with MSG_DONTWAIT for better performance
        struct pollfd pfd;
        pfd.fd = conn->socketFd;
        pfd.events = POLLIN | POLLPRI;
        pfd.revents = 0;
        
        int poll_result = poll(&pfd, 1, 0); // Non-blocking poll
        if (poll_result > 0 && (pfd.revents & (POLLIN | POLLPRI))) {
            uint8_t responseBuffer[8192]; // Larger buffer for better performance
            ssize_t received = recv(conn->socketFd, responseBuffer, sizeof(responseBuffer), MSG_DONTWAIT);
            if (received > 0) {
                LOGD("⬅️ Received %zd bytes TCP response from %s:%d", received,
                     inet_ntoa(*(struct in_addr*)&dstIp), dstPort);

                // Write response back to TUN only if legacy synthesis is enabled (lab mode)
                if (g_enableLegacyTcpResponseSynthesis) {
                    writeTcpResponseToTun(m_tunFd, dstIp, srcIp, dstPort, srcPort,
                                          responseBuffer, received);
                }

                std::lock_guard<std::mutex> statsLock(m_statsMutex);
                m_stats.packetsForwarded++;
                m_stats.bytesForwarded += received;
            } else if (received < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
                LOGE("❌ TCP recv error: %s", strerror(errno));
                // Mark connection as disconnected on error
                conn->connected = false;
            }
        }
    }
}

void PacketForwarder::handleUdpPacket(const uint8_t* ipHeader, size_t ipLen) {
    uint32_t srcIp, dstIp;
    uint8_t protocol;
    uint16_t ipTotalLen = parseIpHeader(ipHeader, ipLen, &srcIp, &dstIp, &protocol);
    
    if (ipTotalLen < 28 || ipLen < ipTotalLen) { // IP header + UDP header
        return;
    }
    
    uint8_t ihl = (ipHeader[0] & 0x0F) * 4;
    const uint8_t* udpHeader = ipHeader + ihl;
    uint16_t srcPort = (udpHeader[0] << 8) | udpHeader[1];
    uint16_t dstPort = (udpHeader[2] << 8) | udpHeader[3];
    uint16_t udpLen = (udpHeader[4] << 8) | udpHeader[5];
    
    // Skip packets to localhost - they should not be forwarded
    if (dstIp == inet_addr("127.0.0.1") || srcIp == inet_addr("127.0.0.1")) {
        LOGD("Skipping localhost UDP packet: %s:%d -> %s:%d", 
             inet_ntoa(*(struct in_addr*)&srcIp), srcPort,
             inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
        return;
    }
    
    // Check if DNS (port 53) - PCAPdroid-style special handling
    if (dstPort == 53 || srcPort == 53) {
        {
            std::lock_guard<std::mutex> lock(m_statsMutex);
            m_stats.dnsQueries++;
        }
        handleDnsPacket(udpHeader + 8, udpLen - 8, srcIp, dstIp, srcPort, dstPort);
        return;
    }
    
    // Handle other UDP - PCAPdroid approach with connection map
    ConnKey key = {srcIp, dstIp, srcPort, dstPort};
    
    std::lock_guard<std::mutex> lock(m_connectionsMutex);
    
    // Check session map for faster lookup
    auto it = m_udpSessionMap.find(key);
    UdpSession* session = nullptr;
    
    if (it != m_udpSessionMap.end()) {
        // Found existing session
        size_t index = it->second;
        if (index < m_udpSessions.size()) {
            session = &m_udpSessions[index];
        }
    }
    
    if (!session) {
        // Create new session - PCAPdroid approach
        UdpSession newSession;
        newSession.srcIp = srcIp;
        newSession.dstIp = dstIp;
        newSession.srcPort = srcPort;
        newSession.dstPort = dstPort;
        newSession.socketFd = createUdpSocket();
        newSession.lastActivity = std::chrono::steady_clock::now();
        
        if (newSession.socketFd >= 0) {
            // CRITICAL: Protect socket BEFORE using to prevent routing loops
            // This is the key fix from PCAPdroid - proper socket protection timing
            if (m_protectCallback && !m_protectCallback(newSession.socketFd)) {
                LOGE("❌ Failed to protect UDP socket %d - closing", newSession.socketFd);
                close(newSession.socketFd);
                return;
            }
            
            // Add to session vector and map for faster lookup
            size_t index = m_udpSessions.size();
            m_udpSessions.push_back(newSession);
            m_udpSessionMap[key] = index;
            session = &m_udpSessions.back();
            
            {
                std::lock_guard<std::mutex> statsLock(m_statsMutex);
                m_stats.udpSessions++;
            }
            LOGD("✅ New UDP session established: %s:%d -> %s:%d", 
                 inet_ntoa(*(struct in_addr*)&srcIp), srcPort,
                 inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
        } else {
            return;
        }
    }
    
    if (session && session->socketFd >= 0) {
        session->lastActivity = std::chrono::steady_clock::now();
        
        const uint8_t* udpData = udpHeader + 8;
        size_t udpDataLen = udpLen - 8;
        
        if (udpDataLen > 0) {
            // PCAPdroid-style send with proper error handling
            struct sockaddr_in addr;
            memset(&addr, 0, sizeof(addr));
            addr.sin_family = AF_INET;
            addr.sin_port = htons(dstPort);
            addr.sin_addr.s_addr = dstIp;
            
            ssize_t sent = sendto(session->socketFd, udpData, udpDataLen, 
                                 0, (struct sockaddr*)&addr, sizeof(addr));
            if (sent > 0) {
                std::lock_guard<std::mutex> statsLock(m_statsMutex);
                m_stats.packetsForwarded++;
                m_stats.bytesForwarded += sent;
                LOGD("➡️ UDP sent %zd bytes to %s:%d", sent, 
                     inet_ntoa(*(struct in_addr*)&dstIp), dstPort);
            } else if (sent < 0) {
                if (errno != EAGAIN && errno != EWOULDBLOCK) {
                    LOGE("❌ UDP sendto failed: %s", strerror(errno));
                }
            }
        }
        
        // PCAPdroid-style response handling with proper polling
        struct pollfd pfd;
        pfd.fd = session->socketFd;
        pfd.events = POLLIN | POLLPRI;
        pfd.revents = 0;
        
        int poll_result = poll(&pfd, 1, 0); // Non-blocking poll
        if (poll_result > 0 && (pfd.revents & (POLLIN | POLLPRI))) {
            uint8_t responseBuffer[8192];
            struct sockaddr_in responseAddr;
            socklen_t addrLen = sizeof(responseAddr);
            
            ssize_t received = recvfrom(session->socketFd, responseBuffer, sizeof(responseBuffer), 
                                       MSG_DONTWAIT, (struct sockaddr*)&responseAddr, &addrLen);
            if (received > 0) {
                LOGD("⬅️ Received %zd bytes UDP response from %s:%d", received,
                     inet_ntoa(responseAddr.sin_addr), ntohs(responseAddr.sin_port));

                // Write response back to TUN using PCAPdroid-style approach
                writeUdpResponseToTun(m_tunFd, responseAddr.sin_addr.s_addr, srcIp, 
                                     ntohs(responseAddr.sin_port), srcPort,
                                     responseBuffer, received);

                std::lock_guard<std::mutex> statsLock(m_statsMutex);
                m_stats.packetsForwarded++;
                m_stats.bytesForwarded += received;
            } else if (received < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
                LOGE("❌ UDP recvfrom error: %s", strerror(errno));
            }
        }
    }
}

void PacketForwarder::handleDnsPacket(const uint8_t* udpData, size_t udpLen,
                                     uint32_t srcIp, uint32_t dstIp,
                                     uint16_t srcPort, uint16_t dstPort) {
    // Create UDP socket for DNS
    int dnsSocket = socket(AF_INET, SOCK_DGRAM, 0);
    if (dnsSocket < 0) {
        LOGE("Failed to create DNS socket: %s", strerror(errno));
        return;
    }
    
    // Protect socket
    if (m_protectCallback && !m_protectCallback(dnsSocket)) {
        LOGE("Failed to protect DNS socket");
        close(dnsSocket);
        return;
    }
    
    // Connect to DNS server
    struct sockaddr_in dnsAddr;
    memset(&dnsAddr, 0, sizeof(dnsAddr));
    dnsAddr.sin_family = AF_INET;
    dnsAddr.sin_port = htons(53);
    dnsAddr.sin_addr.s_addr = dstIp;
    
    if (connect(dnsSocket, (struct sockaddr*)&dnsAddr, sizeof(dnsAddr)) < 0) {
        LOGE("Failed to connect DNS socket: %s", strerror(errno));
        close(dnsSocket);
        return;
    }
    
    // Send DNS query
    if (send(dnsSocket, udpData, udpLen, 0) < 0) {
        LOGE("Failed to send DNS query: %s", strerror(errno));
        close(dnsSocket);
        return;
    }
    
    // Receive DNS response
    uint8_t responseBuffer[4096];
    ssize_t received = recv(dnsSocket, responseBuffer, sizeof(responseBuffer), 0);
    if (received > 0) {
        LOGD("DNS response received: %zd bytes", received);
        
        // Write DNS response back to TUN
        writeUdpResponseToTun(m_tunFd, dstIp, srcIp, dstPort, srcPort,
                             responseBuffer, received);
        
        std::lock_guard<std::mutex> statsLock(m_statsMutex);
        m_stats.packetsForwarded++;
        m_stats.bytesForwarded += received;
    }
    
    close(dnsSocket);
}

int PacketForwarder::createTcpSocket(uint32_t dstIp, uint16_t dstPort) {
    int fd = socket(AF_INET, SOCK_STREAM, 0);
    if (fd < 0) {
        LOGE("Failed to create TCP socket: %s", strerror(errno));
        return -1;
    }
    
    // Set non-blocking
    int flags = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    
    return fd;
}

int PacketForwarder::createUdpSocket() {
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0) {
        LOGE("Failed to create UDP socket: %s", strerror(errno));
        return -1;
    }
    
    // Set non-blocking
    int flags = fcntl(fd, F_GETFL, 0);
    fcntl(fd, F_SETFL, flags | O_NONBLOCK);
    
    return fd;
}

bool PacketForwarder::connectTcpSocket(int fd, uint32_t dstIp, uint16_t dstPort) {
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(dstPort);
    addr.sin_addr.s_addr = dstIp;
    
    // CRITICAL: Set socket options BEFORE connecting
    // Enable keepalive for long-lived connections
    int keepalive = 1;
    setsockopt(fd, SOL_SOCKET, SO_KEEPALIVE, &keepalive, sizeof(keepalive));
    
    // Set TCP_NODELAY to disable Nagle's algorithm for better responsiveness
    int nodelay = 1;
    setsockopt(fd, IPPROTO_TCP, TCP_NODELAY, &nodelay, sizeof(nodelay));
    
    int result = connect(fd, (struct sockaddr*)&addr, sizeof(addr));
    if (result < 0 && errno != EINPROGRESS) {
        LOGE("TCP connect failed: %s", strerror(errno));
        return false;
    }
    
    // Use proper select/poll for connection instead of usleep
    if (errno == EINPROGRESS) {
        fd_set write_fds;
        FD_ZERO(&write_fds);
        FD_SET(fd, &write_fds);
        
        struct timeval timeout;
        timeout.tv_sec = 5;  // 5 second timeout
        timeout.tv_usec = 0;
        
        int select_result = select(fd + 1, NULL, &write_fds, NULL, &timeout);
        if (select_result <= 0) {
            LOGE("TCP connect timeout or error: %s", strerror(errno));
            return false;
        }
        
        // Check if connection was successful
        int error = 0;
        socklen_t len = sizeof(error);
        if (getsockopt(fd, SOL_SOCKET, SO_ERROR, &error, &len) == 0 && error == 0) {
            LOGD("✅ TCP connection established via select");
            return true;
        } else {
            LOGE("TCP connect failed after select: %s", strerror(error));
            return false;
        }
    }
    
    LOGD("✅ TCP connection established immediately");
    return true;
}

bool PacketForwarder::sendUdpPacket(int fd, const uint8_t* data, size_t len,
                                    uint32_t dstIp, uint16_t dstPort) {
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_port = htons(dstPort);
    addr.sin_addr.s_addr = dstIp;
    
    ssize_t sent = sendto(fd, data, len, 0, (struct sockaddr*)&addr, sizeof(addr));
    return sent == static_cast<ssize_t>(len);
}

void PacketForwarder::writeToTun(const uint8_t* data, size_t len) {
    if (m_tunFd < 0 || len == 0) {
        return;
    }
    
    size_t totalWritten = 0;
    const uint8_t* ptr = data;
    
    // Handle partial writes with loop
    while (totalWritten < len) {
        ssize_t written = write(m_tunFd, ptr + totalWritten, len - totalWritten);
        
        if (written < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                // Would block - wait a bit and retry
                usleep(100);
                continue;
            } else if (errno == EINTR) {
                // Interrupted - retry
                continue;
            } else {
                LOGE("Failed to write to TUN: %s", strerror(errno));
                return;
            }
        } else if (written == 0) {
            LOGW("Write to TUN returned 0 bytes");
            return;
        }
        
        totalWritten += written;
    }
    
    if (totalWritten < len) {
        LOGW("Partial write to TUN: %zu/%zu bytes", totalWritten, len);
    } else {
        LOGD("⬇️ Wrote %zu bytes to TUN (inbound packet)", totalWritten);
    }
}

// Calculate IP checksum
static uint16_t calculateIpChecksum(const uint8_t* header, size_t len) {
    uint32_t sum = 0;
    const uint16_t* ptr = (const uint16_t*)header;

    for (size_t i = 0; i < len / 2; i++) {
        sum += ntohs(ptr[i]);
    }

    while (sum >> 16) {
        sum = (sum & 0xFFFF) + (sum >> 16);
    }

    return htons(~sum);
}

// Write UDP response packet back to TUN
static void writeUdpResponseToTun(int tunFd, uint32_t srcIp, uint32_t dstIp,
                                   uint16_t srcPort, uint16_t dstPort,
                                   const uint8_t* data, size_t dataLen) {
    size_t totalLen = 20 + 8 + dataLen; // IP + UDP + data
    uint8_t* packet = new uint8_t[totalLen];
    memset(packet, 0, totalLen);

    // IP Header
    packet[0] = 0x45; // Version 4, IHL 5
    packet[1] = 0;    // DSCP/ECN
    *(uint16_t*)(packet + 2) = htons(totalLen);
    *(uint16_t*)(packet + 4) = htons(rand() & 0xFFFF); // Random ID
    *(uint16_t*)(packet + 6) = htons(0x4000); // Don't fragment
    packet[8] = 64;   // TTL
    packet[9] = 17;   // Protocol: UDP
    *(uint16_t*)(packet + 10) = 0; // Checksum placeholder
    *(uint32_t*)(packet + 12) = srcIp;
    *(uint32_t*)(packet + 16) = dstIp;

    // Calculate IP checksum
    *(uint16_t*)(packet + 10) = calculateIpChecksum(packet, 20);

    // UDP Header
    *(uint16_t*)(packet + 20) = htons(srcPort);
    *(uint16_t*)(packet + 22) = htons(dstPort);
    *(uint16_t*)(packet + 24) = htons(8 + dataLen);
    *(uint16_t*)(packet + 26) = 0; // Checksum placeholder

    // Copy data
    if (dataLen > 0) {
        memcpy(packet + 28, data, dataLen);
    }

    // Calculate UDP checksum (optional but recommended)
    *(uint16_t*)(packet + 26) = calculateTransportChecksum(srcIp, dstIp, 17, packet + 20, 8 + dataLen);

    // Write to TUN
    ssize_t written = write(tunFd, packet, totalLen);
    if (written < 0) {
        LOGE("Failed to write UDP response to TUN: %s", strerror(errno));
    } else {
        LOGD("Wrote %zd bytes UDP response to TUN", written);
    }

    delete[] packet;
}

// Calculate TCP/UDP checksum with pseudo-header
static uint16_t calculateTransportChecksum(uint32_t srcIp, uint32_t dstIp,
                                           uint8_t protocol, const uint8_t* data, size_t len) {
    // Create pseudo-header
    uint32_t sum = 0;

    // Add source IP (2 x 16-bit words)
    sum += (srcIp >> 16) & 0xFFFF;
    sum += srcIp & 0xFFFF;

    // Add dest IP (2 x 16-bit words)
    sum += (dstIp >> 16) & 0xFFFF;
    sum += dstIp & 0xFFFF;

    // Add protocol and length
    sum += htons(protocol);
    sum += htons(len);

    // Add data
    const uint16_t* ptr = (const uint16_t*)data;
    for (size_t i = 0; i < len / 2; i++) {
        sum += ntohs(ptr[i]);
    }

    // Handle odd length
    if (len % 2) {
        sum += data[len - 1] << 8;
    }

    // Fold 32-bit sum to 16 bits
    while (sum >> 16) {
        sum = (sum & 0xFFFF) + (sum >> 16);
    }

    return htons(~sum);
}

// Write TCP response packet back to TUN (simplified - no proper seq/ack tracking)
static void writeTcpResponseToTun(int tunFd, uint32_t srcIp, uint32_t dstIp,
                                   uint16_t srcPort, uint16_t dstPort,
                                   const uint8_t* data, size_t dataLen) {
    size_t totalLen = 20 + 20 + dataLen; // IP + TCP + data
    uint8_t* packet = new uint8_t[totalLen];
    memset(packet, 0, totalLen);

    // IP Header
    packet[0] = 0x45;
    packet[1] = 0;
    *(uint16_t*)(packet + 2) = htons(totalLen);
    *(uint16_t*)(packet + 4) = htons(rand() & 0xFFFF); // Random ID
    *(uint16_t*)(packet + 6) = htons(0x4000); // Don't fragment
    packet[8] = 64; // TTL
    packet[9] = 6; // Protocol: TCP
    *(uint16_t*)(packet + 10) = 0; // Checksum placeholder
    *(uint32_t*)(packet + 12) = srcIp;
    *(uint32_t*)(packet + 16) = dstIp;

    // Calculate IP checksum
    *(uint16_t*)(packet + 10) = calculateIpChecksum(packet, 20);

    // TCP Header (simplified - missing proper seq/ack)
    *(uint16_t*)(packet + 20) = htons(srcPort);
    *(uint16_t*)(packet + 22) = htons(dstPort);
    *(uint32_t*)(packet + 24) = htonl(rand()); // Seq (should track state)
    *(uint32_t*)(packet + 28) = htonl(rand()); // Ack (should track state)
    packet[32] = 0x50; // Data offset: 5 words (20 bytes)
    packet[33] = 0x18; // Flags: PSH, ACK
    *(uint16_t*)(packet + 34) = htons(65535); // Window
    *(uint16_t*)(packet + 36) = 0; // Checksum placeholder
    *(uint16_t*)(packet + 38) = 0; // Urgent pointer

    // Copy data
    if (dataLen > 0) {
        memcpy(packet + 40, data, dataLen);
    }

    // Calculate TCP checksum
    *(uint16_t*)(packet + 36) = calculateTransportChecksum(srcIp, dstIp, 6, packet + 20, 20 + dataLen);

    // Write to TUN
    ssize_t written = write(tunFd, packet, totalLen);
    if (written < 0) {
        LOGE("Failed to write TCP response to TUN: %s", strerror(errno));
    } else {
        LOGD("Wrote %zd bytes TCP response to TUN", written);
    }

    delete[] packet;
}

void PacketForwarder::cleanupConnections() {
    std::lock_guard<std::mutex> lock(m_connectionsMutex);
    
    auto now = std::chrono::steady_clock::now();
    auto tcpTimeout = std::chrono::seconds(30);  // 30 second timeout
    auto udpTimeout = std::chrono::seconds(30);  // 30 second timeout
    
    // Clean up stale TCP connections
    auto tcpIt = m_tcpConnections.begin();
    while (tcpIt != m_tcpConnections.end()) {
        auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - tcpIt->lastActivity);
        if (elapsed > tcpTimeout || !tcpIt->connected) {
            if (tcpIt->socketFd >= 0) {
                close(tcpIt->socketFd);
                LOGD("Closed stale TCP connection: %s:%d -> %s:%d", 
                     inet_ntoa(*(struct in_addr*)&tcpIt->srcIp), tcpIt->srcPort,
                     inet_ntoa(*(struct in_addr*)&tcpIt->dstIp), tcpIt->dstPort);
            }
            tcpIt = m_tcpConnections.erase(tcpIt);
        } else {
            ++tcpIt;
        }
    }
    
    // Clean up stale UDP sessions
    auto udpIt = m_udpSessions.begin();
    while (udpIt != m_udpSessions.end()) {
        auto elapsed = std::chrono::duration_cast<std::chrono::seconds>(now - udpIt->lastActivity);
        if (elapsed > udpTimeout) {
            if (udpIt->socketFd >= 0) {
                close(udpIt->socketFd);
                LOGD("Closed stale UDP session: %s:%d -> %s:%d", 
                     inet_ntoa(*(struct in_addr*)&udpIt->srcIp), udpIt->srcPort,
                     inet_ntoa(*(struct in_addr*)&udpIt->dstIp), udpIt->dstPort);
            }
            udpIt = m_udpSessions.erase(udpIt);
        } else {
            ++udpIt;
        }
    }
    
    // Clear connection maps
    m_tcpConnMap.clear();
    m_udpSessionMap.clear();
    
    // Rebuild connection maps for faster lookup
    for (size_t i = 0; i < m_tcpConnections.size(); i++) {
        ConnKey key = {
            m_tcpConnections[i].srcIp,
            m_tcpConnections[i].dstIp,
            m_tcpConnections[i].srcPort,
            m_tcpConnections[i].dstPort
        };
        m_tcpConnMap[key] = i;
    }
    
    for (size_t i = 0; i < m_udpSessions.size(); i++) {
        ConnKey key = {
            m_udpSessions[i].srcIp,
            m_udpSessions[i].dstIp,
            m_udpSessions[i].srcPort,
            m_udpSessions[i].dstPort
        };
        m_udpSessionMap[key] = i;
    }
}

// TCP response handler loop
void PacketForwarder::tcpResponseLoop() {
    LOGI("TCP response loop started");
    
    while (m_running.load()) {
        std::vector<TcpConnection*> activeConns;
        
        // Build list of active connections
        {
            std::lock_guard<std::mutex> lock(m_connectionsMutex);
            for (auto& conn : m_tcpConnections) {
                if (conn.connected && conn.socketFd >= 0) {
                    activeConns.push_back(&conn);
                }
            }
        }
        
        if (activeConns.empty()) {
            usleep(10000); // 10ms
            continue;
        }
        
        // Poll all TCP sockets for responses
        std::vector<struct pollfd> fds;
        for (auto* conn : activeConns) {
            struct pollfd pfd;
            pfd.fd = conn->socketFd;
            pfd.events = POLLIN;
            pfd.revents = 0;
            fds.push_back(pfd);
        }
        
        int ready = poll(fds.data(), fds.size(), 10); // 10ms timeout
        if (ready <= 0) {
            continue;
        }
        
        // Process responses
        for (size_t i = 0; i < fds.size(); i++) {
            if (fds[i].revents & POLLIN) {
                TcpConnection* conn = activeConns[i];
                uint8_t responseBuffer[8192];
                ssize_t received = recv(conn->socketFd, responseBuffer, sizeof(responseBuffer), MSG_DONTWAIT);
                
                if (received > 0) {
                    LOGD("⬅️ TCP recv %zd bytes from %s:%d",
                         received,
                         inet_ntoa(*(struct in_addr*)&conn->dstIp),
                         conn->dstPort);
                    
                    // Write response back to TUN only if legacy synthesis is enabled (lab mode)
                    if (g_enableLegacyTcpResponseSynthesis) {
                        writeTcpResponseToTun(m_tunFd, conn->dstIp, conn->srcIp, 
                                             conn->dstPort, conn->srcPort,
                                             responseBuffer, received);
                    }
                    
                    std::lock_guard<std::mutex> statsLock(m_statsMutex);
                    m_stats.packetsForwarded++;
                    m_stats.bytesForwarded += received;
                } else if (received == 0) {
                    LOGI("❌ TCP connection closed by peer: %s:%d",
                         inet_ntoa(*(struct in_addr*)&conn->dstIp),
                         conn->dstPort);
                    conn->connected = false;
                } else if (errno != EAGAIN && errno != EWOULDBLOCK) {
                    LOGE("❌ TCP recv error: %s", strerror(errno));
                    conn->connected = false;
                }
            }
        }
    }
    
    LOGI("TCP response loop ended");
}

// UDP response handler loop
void PacketForwarder::udpResponseLoop() {
    LOGI("UDP response loop started");
    
    while (m_running.load()) {
        std::vector<UdpSession*> activeSessions;
        
        // Build list of active sessions
        {
            std::lock_guard<std::mutex> lock(m_connectionsMutex);
            for (auto& session : m_udpSessions) {
                if (session.socketFd >= 0) {
                    activeSessions.push_back(&session);
                }
            }
        }
        
        if (activeSessions.empty()) {
            usleep(10000); // 10ms
            continue;
        }
        
        // Poll all UDP sockets for responses
        std::vector<struct pollfd> fds;
        for (auto* session : activeSessions) {
            struct pollfd pfd;
            pfd.fd = session->socketFd;
            pfd.events = POLLIN;
            pfd.revents = 0;
            fds.push_back(pfd);
        }
        
        int ready = poll(fds.data(), fds.size(), 10); // 10ms timeout
        if (ready <= 0) {
            continue;
        }
        
        // Process responses
        for (size_t i = 0; i < fds.size(); i++) {
            if (fds[i].revents & POLLIN) {
                UdpSession* session = activeSessions[i];
                uint8_t responseBuffer[4096];
                struct sockaddr_in serverAddr;
                socklen_t addrLen = sizeof(serverAddr);
                
                ssize_t received = recvfrom(session->socketFd, responseBuffer, 
                                           sizeof(responseBuffer), MSG_DONTWAIT,
                                           (struct sockaddr*)&serverAddr, &addrLen);
                
                if (received > 0) {
                    LOGD("⬅️ UDP recv %zd bytes from %s:%d",
                         received,
                         inet_ntoa(*(struct in_addr*)&session->dstIp),
                         session->dstPort);
                    
                    // Write response back to TUN
                    writeUdpResponseToTun(m_tunFd, session->dstIp, session->srcIp,
                                         session->dstPort, session->srcPort,
                                         responseBuffer, received);
                    
                    std::lock_guard<std::mutex> statsLock(m_statsMutex);
                    m_stats.packetsForwarded++;
                    m_stats.bytesForwarded += received;
                } else if (received < 0 && errno != EAGAIN && errno != EWOULDBLOCK) {
                    LOGE("❌ UDP recvfrom error: %s", strerror(errno));
                }
            }
        }
    }
    
    LOGI("UDP response loop ended");
}

// Stats monitor loop - logs stats every 5 seconds
void PacketForwarder::statsMonitorLoop() {
    LOGI("Stats monitor loop started");
    
    while (m_running.load()) {
        std::this_thread::sleep_for(std::chrono::seconds(5));
        
        // Get and log stats
        Stats currentStats;
        {
            std::lock_guard<std::mutex> lock(m_statsMutex);
            currentStats = m_stats;
        }
        
        // Format bytes for human readable output
        auto formatBytes = [](uint64_t bytes) -> std::string {
            const char* units[] = {"B", "KB", "MB", "GB"};
            int unitIndex = 0;
            double value = static_cast<double>(bytes);
            
            while (value >= 1024.0 && unitIndex < 3) {
                value /= 1024.0;
                unitIndex++;
            }
            
            char buffer[32];
            if (unitIndex == 0) {
                snprintf(buffer, sizeof(buffer), "%.0f %s", value, units[unitIndex]);
            } else {
                snprintf(buffer, sizeof(buffer), "%.2f %s", value, units[unitIndex]);
            }
            return std::string(buffer);
        };
        
        LOGI("✅ FORWARDER ALIVE ✅ | IN: %lu pkts (%s) | OUT: %lu pkts (%s) | TCP: %lu conns | UDP: %lu sessions | DNS: %lu queries | Errors: %lu",
             currentStats.packetsRead,
             formatBytes(currentStats.bytesRead).c_str(),
             currentStats.packetsForwarded,
             formatBytes(currentStats.bytesForwarded).c_str(),
             currentStats.tcpConnections,
             currentStats.udpSessions,
             currentStats.dnsQueries,
             currentStats.errors);
        
        // Periodic cleanup of stale connections (every 30 seconds)
        static int cleanupCounter = 0;
        cleanupCounter++;
        if (cleanupCounter >= 6) { // 6 * 5 seconds = 30 seconds
            cleanupConnections();
            cleanupCounter = 0;
        }
    }
    
    LOGI("Stats monitor loop ended");
}

// Pause/resume session for interception
bool PacketForwarder::pauseSession(const std::string& sessionId) {
    std::lock_guard<std::mutex> lock(m_pausedSessionsMutex);
    
    // Check if session already paused
    if (m_pausedSessions.find(sessionId) != m_pausedSessions.end()) {
        LOGW("Session %s already paused", sessionId.c_str());
        return false;
    }
    
    // Create paused session
    PausedSession session;
    session.sessionId = sessionId;
    session.pauseTime = std::chrono::steady_clock::now();
    m_pausedSessions[sessionId] = session;
    
    LOGI("⏸️ Paused session: %s", sessionId.c_str());
    return true;
}

bool PacketForwarder::resumeSession(const std::string& sessionId, 
                                     const uint8_t* modifiedPayload, 
                                     size_t payloadSize) {
    std::lock_guard<std::mutex> lock(m_pausedSessionsMutex);
    
    auto it = m_pausedSessions.find(sessionId);
    if (it == m_pausedSessions.end()) {
        LOGW("Session %s not found in paused sessions", sessionId.c_str());
        return false;
    }
    
    PausedSession& session = it->second;
    
    // If modified payload provided, use it; otherwise use queued packets
    if (modifiedPayload != nullptr && payloadSize > 0) {
        LOGI("▶️ Resuming session %s with modified payload (%zu bytes)", 
             sessionId.c_str(), payloadSize);
        // Forward modified payload
        // Note: In full implementation, would reconstruct IP packet with modified payload
        // For now, just log
    } else if (!session.queuedPackets.empty()) {
        LOGI("▶️ Resuming session %s with %zu queued packets", 
             sessionId.c_str(), session.queuedPackets.size());
        // Forward queued packets
        // Note: Would process queued packets here
    } else {
        LOGI("▶️ Resuming session %s (no queued packets)", sessionId.c_str());
    }
    
    // Remove from paused sessions
    m_pausedSessions.erase(it);
    return true;
}

