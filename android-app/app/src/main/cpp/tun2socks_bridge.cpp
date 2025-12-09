#include "tun2socks_bridge.h"
#include "lwip_tun2socks.h"
#include <android/log.h>
#include <pthread.h>

#define LOG_TAG "tun2socks_bridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static pthread_t g_worker_thread = 0;

static void* tun2socks_worker(void* arg) {
    LOGI("🚀 tun2socks worker thread started");
    lwip_tun2socks_run();  // Blocks until stopped
    LOGI("❌ tun2socks worker thread ended");
    return NULL;
}

bool start_tun2socks(int tun_fd, JNIEnv* env, jobject protector) {
    if (lwip_tun2socks_is_running()) {
        LOGW("tun2socks already running");
        return true;
    }
    
    // Initialize lwIP tun2socks
    if (!lwip_tun2socks_init(tun_fd, env, protector)) {
        LOGE("Failed to initialize lwip_tun2socks");
        return false;
    }
    
    // Start worker thread
    int result = pthread_create(&g_worker_thread, NULL, tun2socks_worker, NULL);
    if (result != 0) {
        LOGE("Failed to create tun2socks worker thread: %d", result);
        lwip_tun2socks_stop();
        return false;
    }
    
    LOGI("✅ tun2socks started successfully (DIRECT mode, no SOCKS)");
    return true;
}

void stop_tun2socks() {
    if (!lwip_tun2socks_is_running()) {
        return;
    }
    
    LOGI("Stopping tun2socks...");
    lwip_tun2socks_stop();
    
    if (g_worker_thread != 0) {
        pthread_join(g_worker_thread, NULL);
        g_worker_thread = 0;
    }
    
    LOGI("✅ tun2socks stopped");
}

bool tun2socks_is_running() {
    return lwip_tun2socks_is_running();
}
