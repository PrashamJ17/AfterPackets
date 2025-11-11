package com.packethunter.mobile.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Predefined security filter types
 */
enum class SecurityFilter {
    HTTP,
    HTTPS_TLS,
    DNS,
    ICMP,
    SUSPICIOUS_PORTS,
    LARGE_OUTBOUND,
    ALL
}

/**
 * Field type for advanced filtering
 */
enum class FilterField {
    SOURCE_IP,
    DESTINATION_IP,
    SOURCE_PORT,
    DESTINATION_PORT,
    PROTOCOL
}

/**
 * Filter condition operator
 */
enum class FilterOperator {
    EQUALS,
    CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    GREATER_THAN,
    LESS_THAN
}

/**
 * Advanced filter rule
 */
data class FilterRule(
    val field: FilterField,
    val operator: FilterOperator,
    val value: String,
    val isActive: Boolean = true
)

/**
 * Filter preset - saved combination of filters
 */
@Entity(tableName = "filter_presets")
data class FilterPreset(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val securityFilters: String, // Comma-separated SecurityFilter names
    val rules: String, // JSON string of FilterRule list
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis()
)

/**
 * Active filter state
 */
data class ActiveFilters(
    val securityFilters: Set<SecurityFilter> = emptySet(),
    val advancedRules: List<FilterRule> = emptyList(),
    val presetId: Long? = null
)

/**
 * Suspicious traffic detection result
 */
data class SuspiciousTrafficAlert(
    val packetId: Long,
    val severity: String, // "low", "medium", "high"
    val type: String, // "non_standard_icmp", "unencrypted_http", "suspicious_port", etc.
    val message: String,
    val recommendation: String? = null
)

