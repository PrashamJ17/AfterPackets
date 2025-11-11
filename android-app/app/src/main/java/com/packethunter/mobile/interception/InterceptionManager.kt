package com.packethunter.mobile.interception

import android.util.Log
import com.packethunter.mobile.data.PacketInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages passive interception of network packets matching breakpoint filters
 * Non-blocking: packets are intercepted and queued without affecting forwarding
 */
class InterceptionManager {
    
    companion object {
        private const val TAG = "InterceptionManager"
        private const val MAX_INTERCEPTS = 200
        private const val MAX_PAYLOAD_PREVIEW = 4096 // 4KB max
        private const val MAX_INTERCEPT_SIZE = 1_000_000 // 1MB max payload size to intercept
        private const val INTERCEPT_TIMEOUT_MS = 30_000L // 30 seconds timeout
    }
    
    private val interceptsQueue = ConcurrentLinkedQueue<InterceptItem>()
    private val _intercepts = MutableStateFlow<List<InterceptItem>>(emptyList())
    val intercepts: StateFlow<List<InterceptItem>> = _intercepts.asStateFlow()
    
    private var isEnabled = false
    private var enabledFilters = mutableSetOf<InterceptFilterType>()
    
    // Track active interception sessions with timeout
    private val activeSessions = ConcurrentHashMap<String, InterceptSession>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var auditLogger: AuditLogger? = null
    
    /**
     * Initialize with AuditLogger for logging interception events
     */
    fun initialize(auditLogger: AuditLogger) {
        this.auditLogger = auditLogger
        Log.i(TAG, "✅ InterceptionManager initialized with audit logging")
    }
    
    /**
     * Enable or disable interception
     * REQUIRES: Authorized Testing Mode to be enabled
     */
    fun setEnabled(enabled: Boolean) {
        if (enabled && !AuthorizedTestingMode.isEnabled()) {
            Log.w(TAG, "❌ Cannot enable interception - Authorized Testing Mode is disabled")
            Log.w(TAG, "User must explicitly consent to packet interception in Settings")
            isEnabled = false
            return
        }
        
        isEnabled = enabled
        Log.i(TAG, "Interception ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Set which filter types to intercept
     * REQUIRES: Authorized Testing Mode to be enabled
     */
    fun setEnabledFilters(filters: Set<InterceptFilterType>) {
        if (!AuthorizedTestingMode.isEnabled() && filters.isNotEmpty()) {
            Log.w(TAG, "❌ Cannot set interception filters - Authorized Testing Mode is disabled")
            return
        }
        
        enabledFilters.clear()
        enabledFilters.addAll(filters)
        Log.d(TAG, "Enabled filters: ${enabledFilters.joinToString()}")
    }
    
    /**
     * Check if interception is enabled
     */
    fun isInterceptionEnabled(): Boolean = isEnabled
    
    /**
     * Process a parsed packet and intercept if it matches filters
     * This is called from PacketProcessor after parsing but before forwarding
     * 
     * SAFETY CHECKS:
     * - Verifies Authorized Testing Mode is enabled
     * - Non-blocking: does not delay packet forwarding
     * - Handles null payloads gracefully
     * - Enforces queue limits to prevent memory exhaustion
     * - Wraps entire handler in try/catch
     * - Only intercepts plaintext protocols (DNS, HTTP)
     * - Enforces size and timeout limits
     */
    fun interceptIfMatches(packet: PacketInfo) {
        try {
            // Double-check authorization even if isEnabled is true
            if (!isEnabled || !AuthorizedTestingMode.isEnabled()) {
                return
            }
            
            // Null checks for safety
            if (packet.sourceIp.isEmpty() || packet.destIp.isEmpty()) {
                Log.w(TAG, "Skipping intercept - invalid packet addresses")
                return
            }
            
            // Size check: skip oversized payloads
            if (packet.payload != null && packet.payload.size > MAX_INTERCEPT_SIZE) {
                Log.w(TAG, "Skipping intercept - payload too large: ${packet.payload.size} bytes > $MAX_INTERCEPT_SIZE")
                return
            }
            
            val matchedFilter = matchesFilter(packet)
            if (matchedFilter != null) {
                // Only intercept plaintext protocols
                if (isPlaintextProtocol(packet, matchedFilter)) {
                    val interceptItem = createInterceptItem(packet, matchedFilter)
                    val sessionId = "${packet.timestamp}_${packet.sourcePort}"
                    enqueueIntercept(interceptItem, sessionId)
                } else {
                    Log.d(TAG, "Skipping encrypted protocol: ${packet.protocol} on port ${packet.destPort}")
                    auditLogger?.logFilterDisabled("Encrypted - cannot intercept: ${packet.protocol}")
                }
            }
        } catch (e: Exception) {
            // CRITICAL: Never let exceptions escape
            Log.e(TAG, "❌ Exception in interceptIfMatches - auto-resuming flow", e)
            auditLogger?.logConsentRevoked() // Log error event
        }
    }
    
    /**
     * Check if protocol is plaintext and safe to intercept
     * Only allow DNS, HTTP (port 80/8080), and optionally parsed HTTP headers
     * Deny TLS/HTTPS unless user has proper proxy setup
     */
    private fun isPlaintextProtocol(packet: PacketInfo, filterType: InterceptFilterType): Boolean {
        return when (filterType) {
            InterceptFilterType.DNS -> true // DNS is plaintext
            InterceptFilterType.HTTP -> {
                // Only allow HTTP on known plaintext ports
                packet.destPort == 80 || packet.destPort == 8080 ||
                packet.sourcePort == 80 || packet.sourcePort == 8080
            }
            InterceptFilterType.TLS -> {
                // Skip TLS - encrypted, cannot intercept without proxy
                false
            }
            InterceptFilterType.ICMP -> true // ICMP is plaintext
            else -> false
        }
    }
    
    /**
     * Check if packet matches any enabled filter
     */
    private fun matchesFilter(packet: PacketInfo): InterceptFilterType? {
        if (enabledFilters.contains(InterceptFilterType.ALL)) {
            return InterceptFilterType.ALL
        }
        
        // HTTP filter
        if (enabledFilters.contains(InterceptFilterType.HTTP)) {
            if (packet.destPort == 80 || packet.sourcePort == 80 ||
                packet.protocol == "HTTP" ||
                packet.httpMethod != null) {
                return InterceptFilterType.HTTP
            }
        }
        
        // DNS filter
        if (enabledFilters.contains(InterceptFilterType.DNS)) {
            if (packet.destPort == 53 || packet.sourcePort == 53 ||
                packet.protocol == "DNS" ||
                packet.dnsQuery != null || packet.dnsResponse != null) {
                return InterceptFilterType.DNS
            }
        }
        
        // TLS filter
        if (enabledFilters.contains(InterceptFilterType.TLS)) {
            if (packet.destPort == 443 || packet.sourcePort == 443 ||
                packet.protocol == "HTTPS" ||
                packet.tlsSni != null || packet.tlsCertFingerprint != null) {
                return InterceptFilterType.TLS
            }
        }
        
        // ICMP filter
        if (enabledFilters.contains(InterceptFilterType.ICMP)) {
            if (packet.protocol == "ICMP") {
                return InterceptFilterType.ICMP
            }
        }
        
        return null
    }
    
    /**
     * Create an InterceptItem from a PacketInfo
     */
    private fun createInterceptItem(packet: PacketInfo, filterType: InterceptFilterType): InterceptItem {
        val summary = generateSummary(packet, filterType)
        val payload = packet.payload
        
        // Limit payload preview to 4KB
        val previewSize = if (payload != null) {
            minOf(payload.size, MAX_PAYLOAD_PREVIEW)
        } else {
            0
        }
        
        val payloadPreview = if (payload != null && previewSize > 0) {
            payload.copyOfRange(0, previewSize)
        } else {
            null
        }
        
        val (hex, ascii) = if (payloadPreview != null) {
            formatPayloadPreview(payloadPreview)
        } else {
            Pair("", "")
        }
        
        // Compute hash of original payload
        val originalHash = computeHash(packet.payload)
        
        return InterceptItem(
            timestamp = packet.timestamp,
            protocol = packet.protocol,
            sourceIp = packet.sourceIp,
            destIp = packet.destIp,
            sourcePort = packet.sourcePort,
            destPort = packet.destPort,
            summary = summary,
            payloadPreview = payloadPreview,
            payloadHex = hex,
            payloadAscii = ascii,
            filterType = filterType.name,
            sni = packet.tlsSni,
            originalHash = originalHash
        )
    }
    
    /**
     * Generate a human-readable summary for the intercepted packet
     */
    private fun generateSummary(packet: PacketInfo, filterType: InterceptFilterType): String {
        return when (filterType) {
            InterceptFilterType.HTTP -> {
                when {
                    packet.httpMethod != null && packet.httpUrl != null -> {
                        "${packet.httpMethod} ${packet.httpUrl}"
                    }
                    packet.httpMethod != null -> {
                        "${packet.httpMethod} request"
                    }
                    else -> "HTTP traffic on port ${packet.destPort}"
                }
            }
            InterceptFilterType.DNS -> {
                when {
                    packet.dnsQuery != null -> "DNS query: ${packet.dnsQuery}"
                    packet.dnsResponse != null -> "DNS response: ${packet.dnsResponse}"
                    else -> "DNS traffic on port ${packet.destPort}"
                }
            }
            InterceptFilterType.TLS -> {
                when {
                    packet.tlsSni != null -> "TLS handshake: ${packet.tlsSni}"
                    else -> "TLS traffic on port ${packet.destPort}"
                }
            }
            InterceptFilterType.ICMP -> {
                "ICMP packet: ${packet.flags}"
            }
            else -> {
                "${packet.protocol} packet: ${packet.sourceIp}:${packet.sourcePort} -> ${packet.destIp}:${packet.destPort}"
            }
        }
    }
    
    /**
     * Format payload as hex and ASCII strings
     */
    private fun formatPayloadPreview(payload: ByteArray): Pair<String, String> {
        val hex = StringBuilder()
        val ascii = StringBuilder()
        
        for (i in payload.indices) {
            // Hex representation
            hex.append(String.format("%02X ", payload[i]))
            if ((i + 1) % 16 == 0) {
                hex.append("\n")
            }
            
            // ASCII representation
            val char = payload[i].toInt().toChar()
            ascii.append(if (char.isLetterOrDigit() || char.isWhitespace() || char in ".,!?;:") char else '.')
        }
        
        return Pair(hex.toString().trim(), ascii.toString())
    }
    
    /**
     * Enqueue an intercepted item (thread-safe)
     * Starts timeout timer for auto-resume
     */
    private fun enqueueIntercept(item: InterceptItem, sessionId: String) {
        synchronized(interceptsQueue) {
            interceptsQueue.offer(item)
            
            // Maintain max capacity by removing oldest
            while (interceptsQueue.size > MAX_INTERCEPTS) {
                val removed = interceptsQueue.poll()
                removed?.let {
                    activeSessions.remove("${it.timestamp}_${it.sourcePort}")
                }
            }
            
            // Update StateFlow with current list
            _intercepts.value = interceptsQueue.toList().reversed() // Newest first
        }
        
        // Create session and start timeout
        val session = InterceptSession(sessionId, item, System.currentTimeMillis())
        activeSessions[sessionId] = session
        
        // Start timeout job
        scope.launch {
            delay(INTERCEPT_TIMEOUT_MS)
            if (activeSessions.containsKey(sessionId)) {
                Log.w(TAG, "⏱️ Intercept timeout for session $sessionId - auto-resuming")
                resumeSessionUnmodified(sessionId)
                auditLogger?.logFilterDisabled("intercept_timeout: $sessionId")
            }
        }
        
        Log.d(TAG, "Intercepted ${item.filterType} packet: ${item.summary} (queue size: ${interceptsQueue.size})")
    }
    
    /**
     * Resume session with unmodified original packet
     * Called on timeout or user cancel
     */
    fun resumeSessionUnmodified(sessionId: String) {
        try {
            val session = activeSessions.remove(sessionId)
            if (session != null) {
                Log.i(TAG, "✅ Resumed session $sessionId unmodified")
                auditLogger?.logFilterDisabled("session_resumed_unmodified: $sessionId")
            } else {
                Log.w(TAG, "Session $sessionId not found in active sessions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming session $sessionId", e)
        }
    }
    
    /**
     * Resume session with modified packet
     * Called when user accepts/modifies intercepted packet
     */
    fun resumeSessionModified(sessionId: String, modifiedPayload: ByteArray) {
        try {
            val session = activeSessions.remove(sessionId)
            if (session != null) {
                val originalHash = computeHash(session.item.payloadPreview)
                val modifiedHash = computeHash(modifiedPayload)
                
                // Log modification to audit trail
                auditLogger?.logPacketModification(
                    originalHash = originalHash,
                    modifiedHash = modifiedHash,
                    sourceIp = session.item.sourceIp,
                    destIp = session.item.destIp,
                    protocol = session.item.protocol,
                    modificationType = "user_edit"
                )
                
                Log.i(TAG, "✅ Resumed session $sessionId with modifications (${modifiedPayload.size} bytes)")
            } else {
                Log.w(TAG, "Session $sessionId not found in active sessions")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming modified session $sessionId", e)
            // On error, try to resume unmodified
            resumeSessionUnmodified(sessionId)
        }
    }
    
    /**
     * Clear all intercepted items
     */
    fun clearIntercepts() {
        synchronized(interceptsQueue) {
            interceptsQueue.clear()
            _intercepts.value = emptyList()
        }
        Log.d(TAG, "Cleared all intercepts")
    }
    
    /**
     * Get current intercept count
     */
    fun getInterceptCount(): Int = interceptsQueue.size
    
    /**
     * Compute SHA-256 hash of payload
     */
    private fun computeHash(data: ByteArray?): String {
        if (data == null || data.isEmpty()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing hash", e)
            ""
        }
    }
}

