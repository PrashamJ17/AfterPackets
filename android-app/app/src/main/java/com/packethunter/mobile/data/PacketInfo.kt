package com.packethunter.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a captured network packet with parsed metadata
 */
@Entity(tableName = "packets")
data class PacketInfo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val length: Int,
    val flags: String,
    val payload: ByteArray? = null,
    val payloadPreview: String = "",
    val sessionId: String = "",
    val direction: String = "outbound", // outbound or inbound
    
    // Protocol-specific fields
    val httpMethod: String? = null,
    val httpUrl: String? = null,
    val dnsQuery: String? = null,
    val dnsResponse: String? = null,
    val tlsSni: String? = null,
    val tlsCertFingerprint: String? = null,
    
    // Geolocation
    val destCountry: String? = null,
    val destCity: String? = null,
    val destLat: Double? = null,
    val destLon: Double? = null,
    val destAsn: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PacketInfo

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
 * Statistics for a capture session
 */
data class CaptureStats(
    val totalPackets: Long = 0,
    val packetsPerSecond: Double = 0.0,
    val totalBytes: Long = 0,
    val bytesPerSecond: Double = 0.0,
    val protocolDistribution: Map<String, Int> = emptyMap(),
    val topTalkers: List<IpTalker> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val lastUpdate: Long = System.currentTimeMillis()
)

data class IpTalker(
    val ip: String,
    val packetCount: Int,
    val bytes: Long,
    val country: String? = null,
    val lat: Double? = null,
    val lon: Double? = null
)

/**
 * App-level packet statistics
 */
data class AppTalker(
    val packageName: String,
    val appName: String,
    val sentPackets: Int,
    val receivedPackets: Int,
    val totalBytes: Long,
    val sentBytes: Long,
    val receivedBytes: Long,
    val uniqueRemoteHosts: Int,
    val protocols: Set<String>
)

/**
 * Alert/Detection result
 */
@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val severity: String, // low, medium, high, critical
    val type: String, // mitm, dns_spoof, data_exfil, arp_spoof, etc.
    val title: String,
    val description: String,
    val relatedPacketIds: String, // comma-separated IDs
    val acknowledged: Boolean = false
)

/**
 * Custom detection rule
 */
@Entity(tableName = "rules")
data class DetectionRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val enabled: Boolean = true,
    val metric: String, // outbound_bytes, packet_rate, etc.
    val condition: String, // >, <, ==, !=
    val threshold: Double,
    val timeWindowSeconds: Int,
    val action: String, // alert, log, block
    val severity: String = "medium"
)

/**
 * Export format options
 */
enum class ExportFormat {
    PCAP,
    JSON,
    EVIDENCE_BUNDLE
}

/**
 * Filter types for quick filtering
 */
enum class QuickFilter {
    ALL,
    HTTP_HTTPS,
    DNS,
    LARGE_TRANSFER,
    ICMP,
    TLS_CERT_CHANGE
}
