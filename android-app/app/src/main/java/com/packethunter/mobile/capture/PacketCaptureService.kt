package com.packethunter.mobile.capture

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.packethunter.mobile.MainActivity
import com.packethunter.mobile.R
import com.packethunter.mobile.data.PacketDatabase
import com.packethunter.mobile.data.PacketInfo
import com.packethunter.mobile.interception.BreakpointAction
import kotlinx.coroutines.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.min

/**
 * VPN Service for capturing network packets
 * Uses VpnService API to intercept all network traffic
 */
class PacketCaptureService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var captureJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private lateinit var packetProcessor: PacketProcessor
    private val nativeParser = NativePacketParser()

    // Native packet forwarder for better performance
    private var nativeForwarder: NativeForwarder? = null

    // PCAPdroid-style packet forwarder (fallback)
    private var packetForwarder: PacketForwarder? = null

    // Active Breakpoint Manager
    val activeBreakpointManager = com.packethunter.mobile.interception.ActiveBreakpointManager()
    
    private var isRunning = false
    
    // Forwarding metrics
    private val forwardedPackets = java.util.concurrent.atomic.AtomicLong(0)
    private val forwardedBytes = java.util.concurrent.atomic.AtomicLong(0)
    private val forwardingStartTime = java.util.concurrent.atomic.AtomicLong(0)
    
    companion object {
        private const val TAG = "PacketCaptureService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "packet_capture_channel"
        const val ACTION_START = "com.packethunter.mobile.START_CAPTURE"
        const val ACTION_STOP = "com.packethunter.mobile.STOP_CAPTURE"
        
        // MTU and buffer sizes
        private const val MTU_SIZE = 1400 // Optimized to prevent fragmentation
        private const val BUFFER_SIZE = 32767
        
        // Static reference to service instance for accessing data
        @Volatile
        private var instance: PacketCaptureService? = null
        
        fun getInstance(): PacketCaptureService? = instance
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Set instance reference
        instance = this
        
        // Initialize native parser
        nativeParser.initParser()
        
        // Set app context for native interface
        try {
            NativeInterface.setAppContext(this)
            Log.i(TAG, "✅ Native app context set successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set native app context", e)
        }
        
        // Initialize packet processor with NetworkStatsTracker
        val database = PacketDatabase.getDatabase(applicationContext)
        val appTracker = AppTracker(applicationContext)
        val networkStatsTracker = NetworkStatsTracker(applicationContext)
        packetProcessor = PacketProcessor(database, nativeParser, appTracker, networkStatsTracker)
        
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: ${intent?.action}")
        
        when (intent?.action) {
            ACTION_START -> startCapture()
            ACTION_STOP -> stopCapture()
        }
        
        return START_STICKY
    }

    private fun startCapture() {
        if (isRunning) {
            Log.d(TAG, "Capture already running")
            return
        }
        
        Log.d(TAG, "Starting packet capture")
        
        // Initialize VpnSocketProtector with this service instance
        // CRITICAL: This must be done BEFORE starting the native forwarder
        VpnSocketProtector.initialize(this)
        
        // Show notification
        val notification = createNotification()
        
        // Start foreground with proper service type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        // Establish VPN interface
        vpnInterface = establishVpnInterface()
        
        if (vpnInterface == null) {
            Log.e(TAG, "Failed to establish VPN interface")
            stopSelf()
            return
        }
        
        // Verify VPN interface
        if (!VpnDiagnostics.verifyVpnInterface(vpnInterface)) {
            Log.e(TAG, "VPN interface verification failed")
            stopSelf()
            return
        }
        
        // Start native packet forwarder for full duplex operation
        val vpn = vpnInterface
        if (vpn != null) {
            val tunFd = vpn.fd
            Log.i(TAG, "Starting native forwarder with TUN FD: $tunFd")

            // Use ONLY the native forwarder (no Kotlin forwarder fallback)
            try {
                nativeForwarder = NativeForwarder()
                // Ensure legacy synthesis is disabled in production
                nativeForwarder?.setLegacyTcpResponseSynthesisEnabled(false)
                val started = nativeForwarder?.start(tunFd, this, packetProcessor)

                if (started == true) {
                    Log.i(TAG, "✅ Native forwarder started successfully - full duplex mode active")
                    Log.i(TAG, "Native forwarder will handle all packet forwarding and parsing")
                } else {
                    Log.e(TAG, "❌ Native forwarder failed to start - VPN will not work properly")
                    stopSelf()
                    return
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error starting native forwarder - VPN cannot continue", e)
                stopSelf()
                return
            }
        } else {
            Log.e(TAG, "VPN interface is null")
        }

        isRunning = true

        // Start packet processing
        packetProcessor.startProcessing()

        // Run diagnostic tests (non-blocking) after VPN stabilizes
        scope.launch {
            delay(2000) // Wait 2 seconds for VPN to stabilize
            Log.i(TAG, "Running VPN diagnostic tests...")
            VpnDiagnostics.testUdpForward(this@PacketCaptureService)
            VpnDiagnostics.testDnsResolution()
            runConnectivityTests()
        }

        Log.i(TAG, "Packet capture service fully started - forwarder active")
    }

    /**
     * DISABLED: Kotlin-based forwarder - using native forwarder only
     * Keeping this code for reference but it should never be called
     */
    @Deprecated("Use native forwarder only", level = DeprecationLevel.ERROR)
    private fun startKotlinForwarder(vpn: ParcelFileDescriptor) {
        throw UnsupportedOperationException("Kotlin forwarder disabled - use native forwarder only")
        // packetForwarder = PacketForwarder(
        //     vpnService = this,
        //     vpnInterface = vpn,
        //     packetProcessor = { packetData ->
        //         scope.launch(Dispatchers.IO) {
        //             try {
        //                 packetProcessor.processPacket(packetData)
        //             } catch (e: Exception) {
        //                 Log.e(TAG, "Error processing packet", e)
        //             }
        //         }
        //     }
        // )
        //
        // packetForwarder?.start()
        // Log.i(TAG, "Kotlin packet forwarder started successfully")
    }

    /**
     * Establish VPN interface with optimized routing and DNS configuration
     * 
     * Configuration:
     * - Address: 10.0.0.2/32 (single host)
     * - Route: 0.0.0.0/0 (all traffic)
     * - DNS: 1.1.1.1 (Cloudflare) and 8.8.8.8 (Google) for reliability
     * - MTU: 1400 to prevent fragmentation and improve performance
     */
    private fun establishVpnInterface(): ParcelFileDescriptor? {
        return try {
            Log.i(TAG, "=== VPN ESTABLISHMENT START ===")
            Log.i(TAG, "Establishing VPN: addr=10.0.0.2/32 route=0.0.0.0/0 dns=1.1.1.1,8.8.8.8 mtu=1400")
            
            // Configuration flags - NOW ENABLED with proper tun2socks
            val routeAllTraffic = true // ENABLED: lwip_tun2socks can handle full routing
            val builder = Builder()
                .setSession("AFTERPACKETS")
                .addAddress("10.0.0.2", 32) // Single host address
                .setMtu(1400) // Optimized MTU to prevent fragmentation
                .setBlocking(false) // Non-blocking is safer on some devices

            // Add routes - FULL ROUTING with proper tun2socks
            builder.addRoute("0.0.0.0", 0) // Route all traffic through VPN

            // Use system DNS servers instead of hard-coded ones
            val systemDns = VpnDiagnostics.getSystemDnsServers(this)
            if (systemDns.isNotEmpty()) {
                systemDns.forEach { dns ->
                    try { builder.addDnsServer(dns.hostAddress) } catch (_: Exception) {}
                }
            }
            
            // CRITICAL: Allow apps to bypass VPN if needed (Android 10+)
            // This prevents routing loops and allows other apps to work properly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                builder.allowBypass()
                Log.d(TAG, "allowBypass() enabled for Android 10+")
            }
            
            // CRITICAL: Set configure intent to allow users to manage the VPN
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            builder.setConfigureIntent(pendingIntent)
            
            // If no system DNS was found, fall back to the default (do not set custom DNS)
            if (systemDns.isEmpty()) {
                Log.w(TAG, "No system DNS found; relying on platform defaults")
            }

            Log.i(TAG, "Calling builder.establish()...")
            val vpnInterface = try {
                builder.establish()
            } catch (e: Exception) {
                Log.e(TAG, "VPN establish() threw exception", e)
                throw e
            }
            
            if (vpnInterface == null) {
                Log.e(TAG, "VPN interface establishment returned null")
                return null
            }
            
            Log.i(TAG, "VPN established successfully: pfd=$vpnInterface")
            Log.i(TAG, "VPN file descriptor: ${vpnInterface.fileDescriptor}")
            Log.i(TAG, "VPN Configuration: 10.0.0.2/32, MTU=1400, DNS=[1.1.1.1, 8.8.8.8]")
            Log.i(TAG, "=== VPN ESTABLISHMENT SUCCESS ===")
            
            vpnInterface
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception establishing VPN - permission denied", e)
            null
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal state establishing VPN - another VPN may be active", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error establishing VPN", e)
            null
        }
    }

    /**
     * Get forwarding statistics
     */
    fun getForwardingStats(): Pair<Long, Long> {
        val stats = packetForwarder?.getStats()
        return if (stats != null) {
            // Return packets and bytes forwarded
            Pair(stats.packetsForwarded, stats.bytesForwarded)
        } else {
            Pair(forwardedPackets.get(), forwardedBytes.get())
        }
    }

    private fun stopCapture() {
        Log.d(TAG, "Stopping packet capture")

        isRunning = false
        
        // Clear VpnSocketProtector
        VpnSocketProtector.clear()

        // Stop native forwarder first
        try {
            nativeForwarder?.stopForwarder()
            nativeForwarder = null
            Log.d(TAG, "Native forwarder stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping native forwarder", e)
        }

        // Stop Kotlin packet forwarder (if used)
        try {
            packetForwarder?.stop()
            packetForwarder = null
            Log.d(TAG, "Kotlin forwarder stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Kotlin forwarder", e)
        }

        // Cancel capture job
        captureJob?.cancel()
        captureJob = null

        // Stop packet processor
        packetProcessor.stopProcessing()

        // Close VPN interface
        try {
            vpnInterface?.close()
            Log.d(TAG, "VPN interface closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }
        vpnInterface = null

        // Stop foreground service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()

        Log.d(TAG, "Packet capture stopped successfully")
    }
    
    
    /**
     * Run connectivity tests to verify forwarder is working
     */
    private suspend fun runConnectivityTests() {
        Log.i(TAG, "=== Running Connectivity Tests ===")
        
        // Test 1: Ping 1.1.1.1
        scope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("ping -c 3 1.1.1.1")
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText()
                if (exitCode == 0) {
                    Log.i(TAG, "✓ Ping test PASSED")
                    Log.d(TAG, "Ping output: $output")
                } else {
                    Log.w(TAG, "✗ Ping test FAILED (exit code: $exitCode)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ping test error", e)
            }
        }
        
        delay(2000)
        
        // Test 2: DNS lookup
        scope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("nslookup example.com")
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText()
                if (exitCode == 0 && output.contains("example.com")) {
                    Log.i(TAG, "✓ DNS test PASSED")
                    Log.d(TAG, "DNS output: $output")
                } else {
                    Log.w(TAG, "✗ DNS test FAILED")
                }
            } catch (e: Exception) {
                Log.e(TAG, "DNS test error", e)
            }
        }
        
        delay(2000)
        
        // Test 3: HTTP request
        scope.launch(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec("curl -s -o /dev/null -w '%{http_code}' https://example.com")
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText()
                if (exitCode == 0 && output.contains("200")) {
                    Log.i(TAG, "✓ HTTP test PASSED")
                } else {
                    Log.w(TAG, "✗ HTTP test FAILED (exit code: $exitCode, output: $output)")
                }
            } catch (e: Exception) {
                Log.e(TAG, "HTTP test error", e)
            }
        }
        
        Log.i(TAG, "=== Connectivity Tests Complete ===")
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")
        
        stopCapture()
        nativeParser.destroyParser()
        scope.cancel()
        instance = null
        
        super.onDestroy()
    }
    
    // Public methods to access processor data
    fun getStats() = packetProcessor.stats
    fun getAppTalkers() = packetProcessor.getAppTalkers()
    fun getPacketProcessor() = packetProcessor
    
    /**
     * Forward a paused packet (called from UI when user clicks Forward)
     */
    fun forwardPausedPacket(packetData: ByteArray) {
        val vpn = vpnInterface ?: return
        try {
            val outputStream = FileOutputStream(vpn.fileDescriptor)
            outputStream.write(packetData, 0, packetData.size)
            outputStream.flush()
            forwardedPackets.incrementAndGet()
            forwardedBytes.addAndGet(packetData.size.toLong())
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding paused packet", e)
        }
    }

    override fun onRevoke() {
        Log.d(TAG, "VPN permission revoked")
        stopCapture()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_service_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when packet capture is active"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_service_notification_title))
            .setContentText(getString(R.string.vpn_service_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
