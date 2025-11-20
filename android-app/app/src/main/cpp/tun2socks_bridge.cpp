#include "tun2socks_bridge.h"
#include <android/log.h>
#include <atomic>
#include <thread>

#define LOG_TAG "tun2socks"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static std::atomic<bool> g_running{false};
static std::thread g_worker;

bool start_tun2socks(int tun_fd, JNIEnv* /*env*/, jobject /*protector*/) {
    if (g_running.load()) {
        LOGW("start_tun2socks called but already running");
        return true;
    }
    // Placeholder implementation: not a real tun2socks.
    // We only mark running and spawn a no-op thread so that JNI plumbing works.
    g_running.store(true);
    g_worker = std::thread([](){
        LOGI("tun2socks placeholder thread started (no real forwarding)");
        while (g_running.load()) {
            std::this_thread::sleep_for(std::chrono::milliseconds(200));
        }
        LOGI("tun2socks placeholder thread stopped");
    });
    LOGW("tun2socks placeholder started; no real traffic forwarding is performed");
    return true;
}

void stop_tun2socks() {
    if (!g_running.load()) return;
    g_running.store(false);
    if (g_worker.joinable()) g_worker.join();
}

bool tun2socks_is_running() {
    return g_running.load();
}
