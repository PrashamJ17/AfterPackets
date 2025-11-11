#ifndef PACKET_FORWARDER_H
#define PACKET_FORWARDER_H

#include <cstdint>
#include <functional>
#include <thread>
#include <atomic>
#include <mutex>
#include <queue>
#include <vector>
#include <map>
#include <chrono>
#include <condition_variable>

// Forward declaration
struct ForwarderContext;

/**
 * Native packet forwarder - forwards TUN packets to real network
 * Similar to tun2socks behavior
 */
class PacketForwarder {
public:
    PacketForwarder();
    ~PacketForwarder();
    
    /**
     * Start the forwarder with TUN file descriptor
     * @param tunFd TUN file descriptor
     * @param protectCallback Callback to protect socket from VPN routing
     * @param packetCallback Callback to process captured packets (optional, can be nullptr)
     * @return true if started successfully
     */
    bool start(int tunFd, 
               std::function<bool(int)> protectCallback,
               std::function<void(const uint8_t*, size_t)> packetCallback = nullptr);
    
    /**
     * Stop the forwarder
     */
    void stop();
    
    /**
     * Check if forwarder is running
     */
    bool isRunning() const;
    
    /**
     * Pause forwarding for a session (for interception)
     * @param sessionId Unique session identifier
     * @return true if session was paused
     */
    bool pauseSession(const std::string& sessionId);
    
    /**
     * Resume forwarding for a session
     * @param sessionId Unique session identifier
     * @param modifiedPayload Optional modified payload (nullptr to use original)
     * @param payloadSize Size of modified payload (0 to use original)
     * @return true if session was resumed
     */
    bool resumeSession(const std::string& sessionId, 
                       const uint8_t* modifiedPayload = nullptr, 
                       size_t payloadSize = 0);
    
    /**
     * Get statistics
     */
    struct Stats {
        uint64_t packetsRead = 0;
        uint64_t packetsForwarded = 0;
        uint64_t bytesRead = 0;
        uint64_t bytesForwarded = 0;
        uint64_t tcpConnections = 0;
        uint64_t udpSessions = 0;
        uint64_t dnsQueries = 0;
        uint64_t errors = 0;
    };
    
    Stats getStats() const;
    
private:
    void tunReadLoop();
    void processPacket(const uint8_t* data, size_t len);
    void handleTcpPacket(const uint8_t* ipHeader, size_t ipLen);
    void handleUdpPacket(const uint8_t* ipHeader, size_t ipLen);
    void handleDnsPacket(const uint8_t* udpData, size_t udpLen, 
                        uint32_t srcIp, uint32_t dstIp, 
                        uint16_t srcPort, uint16_t dstPort);
    void tcpResponseLoop();
    void udpResponseLoop();
    void statsMonitorLoop();
    
    int m_tunFd = -1;
    std::atomic<bool> m_running{false};
    std::thread m_tunThread;
    std::thread m_tcpResponseThread;
    std::thread m_udpResponseThread;
    std::thread m_statsMonitorThread;
    std::function<bool(int)> m_protectCallback;
    std::function<void(const uint8_t*, size_t)> m_packetCallback;
    
    mutable std::mutex m_statsMutex;
    Stats m_stats;
    std::chrono::steady_clock::time_point m_lastStatsLog;
    
    // Connection tracking
    struct TcpConnection {
        uint32_t srcIp;
        uint32_t dstIp;
        uint16_t srcPort;
        uint16_t dstPort;
        int socketFd = -1;
        bool connected = false;
        uint32_t seqNum = 0;
        uint32_t ackNum = 0;
        std::chrono::steady_clock::time_point lastActivity;
    };
    
    struct UdpSession {
        uint32_t srcIp;
        uint32_t dstIp;
        uint16_t srcPort;
        uint16_t dstPort;
        int socketFd = -1;
        std::chrono::steady_clock::time_point lastActivity;
    };
    
    // Session pause/resume for interception
    struct PausedSession {
        std::string sessionId;
        std::vector<uint8_t> queuedPackets;
        std::chrono::steady_clock::time_point pauseTime;
        static constexpr size_t MAX_QUEUE_SIZE = 100; // Max queued packets
    };
    
    std::mutex m_pausedSessionsMutex;
    std::map<std::string, PausedSession> m_pausedSessions;
    static constexpr auto SESSION_TIMEOUT = std::chrono::seconds(30);
    
    std::mutex m_connectionsMutex;
    std::vector<TcpConnection> m_tcpConnections;
    std::vector<UdpSession> m_udpSessions;
    
    // Connection lookup maps for faster access
    struct ConnKey {
        uint32_t srcIp;
        uint32_t dstIp;
        uint16_t srcPort;
        uint16_t dstPort;
        
        bool operator<(const ConnKey& other) const {
            if (srcIp != other.srcIp) return srcIp < other.srcIp;
            if (dstIp != other.dstIp) return dstIp < other.dstIp;
            if (srcPort != other.srcPort) return srcPort < other.srcPort;
            return dstPort < other.dstPort;
        }
    };
    
    std::map<ConnKey, size_t> m_tcpConnMap;  // Maps to index in m_tcpConnections
    std::map<ConnKey, size_t> m_udpSessionMap;  // Maps to index in m_udpSessions
    
    // Helper functions
    uint16_t parseIpHeader(const uint8_t* data, size_t len, 
                          uint32_t* srcIp, uint32_t* dstIp, 
                          uint8_t* protocol);
    void writeToTun(const uint8_t* data, size_t len);
    int createTcpSocket(uint32_t dstIp, uint16_t dstPort);
    int createUdpSocket();
    bool connectTcpSocket(int fd, uint32_t dstIp, uint16_t dstPort);
    bool sendUdpPacket(int fd, const uint8_t* data, size_t len, 
                      uint32_t dstIp, uint16_t dstPort);
    void cleanupConnections();
};

#endif // PACKET_FORWARDER_H

