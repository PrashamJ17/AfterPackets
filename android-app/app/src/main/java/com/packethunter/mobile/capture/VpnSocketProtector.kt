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

