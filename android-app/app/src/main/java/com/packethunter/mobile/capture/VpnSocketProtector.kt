package com.packethunter.mobile.capture

import android.net.VpnService
import android.util.Log
import java.net.DatagramSocket
import java.net.Socket
import java.nio.channels.DatagramChannel
import java.nio.channels.SocketChannel

/**
 * Helper utility to protect sockets from VPN routing loops
 * 
 * CRITICAL: All sockets opened by the app or forwarding process must be protected
 * by calling vpnService.protect() to prevent routing loops.
 * 
 * Without protection, sockets opened by the app will be routed back through
 * the VPN interface, creating an infinite loop and blocking network traffic.
 */
object VpnSocketProtector {
    private const val TAG = "VpnSocketProtector"
    
    // Track statistics for debugging
    @Volatile
    private var protectSuccessCount = 0
    @Volatile
    private var protectFailCount = 0
    
    // Store VpnService instance for JNI access
    @Volatile
    private var vpnServiceInstance: VpnService? = null
    
    /**
     * Initialize with VpnService instance (called from PacketCaptureService)
     */
    fun initialize(vpnService: VpnService) {
        vpnServiceInstance = vpnService
        Log.i(TAG, "VpnSocketProtector initialized")
    }
    
    /**
     * Clear VpnService instance (called on service destroy)
     */
    fun clear() {
        vpnServiceInstance = null
        Log.i(TAG, "VpnSocketProtector cleared. Stats: success=$protectSuccessCount, fail=$protectFailCount")
    }
    
    /**
     * Protect a socket by file descriptor - JNI accessible
     * CRITICAL: This is called from native code via JNI
     * 
     * @param fd The file descriptor
     * @return true if protection succeeded, false otherwise
     */
    @JvmStatic
    fun protectFd(fd: Int): Boolean {
        val vpnService = vpnServiceInstance
        if (vpnService == null) {
            Log.e(TAG, "Cannot protect FD $fd - VpnService not initialized")
            protectFailCount++
            return false
        }
        
        return try {
            val result = vpnService.protect(fd)
            if (result) {
                protectSuccessCount++
                Log.d(TAG, "✅ Protected socket FD=$fd (success count: $protectSuccessCount)")
            } else {
                protectFailCount++
                Log.e(TAG, "❌ Failed to protect socket FD=$fd (fail count: $protectFailCount)")
            }
            result
        } catch (e: Exception) {
            protectFailCount++
            Log.e(TAG, "❌ Exception protecting socket FD=$fd", e)
            false
        }
    }
    
    /**
     * Protect a TCP Socket from VPN routing
     * 
     * @param vpnService The VpnService instance
     * @param socket The Socket to protect
     * @return true if protection succeeded, false otherwise
     */
    fun protectSocket(vpnService: VpnService, socket: Socket): Boolean {
        return try {
            val result = vpnService.protect(socket)
            if (!result) {
                Log.w(TAG, "Failed to protect TCP socket")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error protecting TCP socket", e)
            false
        }
    }
    
    /**
     * Protect a UDP DatagramSocket from VPN routing
     * 
     * @param vpnService The VpnService instance
     * @param socket The DatagramSocket to protect
     * @return true if protection succeeded, false otherwise
     */
    fun protectSocket(vpnService: VpnService, socket: DatagramSocket): Boolean {
        return try {
            val result = vpnService.protect(socket)
            if (!result) {
                Log.w(TAG, "Failed to protect UDP socket")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error protecting UDP socket", e)
            false
        }
    }
    
    /**
     * Protect a SocketChannel from VPN routing
     * 
     * @param vpnService The VpnService instance
     * @param channel The SocketChannel to protect
     * @return true if protection succeeded, false otherwise
     */
    fun protectSocket(vpnService: VpnService, channel: SocketChannel): Boolean {
        return try {
            val result = vpnService.protect(channel.socket())
            if (!result) {
                Log.w(TAG, "Failed to protect SocketChannel")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error protecting SocketChannel", e)
            false
        }
    }
    
    /**
     * Protect a DatagramChannel from VPN routing
     * 
     * @param vpnService The VpnService instance
     * @param channel The DatagramChannel to protect
     * @return true if protection succeeded, false otherwise
     */
    fun protectSocket(vpnService: VpnService, channel: DatagramChannel): Boolean {
        return try {
            val result = vpnService.protect(channel.socket())
            if (!result) {
                Log.w(TAG, "Failed to protect DatagramChannel")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error protecting DatagramChannel", e)
            false
        }
    }
    
    /**
     * Protect a socket by file descriptor
     * 
     * @param vpnService The VpnService instance
     * @param fd The file descriptor
     * @return true if protection succeeded, false otherwise
     */
    fun protectSocket(vpnService: VpnService, fd: Int): Boolean {
        return try {
            val result = vpnService.protect(fd)
            if (!result) {
                Log.w(TAG, "Failed to protect socket by FD: $fd")
            }
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error protecting socket by FD: $fd", e)
            false
        }
    }
}

