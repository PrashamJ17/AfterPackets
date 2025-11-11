package com.packethunter.mobile.interception

import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a packet that is paused in Active Breakpoint Mode
 */
data class PausedIntercept(
    val id: Long,
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val summary: String,
    val filterType: String,
    val rawPacketData: ByteArray, // Original packet bytes to forward
    val pauseStartTime: Long = System.currentTimeMillis(),
    val autoForwardDelay: Long = 30000L, // 30 seconds default
    val connKey: String = "" // Connection key for async resolution
) {
    /**
     * Get remaining time until auto-forward (in milliseconds)
     */
    fun getRemainingTime(): Long {
        val elapsed = System.currentTimeMillis() - pauseStartTime
        return maxOf(0, autoForwardDelay - elapsed)
    }
    
    /**
     * Check if auto-forward time has expired
     */
    fun isExpired(): Boolean {
        return getRemainingTime() <= 0
    }
    
    /**
     * Get remaining seconds (for display)
     */
    fun getRemainingSeconds(): Int {
        return (getRemainingTime() / 1000).toInt()
    }
}

/**
 * Action taken on a paused intercept
 */
enum class BreakpointAction {
    FORWARDED,
    FORWARDED_MODIFIED,
    DROPPED,
    AUTO_FORWARDED
}

/**
 * History entry for breakpoint actions
 */
data class BreakpointHistory(
    val id: Long,
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val summary: String,
    val filterType: String,
    val action: BreakpointAction
)

