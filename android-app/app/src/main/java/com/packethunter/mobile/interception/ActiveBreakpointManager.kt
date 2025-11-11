package com.packethunter.mobile.interception

import android.util.Log
import com.packethunter.mobile.data.PacketInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * Manages Active Breakpoint Mode - pauses matching packets for manual review
 */
class ActiveBreakpointManager {
    
    companion object {
        private const val TAG = "ActiveBreakpointManager"
        private const val MAX_PAUSED = 5 // Maximum paused packets at once
        private const val AUTO_FORWARD_DELAY_MS = 30000L // 30 seconds (configurable)
    }
    
    private val pausedQueue = ConcurrentLinkedQueue<PausedIntercept>()
    private val _pausedIntercepts = MutableStateFlow<List<PausedIntercept>>(emptyList())
    val pausedIntercepts: StateFlow<List<PausedIntercept>> = _pausedIntercepts.asStateFlow()
    
    private val historyQueue = ConcurrentLinkedQueue<BreakpointHistory>()
    private val _history = MutableStateFlow<List<BreakpointHistory>>(emptyList())
    val history: StateFlow<List<BreakpointHistory>> = _history.asStateFlow()
    
    // Connection key -> CompletableDeferred<Action> for async pause resolution
    private val pendingActions = ConcurrentHashMap<String, CompletableDeferred<BreakpointAction>>()
    
    private var isEnabled = false
    private var enabledFilters = mutableSetOf<InterceptFilterType>()
    private val idGenerator = AtomicLong(1)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Configurable timeout (default 30s)
    var autoForwardTimeoutMs: Long = AUTO_FORWARD_DELAY_MS
        set(value) {
            field = value.coerceIn(1000L, 300000L) // 1s to 5min
        }
    
    /**
     * Enable or disable active breakpoint mode
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.d(TAG, "Active Breakpoint Mode ${if (enabled) "enabled" else "disabled"}")
        
        // If disabling, auto-forward all paused packets
        if (!enabled) {
            autoForwardAll()
        }
    }
    
    /**
     * Set which filter types to pause
     */
    fun setEnabledFilters(filters: Set<InterceptFilterType>) {
        enabledFilters.clear()
        enabledFilters.addAll(filters)
        Log.d(TAG, "Breakpoint filters: ${enabledFilters.joinToString()}")
    }
    
    /**
     * Check if active breakpoint mode is enabled
     */
    fun isBreakpointEnabled(): Boolean = isEnabled
    
    /**
     * Check if a packet should be paused based on filters
     */
    fun shouldPausePacket(packet: PacketInfo): Boolean {
        if (!isEnabled) return false
        
        // Check if packet matches any enabled filter
        return matchesFilter(packet) != null
    }
    
    /**
     * Pause a packet - returns CompletableDeferred<Action> that resolves when user decides
     * Returns null if packet should be forwarded immediately
     */
    fun pausePacket(
        packet: PacketInfo,
        rawPacketData: ByteArray
    ): CompletableDeferred<BreakpointAction>? {
        if (!isEnabled) return null
        
        val matchedFilter = matchesFilter(packet)
        if (matchedFilter == null) return null
        
        val connKey = "${packet.sourceIp}:${packet.sourcePort}-${packet.destIp}:${packet.destPort}-${packet.protocol}"
        
        // Check if we've reached max paused limit
        synchronized(pausedQueue) {
            // Auto-forward oldest if at limit
            while (pausedQueue.size >= MAX_PAUSED) {
                val oldest = pausedQueue.poll()
                if (oldest != null) {
                    val oldConnKey = "${oldest.sourceIp}:${oldest.sourcePort}-${oldest.destIp}:${oldest.destPort}-${oldest.protocol}"
                    pendingActions[oldConnKey]?.complete(BreakpointAction.AUTO_FORWARDED)
                    pendingActions.remove(oldConnKey)
                    autoForward(oldest, BreakpointAction.AUTO_FORWARDED)
                }
            }
            
            // Create deferred for this connection
            val deferred = CompletableDeferred<BreakpointAction>()
            pendingActions[connKey] = deferred
            
            // Create paused intercept
            val paused = PausedIntercept(
                id = idGenerator.getAndIncrement(),
                timestamp = packet.timestamp,
                protocol = packet.protocol,
                sourceIp = packet.sourceIp,
                destIp = packet.destIp,
                sourcePort = packet.sourcePort,
                destPort = packet.destPort,
                summary = generateSummary(packet, matchedFilter),
                filterType = matchedFilter.name,
                rawPacketData = rawPacketData.copyOf(),
                connKey = connKey
            )
            
            pausedQueue.offer(paused)
            updatePausedList()
            
            // Auto-forward after timeout
            scope.launch {
                delay(autoForwardTimeoutMs)
                if (deferred.isActive) {
                    deferred.complete(BreakpointAction.AUTO_FORWARDED)
                    pendingActions.remove(connKey)
                    forwardPaused(paused.id) { } // Forward callback
                }
            }
            
            Log.d(TAG, "Paused packet: ${paused.summary} (${pausedQueue.size} paused)")
            return deferred
        }
    }
    
    /**
     * Forward a paused packet
     */
    fun forwardPaused(id: Long, forwardCallback: (ByteArray) -> Unit): Boolean {
        synchronized(pausedQueue) {
            val paused = pausedQueue.firstOrNull { it.id == id }
            if (paused != null) {
                pausedQueue.remove(paused)
                updatePausedList()
                
                // Resolve deferred
                pendingActions[paused.connKey]?.complete(BreakpointAction.FORWARDED)
                pendingActions.remove(paused.connKey)
                
                // Forward the packet
                try {
                    forwardCallback(paused.rawPacketData)
                    addToHistory(paused, BreakpointAction.FORWARDED)
                    Log.d(TAG, "Forwarded paused packet: ${paused.summary}")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error forwarding paused packet", e)
                    return false
                }
            }
            return false
        }
    }
    
    /**
     * Forward modified packet
     */
    fun forwardModified(id: Long, modifiedData: ByteArray, forwardCallback: (ByteArray) -> Unit): Boolean {
        synchronized(pausedQueue) {
            val paused = pausedQueue.firstOrNull { it.id == id }
            if (paused != null) {
                pausedQueue.remove(paused)
                updatePausedList()
                
                // Resolve deferred
                pendingActions[paused.connKey]?.complete(BreakpointAction.FORWARDED_MODIFIED)
                pendingActions.remove(paused.connKey)
                
                // Forward modified packet
                try {
                    forwardCallback(modifiedData)
                    addToHistory(paused, BreakpointAction.FORWARDED_MODIFIED)
                    Log.d(TAG, "Forwarded modified packet: ${paused.summary}")
                    return true
                } catch (e: Exception) {
                    Log.e(TAG, "Error forwarding modified packet", e)
                    return false
                }
            }
            return false
        }
    }
    
    /**
     * Drop a paused packet
     */
    fun dropPaused(id: Long): Boolean {
        synchronized(pausedQueue) {
            val paused = pausedQueue.firstOrNull { it.id == id }
            if (paused != null) {
                pausedQueue.remove(paused)
                updatePausedList()
                
                // Resolve deferred
                pendingActions[paused.connKey]?.complete(BreakpointAction.DROPPED)
                pendingActions.remove(paused.connKey)
                
                addToHistory(paused, BreakpointAction.DROPPED)
                Log.d(TAG, "Dropped paused packet: ${paused.summary}")
                return true
            }
            return false
        }
    }
    
    /**
     * Auto-forward a paused packet (when timer expires)
     */
    private fun autoForward(paused: PausedIntercept, action: BreakpointAction) {
        addToHistory(paused, action)
        Log.d(TAG, "Auto-forwarded paused packet: ${paused.summary}")
    }
    
    /**
     * Auto-forward all paused packets (when mode is disabled)
     */
    private fun autoForwardAll() {
        synchronized(pausedQueue) {
            while (pausedQueue.isNotEmpty()) {
                val paused = pausedQueue.poll()
                if (paused != null) {
                    autoForward(paused, BreakpointAction.AUTO_FORWARDED)
                }
            }
            updatePausedList()
        }
    }
    
    /**
     * Check for expired packets and auto-forward them
     * Should be called periodically
     */
    fun checkAndAutoForwardExpired(forwardCallback: (ByteArray) -> Unit) {
        synchronized(pausedQueue) {
            val expired = pausedQueue.filter { it.isExpired() }
            expired.forEach { paused ->
                pausedQueue.remove(paused)
                forwardCallback(paused.rawPacketData)
                autoForward(paused, BreakpointAction.AUTO_FORWARDED)
            }
            if (expired.isNotEmpty()) {
                updatePausedList()
            }
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
     * Generate summary for paused packet
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
     * Add action to history
     */
    private fun addToHistory(paused: PausedIntercept, action: BreakpointAction) {
        synchronized(historyQueue) {
            val history = BreakpointHistory(
                id = paused.id,
                timestamp = System.currentTimeMillis(),
                protocol = paused.protocol,
                sourceIp = paused.sourceIp,
                destIp = paused.destIp,
                sourcePort = paused.sourcePort,
                destPort = paused.destPort,
                summary = paused.summary,
                filterType = paused.filterType,
                action = action
            )
            
            historyQueue.offer(history)
            // Keep last 50 history entries
            while (historyQueue.size > 50) {
                historyQueue.poll()
            }
            
            _history.value = historyQueue.toList().reversed() // Newest first
        }
    }
    
    /**
     * Update paused list StateFlow
     */
    private fun updatePausedList() {
        _pausedIntercepts.value = pausedQueue.toList().reversed() // Newest first
    }
    
    /**
     * Clear all paused packets
     */
    fun clearPaused() {
        synchronized(pausedQueue) {
            pausedQueue.clear()
            updatePausedList()
        }
    }
    
    /**
     * Clear history
     */
    fun clearHistory() {
        synchronized(historyQueue) {
            historyQueue.clear()
            _history.value = emptyList()
        }
    }
}

