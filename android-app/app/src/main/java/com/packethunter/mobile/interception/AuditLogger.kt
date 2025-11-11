package com.packethunter.mobile.interception

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Manages audit logging with encryption
 * Uses Android Keystore + EncryptedFile for secure storage
 */
class AuditLogger(private val context: Context) {
    
    companion object {
        private const val TAG = "AuditLogger"
        private const val MAX_MEMORY_ENTRIES = 1000
        private const val AUDIT_FILE_NAME = "audit_log.json"
    }
    
    private val memoryQueue = ConcurrentLinkedQueue<AuditLogEntry>()
    private val _auditEntries = MutableStateFlow<List<AuditLogEntry>>(emptyList())
    val auditEntries: StateFlow<List<AuditLogEntry>> = _auditEntries.asStateFlow()
    
    private var masterKey: MasterKey? = null
    private var auditFile: File? = null
    
    init {
        initializeEncryption()
    }
    
    private fun initializeEncryption() {
        try {
            masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            auditFile = File(context.filesDir, AUDIT_FILE_NAME)
            Log.d(TAG, "Audit encryption initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize encryption", e)
        }
    }
    
    /**
     * Log an interception decision
     */
    suspend fun logDecision(
        connKey: String,
        protocol: String,
        action: String,
        originalPayload: ByteArray?,
        modifiedPayload: ByteArray? = null,
        autoForwarded: Boolean = false,
        summary: String = "",
        metadata: Map<String, Any> = emptyMap()
    ) = withContext(Dispatchers.IO) {
        try {
            val originalHash = AuditLogEntry.computeHash(originalPayload)
            val modifiedHash = modifiedPayload?.let { AuditLogEntry.computeHash(it) }
            
            val entry = AuditLogEntry(
                timestamp = System.currentTimeMillis(),
                connKey = connKey,
                protocol = protocol,
                action = action,
                originalHash = originalHash,
                modifiedHash = modifiedHash,
                autoForwarded = autoForwarded,
                consentVersion = 1,
                summary = summary,
                metadata = JSONObject(metadata).toString()
            )
            
            // Add to memory queue
            synchronized(memoryQueue) {
                memoryQueue.offer(entry)
                while (memoryQueue.size > MAX_MEMORY_ENTRIES) {
                    memoryQueue.poll()
                }
                _auditEntries.value = memoryQueue.toList().reversed()
            }
            
            // Persist to encrypted file
            persistEntry(entry)
            
            Log.d(TAG, "Logged decision: $action for $connKey")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging decision", e)
        }
    }
    
    /**
     * Persist entry to encrypted file
     */
    private suspend fun persistEntry(entry: AuditLogEntry) = withContext(Dispatchers.IO) {
        try {
            val masterKey = this@AuditLogger.masterKey ?: return@withContext
            val auditFile = this@AuditLogger.auditFile ?: return@withContext
            
            val encryptedFile = EncryptedFile.Builder(
                context,
                auditFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            // Append entry as JSON line
            val json = JSONObject().apply {
                put("id", entry.id)
                put("timestamp", entry.timestamp)
                put("connKey", entry.connKey)
                put("protocol", entry.protocol)
                put("action", entry.action)
                put("originalHash", entry.originalHash)
                put("modifiedHash", entry.modifiedHash ?: "")
                put("autoForwarded", entry.autoForwarded)
                put("consentVersion", entry.consentVersion)
                put("summary", entry.summary)
                put("metadata", entry.metadata)
            }
            
            encryptedFile.openFileOutput().use { fos ->
                fos.write("${json.toString()}\n".toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error persisting audit entry", e)
        }
    }
    
    /**
     * Export audit log for a session
     */
    suspend fun exportSessionLog(sessionStartTime: Long, sessionEndTime: Long): List<AuditLogEntry> {
        return withContext(Dispatchers.IO) {
            memoryQueue.filter { 
                it.timestamp >= sessionStartTime && it.timestamp <= sessionEndTime 
            }
        }
    }
    
    /**
     * Clear all audit logs
     */
    fun clearAll() {
        synchronized(memoryQueue) {
            memoryQueue.clear()
            _auditEntries.value = emptyList()
        }
        try {
            auditFile?.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing audit file", e)
        }
    }
}

