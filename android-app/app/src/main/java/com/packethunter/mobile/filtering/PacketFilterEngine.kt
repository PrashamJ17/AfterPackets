package com.packethunter.mobile.filtering

import android.util.Log
import com.packethunter.mobile.data.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Packet filtering engine with security filters and advanced rules
 */
class PacketFilterEngine {

    // Track data bursts per app
    private val appDataBursts = mutableMapOf<String, MutableList<Pair<Long, Long>>>() // app -> (timestamp, bytes)

    // Track DNS queries per app
    private val appDnsQueries = mutableMapOf<String, MutableList<Long>>() // app -> timestamps

    // Track foreign connections per app
    private val foreignConnections = mutableMapOf<String, MutableSet<String>>() // app -> IPs

    companion object {
        private const val TAG = "PacketFilterEngine"

        // Alert thresholds
        private const val DATA_BURST_THRESHOLD_BYTES = 10 * 1024 * 1024 // 10 MB in 5 seconds
        private const val DATA_BURST_WINDOW_MS = 5000L // 5 seconds
        private const val DNS_FLOOD_THRESHOLD = 50 // 50 queries in 10 seconds
        private const val DNS_FLOOD_WINDOW_MS = 10000L // 10 seconds

        // Suspicious ports (common malware/backdoor ports)
        private val SUSPICIOUS_PORTS = setOf(
            4444, 31337, 12345, 54321, 6667, 6668, 6669, // IRC/backdoors
            5555, 5556, // Android debugging (legitimate but suspicious if unexpected)
            8080, 8888, 9999, // Common alternative HTTP (can be suspicious)
            1337, 1338, // Leet speak ports
            1234, 4321, // Common backdoors
            9998, 9999, // Alternative services
            31337, 31338 // Back Orifice
        )

        // Standard ports that should be encrypted but aren't
        private val SHOULD_BE_ENCRYPTED = setOf(443, 993, 995, 465, 636)

        // Domestic IP ranges (example for US - should be configurable)
        private val DOMESTIC_IP_RANGES = listOf(
            "10.0.0.0/8",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "127.0.0.0/8"
        )
    }
    
    /**
     * Filter packets based on active filters
     */
    fun filterPackets(
        packets: List<PacketInfo>,
        activeFilters: ActiveFilters
    ): FilteredResult {
        if (activeFilters.securityFilters.isEmpty() && activeFilters.advancedRules.isEmpty()) {
            return FilteredResult(
                filteredPackets = packets,
                totalCaptured = packets.size,
                totalFiltered = packets.size,
                suspiciousAlerts = emptyList()
            )
        }
        
        val filtered = mutableListOf<PacketInfo>()
        val suspiciousAlerts = mutableListOf<SuspiciousTrafficAlert>()
        
        for (packet in packets) {
            var matches = false
            
            // Check security filters
            for (filter in activeFilters.securityFilters) {
                if (matchesSecurityFilter(packet, filter)) {
                    matches = true
                    break
                }
            }
            
            // Check advanced rules (if no security filter matched, or if we want union)
            if (!matches || activeFilters.advancedRules.isNotEmpty()) {
                for (rule in activeFilters.advancedRules) {
                    if (rule.isActive && matchesAdvancedRule(packet, rule)) {
                        matches = true
                        break
                    }
                }
            }
            
            if (matches) {
                filtered.add(packet)
                
                // Check for suspicious traffic
                val alert = detectSuspiciousTraffic(packet)
                if (alert != null) {
                    suspiciousAlerts.add(alert)
                }
            }
        }
        
        return FilteredResult(
            filteredPackets = filtered,
            totalCaptured = packets.size,
            totalFiltered = filtered.size,
            suspiciousAlerts = suspiciousAlerts
        )
    }
    
    /**
     * Check if packet matches a security filter
     */
    private fun matchesSecurityFilter(packet: PacketInfo, filter: SecurityFilter): Boolean {
        return when (filter) {
            SecurityFilter.ALL -> true
            SecurityFilter.HTTP -> {
                packet.protocol == "HTTP" || 
                packet.destPort == 80 || 
                packet.sourcePort == 80 ||
                packet.httpMethod != null
            }
            SecurityFilter.HTTPS_TLS -> {
                packet.protocol == "HTTPS" ||
                packet.destPort == 443 ||
                packet.sourcePort == 443 ||
                packet.tlsSni != null ||
                packet.tlsCertFingerprint != null
            }
            SecurityFilter.DNS -> {
                packet.protocol == "DNS" ||
                packet.destPort == 53 ||
                packet.sourcePort == 53 ||
                packet.dnsQuery != null ||
                packet.dnsResponse != null
            }
            SecurityFilter.ICMP -> {
                packet.protocol == "ICMP"
            }
            SecurityFilter.SUSPICIOUS_PORTS -> {
                SUSPICIOUS_PORTS.contains(packet.destPort) ||
                SUSPICIOUS_PORTS.contains(packet.sourcePort)
            }
            SecurityFilter.LARGE_OUTBOUND -> {
                packet.direction == "outbound" && packet.length > 10000
            }
        }
    }
    
    /**
     * Check if packet matches an advanced rule
     */
    private fun matchesAdvancedRule(packet: PacketInfo, rule: FilterRule): Boolean {
        val fieldValue = when (rule.field) {
            FilterField.SOURCE_IP -> packet.sourceIp
            FilterField.DESTINATION_IP -> packet.destIp
            FilterField.SOURCE_PORT -> packet.sourcePort.toString()
            FilterField.DESTINATION_PORT -> packet.destPort.toString()
            FilterField.PROTOCOL -> packet.protocol
        }
        
        return when (rule.operator) {
            FilterOperator.EQUALS -> fieldValue.equals(rule.value, ignoreCase = true)
            FilterOperator.CONTAINS -> fieldValue.contains(rule.value, ignoreCase = true)
            FilterOperator.STARTS_WITH -> fieldValue.startsWith(rule.value, ignoreCase = true)
            FilterOperator.ENDS_WITH -> fieldValue.endsWith(rule.value, ignoreCase = true)
            FilterOperator.GREATER_THAN -> {
                try {
                    fieldValue.toIntOrNull()?.let { it > rule.value.toIntOrNull() ?: 0 } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            FilterOperator.LESS_THAN -> {
                try {
                    fieldValue.toIntOrNull()?.let { it < rule.value.toIntOrNull() ?: 0 } ?: false
                } catch (e: Exception) {
                    false
                }
            }
        }
    }
    
    /**
     * Detect suspicious traffic patterns
     */
    private fun detectSuspiciousTraffic(packet: PacketInfo): SuspiciousTrafficAlert? {
        // Non-standard ICMP
        if (packet.protocol == "ICMP" && packet.flags.isNotEmpty() && 
            !packet.flags.contains("ECHO") && !packet.flags.contains("REPLY")) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "medium",
                type = "non_standard_icmp",
                message = "Non-standard ICMP type detected - possible ping tunnel or covert channel",
                recommendation = "Investigate ICMP payload for data exfiltration"
            )
        }
        
        // Unencrypted HTTP on standard HTTPS port
        if (packet.destPort == 443 && packet.protocol == "HTTP" && packet.httpMethod != null) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "high",
                type = "unencrypted_on_https_port",
                message = "Unencrypted HTTP request on port 443 (should be HTTPS)",
                recommendation = "Possible downgrade attack or misconfiguration"
            )
        }
        
        // HTTP on non-standard ports (potential data exfiltration)
        if (packet.protocol == "HTTP" && packet.destPort != 80 && packet.destPort != 8080) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "low",
                type = "http_non_standard_port",
                message = "HTTP traffic on non-standard port ${packet.destPort}",
                recommendation = "Verify if this is expected behavior"
            )
        }
        
        // Suspicious ports
        if (SUSPICIOUS_PORTS.contains(packet.destPort) || SUSPICIOUS_PORTS.contains(packet.sourcePort)) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "medium",
                type = "suspicious_port",
                message = "Traffic on known suspicious port ${packet.destPort}",
                recommendation = "Investigate for potential backdoor or malware communication"
            )
        }
        
        // Large outbound transfers
        if (packet.direction == "outbound" && packet.length > 50000) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "medium",
                type = "large_outbound_transfer",
                message = "Large outbound data transfer detected (${packet.length} bytes)",
                recommendation = "Monitor for potential data exfiltration"
            )
        }
        
        // DNS over non-standard port
        if (packet.dnsQuery != null && packet.destPort != 53) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "low",
                type = "dns_non_standard_port",
                message = "DNS query on non-standard port ${packet.destPort}",
                recommendation = "May indicate DNS tunneling or bypass attempt"
            )
        }

        // Check for new alert types
        checkDataBurst(packet)?.let { return it }
        checkDnsFlood(packet)?.let { return it }
        checkForeignConnection(packet)?.let { return it }
        checkUnencryptedHttp(packet)?.let { return it }

        return null
    }

    /**
     * Detect large data bursts (potential data exfiltration)
     */
    private fun checkDataBurst(packet: PacketInfo): SuspiciousTrafficAlert? {
        // Use source IP as identifier (app tracking would require context)
        val identifier = "${packet.sourceIp}:${packet.sourcePort}"
        val now = System.currentTimeMillis()

        // Track bytes per connection
        val bursts = appDataBursts.getOrPut(identifier) { mutableListOf() }
        bursts.add(Pair(now, packet.length.toLong()))

        // Remove old entries outside the window
        bursts.removeAll { (timestamp, _) -> now - timestamp > DATA_BURST_WINDOW_MS }

        // Calculate total bytes in window
        val totalBytes = bursts.sumOf { it.second }

        if (totalBytes > DATA_BURST_THRESHOLD_BYTES) {
            Log.w(TAG, "Data burst detected for $identifier: ${totalBytes / 1024 / 1024} MB in ${DATA_BURST_WINDOW_MS / 1000}s")
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "high",
                type = "data_burst",
                message = "Large data burst detected: ${totalBytes / 1024 / 1024} MB in ${DATA_BURST_WINDOW_MS / 1000} seconds from ${packet.sourceIp}",
                recommendation = "Possible data exfiltration - investigate connection"
            )
        }

        return null
    }

    /**
     * Detect DNS flood (potential DNS tunneling or DDoS)
     */
    private fun checkDnsFlood(packet: PacketInfo): SuspiciousTrafficAlert? {
        if (packet.dnsQuery == null) return null

        // Use source IP as identifier
        val identifier = packet.sourceIp
        val now = System.currentTimeMillis()

        // Track DNS queries per source
        val queries = appDnsQueries.getOrPut(identifier) { mutableListOf() }
        queries.add(now)

        // Remove old entries outside the window
        queries.removeAll { timestamp -> now - timestamp > DNS_FLOOD_WINDOW_MS }

        if (queries.size > DNS_FLOOD_THRESHOLD) {
            Log.w(TAG, "DNS flood detected for $identifier: ${queries.size} queries in ${DNS_FLOOD_WINDOW_MS / 1000}s")
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "high",
                type = "dns_flood",
                message = "High DNS request frequency: ${queries.size} queries in ${DNS_FLOOD_WINDOW_MS / 1000} seconds from ${packet.sourceIp}",
                recommendation = "Possible DNS tunneling or DDoS attack - investigate DNS queries"
            )
        }

        return null
    }

    /**
     * Detect foreign server connections (GeoIP-based)
     * Note: This is a simplified version. For production, integrate MaxMind GeoLite2
     */
    private fun checkForeignConnection(packet: PacketInfo): SuspiciousTrafficAlert? {
        val destIp = packet.destIp

        // Skip private/local IPs
        if (isPrivateIp(destIp)) return null

        // Use source IP as identifier
        val identifier = packet.sourceIp

        // Track foreign connections per source
        val connections = foreignConnections.getOrPut(identifier) { mutableSetOf() }

        // For now, mark as foreign if not in domestic ranges
        // TODO: Integrate MaxMind GeoLite2 for proper GeoIP lookup
        if (!isDomesticIp(destIp)) {
            if (connections.add(destIp)) {
                Log.i(TAG, "Foreign connection detected from $identifier to $destIp")
                return SuspiciousTrafficAlert(
                    packetId = packet.id,
                    severity = "medium",
                    type = "foreign_server",
                    message = "Connection to foreign server: $destIp from ${packet.sourceIp}",
                    recommendation = "Verify if this connection is expected"
                )
            }
        }

        return null
    }

    /**
     * Detect unencrypted HTTP on port 80
     */
    private fun checkUnencryptedHttp(packet: PacketInfo): SuspiciousTrafficAlert? {
        if (packet.protocol == "HTTP" && packet.destPort == 80 && packet.httpMethod != null) {
            return SuspiciousTrafficAlert(
                packetId = packet.id,
                severity = "low",
                type = "unencrypted_http",
                message = "Unencrypted HTTP request to ${packet.destIp}:${packet.destPort}",
                recommendation = "Consider using HTTPS for secure communication"
            )
        }
        return null
    }

    /**
     * Check if IP is private/local
     */
    private fun isPrivateIp(ip: String): Boolean {
        return ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               ip.startsWith("172.16.") || ip.startsWith("172.17.") || ip.startsWith("172.18.") ||
               ip.startsWith("172.19.") || ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
               ip.startsWith("172.22.") || ip.startsWith("172.23.") || ip.startsWith("172.24.") ||
               ip.startsWith("172.25.") || ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
               ip.startsWith("172.28.") || ip.startsWith("172.29.") || ip.startsWith("172.30.") ||
               ip.startsWith("172.31.") ||
               ip.startsWith("127.") ||
               ip == "0.0.0.0"
    }

    /**
     * Check if IP is domestic (simplified - should use GeoIP database)
     */
    private fun isDomesticIp(ip: String): Boolean {
        // For now, just check if it's a private IP
        // TODO: Integrate MaxMind GeoLite2 for proper country lookup
        return isPrivateIp(ip)
    }
}

/**
 * Result of filtering operation
 */
data class FilteredResult(
    val filteredPackets: List<PacketInfo>,
    val totalCaptured: Int,
    val totalFiltered: Int,
    val suspiciousAlerts: List<SuspiciousTrafficAlert>
)

