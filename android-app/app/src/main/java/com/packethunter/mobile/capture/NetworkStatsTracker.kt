package com.packethunter.mobile.capture

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.packethunter.mobile.data.AppTalker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Per-App Data Usage Tracker using NetworkStatsManager
 * 
 * This provides accurate per-app network usage statistics even on non-rooted devices.
 * NetworkStatsManager tracks data usage per UID, which we map to package names.
 * 
 * Requirements:
 * - PACKAGE_USAGE_STATS permission (optional, for better accuracy)
 * - Works without root access
 * - More accurate than TrafficStats API
 */
class NetworkStatsTracker(private val context: Context) {
    
    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val packageManager = context.packageManager
    
    private val appStats = mutableMapOf<String, AppUsageData>()
    private var lastQueryTime = System.currentTimeMillis()
    
    companion object {
        private const val TAG = "NetworkStatsTracker"
        private const val QUERY_INTERVAL_MS = 1000L // Query every 1 second for better responsiveness
    }
    
    data class AppUsageData(
        var sentBytes: Long = 0,
        var receivedBytes: Long = 0,
        var sentPackets: Long = 0,
        var receivedPackets: Long = 0,
        var lastUpdate: Long = System.currentTimeMillis()
    )
    
    /**
     * Query network statistics for all apps
     * 
     * @param startTime Start timestamp in milliseconds
     * @param endTime End timestamp in milliseconds
     * @return Map of package name to AppUsageData
     */
    suspend fun queryNetworkStats(startTime: Long, endTime: Long): Map<String, AppUsageData> = withContext(Dispatchers.IO) {
        if (networkStatsManager == null) {
            Log.w(TAG, "NetworkStatsManager not available")
            return@withContext emptyMap()
        }
        
        val results = mutableMapOf<String, AppUsageData>()
        val duration = endTime - startTime
        
        Log.i(TAG, "=== NetworkStats Query Start ===")
        Log.i(TAG, "Query period: ${duration}ms (${startTime} -> ${endTime})")
        
        try {
            // Query for WiFi network type
            Log.d(TAG, "Querying WiFi network stats...")
            val wifiSummary = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            
            processNetworkStatsSummary(wifiSummary, results)
            wifiSummary.close()
            
            // Query for mobile network type
            Log.d(TAG, "Querying mobile network stats...")
            val mobileSummary = networkStatsManager.querySummary(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startTime,
                endTime
            )
            
            processNetworkStatsSummary(mobileSummary, results)
            mobileSummary.close()
            
            Log.i(TAG, "NetworkStats query complete: ${results.size} apps found")
            Log.i(TAG, "=== NetworkStats Query End ===")
            
        } catch (e: SecurityException) {
            Log.w(TAG, "Permission denied for NetworkStatsManager - PACKAGE_USAGE_STATS may be required", e)
            Log.w(TAG, "To enable: Settings → Apps → Special access → Usage access → AFTERPACKETS")
            // Fallback to TrafficStats if permission missing
            return@withContext emptyMap()
        } catch (e: Exception) {
            Log.e(TAG, "Error querying network stats", e)
            return@withContext emptyMap()
        }
        
        results
    }
    
    /**
     * Process NetworkStats summary and aggregate by package name
     */
    private fun processNetworkStatsSummary(
        summary: NetworkStats,
        results: MutableMap<String, AppUsageData>
    ) {
        val bucket = NetworkStats.Bucket()
        var bucketCount = 0
        var nonZeroBuckets = 0
        
        Log.d(TAG, "Processing NetworkStats summary...")
        
        while (summary.hasNextBucket()) {
            summary.getNextBucket(bucket)
            bucketCount++
            
            val uid = bucket.uid
            if (uid == android.os.Process.myUid()) {
                // Skip our own UID (packet hunter app)
                continue
            }
            
            val txBytes = bucket.txBytes
            val rxBytes = bucket.rxBytes
            val txPackets = bucket.txPackets
            val rxPackets = bucket.rxPackets
            
            if (txBytes == 0L && rxBytes == 0L) {
                continue
            }
            
            nonZeroBuckets++
            
            // Map UID to package name(s)
            val packages = packageManager.getPackagesForUid(uid)
            if (packages == null || packages.isEmpty()) {
                // System UID or unknown package - log for debugging
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "NetworkStats UID $uid: tx=${txBytes}B rx=${rxBytes}B (no package mapping)")
                }
                continue
            }
            
            // Aggregate stats for all packages with this UID
            for (packageName in packages) {
                if (packageName == "com.packethunter.mobile") {
                    continue // Skip our own app
                }
                
                val appData = results.getOrPut(packageName) { AppUsageData() }
                appData.sentBytes += txBytes
                appData.receivedBytes += rxBytes
                appData.sentPackets += txPackets
                appData.receivedPackets += rxPackets
                appData.lastUpdate = System.currentTimeMillis()
                
                // Log mapping for debugging
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "NetworkStats UID $uid -> $packageName: tx=${txBytes}B rx=${rxBytes}B txPkts=$txPackets rxPkts=$rxPackets")
                }
            }
        }
        
        Log.i(TAG, "NetworkStats processing complete: $bucketCount total buckets, $nonZeroBuckets non-zero, ${results.size} apps tracked")
    }
    
    /**
     * Update app statistics from NetworkStatsManager
     * Should be called periodically (every 1-2 seconds)
     */
    suspend fun updateStats() {
        val currentTime = System.currentTimeMillis()
        val startTime = lastQueryTime
        val endTime = currentTime
    
        // Reduced minimum interval for better responsiveness
        if (endTime - startTime < 500) {
            // Don't query too frequently
            return
        }
        
        try {
            val newStats = queryNetworkStats(startTime, endTime)
            
            // Merge new stats with existing
            for ((packageName, newData) in newStats) {
                val existing = appStats.getOrPut(packageName) { AppUsageData() }
                
                // Calculate deltas (only add new traffic)
                val deltaTx = (newData.sentBytes - existing.sentBytes).coerceAtLeast(0)
                val deltaRx = (newData.receivedBytes - existing.receivedBytes).coerceAtLeast(0)
                
                // Only update if there's new traffic
                if (deltaTx > 0 || deltaRx > 0) {
                    existing.sentBytes += deltaTx
                    existing.receivedBytes += deltaRx
                    existing.sentPackets = newData.sentPackets
                    existing.receivedPackets = newData.receivedPackets
                    existing.lastUpdate = currentTime
                }
            }
            
            // Remove apps that haven't been active recently (older than 30 seconds)
            val cutoffTime = currentTime - 30000
            appStats.entries.removeAll { it.value.lastUpdate < cutoffTime }
            
            lastQueryTime = currentTime
        } catch (e: Exception) {
            Log.e(TAG, "Error updating network stats", e)
            // Don't update lastQueryTime on error so we retry sooner
        }
    }
    
    /**
     * Get all app talkers sorted by total data usage
     */
    fun getAppTalkers(): List<AppTalker> {
        // Return empty list if no data to avoid UI issues
        if (appStats.isEmpty()) {
            return emptyList()
        }
        
        return appStats.map { (packageName, data) ->
            val appName = getAppName(packageName)
            
            AppTalker(
                packageName = packageName,
                appName = appName,
                sentPackets = data.sentPackets.toInt(),
                receivedPackets = data.receivedPackets.toInt(),
                totalBytes = data.sentBytes + data.receivedBytes,
                sentBytes = data.sentBytes,
                receivedBytes = data.receivedBytes,
                uniqueRemoteHosts = 0, // NetworkStatsManager doesn't provide this
                protocols = emptySet() // NetworkStatsManager doesn't provide this
            )
        }.sortedByDescending { it.totalBytes }
        .take(50) // Limit to top 50 apps to avoid overwhelming the UI
    }
    
    /**
     * Get app name from package name
     */
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
    
    /**
     * Check if PACKAGE_USAGE_STATS permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? android.app.AppOpsManager
            val mode = appOps?.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } else {
            true // Permission not required on older versions
        }
    }
    
    /**
     * Get intent to request PACKAGE_USAGE_STATS permission
     */
    fun getUsageStatsPermissionIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }
    
    /**
     * Clear all statistics
     */
    fun clear() {
        appStats.clear()
        lastQueryTime = System.currentTimeMillis()
    }
}

