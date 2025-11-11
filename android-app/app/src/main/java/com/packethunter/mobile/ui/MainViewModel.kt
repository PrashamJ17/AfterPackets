package com.packethunter.mobile.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.packethunter.mobile.data.*
import com.packethunter.mobile.export.ExportManager
import com.packethunter.mobile.filtering.*
import com.packethunter.mobile.interception.InterceptItem
import com.packethunter.mobile.interception.InterceptFilterType
import com.packethunter.mobile.interception.PausedIntercept
import com.packethunter.mobile.interception.BreakpointHistory
import com.packethunter.mobile.capture.PacketCaptureService
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.io.File

/**
 * Main ViewModel for the app
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PacketDatabase.getDatabase(application)
    private val exportManager = ExportManager(application)
    private val filterEngine = PacketFilterEngine()
    private val gson = Gson()

    // UI State
    private val _uiState = MutableStateFlow(PacketHunterUiState())
    val uiState: StateFlow<PacketHunterUiState> = _uiState.asStateFlow()

    // Packets flow - use Eagerly to ensure it's always active
    val packets: StateFlow<List<PacketInfo>> = database.packetDao()
        .getRecentPackets(1000)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Alerts flow
    val alerts: StateFlow<List<Alert>> = database.alertDao()
        .getUnacknowledgedAlerts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Rules flow
    val rules: StateFlow<List<DetectionRule>> = database.ruleDao()
        .getAllRules()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Filter presets flow
    val filterPresets: StateFlow<List<FilterPreset>> = database.filterPresetDao()
        .getAllPresets()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Intercepts flow - will be updated via polling from service
    private val _intercepts = MutableStateFlow<List<InterceptItem>>(emptyList())
    val intercepts: StateFlow<List<InterceptItem>> = _intercepts.asStateFlow()
    
    // Paused intercepts flow
    private val _pausedIntercepts = MutableStateFlow<List<PausedIntercept>>(emptyList())
    val pausedIntercepts: StateFlow<List<PausedIntercept>> = _pausedIntercepts.asStateFlow()
    
    // Breakpoint history flow
    private val _breakpointHistory = MutableStateFlow<List<BreakpointHistory>>(emptyList())
    val breakpointHistory: StateFlow<List<BreakpointHistory>> = _breakpointHistory.asStateFlow()
    
    init {
        // Load initial data
        loadStats()
        
        // Poll interception manager for updates
        viewModelScope.launch {
            while (true) {
                val service = PacketCaptureService.getInstance()
                if (service != null) {
                    val processor = service.getPacketProcessor()
                    if (processor != null) {
                        _intercepts.value = processor.interceptionManager.intercepts.value
                    } else {
                        _intercepts.value = emptyList()
                    }
                    
                    // Update paused intercepts
                    _pausedIntercepts.value = service.activeBreakpointManager.pausedIntercepts.value
                    _breakpointHistory.value = service.activeBreakpointManager.history.value
                } else {
                    _intercepts.value = emptyList()
                    _pausedIntercepts.value = emptyList()
                    _breakpointHistory.value = emptyList()
                }
                kotlinx.coroutines.delay(500) // Poll every 500ms
            }
        }
    }

    fun setCapturing(isCapturing: Boolean) {
        _uiState.update { it.copy(isCapturing = isCapturing) }
    }

    fun setConsentGiven(consentGiven: Boolean) {
        _uiState.update { it.copy(consentGiven = consentGiven) }
    }

    fun setCurrentFilter(filter: QuickFilter) {
        _uiState.update { it.copy(currentFilter = filter) }
    }

    fun setSelectedPacket(packet: PacketInfo?) {
        _uiState.update { it.copy(selectedPacket = packet) }
    }
    
    fun getConnectionPackets(packet: PacketInfo) {
        viewModelScope.launch {
            try {
                val connectionPackets = database.packetDao().getPacketsByConnection(
                    packet.sourceIp,
                    packet.sourcePort,
                    packet.destIp,
                    packet.destPort
                )
                _uiState.update { it.copy(connectionPackets = connectionPackets) }
            } catch (e: Exception) {
                _uiState.update { it.copy(connectionPackets = emptyList()) }
            }
        }
    }
    
    fun setConnectionPackets(packets: List<PacketInfo>) {
        _uiState.update { it.copy(connectionPackets = packets) }
    }

    fun setCurrentScreen(screen: Screen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun updateStats(stats: CaptureStats) {
        _uiState.update { it.copy(stats = stats) }
    }
    
    fun updateAppTalkers(appTalkers: List<AppTalker>) {
        _uiState.update { it.copy(appTalkers = appTalkers) }
    }
    
    fun setSelectedApp(app: AppTalker?) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Calculate stats from database
            val totalPackets = database.packetDao().getPacketCount()
            val totalBytes = database.packetDao().getTotalBytes() ?: 0L
            val protocolDist = database.packetDao().getProtocolDistribution()
                .associate { it.protocol to it.count }
            val topTalkers = database.packetDao().getTopTalkers(10)
                .map { IpTalker(it.ip, it.count, it.bytes) }

            val stats = CaptureStats(
                totalPackets = totalPackets,
                totalBytes = totalBytes,
                protocolDistribution = protocolDist,
                topTalkers = topTalkers
            )

            _uiState.update { it.copy(stats = stats) }
        }
    }

    fun getFilteredPackets(allPackets: List<PacketInfo> = packets.value): List<PacketInfo> {
        val activeFilters = uiState.value.activeFilters
        if (activeFilters.securityFilters.isEmpty() && activeFilters.advancedRules.isEmpty()) {
            // Fallback to old QuickFilter for backward compatibility
            return when (uiState.value.currentFilter) {
                QuickFilter.ALL -> allPackets
                QuickFilter.HTTP_HTTPS -> allPackets.filter {
                    it.destPort == 80 || it.destPort == 443 ||
                            it.sourcePort == 80 || it.sourcePort == 443
                }
                QuickFilter.DNS -> allPackets.filter {
                    it.destPort == 53 || it.sourcePort == 53
                }
                QuickFilter.LARGE_TRANSFER -> allPackets.filter {
                    it.length > 10000
                }
                QuickFilter.ICMP -> allPackets.filter {
                    it.protocol == "ICMP"
                }
                QuickFilter.TLS_CERT_CHANGE -> allPackets.filter {
                    it.tlsCertFingerprint != null
                }
            }
        }
        
        // Use new filter engine
        val result = filterEngine.filterPackets(allPackets, activeFilters)
        return result.filteredPackets
    }
    
    fun getFilteredResult(allPackets: List<PacketInfo> = packets.value): FilteredResult {
        val activeFilters = uiState.value.activeFilters
        if (activeFilters.securityFilters.isEmpty() && activeFilters.advancedRules.isEmpty()) {
            return FilteredResult(
                filteredPackets = allPackets,
                totalCaptured = allPackets.size,
                totalFiltered = allPackets.size,
                suspiciousAlerts = emptyList()
            )
        }
        return filterEngine.filterPackets(allPackets, activeFilters)
    }
    
    // Filter management
    fun toggleSecurityFilter(filter: SecurityFilter) {
        val current = uiState.value.activeFilters
        val newFilters = if (current.securityFilters.contains(filter)) {
            current.securityFilters - filter
        } else {
            current.securityFilters + filter
        }
        _uiState.update { 
            it.copy(activeFilters = current.copy(securityFilters = newFilters))
        }
    }
    
    fun addAdvancedRule(rule: FilterRule) {
        val current = uiState.value.activeFilters
        _uiState.update {
            it.copy(activeFilters = current.copy(
                advancedRules = current.advancedRules + rule
            ))
        }
    }
    
    fun removeAdvancedRule(rule: FilterRule) {
        val current = uiState.value.activeFilters
        _uiState.update {
            it.copy(activeFilters = current.copy(
                advancedRules = current.advancedRules.filter { it != rule }
            ))
        }
    }
    
    fun clearAllFilters() {
        _uiState.update {
            it.copy(activeFilters = ActiveFilters())
        }
    }
    
    fun saveFilterPreset(name: String) {
        viewModelScope.launch {
            val activeFilters = uiState.value.activeFilters
            val securityFiltersStr = activeFilters.securityFilters.joinToString(",") { it.name }
            val rulesJson = gson.toJson(activeFilters.advancedRules)
            
            val preset = FilterPreset(
                name = name,
                securityFilters = securityFiltersStr,
                rules = rulesJson
            )
            database.filterPresetDao().insertPreset(preset)
        }
    }
    
    fun loadFilterPreset(preset: FilterPreset) {
        viewModelScope.launch {
            val securityFilters = preset.securityFilters.split(",")
                .mapNotNull { 
                    try { SecurityFilter.valueOf(it.trim()) } 
                    catch (e: Exception) { null }
                }
                .toSet()
            
            val rules = try {
                val type = object : TypeToken<List<FilterRule>>() {}.type
                gson.fromJson<List<FilterRule>>(preset.rules, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
            
            _uiState.update {
                it.copy(activeFilters = ActiveFilters(
                    securityFilters = securityFilters,
                    advancedRules = rules,
                    presetId = preset.id
                ))
            }
            
            // Update last used
            database.filterPresetDao().updateLastUsed(preset.id, System.currentTimeMillis())
        }
    }
    
    fun deleteFilterPreset(preset: FilterPreset) {
        viewModelScope.launch {
            database.filterPresetDao().deletePreset(preset)
        }
    }

    fun acknowledgeAlert(alertId: Long) {
        viewModelScope.launch {
            database.alertDao().acknowledgeAlert(alertId)
        }
    }

    fun exportPcap() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val file = exportManager.exportToPcap(packets.value)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "✅ PCAP saved to Downloads/PacketHunter/${file.name}\nCheck Files app to view"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun exportJson() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val file = exportManager.exportToJson(
                    packets.value,
                    alerts.value,
                    uiState.value.stats
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "✅ JSON saved to Downloads/PacketHunter/${file.name}\nCheck Files app to view"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun exportBundle() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val file = exportManager.exportEvidenceBundle(
                    packets.value,
                    alerts.value,
                    uiState.value.stats
                )
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "✅ Evidence bundle saved to Downloads/PacketHunter/${file.name}\nCheck Files app to view"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null, shareableUri = null) }
    }

    fun exportFilterPresets() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val presets = database.filterPresetDao().getAllPresets().first()
                val file = exportManager.exportFilterPresets(presets)
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Filter presets exported: ${file.name}"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun importFilterPresets(file: File) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val importedPresets = exportManager.importFilterPresets(file)
                
                // Insert imported presets into database
                val existingPresets = database.filterPresetDao().getAllPresets().first()
                for (importedPreset in importedPresets) {
                    // Check if preset with same name already exists
                    var existing: FilterPreset? = null
                    for (preset in existingPresets) {
                        if (preset.name == importedPreset.name) {
                            existing = preset
                            break
                        }
                    }
                    if (existing == null) {
                        // Create new preset with unique ID
                        val newPreset = importedPreset.copy(id = 0)
                        database.filterPresetDao().insertPreset(newPreset)
                    } else {
                        // Update existing preset
                        database.filterPresetDao().updatePreset(importedPreset.copy(id = existing.id))
                    }
                }
                
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Imported ${importedPresets.size} filter presets"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun importFilterPresetsFromJson(jsonContent: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isExporting = true) }
                val importedPresets = exportManager.importFilterPresetsFromJson(jsonContent)
                
                // Insert imported presets into database
                val existingPresets = database.filterPresetDao().getAllPresets().first()
                for (importedPreset in importedPresets) {
                    // Check if preset with same name already exists
                    var existing: FilterPreset? = null
                    for (preset in existingPresets) {
                        if (preset.name == importedPreset.name) {
                            existing = preset
                            break
                        }
                    }
                    if (existing == null) {
                        // Create new preset with unique ID
                        val newPreset = importedPreset.copy(id = 0)
                        database.filterPresetDao().insertPreset(newPreset)
                    } else {
                        // Update existing preset
                        database.filterPresetDao().updatePreset(importedPreset.copy(id = existing.id))
                    }
                }
                
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Imported ${importedPresets.size} filter presets"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportResult = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun shareFilterPresetsFile(file: File) {
        viewModelScope.launch {
            try {
                val uri = exportManager.getShareableUri(file)
                if (uri != null) {
                    _uiState.update {
                        it.copy(
                            exportResult = "Sharing filter presets: ${file.name}",
                            shareableUri = uri
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            exportResult = "Failed to create shareable link for ${file.name}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        exportResult = "Share failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun addRule(rule: DetectionRule) {
        viewModelScope.launch {
            database.ruleDao().insertRule(rule)
        }
    }

    fun updateRule(rule: DetectionRule) {
        viewModelScope.launch {
            database.ruleDao().updateRule(rule)
        }
    }

    fun deleteRule(rule: DetectionRule) {
        viewModelScope.launch {
            database.ruleDao().deleteRule(rule)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            database.packetDao().deleteAllPackets()
            database.alertDao().deleteAllAlerts()
            loadStats()
        }
    }
    
    // Interception management
    fun setInterceptionEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.getPacketProcessor()?.interceptionManager?.setEnabled(enabled)
            _uiState.update { it.copy(isInterceptionEnabled = enabled) }
        }
    }
    
    fun setInterceptionFilters(filters: Set<InterceptFilterType>) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.getPacketProcessor()?.interceptionManager?.setEnabledFilters(filters)
            _uiState.update { it.copy(enabledInterceptFilters = filters) }
        }
    }
    
    fun clearIntercepts() {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.getPacketProcessor()?.interceptionManager?.clearIntercepts()
        }
    }
    
    fun getInterceptCount(): Int {
        val service = PacketCaptureService.getInstance()
        return service?.getPacketProcessor()?.interceptionManager?.getInterceptCount() ?: 0
    }
    
    fun setInterceptionConsent(consentGiven: Boolean) {
        _uiState.update { it.copy(interceptionConsentGiven = consentGiven) }
    }
    
    fun setShowInterceptionConsentDialog(show: Boolean) {
        _uiState.update { it.copy(showInterceptionConsentDialog = show) }
    }
    
    // Active Breakpoint Mode management
    fun setActiveBreakpointEnabled(enabled: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.activeBreakpointManager?.setEnabled(enabled)
            _uiState.update { it.copy(isActiveBreakpointEnabled = enabled) }
        }
    }
    
    fun setActiveBreakpointFilters(filters: Set<InterceptFilterType>) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.activeBreakpointManager?.setEnabledFilters(filters)
            _uiState.update { it.copy(enabledBreakpointFilters = filters) }
        }
    }
    
    fun forwardPausedIntercept(pausedId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            if (service != null) {
                service.activeBreakpointManager.forwardPaused(pausedId) { packetData ->
                    service.forwardPausedPacket(packetData)
                }
            }
        }
    }
    
    fun dropPausedIntercept(pausedId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.activeBreakpointManager?.dropPaused(pausedId)
        }
    }
    
    fun clearPausedIntercepts() {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.activeBreakpointManager?.clearPaused()
        }
    }
    
    fun clearBreakpointHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            val service = PacketCaptureService.getInstance()
            service?.activeBreakpointManager?.clearHistory()
        }
    }
}

/**
 * UI State
 */
data class PacketHunterUiState(
    val isCapturing: Boolean = false,
    val currentFilter: QuickFilter = QuickFilter.ALL,
    val selectedPacket: PacketInfo? = null,
    val currentScreen: Screen = Screen.Dashboard,
    val stats: CaptureStats = CaptureStats(),
    val isExporting: Boolean = false,
    val exportResult: String? = null,
    val consentGiven: Boolean = false,
    val appTalkers: List<AppTalker> = emptyList(),
    val selectedApp: AppTalker? = null,
    val connectionPackets: List<PacketInfo> = emptyList(),
    val activeFilters: ActiveFilters = ActiveFilters(),
    val shareableUri: Uri? = null,
    val isInterceptionEnabled: Boolean = false,
    val enabledInterceptFilters: Set<InterceptFilterType> = setOf(InterceptFilterType.HTTP, InterceptFilterType.DNS, InterceptFilterType.TLS, InterceptFilterType.ICMP),
    val interceptionConsentGiven: Boolean = false,
    val showInterceptionConsentDialog: Boolean = false,
    val isActiveBreakpointEnabled: Boolean = false,
    val enabledBreakpointFilters: Set<InterceptFilterType> = setOf(InterceptFilterType.HTTP, InterceptFilterType.DNS, InterceptFilterType.TLS, InterceptFilterType.ICMP)
)

enum class Screen {
    Dashboard,
    Packets,
    AppTalkers,
    Alerts,
    Rules,
    Export,
    Intercepts
}
