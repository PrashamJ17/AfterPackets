package com.packethunter.mobile.interception

/**
 * Represents an active interception session
 * Tracks packet data and timestamp for timeout handling
 */
data class InterceptSession(
    val sessionId: String,
    val item: InterceptItem,
    val startTime: Long
)
