package com.packethunter.mobile.capture

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * VPN Diagnostics and Testing Utilities
 * 
 * Provides test functions to verify VPN functionality:
 * - DNS resolution
 * - Socket protection
 * - Packet forwarding
 */
object VpnDiagnostics {
    private const val TAG = "VpnDiagnostics"
    
    /**
     * Test UDP forwarding with socket protection
     * 
     * This verifies that:
     * 1. Socket protection works correctly
     * 2. Packets can be sent through protected sockets
     * 3. VPN doesn't block outbound traffic
     * 
     * Run this with VPN active to verify protect() is working.
     */
    suspend fun testUdpForward(vpn: VpnService) = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== UDP Forward Test Start ===")
        
        try {
            val ds = DatagramSocket()
            Log.d(TAG, "Created DatagramSocket: $ds")
            
            // Protect the socket from VPN routing
            val ok = try {
                val result = vpn.protect(ds)
                Log.i(TAG, "testUdpForward protect() -> $result")
                result
            } catch (e: Exception) {
                Log.e(TAG, "protect() failed for DatagramSocket", e)
                false
            }
            
            if (!ok) {
                Log.e(TAG, "Socket protection failed - packets will loop through VPN!")
                ds.close()
                return@withContext
            }
            
            // Send test packet to Cloudflare DNS
            val targetHost = "1.1.1.1"
            val targetPort = 53
            val buf = "hello".toByteArray()
            val pkt = DatagramPacket(buf, buf.size, InetAddress.getByName(targetHost), targetPort)
            
            Log.i(TAG, "Sending test packet to $targetHost:$targetPort")
            ds.send(pkt)
            Log.i(TAG, "Test packet sent successfully")
            
            ds.close()
            Log.i(TAG, "=== UDP Forward Test Success ===")
        } catch (e: Exception) {
            Log.e(TAG, "testUdpForward exception", e)
            Log.e(TAG, "=== UDP Forward Test Failed ===")
        }
    }
    
    /**
     * Test DNS resolution
     * 
     * Verifies that DNS queries work correctly through the VPN.
     */
    suspend fun testDnsResolution() = withContext(Dispatchers.IO) {
        Log.i(TAG, "=== DNS Resolution Test Start ===")
        
        try {
            val hostname = "example.com"
            val dnsServer = "1.1.1.1"
            
            Log.i(TAG, "Resolving $hostname via $dnsServer")
            val address = InetAddress.getByName(hostname)
            Log.i(TAG, "DNS resolution successful: $hostname -> ${address.hostAddress}")
            Log.i(TAG, "=== DNS Resolution Test Success ===")
        } catch (e: Exception) {
            Log.e(TAG, "DNS resolution failed", e)
            Log.e(TAG, "=== DNS Resolution Test Failed ===")
        }
    }
    
    /**
     * Verify VPN interface is active
     * 
     * Checks if VPN interface file descriptor is valid.
     */
    fun verifyVpnInterface(pfd: ParcelFileDescriptor?): Boolean {
        return try {
            if (pfd == null) {
                Log.e(TAG, "VPN interface is null")
                return false
            }
            
            val fdObj = pfd.fileDescriptor
            val fd = fdObj?.let { 
                try {
                    // Get the int value from FileDescriptor using reflection or direct access
                    val field = it.javaClass.getDeclaredField("descriptor")
                    field.isAccessible = true
                    field.getInt(it)
                } catch (e: Exception) {
                    -1
                }
            } ?: -1
            
            if (fd < 0) {
                Log.e(TAG, "VPN file descriptor is invalid: $fd")
                return false
            }
            
            Log.i(TAG, "VPN interface verified: fd=$fd")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying VPN interface", e)
            false
        }
    }
}

