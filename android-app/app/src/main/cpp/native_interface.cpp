#include <jni.h>
#include <string>
#include <vector>
#include "packet_parser.h"
#include "packet_forwarder.h"
#include <android/log.h>

#define LOG_TAG "NativeInterface"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static PacketParser* parser = nullptr;
static PacketForwarder* forwarder = nullptr;
static JavaVM* g_jvm = nullptr;
static jobject g_protectCallback = nullptr;
static jmethodID g_protectMethod = nullptr;
static jobject g_packetCallback = nullptr;
static jmethodID g_packetProcessMethod = nullptr;

extern "C" {

JNIEXPORT void JNICALL
Java_com_packethunter_mobile_capture_NativePacketParser_initParser(JNIEnv* env, jobject /* this */) {
    if (parser == nullptr) {
        parser = new PacketParser();
        LOGD("Native parser initialized");
    }
}

JNIEXPORT void JNICALL
Java_com_packethunter_mobile_capture_NativePacketParser_destroyParser(JNIEnv* env, jobject /* this */) {
    if (parser != nullptr) {
        delete parser;
        parser = nullptr;
        LOGD("Native parser destroyed");
    }
}

JNIEXPORT jobject JNICALL
Java_com_packethunter_mobile_capture_NativePacketParser_parsePacket(
    JNIEnv* env,
    jobject /* this */,
    jbyteArray data
) {
    if (parser == nullptr) {
        LOGD("Parser not initialized, initializing now");
        parser = new PacketParser();
    }
    
    jsize len = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    
    if (bytes == nullptr) {
        LOGD("Failed to get byte array");
        return nullptr;
    }
    
    // Parse the packet
    ParsedPacket result = parser->parsePacket(
        reinterpret_cast<const uint8_t*>(bytes),
        static_cast<size_t>(len)
    );
    
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    
    // Find the ParsedPacketData class
    jclass packetClass = env->FindClass("com/packethunter/mobile/capture/ParsedPacketData");
    if (packetClass == nullptr) {
        LOGD("Could not find ParsedPacketData class");
        return nullptr;
    }
    
    // Get constructor
    jmethodID constructor = env->GetMethodID(packetClass, "<init>", "()V");
    if (constructor == nullptr) {
        LOGD("Could not find constructor");
        return nullptr;
    }
    
    // Create new instance
    jobject packetObj = env->NewObject(packetClass, constructor);
    if (packetObj == nullptr) {
        LOGD("Could not create object");
        return nullptr;
    }
    
    // Set fields
    jfieldID protocolField = env->GetFieldID(packetClass, "protocol", "Ljava/lang/String;");
    jfieldID sourceIpField = env->GetFieldID(packetClass, "sourceIp", "Ljava/lang/String;");
    jfieldID destIpField = env->GetFieldID(packetClass, "destIp", "Ljava/lang/String;");
    jfieldID sourcePortField = env->GetFieldID(packetClass, "sourcePort", "I");
    jfieldID destPortField = env->GetFieldID(packetClass, "destPort", "I");
    jfieldID lengthField = env->GetFieldID(packetClass, "length", "I");
    jfieldID flagsField = env->GetFieldID(packetClass, "flags", "Ljava/lang/String;");
    jfieldID payloadField = env->GetFieldID(packetClass, "payload", "[B");
    jfieldID payloadPreviewField = env->GetFieldID(packetClass, "payloadPreview", "Ljava/lang/String;");
    jfieldID httpMethodField = env->GetFieldID(packetClass, "httpMethod", "Ljava/lang/String;");
    jfieldID httpUrlField = env->GetFieldID(packetClass, "httpUrl", "Ljava/lang/String;");
    jfieldID dnsQueryField = env->GetFieldID(packetClass, "dnsQuery", "Ljava/lang/String;");
    jfieldID tlsSniField = env->GetFieldID(packetClass, "tlsSni", "Ljava/lang/String;");
    
    // Set string fields
    env->SetObjectField(packetObj, protocolField, env->NewStringUTF(result.protocol.c_str()));
    env->SetObjectField(packetObj, sourceIpField, env->NewStringUTF(result.source_ip.c_str()));
    env->SetObjectField(packetObj, destIpField, env->NewStringUTF(result.dest_ip.c_str()));
    env->SetIntField(packetObj, sourcePortField, result.source_port);
    env->SetIntField(packetObj, destPortField, result.dest_port);
    env->SetIntField(packetObj, lengthField, result.length);
    env->SetObjectField(packetObj, flagsField, env->NewStringUTF(result.flags.c_str()));
    env->SetObjectField(packetObj, payloadPreviewField, env->NewStringUTF(result.payload_preview.c_str()));
    
    // Set optional fields
    if (!result.http_method.empty()) {
        env->SetObjectField(packetObj, httpMethodField, env->NewStringUTF(result.http_method.c_str()));
    }
    if (!result.http_url.empty()) {
        env->SetObjectField(packetObj, httpUrlField, env->NewStringUTF(result.http_url.c_str()));
    }
    if (!result.dns_query.empty()) {
        env->SetObjectField(packetObj, dnsQueryField, env->NewStringUTF(result.dns_query.c_str()));
    }
    if (!result.tls_sni.empty()) {
        env->SetObjectField(packetObj, tlsSniField, env->NewStringUTF(result.tls_sni.c_str()));
    }
    
    // Set payload byte array
    if (!result.payload.empty()) {
        jbyteArray payloadArray = env->NewByteArray(result.payload.size());
        env->SetByteArrayRegion(payloadArray, 0, result.payload.size(), 
                               reinterpret_cast<const jbyte*>(result.payload.data()));
        env->SetObjectField(packetObj, payloadField, payloadArray);
    }
    
    return packetObj;
}

// Forwarder JNI methods
JNIEXPORT jboolean JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_startForwarderNative(
    JNIEnv* env,
    jobject /* this */,
    jint tunFd,
    jobject protectCallback,
    jobject packetCallback
) {
    if (forwarder != nullptr) {
        LOGE("Forwarder already exists");
        return JNI_FALSE;
    }
    
    if (tunFd < 0) {
        LOGE("Invalid TUN file descriptor: %d", tunFd);
        return JNI_FALSE;
    }
    
    // Store JVM and callback for protect() calls
    env->GetJavaVM(&g_jvm);
    g_protectCallback = env->NewGlobalRef(protectCallback);
    g_packetCallback = env->NewGlobalRef(packetCallback);
    
    // Get protect method
    jclass protectClass = env->GetObjectClass(protectCallback);
    g_protectMethod = env->GetMethodID(protectClass, "protect", "(I)Z");
    if (g_protectMethod == nullptr) {
        LOGE("Could not find protect() method");
        env->DeleteGlobalRef(g_protectCallback);
        env->DeleteGlobalRef(g_packetCallback);
        g_protectCallback = nullptr;
        g_packetCallback = nullptr;
        return JNI_FALSE;
    }
    
    // Get packet callback method
    jclass packetClass = env->GetObjectClass(packetCallback);
    g_packetProcessMethod = env->GetMethodID(packetClass, "onPacket", "([B)V");
    if (g_packetProcessMethod == nullptr) {
        LOGE("Could not find onPacket() method");
        env->DeleteGlobalRef(g_protectCallback);
        env->DeleteGlobalRef(g_packetCallback);
        g_protectCallback = nullptr;
        g_packetCallback = nullptr;
        return JNI_FALSE;
    }
    
    LOGI("JNI callbacks initialized successfully");
    
    // Create forwarder
    forwarder = new PacketForwarder();
    
    // Protect callback wrapper - calls VpnSocketProtector.protectFd()
    auto protectWrapper = [](int fd) -> bool {
        if (g_jvm == nullptr) {
            LOGE("JVM is null in protect callback");
            return false;
        }
        
        JNIEnv* env = nullptr;
        int getEnvStat = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        
        if (getEnvStat == JNI_EDETACHED) {
            if (g_jvm->AttachCurrentThread(&env, nullptr) != 0) {
                LOGE("Failed to attach thread for protect()");
                return false;
            }
        } else if (getEnvStat != JNI_OK) {
            LOGE("Failed to get JNI environment");
            return false;
        }
        
        // Call VpnSocketProtector.protectFd(fd)
        jclass protectorClass = env->FindClass("com/packethunter/mobile/capture/VpnSocketProtector");
        if (protectorClass == nullptr) {
            LOGE("Cannot find VpnSocketProtector class");
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }
        
        jmethodID protectMethod = env->GetStaticMethodID(protectorClass, "protectFd", "(I)Z");
        if (protectMethod == nullptr) {
            LOGE("Cannot find protectFd method");
            env->DeleteLocalRef(protectorClass);
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }
        
        jboolean result = env->CallStaticBooleanMethod(protectorClass, protectMethod, fd);
        env->DeleteLocalRef(protectorClass);
        
        if (getEnvStat == JNI_EDETACHED) {
            g_jvm->DetachCurrentThread();
        }
        
        return result == JNI_TRUE;
    };
    
    // Packet processing callback wrapper
    auto packetWrapper = [](const uint8_t* data, size_t len) -> void {
        if (g_jvm == nullptr || g_packetCallback == nullptr) {
            return;
        }
        
        JNIEnv* env = nullptr;
        int getEnvStat = g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6);
        
        if (getEnvStat == JNI_EDETACHED) {
            if (g_jvm->AttachCurrentThread(&env, nullptr) != 0) {
                LOGE("Failed to attach thread for packet callback");
                return;
            }
        } else if (getEnvStat == JNI_OK) {
            // Already attached
        } else {
            LOGE("Failed to get JNI environment for packet callback");
            return;
        }
        
        // Create byte array
        jbyteArray packetData = env->NewByteArray(len);
        if (packetData != nullptr) {
            env->SetByteArrayRegion(packetData, 0, len, reinterpret_cast<const jbyte*>(data));
            
            // Call Java callback
            env->CallVoidMethod(g_packetCallback, g_packetProcessMethod, packetData);
            
            // Clean up local reference
            env->DeleteLocalRef(packetData);
        }
        
        if (getEnvStat == JNI_EDETACHED) {
            g_jvm->DetachCurrentThread();
        }
    };
    
    bool started = forwarder->start(tunFd, protectWrapper, packetWrapper);
    
    if (started) {
        LOGI("✅ Native forwarder started successfully");
        return JNI_TRUE;
    } else {
        LOGE("❌ Failed to start native forwarder");
        delete forwarder;
        forwarder = nullptr;
        env->DeleteGlobalRef(g_protectCallback);
        env->DeleteGlobalRef(g_packetCallback);
        g_protectCallback = nullptr;
        g_packetCallback = nullptr;
        return JNI_FALSE;
    }
}

JNIEXPORT void JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_stopForwarder(
    JNIEnv* env,
    jobject /* this */
) {
    if (forwarder != nullptr) {
        LOGI("Stopping native forwarder...");
        forwarder->stop();
        delete forwarder;
        forwarder = nullptr;
        LOGI("✅ Native forwarder stopped and cleaned up");
    }
    
    if (g_protectCallback != nullptr) {
        env->DeleteGlobalRef(g_protectCallback);
        g_protectCallback = nullptr;
    }
    
    if (g_packetCallback != nullptr) {
        env->DeleteGlobalRef(g_packetCallback);
        g_packetCallback = nullptr;
    }
    
    g_protectMethod = nullptr;
    g_packetProcessMethod = nullptr;
    g_jvm = nullptr;
    
    LOGI("✅ All JNI references cleaned up");
}

JNIEXPORT jboolean JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_isForwarderRunning(
    JNIEnv* env,
    jobject /* this */
) {
    return forwarder != nullptr && forwarder->isRunning() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_getForwarderStats(
    JNIEnv* env,
    jobject /* this */
) {
    if (forwarder == nullptr) {
        return nullptr;
    }
    
    PacketForwarder::Stats stats = forwarder->getStats();
    
    // Find ForwarderStats class
    jclass statsClass = env->FindClass("com/packethunter/mobile/capture/ForwarderStats");
    if (statsClass == nullptr) {
        LOGE("Could not find ForwarderStats class");
        return nullptr;
    }
    
    // Get constructor
    jmethodID constructor = env->GetMethodID(statsClass, "<init>", "(JJJJJJJJ)V");
    if (constructor == nullptr) {
        LOGE("Could not find ForwarderStats constructor");
        return nullptr;
    }
    
    // Create instance
    return env->NewObject(statsClass, constructor,
                         static_cast<jlong>(stats.packetsRead),
                         static_cast<jlong>(stats.packetsForwarded),
                         static_cast<jlong>(stats.bytesRead),
                         static_cast<jlong>(stats.bytesForwarded),
                         static_cast<jlong>(stats.tcpConnections),
                         static_cast<jlong>(stats.udpSessions),
                         static_cast<jlong>(stats.dnsQueries),
                         static_cast<jlong>(stats.errors));
}

} // extern "C"
