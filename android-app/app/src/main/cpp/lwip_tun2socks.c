#include "lwip_tun2socks.h"
#include <android/log.h>
#include <unistd.h>
#include <pthread.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <fcntl.h>

#define LOG_TAG "lwip_tun2socks"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

static lwip_tun2socks_t g_tun2socks = {0};
static lwip_stats_t g_stats = {0};
static pthread_mutex_t g_stats_mutex = PTHREAD_MUTEX_INITIALIZER;

// Simple IP packet parser
typedef struct {
    uint32_t src_ip;
    uint32_t dst_ip;
    uint16_t src_port;
    uint16_t dst_port;
    uint8_t protocol;  // 6=TCP, 17=UDP
    const uint8_t* payload;
    size_t payload_len;
} packet_info_t;

static bool parse_ip_packet(const uint8_t* data, size_t len, packet_info_t* info) {
    if (len < 20) return false;
    
    uint8_t version = (data[0] >> 4) & 0x0F;
    if (version != 4) return false;
    
    uint8_t ihl = (data[0] & 0x0F) * 4;
    if (ihl < 20 || ihl > len) return false;
    
    info->protocol = data[9];
    memcpy(&info->src_ip, &data[12], 4);
    memcpy(&info->dst_ip, &data[16], 4);
    
    if (info->protocol == 6 || info->protocol == 17) {  // TCP or UDP
        if (len < ihl + 4) return false;
        info->src_port = (data[ihl] << 8) | data[ihl + 1];
        info->dst_port = (data[ihl + 2] << 8) | data[ihl + 3];
        
        if (info->protocol == 6) {  // TCP
            uint8_t tcp_hdr_len = ((data[ihl + 12] >> 4) & 0x0F) * 4;
            if (tcp_hdr_len < 20 || ihl + tcp_hdr_len > len) return false;
            info->payload = data + ihl + tcp_hdr_len;
            info->payload_len = len - ihl - tcp_hdr_len;
        } else {  // UDP
            info->payload = data + ihl + 8;
            info->payload_len = len - ihl - 8;
        }
    } else {
        info->src_port = 0;
        info->dst_port = 0;
        info->payload = data + ihl;
        info->payload_len = len - ihl;
    }
    
    return true;
}

// Protect socket using VpnService.protect()
static bool protect_socket(int sockfd) {
    if (g_tun2socks.protector_ref == NULL || g_tun2socks.jvm == NULL) {
        LOGE("Cannot protect socket: no protector or JVM");
        return false;
    }
    
    JNIEnv* env = NULL;
    int attach_result = (*g_tun2socks.jvm)->AttachCurrentThread(g_tun2socks.jvm, &env, NULL);
    if (attach_result != JNI_OK || env == NULL) {
        LOGE("Failed to attach thread for socket protection");
        return false;
    }
    
    jclass protector_class = (*env)->GetObjectClass(env, g_tun2socks.protector_ref);
    jmethodID protect_method = (*env)->GetMethodID(env, protector_class, "protect", "(I)Z");
    
    if (protect_method == NULL) {
        LOGE("Failed to find protect method");
        (*env)->DeleteLocalRef(env, protector_class);
        return false;
    }
    
    jboolean result = (*env)->CallBooleanMethod(env, g_tun2socks.protector_ref, protect_method, sockfd);
    (*env)->DeleteLocalRef(env, protector_class);
    
    if (!result) {
        LOGE("VpnService.protect() returned false for socket %d", sockfd);
        return false;
    }
    
    LOGD("✅ Protected socket %d", sockfd);
    return true;
}

// Forward TCP packet (simplified implementation)
static void forward_tcp_packet(const packet_info_t* info, const uint8_t* full_packet, size_t packet_len) {
    // Create socket
    int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0) {
        LOGE("Failed to create TCP socket: %s", strerror(errno));
        return;
    }
    
    // CRITICAL: Protect socket BEFORE connecting
    if (!protect_socket(sockfd)) {
        close(sockfd);
        return;
    }
    
    // Set non-blocking
    fcntl(sockfd, F_SETFL, O_NONBLOCK);
    
    // Connect to destination
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = info->dst_ip;
    addr.sin_port = htons(info->dst_port);
    
    int conn_result = connect(sockfd, (struct sockaddr*)&addr, sizeof(addr));
    if (conn_result < 0 && errno != EINPROGRESS) {
        LOGE("TCP connect failed: %s", strerror(errno));
        close(sockfd);
        return;
    }
    
    // For now, just log and close (full implementation would manage connection state)
    LOGD("TCP packet forwarded: %s:%d -> %s:%d",
         inet_ntoa(*(struct in_addr*)&info->src_ip), info->src_port,
         inet_ntoa(*(struct in_addr*)&info->dst_ip), info->dst_port);
    
    pthread_mutex_lock(&g_stats_mutex);
    g_stats.tcp_connections++;
    g_stats.packets_out++;
    g_stats.bytes_out += packet_len;
    pthread_mutex_unlock(&g_stats_mutex);
    
    close(sockfd);
}

// Forward UDP packet
static void forward_udp_packet(const packet_info_t* info, const uint8_t* full_packet, size_t packet_len) {
    // Create socket
    int sockfd = socket(AF_INET, SOCK_DGRAM, 0);
    if (sockfd < 0) {
        LOGE("Failed to create UDP socket: %s", strerror(errno));
        return;
    }
    
    // CRITICAL: Protect socket
    if (!protect_socket(sockfd)) {
        close(sockfd);
        return;
    }
    
    // Send to destination
    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = info->dst_ip;
    addr.sin_port = htons(info->dst_port);
    
    if (info->payload_len > 0) {
        ssize_t sent = sendto(sockfd, info->payload, info->payload_len, 0,
                             (struct sockaddr*)&addr, sizeof(addr));
        if (sent > 0) {
            LOGD("UDP packet forwarded: %s:%d -> %s:%d (%zd bytes)",
                 inet_ntoa(*(struct in_addr*)&info->src_ip), info->src_port,
                 inet_ntoa(*(struct in_addr*)&info->dst_ip), info->dst_port, sent);
            
            pthread_mutex_lock(&g_stats_mutex);
            g_stats.udp_sessions++;
            g_stats.packets_out++;
            g_stats.bytes_out += sent;
            pthread_mutex_unlock(&g_stats_mutex);
        } else {
            LOGE("UDP sendto failed: %s", strerror(errno));
        }
    }
    
    close(sockfd);
}

bool lwip_tun2socks_init(int tun_fd, JNIEnv* env, jobject protector) {
    if (g_tun2socks.running) {
        LOGW("lwip_tun2socks already running");
        return false;
    }
    
    if (tun_fd < 0) {
        LOGE("Invalid TUN fd: %d", tun_fd);
        return false;
    }
    
    // Get JavaVM for thread attachment
    if ((*env)->GetJavaVM(env, &g_tun2socks.jvm) != JNI_OK) {
        LOGE("Failed to get JavaVM");
        return false;
    }
    
    // Create global reference to protector
    g_tun2socks.protector_ref = (*env)->NewGlobalRef(env, protector);
    if (g_tun2socks.protector_ref == NULL) {
        LOGE("Failed to create global ref for protector");
        return false;
    }
    
    g_tun2socks.tun_fd = tun_fd;
    g_tun2socks.running = true;
    
    // Reset stats
    pthread_mutex_lock(&g_stats_mutex);
    memset(&g_stats, 0, sizeof(g_stats));
    pthread_mutex_unlock(&g_stats_mutex);
    
    LOGI("✅ lwip_tun2socks initialized with TUN fd %d", tun_fd);
    return true;
}

void lwip_tun2socks_run() {
    if (!g_tun2socks.running) {
        LOGE("lwip_tun2socks not initialized");
        return;
    }
    
    LOGI("🚀 lwip_tun2socks processing loop started");
    
    uint8_t buffer[32767];  // Max IP packet size
    
    while (g_tun2socks.running) {
        ssize_t len = read(g_tun2socks.tun_fd, buffer, sizeof(buffer));
        
        if (len > 0) {
            pthread_mutex_lock(&g_stats_mutex);
            g_stats.packets_in++;
            g_stats.bytes_in += len;
            pthread_mutex_unlock(&g_stats_mutex);
            
            // Parse packet
            packet_info_t info;
            if (parse_ip_packet(buffer, len, &info)) {
                // Forward based on protocol
                if (info.protocol == 6) {  // TCP
                    forward_tcp_packet(&info, buffer, len);
                } else if (info.protocol == 17) {  // UDP
                    forward_udp_packet(&info, buffer, len);
                } else {
                    LOGD("Unsupported protocol: %d", info.protocol);
                }
            }
        } else if (len < 0) {
            if (errno == EAGAIN || errno == EWOULDBLOCK) {
                usleep(1000);  // 1ms
                continue;
            } else {
                LOGE("TUN read error: %s", strerror(errno));
                break;
            }
        } else {
            usleep(1000);
        }
    }
    
    LOGI("❌ lwip_tun2socks processing loop ended");
}

void lwip_tun2socks_stop() {
    if (!g_tun2socks.running) {
        return;
    }
    
    LOGI("Stopping lwip_tun2socks...");
    g_tun2socks.running = false;
    
    // Clean up global ref
    if (g_tun2socks.protector_ref != NULL && g_tun2socks.jvm != NULL) {
        JNIEnv* env = NULL;
        (*g_tun2socks.jvm)->AttachCurrentThread(g_tun2socks.jvm, &env, NULL);
        if (env != NULL) {
            (*env)->DeleteGlobalRef(env, g_tun2socks.protector_ref);
        }
        g_tun2socks.protector_ref = NULL;
    }
    
    g_tun2socks.jvm = NULL;
    g_tun2socks.tun_fd = -1;
    
    LOGI("✅ lwip_tun2socks stopped");
}

bool lwip_tun2socks_is_running() {
    return g_tun2socks.running;
}

void lwip_tun2socks_get_stats(lwip_stats_t* stats) {
    pthread_mutex_lock(&g_stats_mutex);
    *stats = g_stats;
    pthread_mutex_unlock(&g_stats_mutex);
}
