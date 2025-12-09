#ifndef LWIP_TUN2SOCKS_H
#define LWIP_TUN2SOCKS_H

#include <jni.h>
#include <stdbool.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Complete lwIP-based tun2socks implementation for Android VPN
 * This is a DIRECT forwarder (no SOCKS proxy) like PCAPdroid
 */

typedef struct {
    int tun_fd;
    bool running;
    JavaVM* jvm;
    jobject protector_ref;  // Global ref to VpnService for socket protection
} lwip_tun2socks_t;

/**
 * Initialize lwIP tun2socks engine
 * @param tun_fd The TUN interface file descriptor from VpnService
 * @param env JNI environment
 * @param protector Java object with protect(int fd) method
 * @return true on success
 */
bool lwip_tun2socks_init(int tun_fd, JNIEnv* env, jobject protector);

/**
 * Start the tun2socks processing loop (call from dedicated thread)
 * This will block until stopped
 */
void lwip_tun2socks_run();

/**
 * Stop the tun2socks engine
 */
void lwip_tun2socks_stop();

/**
 * Check if running
 */
bool lwip_tun2socks_is_running();

/**
 * Get statistics
 */
typedef struct {
    uint64_t packets_in;
    uint64_t packets_out;
    uint64_t bytes_in;
    uint64_t bytes_out;
    uint32_t tcp_connections;
    uint32_t udp_sessions;
} lwip_stats_t;

void lwip_tun2socks_get_stats(lwip_stats_t* stats);

#ifdef __cplusplus
}
#endif

#endif // LWIP_TUN2SOCKS_H
