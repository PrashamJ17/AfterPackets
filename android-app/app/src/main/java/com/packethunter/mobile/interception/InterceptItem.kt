package com.packethunter.mobile.interception

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an intercepted packet that matches a breakpoint filter
 */
@Entity(tableName = "intercepts")
data class InterceptItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val summary: String, // e.g., "GET /index.html", "DNS query: google.com"
    val payloadPreview: ByteArray? = null, // First 4KB max
    val payloadHex: String = "", // Hex representation of preview
    val payloadAscii: String = "", // ASCII representation of preview
    val filterType: String, // HTTP, DNS, TLS, ICMP
    val sni: String? = null, // TLS SNI if available
    val originalHash: String = "" // SHA-256 of original payload
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InterceptItem

        if (id != other.id) return false
        if (timestamp != other.timestamp) return false
        if (protocol != other.protocol) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + protocol.hashCode()
        return result
    }
}

/**
 * Filter types for interception
 */
enum class InterceptFilterType {
    HTTP,
    DNS,
    TLS,
    ICMP,
    ALL
}

