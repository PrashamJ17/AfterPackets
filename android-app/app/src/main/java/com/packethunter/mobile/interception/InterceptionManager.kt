package com.packethunter.mobile.interception

import android.util.Log
import com.packethunter.mobile.data.PacketInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.Charset
import java.security.MessageDigest
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
    }
    
    private val interceptsQueue = ConcurrentLinkedQueue<InterceptItem>()
    private val _intercepts = MutableStateFlow<List<InterceptItem>>(emptyList())
    val intercepts: StateFlow<List<InterceptItem>> = _intercepts.asStateFlow()
    
    private var isEnabled = false
    private var enabledFilters = mutableSetOf<InterceptFilterType>()
    
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
     */
    fun interceptIfMatches(packet: PacketInfo) {
        // Double-check authorization even if isEnabled is true
        if (!isEnabled || !AuthorizedTestingMode.isEnabled()) {
            return
        }
        
        // Null checks for safety
        if (packet.sourceIp.isEmpty() || packet.destIp.isEmpty()) {
            Log.w(TAG, "Skipping intercept - invalid packet addresses")
            return
        }
        
        val matchedFilter = matchesFilter(packet)
        if (matchedFilter != null) {
            val interceptItem = createInterceptItem(packet, matchedFilter)
            enqueueIntercept(interceptItem)
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
     */
    private fun enqueueIntercept(item: InterceptItem) {
        synchronized(interceptsQueue) {
            interceptsQueue.offer(item)
            
            // Maintain max capacity by removing oldest
            while (interceptsQueue.size > MAX_INTERCEPTS) {
                interceptsQueue.poll()
            }
            
            // Update StateFlow with current list
            _intercepts.value = interceptsQueue.toList().reversed() // Newest first
        }
        
        Log.d(TAG, "Intercepted ${item.filterType} packet: ${item.summary} (queue size: ${interceptsQueue.size})")
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

