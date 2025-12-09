package com.packethunter.mobile.capture

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Statistics from native forwarder
 */
data class ForwarderStats(
    val packetsRead: Long = 0,
    val packetsForwarded: Long = 0,
    val bytesRead: Long = 0,
    val bytesForwarded: Long = 0,
    val tcpConnections: Long = 0,
    val udpSessions: Long = 0,
    val dnsQueries: Long = 0,
    val errors: Long = 0
)

/**
 * Interface for protecting sockets from VPN routing
 */
interface SocketProtector {
    fun protect(socketFd: Int): Boolean
}

/**
 * JNI wrapper for native packet forwarder
 */
class NativeForwarder {
    
    companion object {
        private const val TAG = "NativeForwarder"
        
        init {
            try {
                System.loadLibrary("packethunter")
                Log.i(TAG, "Native library loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library", e)
            }
        }
    }
    
    /**
     * Start the forwarder with TUN file descriptor and packet processor
     * @param tunFd TUN file descriptor
     * @param vpnService VpnService instance for protect() calls
     * @param packetProcessor PacketProcessor instance for packet analysis
     * @return true if started successfully
     */
    fun start(tunFd: Int, vpnService: VpnService, packetProcessor: PacketProcessor): Boolean {
        Log.i(TAG, "Starting NativeForwarder with lwip_tun2socks integration")
        val protector = object : SocketProtector {
            override fun protect(socketFd: Int): Boolean {
                val result = vpnService.protect(socketFd)
                if (result) {
                    Log.i(TAG, "Protected socket $socketFd from VPN routing")
                } else {
                    Log.e(TAG, "Failed to protect socket $socketFd")
                }
                return result
            }
        }
        
        // Create packet callback to send parsed packets to Kotlin processor
        val packetCallback = object : Any() {
            @JvmName("onPacket")
            fun onPacket(packetData: ByteArray) {
                // Process packet asynchronously on IO dispatcher
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        packetProcessor.processPacket(packetData)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing packet in callback", e)
                    }
                }
            }
        }
        
        return startForwarder(tunFd, protector, packetCallback)
    }
    
    /**
     * Start forwarder with custom protector and packet callback
     * Note: JNI will call protect() method on the protector object
     * and onPacket() method on the packetCallback object
     */
    private fun startForwarder(tunFd: Int, protector: SocketProtector, packetCallback: Any): Boolean {
        // Create a wrapper that implements the interface expected by JNI
        val protectorWrapper = object : Any() {
            @JvmName("protect")
            fun protect(socketFd: Int): Boolean {
                return protector.protect(socketFd)
            }
        }
        return startForwarderNative(tunFd, protectorWrapper, packetCallback)
    }
    
    private external fun startForwarderNative(tunFd: Int, protector: Any, packetCallback: Any): Boolean
    external fun setLegacyTcpResponseSynthesisEnabled(enabled: Boolean)
    
    /**
     * Stop the forwarder
     */
    external fun stopForwarder()
    
    /**
     * Check if forwarder is running
     */
    external fun isForwarderRunning(): Boolean
    
    /**
     * Get forwarder statistics
     */
    external fun getForwarderStats(): ForwarderStats?
    
    /**
     * Pause forwarding for a session (for interception)
     * Native thread will queue packets for this session
     * @param sessionId Unique session identifier
     * @return true if session was paused
     */
    fun pauseSession(sessionId: String): Boolean {
        return try {
            pauseSessionNative(sessionId)
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing session $sessionId", e)
            false
        }
    }
    
    /**
     * Resume forwarding for a session
     * @param sessionId Unique session identifier
     * @param modifiedPayload Optional modified payload (null to use original queued packets)
     * @return true if session was resumed
     */
    fun resumeSession(sessionId: String, modifiedPayload: ByteArray? = null): Boolean {
        return try {
            resumeSessionNative(sessionId, modifiedPayload)
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming session $sessionId", e)
            false
        }
    }
    
    private external fun pauseSessionNative(sessionId: String): Boolean
    private external fun resumeSessionNative(sessionId: String, modifiedPayload: ByteArray?): Boolean
}

