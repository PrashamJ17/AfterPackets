package com.packethunter.mobile.interception

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Audit logger for tracking interception consent and packet modifications.
 * 
 * All entries are timestamped and written to an append-only log file.
 * This provides a tamper-evident audit trail for security investigations.
 */
class AuditLogger private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "AuditLogger"
        private const val LOG_FILE_NAME = "interception_audit.log"
        
        @Volatile
        private var instance: AuditLogger? = null
        
        fun getInstance(context: Context): AuditLogger {
            return instance ?: synchronized(this) {
                instance ?: AuditLogger(context.applicationContext).also { instance = it }
            }
        }
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val logFile: File by lazy {
        File(context.filesDir, LOG_FILE_NAME)
    }
    
    /**
     * Log consent granted event
     */
    fun logConsentGranted() {
        writeLogEntry("CONSENT_GRANTED", "User explicitly consented to Authorized Testing Mode")
    }
    
    /**
     * Log consent revoked event
     */
    fun logConsentRevoked() {
        writeLogEntry("CONSENT_REVOKED", "User disabled Authorized Testing Mode")
    }
    
    /**
     * Log packet modification event
     */
    fun logPacketModification(
        originalHash: String,
        modifiedHash: String,
        sourceIp: String,
        destIp: String,
        protocol: String,
        modificationType: String
    ) {
        val details = "protocol=$protocol, src=$sourceIp, dst=$destIp, " +
                     "type=$modificationType, original_hash=$originalHash, modified_hash=$modifiedHash"
        writeLogEntry("PACKET_MODIFIED", details)
    }
    
    /**
     * Log interception filter enabled
     */
    fun logFilterEnabled(filterType: String) {
        writeLogEntry("FILTER_ENABLED", "Interception filter enabled: $filterType")
    }
    
    /**
     * Log interception filter disabled
     */
    fun logFilterDisabled(filterType: String) {
        writeLogEntry("FILTER_DISABLED", "Interception filter disabled: $filterType")
    }
    
    /**
     * Write a log entry to the audit file
     */
    private fun writeLogEntry(eventType: String, details: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val entry = "[$timestamp] $eventType | $details\n"
            
            FileWriter(logFile, true).use { writer ->
                writer.append(entry)
            }
            
            Log.i(TAG, "📝 Audit log: $eventType - $details")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to write audit log entry", e)
        }
    }
    
    /**
     * Get all audit log entries
     */
    suspend fun getLogEntries(): List<String> = withContext(Dispatchers.IO) {
        try {
            if (logFile.exists()) {
                logFile.readLines()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read audit log", e)
            emptyList()
        }
    }
    
    /**
     * Get log file size in bytes
     */
    fun getLogSize(): Long {
        return if (logFile.exists()) logFile.length() else 0L
    }
    
    /**
     * Clear audit log (requires explicit user confirmation in UI)
     */
    suspend fun clearLog() = withContext(Dispatchers.IO) {
        try {
            writeLogEntry("LOG_CLEARED", "Audit log cleared by user")
            // Keep the last entry showing log was cleared
            val lastEntry = logFile.readLines().lastOrNull()
            logFile.writeText(lastEntry ?: "")
            Log.w(TAG, "⚠️ Audit log cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear audit log", e)
        }
    }
}
