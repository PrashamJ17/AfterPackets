package com.packethunter.mobile.interception

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages authorization state for packet interception and modification features.
 * 
 * SECURITY & LEGAL:
 * - Interception features MUST be disabled unless user explicitly enables "Authorized Testing Mode"
 * - One-time warning dialog and consent checkbox required
 * - All consent events logged to AuditLogger
 * - All packet modifications logged with original hash
 * 
 * This is to ensure:
 * 1. User awareness of packet modification capabilities
 * 2. Legal compliance with wiretapping laws
 * 3. Audit trail for security investigations
 */
object AuthorizedTestingMode {
    
    private const val TAG = "AuthorizedTestingMode"
    private const val PREFS_NAME = "authorized_testing_mode"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"
    private const val KEY_CONSENT_VERSION = "consent_version"
    private const val CURRENT_CONSENT_VERSION = 1
    
    private var prefs: SharedPreferences? = null
    private var auditLogger: AuditLogger? = null
    
    /**
     * Initialize with application context
     */
    fun initialize(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            auditLogger = AuditLogger.getInstance(context)
            
            val enabled = isEnabled()
            Log.i(TAG, "✅ Initialized - Authorized Testing Mode: ${if (enabled) "ENABLED" else "DISABLED"}")
            
            if (enabled) {
                val consentTime = getConsentTimestamp()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                Log.i(TAG, "Consent granted at: ${dateFormat.format(Date(consentTime))}")
            }
        }
    }
    
    /**
     * Check if authorized testing mode is enabled
     */
    fun isEnabled(): Boolean {
        return prefs?.getBoolean(KEY_ENABLED, false) ?: false
    }
    
    /**
     * Enable authorized testing mode with user consent
     * @param consentGiven True if user explicitly consented to packet modification
     */
    fun enable(consentGiven: Boolean) {
        if (!consentGiven) {
            Log.w(TAG, "❌ Cannot enable without explicit consent")
            return
        }
        
        prefs?.edit()?.apply {
            putBoolean(KEY_ENABLED, true)
            putLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis())
            putInt(KEY_CONSENT_VERSION, CURRENT_CONSENT_VERSION)
            apply()
        }
        
        // Log to audit trail
        auditLogger?.logConsentGranted()
        
        Log.i(TAG, "✅ Authorized Testing Mode ENABLED - User consent recorded")
    }
    
    /**
     * Disable authorized testing mode
     */
    fun disable() {
        prefs?.edit()?.apply {
            putBoolean(KEY_ENABLED, false)
            apply()
        }
        
        // Log to audit trail
        auditLogger?.logConsentRevoked()
        
        Log.i(TAG, "⚠️ Authorized Testing Mode DISABLED")
    }
    
    /**
     * Get timestamp when consent was granted
     */
    fun getConsentTimestamp(): Long {
        return prefs?.getLong(KEY_CONSENT_TIMESTAMP, 0L) ?: 0L
    }
    
    /**
     * Check if user needs to see consent dialog again (e.g., consent version changed)
     */
    fun needsConsentUpdate(): Boolean {
        val savedVersion = prefs?.getInt(KEY_CONSENT_VERSION, 0) ?: 0
        return savedVersion < CURRENT_CONSENT_VERSION
    }
    
    /**
     * Get warning text to display in consent dialog
     */
    fun getConsentWarningText(): String {
        return """
            ⚠️ PACKET INTERCEPTION & MODIFICATION WARNING
            
            You are about to enable features that can:
            • Intercept network packets from apps on your device
            • View packet contents (including sensitive data)
            • Modify packet data before forwarding
            • Break app functionality if used incorrectly
            
            LEGAL NOTICE:
            • Use ONLY on your own devices for testing purposes
            • Do NOT use on networks you don't own or have permission to test
            • Unauthorized packet interception may violate laws in your jurisdiction
            • You are solely responsible for how you use these features
            
            TECHNICAL IMPACT:
            • Apps may fail to connect or function properly during interception
            • Modified packets may trigger security alerts in apps/servers
            • All modifications are logged with timestamps and original hashes
            
            By enabling this mode, you confirm:
            ✓ You own or have authorization to test this device
            ✓ You understand the legal and technical implications
            ✓ You will use these features responsibly for authorized testing only
            
            Do you consent to enable Authorized Testing Mode?
        """.trimIndent()
    }
}
