package com.packethunter.mobile.interception

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.security.MessageDigest

/**
 * Audit log entry for interception decisions
 * Stored in encrypted database
 */
@Entity(tableName = "audit_log")
data class AuditLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val connKey: String, // 5-tuple: srcIp:srcPort-dstIp:dstPort:protocol
    val protocol: String,
    val action: String, // FORWARDED, DROPPED, AUTO_FORWARDED, FORWARDED_MODIFIED
    val originalHash: String, // SHA-256 of original payload
    val modifiedHash: String? = null, // SHA-256 of modified payload (if edited)
    val autoForwarded: Boolean = false,
    val consentVersion: Int = 1,
    val userId: String = "default", // For multi-user scenarios
    val summary: String = "",
    val metadata: String = "" // JSON metadata
) {
    companion object {
        fun computeHash(data: ByteArray?): String {
            if (data == null || data.isEmpty()) return ""
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(data)
            return hash.joinToString("") { "%02x".format(it) }
        }
    }
}

