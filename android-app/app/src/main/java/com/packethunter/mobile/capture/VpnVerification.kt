package com.packethunter.mobile.capture

import android.content.Context
import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * VPN Verification and Testing Utilities
 * 
 * Provides methods to verify VPN functionality and test packet forwarding
 */
object VpnVerification {
    private const val TAG = "VpnVerification"
    
    /**
     * Test UDP packet forwarding to verify protect() works
     * 
     * This sends a test UDP packet to 1.1.1.1:53 (Cloudflare DNS) to verify
     * that socket protection is working and packets can be forwarded.
     * 
     * @param vpnService The VpnService instance
     * @return true if test succeeded, false otherwise
     */
    suspend fun testUdpForward(vpnService: VpnService): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== UDP FORWARD TEST START ===")
            
            val ds = DatagramSocket()
            Log.d(TAG, "Created DatagramSocket: $ds")
            
            // Protect the socket from VPN routing
            val ok = try {
                vpnService.protect(ds)
            } catch (e: Exception) {
                Log.e(TAG, "protect() failed for DatagramSocket", e)
                false
            }
            
            Log.i(TAG, "testUdpForward protect() -> $ok")
            
            if (!ok) {
                Log.e(TAG, "Socket protection failed - packets will loop through VPN")
                ds.close()
                return@withContext false
            }
            
            // Send test packet to Cloudflare DNS
            val buf = "test".toByteArray()
            val targetAddr = InetAddress.getByName("1.1.1.1")
            val pkt = DatagramPacket(buf, buf.size, targetAddr, 53)
            
            Log.d(TAG, "Sending test packet to ${targetAddr.hostAddress}:53")
            ds.send(pkt)
            Log.i(TAG, "Successfully sent test packet to 1.1.1.1:53")
            
            ds.close()
            Log.i(TAG, "=== UDP FORWARD TEST SUCCESS ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "testUdpForward exception", e)
            Log.e(TAG, "=== UDP FORWARD TEST FAILED ===")
            false
        }
    }
    
    /**
     * Test TCP socket protection
     */
    suspend fun testTcpSocket(vpnService: VpnService): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "=== TCP SOCKET TEST START ===")
            
            val socket = java.net.Socket()
            Log.d(TAG, "Created Socket: $socket")
            
            val ok = try {
                vpnService.protect(socket)
            } catch (e: Exception) {
                Log.e(TAG, "protect() failed for Socket", e)
                false
            }
            
            Log.i(TAG, "testTcpSocket protect() -> $ok")
            
            if (!ok) {
                socket.close()
                return@withContext false
            }
            
            // Try to connect (with timeout)
            socket.connect(java.net.InetSocketAddress("1.1.1.1", 80), 5000)
            Log.i(TAG, "TCP socket connected successfully")
            
            socket.close()
            Log.i(TAG, "=== TCP SOCKET TEST SUCCESS ===")
            true
        } catch (e: Exception) {
            Log.e(TAG, "testTcpSocket exception", e)
            Log.e(TAG, "=== TCP SOCKET TEST FAILED ===")
            false
        }
    }
    
    /**
     * Verify VPN is established and active
     * 
     * Checks if VPN interface exists and is valid
     */
    fun verifyVpnEstablished(vpnInterface: android.os.ParcelFileDescriptor?): Boolean {
        return if (vpnInterface == null) {
            Log.e(TAG, "VPN interface is null")
            false
        } else {
            val fd = vpnInterface.fileDescriptor
            val valid = fd.valid()
            Log.i(TAG, "VPN interface check: fd=$fd valid=$valid")
            valid
        }
    }
    
    /**
     * Log current DNS configuration
     */
    fun logDnsConfiguration(context: Context) {
        try {
            Log.i(TAG, "=== DNS CONFIGURATION ===")
            
            // Try to get system DNS via reflection (SystemProperties is hidden API)
            try {
                val systemPropertiesClass = Class.forName("android.os.SystemProperties")
                val getMethod = systemPropertiesClass.getMethod("get", String::class.java, String::class.java)
                
                val dns1 = getMethod.invoke(null, "net.dns1", "unknown") as? String ?: "unknown"
                val dns2 = getMethod.invoke(null, "net.dns2", "unknown") as? String ?: "unknown"
                
                Log.i(TAG, "System DNS1: $dns1")
                Log.i(TAG, "System DNS2: $dns2")
            } catch (e: Exception) {
                Log.w(TAG, "Could not read system DNS (requires reflection): ${e.message}")
            }
            
            // Note: VPN DNS should be 1.1.1.1 and 8.8.8.8 as configured
            Log.i(TAG, "Expected VPN DNS: 1.1.1.1, 8.8.8.8")
            Log.i(TAG, "Note: Check DNS via 'getprop net.dns1' in adb shell")
            Log.i(TAG, "=== DNS CONFIGURATION END ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error reading DNS configuration", e)
        }
    }
}

