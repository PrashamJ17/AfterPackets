#ifndef TUN2SOCKS_BRIDGE_H
#define TUN2SOCKS_BRIDGE_H

#include <jni.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// Starts tun2socks engine bound to the provided TUN file descriptor.
// protector is a Java object exposing a method boolean protect(int fd) to bypass VPN routing.
// Returns true on success.
bool start_tun2socks(int tun_fd, JNIEnv* env, jobject protector);

// Stops the tun2socks engine and releases resources.
void stop_tun2socks();

// Returns whether tun2socks engine is running.
bool tun2socks_is_running();

#ifdef __cplusplus
}
#endif

#endif // TUN2SOCKS_BRIDGE_H
