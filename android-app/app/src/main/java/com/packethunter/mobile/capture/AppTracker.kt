package com.packethunter.mobile.capture

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.TrafficStats
import android.util.Log
import com.packethunter.mobile.data.AppTalker
import java.io.BufferedReader
import java.io.FileReader

/**
 * Tracks which apps are using the network
 * Uses TrafficStats API + /proc/net/tcp fallback
 */
class AppTracker(private val context: Context) {
    
    private val packageManager = context.packageManager
    private val appStats = mutableMapOf<String, AppStats>()
    private val previousTrafficStats = mutableMapOf<Int, TrafficSnapshot>()
    
    companion object {
        private const val TAG = "AppTracker"
    }
    
    data class AppStats(
        var sentPackets: Int = 0,
        var receivedPackets: Int = 0,
        var sentBytes: Long = 0,
        var receivedBytes: Long = 0,
        val remoteHosts: MutableSet<String> = mutableSetOf(),
        val protocols: MutableSet<String> = mutableSetOf()
    )
    
    data class TrafficSnapshot(
        val txBytes: Long,
        val rxBytes: Long,
        val timestamp: Long
    )
    
    /**
     * Track a packet for a specific app
     */
    fun trackPacket(
        packageName: String,
        isOutbound: Boolean,
        bytes: Int,
        remoteIp: String,
        protocol: String
    ) {
        val stats = appStats.getOrPut(packageName) { AppStats() }
        
        if (isOutbound) {
            stats.sentPackets++
            stats.sentBytes += bytes
        } else {
            stats.receivedPackets++
            stats.receivedBytes += bytes
        }
        
        stats.remoteHosts.add(remoteIp)
        stats.protocols.add(protocol)
    }
    
    /**
     * Get app name from package name
     */
    fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
    
    /**
     * Find which app owns a connection based on source/dest IPs and ports
     * When VPN is active, /proc/net/tcp doesn't work, so we use TrafficStats API
     */
    fun findAppForConnection(sourceIp: String, sourcePort: Int, destIp: String, destPort: Int): String? {
        // When VPN is active, /proc/net/tcp shows VPN connections, not app connections
        // So we rely on TrafficStats API which tracks per-UID traffic even through VPN
        // The updateTrafficStats() method will handle tracking via TrafficStats
        
        // Try /proc/net/tcp as fallback (might work for some cases)
        try {
            val uid = findUidForConnection(sourcePort, destPort)
            if (uid != null) {
                val packageName = getPackageNameForUid(uid)
                if (packageName != null) {
                    return packageName
                }
            }
        } catch (e: Exception) {
            // Ignore - will use TrafficStats instead
        }
        
        // Return null - TrafficStats will track via updateTrafficStats()
        return null
    }
    
    /**
     * Read /proc/net/tcp to find UID for connection
     */
    private fun findUidForConnection(sourcePort: Int, destPort: Int): Int? {
        try {
            // Try IPv4 first
            BufferedReader(FileReader("/proc/net/tcp")).use { reader ->
                reader.readLine() // Skip header
                var line = reader.readLine()
                while (line != null) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 8) {
                        val localPort = parts[1].split(":")[1].toInt(16)
                        
                        // Match on local port (source port from packet)
                        if (localPort == sourcePort) {
                            val uid = parts[7].toIntOrNull()
                            if (uid != null && uid > 0) {
                                Log.d(TAG, "Found UID $uid for sourcePort $sourcePort")
                                return uid
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
            
            // Try IPv6
            BufferedReader(FileReader("/proc/net/tcp6")).use { reader ->
                reader.readLine() // Skip header
                var line = reader.readLine()
                while (line != null) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 8) {
                        val localPort = parts[1].split(":")[1].toInt(16)
                        
                        // Match on local port (source port from packet)
                        if (localPort == sourcePort) {
                            val uid = parts[7].toIntOrNull()
                            if (uid != null && uid > 0) {
                                Log.d(TAG, "Found UID $uid for sourcePort $sourcePort (IPv6)")
                                return uid
                            }
                        }
                    }
                    line = reader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading /proc/net/tcp", e)
        }
        return null
    }
    
    /**
     * Get package name from UID
     */
    private fun getPackageNameForUid(uid: Int): String? {
        try {
            val packages = packageManager.getPackagesForUid(uid)
            if (packages != null && packages.isNotEmpty()) {
                return packages[0] // Return first package
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting package for UID $uid", e)
        }
        return null
    }
    
    /**
     * Scan all installed apps and check their network usage using TrafficStats
     * This works even when VPN is active, as TrafficStats tracks per-UID traffic
     */
    fun updateTrafficStats() {
        try {
            val currentTime = System.currentTimeMillis()
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            
            for (appInfo in installedApps) {
                val packageName = appInfo.packageName
                
                // Skip the packet hunter app itself
                if (packageName == "com.packethunter.mobile") {
                    continue
                }
                
                // Include ALL user apps and important system apps
                // Don't skip system apps - they might be using network too
                
                val uid = appInfo.uid
                val txBytes = TrafficStats.getUidTxBytes(uid)
                val rxBytes = TrafficStats.getUidRxBytes(uid)
                
                // Skip if no data or invalid
                if (txBytes == TrafficStats.UNSUPPORTED.toLong() || rxBytes == TrafficStats.UNSUPPORTED.toLong()) {
                    continue
                }
                
                val stats = appStats.getOrPut(packageName) { AppStats() }
                
                // Calculate delta from previous snapshot
                val previous = previousTrafficStats[uid]
                if (previous != null) {
                    val deltaTx = (txBytes - previous.txBytes).coerceAtLeast(0)
                    val deltaRx = (rxBytes - previous.rxBytes).coerceAtLeast(0)
                    
                    // If there's network activity, update app stats
                    if (deltaTx > 0 || deltaRx > 0) {
                        stats.sentBytes += deltaTx
                        stats.receivedBytes += deltaRx
                        // Estimate packet counts (avg 1000 bytes per packet)
                        stats.sentPackets += (deltaTx / 1000).toInt().coerceAtLeast(1)
                        stats.receivedPackets += (deltaRx / 1000).toInt().coerceAtLeast(1)
                        
                        if (Log.isLoggable(TAG, Log.DEBUG)) {
                            Log.d(TAG, "TrafficStats: $packageName - TX: ${deltaTx}B, RX: ${deltaRx}B")
                        }
                    }
                } else {
                    // First time seeing this app - initialize with current values
                    if (txBytes > 0 || rxBytes > 0) {
                        stats.sentBytes = txBytes
                        stats.receivedBytes = rxBytes
                        stats.sentPackets = (txBytes / 1000).toInt().coerceAtLeast(1)
                        stats.receivedPackets = (rxBytes / 1000).toInt().coerceAtLeast(1)
                    }
                }
                
                // Update snapshot for next comparison
                previousTrafficStats[uid] = TrafficSnapshot(txBytes, rxBytes, currentTime)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating traffic stats", e)
        }
    }
    
    /**
     * Get all app talkers sorted by total data
     */
    fun getAppTalkers(): List<AppTalker> {
        // CRITICAL: Always update traffic stats before returning.
        // This is the most reliable way to get per-app stats when a VPN is active,
        // as /proc/net/tcp is often inaccessible or inaccurate.
        updateTrafficStats()
        
        // Filter out apps with zero traffic and exclude the packet hunter app itself
        return appStats
            .filter { (packageName, stats) -> 
                val totalBytes = stats.sentBytes + stats.receivedBytes
                totalBytes > 0 && packageName != "com.packethunter.mobile"
            }
            .map { (packageName, stats) ->
                AppTalker(
                    packageName = packageName,
                    appName = getAppName(packageName),
                    sentPackets = stats.sentPackets,
                    receivedPackets = stats.receivedPackets,
                    totalBytes = stats.sentBytes + stats.receivedBytes,
                    sentBytes = stats.sentBytes,
                    receivedBytes = stats.receivedBytes,
                    uniqueRemoteHosts = stats.remoteHosts.size,
                    protocols = stats.protocols
                )
            }
            .sortedByDescending { it.totalBytes }
    }
    
    /**
     * Clear all stats
     */
    fun clear() {
        appStats.clear()
    }
}
