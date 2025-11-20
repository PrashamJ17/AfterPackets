package com.packethunter.mobile.capture

import android.util.Log
import com.packethunter.mobile.data.*
import com.packethunter.mobile.interception.InterceptionManager
import com.packethunter.mobile.websocket.WebSocketServer
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Processes captured packets: parses, stores, and generates statistics
 */
class PacketProcessor(
    private val database: PacketDatabase,
    private val nativeParser: NativePacketParser,
    private val appTracker: AppTracker,
    private val networkStatsTracker: NetworkStatsTracker? = null
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    // Use unlimited capacity with drop oldest to prevent blocking VPN forwarding
    private val packetChannel = Channel<ByteArray>(capacity = Channel.UNLIMITED)
    private var processingJob: Job? = null
    
    private val _stats = MutableStateFlow(CaptureStats())
    val stats: StateFlow<CaptureStats> = _stats
    
    private val packetCount = AtomicLong(0)
    private val byteCount = AtomicLong(0)
    private var startTime = System.currentTimeMillis()
    private var lastStatsUpdate = System.currentTimeMillis()
    
    // Ring buffer for in-memory packets
    private val recentPackets = mutableListOf<PacketInfo>()
    private val maxRecentPackets = 1000
    
    // Sliding window for real-time metrics (last 5 seconds)
    private val packetTimestamps = mutableListOf<Pair<Long, Int>>() // timestamp, bytes
    private val SLIDING_WINDOW_MS = 5000L // 5 seconds
    
    // Detection engine
    private lateinit var detectionEngine: DetectionEngine
    
    // WebSocket server for real-time streaming
    private val webSocketServer = WebSocketServer(port = 8080)
    
    // Passive interception manager
    val interceptionManager = InterceptionManager()
    
    companion object {
        private const val TAG = "PacketProcessor"
        private const val STATS_UPDATE_INTERVAL_MS = 1000L
        private const val BROADCAST_INTERVAL_MS = 500L // Broadcast every 500ms
    }
    
    init {
        // Initialize detection engine
        scope.launch {
            detectionEngine = DetectionEngine(database)
        }
        
        // Start WebSocket server
        webSocketServer.start()
        Log.d(TAG, "WebSocket server started for real-time streaming")
    }

    fun startProcessing() {
        Log.d(TAG, "Starting packet processing")
        startTime = System.currentTimeMillis()
        lastStatsUpdate = startTime
        
        processingJob = scope.launch {
            processPacketQueue()
        }
        
        // Start stats updater
        scope.launch {
            updateStatsLoop()
        }
        
        // Start periodic NetworkStatsManager updates (preferred method)
        scope.launch {
            while (processingJob?.isActive == true) {
                delay(2000) // Update every 2 seconds
                try {
                    networkStatsTracker?.updateStats()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating network stats", e)
                }
            }
        }
        
        // Fallback: Start periodic TrafficStats updates (works without PACKAGE_USAGE_STATS)
        scope.launch {
            while (processingJob?.isActive == true) {
                delay(2000) // Update every 2 seconds
                try {
                    appTracker.updateTrafficStats()
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating traffic stats", e)
                }
            }
        }
    }

    fun stopProcessing() {
        Log.d(TAG, "Stopping packet processing")
        processingJob?.cancel()
        processingJob = null
        webSocketServer.stop()
    }

    /**
     * Called from JNI native thread - MUST return quickly!
     * Offloads processing to IO dispatcher to avoid blocking native forwarder.
     */
    suspend fun processPacket(data: ByteArray) {
        try {
            // Immediately offload to IO dispatcher and return to native thread
            scope.launch(Dispatchers.IO) {
                try {
                    // Send to channel for processing (non-blocking)
                    val result = packetChannel.trySend(data)
                    if (result.isClosed) {
                        Log.w(TAG, "Packet channel closed, dropping packet")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception in packet processing offload", e)
                }
            }
        } catch (e: Exception) {
            // Critical: never let exceptions escape to JNI layer
            Log.e(TAG, "CRITICAL: Exception in JNI callback wrapper", e)
        }
    }

    private suspend fun processPacketQueue() {
        var processedCount = 0L
        for (data in packetChannel) {
            try {
                processedCount++
                // Log first few packets
                if (processedCount <= 5) {
                    Log.i(TAG, "Processing packet from queue #$processedCount (${data.size} bytes)")
                }
                
                // Parse packet using native parser
                val parsed = nativeParser.parsePacket(data)
                
                if (parsed != null && parsed.protocol.isNotEmpty()) {
                    // Log first successful parse
                    if (processedCount <= 5) {
                        Log.i(TAG, "✅ Successfully parsed packet #$processedCount: ${parsed.protocol} ${parsed.sourceIp} -> ${parsed.destIp}")
                    }
                    // Convert to PacketInfo
                    val packetInfo = createPacketInfo(parsed)
                    
                    // Store in database (with error handling)
                    try {
                        database.packetDao().insertPacket(packetInfo)
                        if (packetCount.get() % 100 == 0L) {
                            Log.d(TAG, "Stored ${packetCount.get()} packets in database")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to store packet in database", e)
                    }
                    
                    // Add to recent packets ring buffer
                    synchronized(recentPackets) {
                        recentPackets.add(packetInfo)
                        if (recentPackets.size > maxRecentPackets) {
                            recentPackets.removeAt(0)
                        }
                    }
                    
                    // Update counters
                    packetCount.incrementAndGet()
                    byteCount.addAndGet(parsed.length.toLong())
                    
                    // Add to sliding window
                    synchronized(packetTimestamps) {
                        packetTimestamps.add(Pair(System.currentTimeMillis(), parsed.length))
                    }
                    
                    // Track app usage
                    val appPackage = appTracker.findAppForConnection(
                        parsed.sourceIp, parsed.sourcePort,
                        parsed.destIp, parsed.destPort
                    )
                    if (appPackage != null) {
                        Log.d(TAG, "Tracking packet for app: $appPackage (${parsed.sourcePort} -> ${parsed.destIp}:${parsed.destPort})")
                        appTracker.trackPacket(
                            packageName = appPackage,
                            isOutbound = true, // Assume outbound from VPN
                            bytes = parsed.length,
                            remoteIp = parsed.destIp,
                            protocol = parsed.protocol
                        )
                    } else {
                        Log.d(TAG, "Could not find app for packet: ${parsed.sourceIp}:${parsed.sourcePort} -> ${parsed.destIp}:${parsed.destPort}")
                    }
                    
                    // Run detection rules
                    if (::detectionEngine.isInitialized) {
                        detectionEngine.checkPacket(packetInfo)
                    }
                    
                    // Passive interception (non-blocking, runs on IO dispatcher)
                    scope.launch(Dispatchers.IO) {
                        try {
                            interceptionManager.interceptIfMatches(packetInfo)
                        } catch (e: Exception) {
                            // Silently ignore interception errors - don't affect forwarding
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Interception error (non-critical)", e)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing packet", e)
            }
        }
    }

    private fun createPacketInfo(parsed: ParsedPacketData): PacketInfo {
        // Determine direction based on source IP
        // VPN address is 10.0.0.2, packets FROM this are outbound, TO this are inbound
        // Also check for loopback addresses (127.0.0.1) which are typically inbound
        val direction = when {
            parsed.sourceIp == "10.0.0.2" -> "outbound"
            parsed.destIp == "10.0.0.2" -> "inbound"
            parsed.destIp == "127.0.0.1" -> "inbound"
            parsed.sourceIp == "127.0.0.1" -> "outbound"
            else -> "outbound" // Default to outbound for external traffic
        }
        
        return PacketInfo(
            timestamp = System.currentTimeMillis(),
            protocol = parsed.protocol,
            sourceIp = parsed.sourceIp,
            destIp = parsed.destIp,
            sourcePort = parsed.sourcePort,
            destPort = parsed.destPort,
            length = parsed.length,
            flags = parsed.flags,
            payload = parsed.payload,
            payloadPreview = parsed.payloadPreview,
            sessionId = generateSessionId(parsed),
            direction = direction,
            httpMethod = parsed.httpMethod,
            httpUrl = parsed.httpUrl,
            dnsQuery = parsed.dnsQuery,
            dnsResponse = parsed.dnsResponse,
            tlsSni = parsed.tlsSni
        )
    }
    
    /**
     * Create PacketInfo for breakpoint checking (lightweight, synchronous)
     * Returns null if parsing fails - caller should forward packet anyway
     */
    fun createPacketInfoForBreakpoint(parsed: ParsedPacketData): PacketInfo? {
        return try {
            // Determine direction based on source/dest IP
            // VPN address is 10.0.0.2, packets FROM this are outbound, TO this are inbound
            val direction = when {
                parsed.sourceIp == "10.0.0.2" -> "outbound"
                parsed.destIp == "10.0.0.2" -> "inbound"
                parsed.destIp == "127.0.0.1" -> "inbound"
                parsed.sourceIp == "127.0.0.1" -> "outbound"
                else -> "outbound" // Default to outbound for external traffic
            }
            
            PacketInfo(
                timestamp = System.currentTimeMillis(),
                protocol = parsed.protocol,
                sourceIp = parsed.sourceIp,
                destIp = parsed.destIp,
                sourcePort = parsed.sourcePort,
                destPort = parsed.destPort,
                length = parsed.length,
                flags = parsed.flags ?: "",
                payload = parsed.payload,
                payloadPreview = parsed.payloadPreview ?: "",
                sessionId = generateSessionId(parsed),
                direction = direction,
                httpMethod = parsed.httpMethod,
                httpUrl = parsed.httpUrl,
                dnsQuery = parsed.dnsQuery,
                dnsResponse = parsed.dnsResponse,
                tlsSni = parsed.tlsSni
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun generateSessionId(parsed: ParsedPacketData): String {
        return "${parsed.sourceIp}:${parsed.sourcePort}-${parsed.destIp}:${parsed.destPort}"
    }

    private suspend fun updateStatsLoop() {
        while (true) {
            delay(STATS_UPDATE_INTERVAL_MS)
            updateStats()
        }
    }

    private suspend fun updateStats() {
        val now = System.currentTimeMillis()
        val elapsedSeconds = (now - startTime) / 1000.0
        
        if (elapsedSeconds < 0.1) return
        
        val currentPackets = packetCount.get()
        val currentBytes = byteCount.get()
        
        // Calculate real-time rates from sliding window
        var recentPacketCount = 0
        var recentByteCount = 0L
        val cutoffTime = now - SLIDING_WINDOW_MS
        
        synchronized(packetTimestamps) {
            // Remove old entries
            packetTimestamps.removeAll { it.first < cutoffTime }
            
            // Count recent packets and bytes
            packetTimestamps.forEach { (_, bytes) ->
                recentPacketCount++
                recentByteCount += bytes
            }
        }
        
        // Calculate rates per second (sliding window is 5 seconds)
        val windowSeconds = SLIDING_WINDOW_MS / 1000.0
        val pps = recentPacketCount / windowSeconds
        val bps = recentByteCount / windowSeconds
        
        // Get protocol distribution
        val protocolDist = try {
            database.packetDao().getProtocolDistribution()
                .associate { it.protocol to it.count }
        } catch (e: Exception) {
            emptyMap()
        }
        
        // Get top talkers
        val talkers = try {
            database.packetDao().getTopTalkers(10)
                .map { IpTalker(it.ip, it.count, it.bytes) }
        } catch (e: Exception) {
            emptyList()
        }
        
        val newStats = CaptureStats(
            totalPackets = currentPackets,
            packetsPerSecond = pps,
            totalBytes = currentBytes,
            bytesPerSecond = bps,
            protocolDistribution = protocolDist,
            topTalkers = talkers,
            startTime = startTime,
            lastUpdate = now
        )
        
        _stats.value = newStats
        
        // Log stats for debugging
        if (currentPackets % 100 == 0L || currentPackets == 1L) {
            Log.d(TAG, "Stats update: packets=$currentPackets, bytes=$currentBytes, pps=$pps, bps=$bps, topTalkers=${talkers.size}")
        }
        
        // Broadcast to connected desktop clients
        webSocketServer.broadcastPackets(getRecentPackets(), newStats)
    }

    fun getRecentPackets(): List<PacketInfo> {
        return synchronized(recentPackets) {
            recentPackets.toList()
        }
    }
    
    fun getAppTalkers(): List<com.packethunter.mobile.data.AppTalker> {
        // Prefer NetworkStatsManager if available (more accurate)
        val networkStatsTalkers = networkStatsTracker?.getAppTalkers()
        if (networkStatsTalkers != null && networkStatsTalkers.isNotEmpty()) {
            Log.d(TAG, "getAppTalkers() returning ${networkStatsTalkers.size} apps from NetworkStatsManager")
            return networkStatsTalkers
        }
        
        // Fallback to TrafficStats
        val talkers = appTracker.getAppTalkers()
        Log.d(TAG, "getAppTalkers() returning ${talkers.size} apps from TrafficStats")
        return talkers
    }
}

/**
 * Detection engine for running rules and generating alerts
 */
class DetectionEngine(private val database: PacketDatabase) {
    private var rules: List<DetectionRule> = emptyList()
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Track state for time-window based rules
    private val recentBytes = mutableMapOf<String, MutableList<Pair<Long, Long>>>()
    private val certFingerprints = mutableMapOf<String, String>()
    
    companion object {
        private const val TAG = "DetectionEngine"
    }
    
    init {
        scope.launch {
            loadRules()
        }
    }
    
    private suspend fun loadRules() {
        rules = database.ruleDao().getEnabledRules()
        Log.d(TAG, "Loaded ${rules.size} detection rules")
    }
    
    suspend fun checkPacket(packet: PacketInfo) {
        // Check built-in detections
        checkForUnencryptedHttp(packet)
        checkForForeignServer(packet)
        checkForMITM(packet)
        checkForDataExfiltration(packet)
        
        // Check custom rules
        for (rule in rules) {
            checkRule(rule, packet)
        }
    }
    
    private suspend fun checkForUnencryptedHttp(packet: PacketInfo) {
        if (packet.protocol == "TCP" && packet.destPort == 80 && packet.httpMethod != null) {
            val alert = Alert(
                timestamp = System.currentTimeMillis(),
                severity = "medium",
                type = "http",
                title = "Unencrypted HTTP Traffic",
                description = "${packet.httpMethod} ${packet.httpUrl ?: "request"} over HTTP (port 80) - data not encrypted",
                relatedPacketIds = packet.id.toString()
            )
            database.alertDao().insertAlert(alert)
            Log.i(TAG, "Alert: Unencrypted HTTP to ${packet.destIp}")
        }
    }
    
    private suspend fun checkForForeignServer(packet: PacketInfo) {
        // Check if connecting to foreign IP (non-RFC1918 private addresses)
        if (packet.direction == "outbound" && !isPrivateIp(packet.destIp)) {
            // Only alert for first connection to this IP
            val key = "foreign_${packet.destIp}"
            if (!certFingerprints.containsKey(key)) {
                certFingerprints[key] = "alerted"
                
                val alert = Alert(
                    timestamp = System.currentTimeMillis(),
                    severity = "low",
                    type = "foreign",
                    title = "Foreign Server Connection",
                    description = "Connection to ${packet.destIp}:${packet.destPort} (${packet.protocol})",
                    relatedPacketIds = packet.id.toString()
                )
                database.alertDao().insertAlert(alert)
                Log.i(TAG, "Alert: Foreign connection to ${packet.destIp}")
            }
        }
    }
    
    private fun isPrivateIp(ip: String): Boolean {
        val parts = ip.split(".").mapNotNull { it.toIntOrNull() }
        if (parts.size != 4) return false
        
        // 10.0.0.0/8
        if (parts[0] == 10) return true
        // 172.16.0.0/12
        if (parts[0] == 172 && parts[1] in 16..31) return true
        // 192.168.0.0/16
        if (parts[0] == 192 && parts[1] == 168) return true
        // localhost
        if (parts[0] == 127) return true
        
        return false
    }
    
    private suspend fun checkForMITM(packet: PacketInfo) {
        if (packet.tlsCertFingerprint != null && packet.tlsSni != null) {
            val knownFingerprint = certFingerprints[packet.tlsSni]
            
            if (knownFingerprint != null && knownFingerprint != packet.tlsCertFingerprint) {
                // Certificate changed!
                val alert = Alert(
                    timestamp = System.currentTimeMillis(),
                    severity = "high",
                    type = "mitm",
                    title = "Possible MITM Attack",
                    description = "TLS certificate fingerprint changed for ${packet.tlsSni}",
                    relatedPacketIds = packet.id.toString()
                )
                database.alertDao().insertAlert(alert)
            }
            
            certFingerprints[packet.tlsSni!!] = packet.tlsCertFingerprint!!
        }
    }
    
    private suspend fun checkForDataExfiltration(packet: PacketInfo) {
        if (packet.direction == "outbound") {
            val key = packet.destIp
            val now = System.currentTimeMillis()
            
            val history = recentBytes.getOrPut(key) { mutableListOf() }
            history.add(Pair(now, packet.length.toLong()))
            
            // Remove old entries (older than 10 seconds)
            history.removeAll { (timestamp, _) -> now - timestamp > 10000 }
            
            // Check if total bytes in last 10 seconds exceeds 1MB
            val totalBytes = history.sumOf { it.second }
            if (totalBytes > 1_000_000) {
                val alert = Alert(
                    timestamp = now,
                    severity = "medium",
                    type = "data_exfil",
                    title = "Large Data Transfer",
                    description = "Sent ${totalBytes / 1024 / 1024}MB to ${packet.destIp} in 10 seconds",
                    relatedPacketIds = packet.id.toString()
                )
                database.alertDao().insertAlert(alert)
                
                // Clear history to avoid duplicate alerts
                history.clear()
            }
        }
    }
    
    private suspend fun checkRule(rule: DetectionRule, packet: PacketInfo) {
        // Custom rule checking logic
        // This is a simplified version - would need more sophisticated implementation
        when (rule.metric) {
            "packet_size" -> {
                if (packet.length > rule.threshold) {
                    triggerAlert(rule, packet, "Packet size: ${packet.length}")
                }
            }
        }
    }
    
    private suspend fun triggerAlert(rule: DetectionRule, packet: PacketInfo, details: String) {
        val alert = Alert(
            timestamp = System.currentTimeMillis(),
            severity = rule.severity,
            type = "custom_rule",
            title = rule.name,
            description = "$details - ${rule.action}",
            relatedPacketIds = packet.id.toString()
        )
        database.alertDao().insertAlert(alert)
    }
}
