package com.packethunter.mobile.firewall

import android.util.Log
import com.packethunter.mobile.data.PacketInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Firewall rule engine for blocking apps, domains, and IP addresses
 * 
 * Similar to PCAPdroid's firewall feature, allows blocking:
 * - Apps by package name
 * - Domains by hostname
 * - IP addresses
 * - Ports
 * 
 * Rules are evaluated in order and first match wins.
 */
class FirewallRuleEngine {
    
    private val rules = mutableListOf<FirewallRule>()
    private var isEnabled = false
    
    companion object {
        private const val TAG = "FirewallRuleEngine"
    }
    
    /**
     * Add a firewall rule
     */
    fun addRule(rule: FirewallRule) {
        synchronized(rules) {
            rules.add(rule)
            Log.d(TAG, "Added firewall rule: ${rule.name} (total: ${rules.size})")
        }
    }
    
    /**
     * Remove a firewall rule
     */
    fun removeRule(ruleId: String) {
        synchronized(rules) {
            rules.removeAll { it.id == ruleId }
            Log.d(TAG, "Removed firewall rule: $ruleId (remaining: ${rules.size})")
        }
    }
    
    /**
     * Clear all rules
     */
    fun clearRules() {
        synchronized(rules) {
            rules.clear()
            Log.d(TAG, "Cleared all firewall rules")
        }
    }
    
    /**
     * Get all rules
     */
    fun getRules(): List<FirewallRule> {
        synchronized(rules) {
            return rules.toList()
        }
    }
    
    /**
     * Enable/disable firewall
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        Log.i(TAG, "Firewall ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if firewall is enabled
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Evaluate packet against firewall rules
     * Returns true if packet should be blocked, false if allowed
     */
    fun shouldBlock(packet: PacketInfo, appPackageName: String? = null): Boolean {
        if (!isEnabled || rules.isEmpty()) {
            return false
        }
        
        synchronized(rules) {
            for (rule in rules) {
                if (!rule.enabled) {
                    continue
                }
                
                if (matchesRule(packet, rule, appPackageName)) {
                    Log.d(TAG, "Packet blocked by rule: ${rule.name} - ${packet.sourceIp}:${packet.sourcePort} -> ${packet.destIp}:${packet.destPort}")
                    return rule.action == FirewallAction.BLOCK
                }
            }
        }
        
        return false
    }
    
    /**
     * Check if packet matches a rule
     */
    private fun matchesRule(packet: PacketInfo, rule: FirewallRule, appPackageName: String?): Boolean {
        // Check app package name
        if (rule.appPackageName != null && appPackageName != null) {
            if (rule.appPackageName == appPackageName) {
                return true
            }
        }
        
        // Check source IP
        if (rule.sourceIp != null) {
            if (matchesIp(packet.sourceIp, rule.sourceIp)) {
                return true
            }
        }
        
        // Check destination IP
        if (rule.destIp != null) {
            if (matchesIp(packet.destIp, rule.destIp)) {
                return true
            }
        }
        
        // Check source port
        if (rule.sourcePort != null) {
            if (packet.sourcePort == rule.sourcePort) {
                return true
            }
        }
        
        // Check destination port
        if (rule.destPort != null) {
            if (packet.destPort == rule.destPort) {
                return true
            }
        }
        
        // Check domain (from HTTP URL or DNS query)
        if (rule.domain != null) {
            val domain = packet.httpUrl?.let { extractDomain(it) } 
                ?: packet.dnsQuery
                ?: packet.tlsSni
            
            if (domain != null && matchesDomain(domain, rule.domain)) {
                return true
            }
        }
        
        // Check protocol
        if (rule.protocol != null) {
            if (packet.protocol.equals(rule.protocol, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Check if IP matches rule (supports CIDR notation)
     */
    private fun matchesIp(ip: String, ruleIp: String): Boolean {
        if (ip == ruleIp) {
            return true
        }
        
        // Check CIDR notation (e.g., 192.168.1.0/24)
        if (ruleIp.contains("/")) {
            try {
                val parts = ruleIp.split("/")
                val ruleIpAddr = parts[0]
                val prefixLength = parts[1].toInt()
                
                // Simple CIDR matching (for production, use proper IP address library)
                if (ip.startsWith(ruleIpAddr.substringBeforeLast("."))) {
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Invalid CIDR notation: $ruleIp", e)
            }
        }
        
        return false
    }
    
    /**
     * Check if domain matches rule (supports wildcards)
     */
    private fun matchesDomain(domain: String, ruleDomain: String): Boolean {
        if (domain.equals(ruleDomain, ignoreCase = true)) {
            return true
        }
        
        // Check wildcard (e.g., *.example.com)
        if (ruleDomain.startsWith("*.")) {
            val suffix = ruleDomain.substring(2)
            if (domain.endsWith(suffix, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Extract domain from URL
     */
    private fun extractDomain(url: String): String? {
        return try {
            val uri = java.net.URI(url)
            uri.host
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Firewall rule
 */
data class FirewallRule(
    val id: String,
    val name: String,
    val enabled: Boolean = true,
    val action: FirewallAction = FirewallAction.BLOCK,
    
    // Match criteria
    val appPackageName: String? = null,
    val sourceIp: String? = null,
    val destIp: String? = null,
    val sourcePort: Int? = null,
    val destPort: Int? = null,
    val domain: String? = null,
    val protocol: String? = null,
    
    // Metadata
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Firewall action
 */
enum class FirewallAction {
    BLOCK,
    ALLOW
}

