#include <jni.h>
#include "tun2socks_bridge.h"
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
static jobject g_appContext = nullptr; // Global reference to app context

// JNI_OnLoad to store JavaVM
jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

// Helper to load class using app Context classloader
jclass loadClassFromContext(JNIEnv* env, jobject context, const char* className) {
    jclass contextClass = env->GetObjectClass(context);
    jmethodID getClassLoader = env->GetMethodID(contextClass, "getClassLoader", "()Ljava/lang/ClassLoader;");
    jobject classLoader = env->CallObjectMethod(context, getClassLoader);

    jclass classLoaderClass = env->FindClass("java/lang/ClassLoader");
    jmethodID loadClass = env->GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    jstring jClassName = env->NewStringUTF(className);
    jobject clsObj = env->CallObjectMethod(classLoader, loadClass, jClassName);
    env->DeleteLocalRef(jClassName);

    return (jclass) clsObj; // local ref
}

// Safe attach/detach helpers for native threads
JNIEnv* attachCurrentThread() {
    JNIEnv* env = nullptr;
    if (g_jvm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        if (g_jvm->AttachCurrentThread(&env, nullptr) != JNI_OK) {
            return nullptr;
        }
    }
    return env;
}

void detachCurrentThread() {
    if (g_jvm) g_jvm->DetachCurrentThread();
}

// JNI function to set app context
extern "C" JNIEXPORT void JNICALL
Java_com_packethunter_mobile_capture_NativeInterface_setAppContext(JNIEnv* env, jclass clazz, jobject context) {
    if (g_appContext != nullptr) {
        env->DeleteGlobalRef(g_appContext);
    }
    g_appContext = env->NewGlobalRef(context);
    LOGI("App context set for native interface");
}

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

// Legacy TCP response synthesis toggle
JNIEXPORT void JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_setLegacyTcpResponseSynthesisEnabled(
    JNIEnv* env,
    jobject /* this */,
    jboolean enabled
) {
    setLegacyTcpResponseSynthesisEnabled(enabled);
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
        
        // Call VpnSocketProtector.protectFd(fd) using robust class loading
        LOGI("Checking app context for VpnSocketProtector");
        if (g_appContext == nullptr) {
            LOGE("App context not set for VpnSocketProtector - this is a critical error!");
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }

        LOGI("Loading VpnSocketProtector class via app classloader");
        JNIEnv* currentEnv = env; // Use the attached env
        jclass protectorClass = loadClassFromContext(currentEnv, g_appContext, "com.packethunter.mobile.capture.VpnSocketProtector");
        if (protectorClass == nullptr) {
            LOGE("Cannot find VpnSocketProtector class via app classloader - check if ProGuard is stripping the class");
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }

        LOGI("Getting INSTANCE field from VpnSocketProtector");
        // Get the INSTANCE field (Kotlin object)
        jfieldID instanceField = currentEnv->GetStaticFieldID(protectorClass, "INSTANCE", "Lcom/packethunter/mobile/capture/VpnSocketProtector;");
        if (instanceField == nullptr) {
            LOGE("Cannot find VpnSocketProtector INSTANCE field - this indicates a Kotlin object issue");
            currentEnv->DeleteLocalRef(protectorClass);
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }

        jobject instanceObj = currentEnv->GetStaticObjectField(protectorClass, instanceField);
        if (instanceObj == nullptr) {
            LOGE("Cannot get VpnSocketProtector INSTANCE - this indicates the Kotlin object is not properly initialized");
            currentEnv->DeleteLocalRef(protectorClass);
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }

        LOGI("Getting protectFd method from VpnSocketProtector");
        // Get the protect method - it's a static method in the Kotlin object
        jmethodID protectMethod = currentEnv->GetStaticMethodID(protectorClass, "protectFd", "(I)Z");
        if (protectMethod == nullptr) {
            LOGE("Cannot find protectFd method - check method signature and ProGuard rules");
            currentEnv->DeleteLocalRef(protectorClass);
            currentEnv->DeleteLocalRef(instanceObj);
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }

        LOGI("Calling protectFd method on VpnSocketProtector with fd=%d", fd);
        jboolean result = currentEnv->CallStaticBooleanMethod(protectorClass, protectMethod, fd);
        currentEnv->DeleteLocalRef(protectorClass);
        currentEnv->DeleteLocalRef(instanceObj);

        // Check for Java exceptions
        if (currentEnv->ExceptionCheck()) {
            LOGE("Exception occurred while calling protectFd method");
            currentEnv->ExceptionDescribe();
            currentEnv->ExceptionClear();
            if (getEnvStat == JNI_EDETACHED) {
                g_jvm->DetachCurrentThread();
            }
            return false;
        }

        LOGI("protectFd returned: %s", result ? "true" : "false");

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

// Pause/resume JNI functions for interception
JNIEXPORT jboolean JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_pauseSessionNative(
    JNIEnv* env,
    jobject /* this */,
    jstring sessionId
) {
    if (forwarder == nullptr) {
        LOGE("Forwarder not running");
        return JNI_FALSE;
    }
    
    const char* sessionIdChars = env->GetStringUTFChars(sessionId, nullptr);
    if (sessionIdChars == nullptr) {
        LOGE("Failed to get session ID string");
        return JNI_FALSE;
    }
    
    std::string sessionIdStr(sessionIdChars);
    env->ReleaseStringUTFChars(sessionId, sessionIdChars);
    
    bool result = forwarder->pauseSession(sessionIdStr);
    LOGI("⏸️ JNI: pauseSession(%s) = %s", sessionIdStr.c_str(), result ? "true" : "false");
    
    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_packethunter_mobile_capture_NativeForwarder_resumeSessionNative(
    JNIEnv* env,
    jobject /* this */,
    jstring sessionId,
    jbyteArray modifiedPayload
) {
    if (forwarder == nullptr) {
        LOGE("Forwarder not running");
        return JNI_FALSE;
    }
    
    const char* sessionIdChars = env->GetStringUTFChars(sessionId, nullptr);
    if (sessionIdChars == nullptr) {
        LOGE("Failed to get session ID string");
        return JNI_FALSE;
    }
    
    std::string sessionIdStr(sessionIdChars);
    env->ReleaseStringUTFChars(sessionId, sessionIdChars);
    
    bool result = false;
    
    if (modifiedPayload != nullptr) {
        jsize len = env->GetArrayLength(modifiedPayload);
        jbyte* bytes = env->GetByteArrayElements(modifiedPayload, nullptr);
        
        if (bytes != nullptr) {
            result = forwarder->resumeSession(sessionIdStr, 
                                             reinterpret_cast<const uint8_t*>(bytes),
                                             static_cast<size_t>(len));
            env->ReleaseByteArrayElements(modifiedPayload, bytes, JNI_ABORT);
        }
    } else {
        // Resume with original (unmodified) packets
        result = forwarder->resumeSession(sessionIdStr, nullptr, 0);
    }
    
    LOGI("▶️ JNI: resumeSession(%s) = %s", sessionIdStr.c_str(), result ? "true" : "false");
    
    return result ? JNI_TRUE : JNI_FALSE;
}

} // extern "C"
