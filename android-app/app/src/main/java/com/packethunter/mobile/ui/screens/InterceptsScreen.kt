package com.packethunter.mobile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.interception.InterceptItem
import com.packethunter.mobile.interception.InterceptFilterType
import com.packethunter.mobile.interception.PausedIntercept
import com.packethunter.mobile.interception.BreakpointHistory
import com.packethunter.mobile.interception.BreakpointAction
import com.packethunter.mobile.interception.AuditLogEntry
import com.packethunter.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

/**
 * Screen displaying intercepted packets matching breakpoint filters
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterceptsScreen(
    intercepts: List<InterceptItem>,
    isInterceptionEnabled: Boolean,
    enabledFilters: Set<InterceptFilterType>,
    onToggleInterception: (Boolean) -> Unit,
    onToggleFilter: (InterceptFilterType) -> Unit,
    onClearIntercepts: () -> Unit,
    onInterceptClick: (InterceptItem) -> Unit,
    // Active Breakpoint Mode parameters
    pausedIntercepts: List<PausedIntercept>,
    breakpointHistory: List<BreakpointHistory>,
    isActiveBreakpointEnabled: Boolean,
    enabledBreakpointFilters: Set<InterceptFilterType>,
    onToggleActiveBreakpoint: (Boolean) -> Unit,
    onToggleBreakpointFilter: (InterceptFilterType) -> Unit,
    onForwardPaused: (Long) -> Unit,
    onDropPaused: (Long) -> Unit,
    onClearPaused: () -> Unit,
    onClearHistory: () -> Unit
) {
    var selectedIntercept by remember { mutableStateOf<InterceptItem?>(null) }
    
    // Update countdown timers every second
    var timerTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            timerTick++
        }
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with toggle and info
        item {
            InterceptionHeader(
                isEnabled = isInterceptionEnabled,
                enabledFilters = enabledFilters,
                interceptCount = intercepts.size,
                onToggle = onToggleInterception,
                onToggleFilter = onToggleFilter,
                onClear = onClearIntercepts
            )
        }
        
        // Active Breakpoint Mode toggle
        item {
            ActiveBreakpointHeader(
                isEnabled = isActiveBreakpointEnabled,
                enabledFilters = enabledBreakpointFilters,
                pausedCount = pausedIntercepts.size,
                onToggle = onToggleActiveBreakpoint,
                onToggleFilter = onToggleBreakpointFilter,
                onClearPaused = onClearPaused
            )
        }
        
        // Info banner
        if (isInterceptionEnabled) {
            item {
                InfoBanner()
            }
        }
        
        // Paused Intercepts section
        if (isActiveBreakpointEnabled) {
            item {
                Text(
                    "Paused Intercepts (${pausedIntercepts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AlertOrange,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            if (pausedIntercepts.isEmpty()) {
                item {
                    EmptyPausedState()
                }
            } else {
                items(
                    items = pausedIntercepts,
                    key = { it.id }
                ) { paused ->
                    PausedInterceptCard(
                        paused = paused,
                        onForward = { onForwardPaused(paused.id) },
                        onDrop = { onDropPaused(paused.id) },
                        timerTick = timerTick
                    )
                }
            }
            
            // History section
            if (breakpointHistory.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "History (${breakpointHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        TextButton(onClick = onClearHistory) {
                            Text("Clear", color = AlertRed)
                        }
                    }
                }
                
                items(
                    items = breakpointHistory.take(20), // Show last 20
                    key = { it.id }
                ) { history ->
                    BreakpointHistoryCard(history = history)
                }
            }
        }
        
        // Passive Intercepts section
        item {
            Text(
                "Passive Intercepts (${intercepts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Intercepts list
        if (intercepts.isEmpty()) {
            item {
                EmptyInterceptsState(isEnabled = isInterceptionEnabled)
            }
        } else {
            items(
                items = intercepts,
                key = { it.id }
            ) { intercept ->
                InterceptCard(
                    intercept = intercept,
                    onClick = { selectedIntercept = intercept }
                )
            }
        }
    }
    
    // Detail sheet
    selectedIntercept?.let { intercept ->
        InterceptDetailSheet(
            intercept = intercept,
            onDismiss = { selectedIntercept = null }
        )
    }
}

@Composable
fun InterceptionHeader(
    isEnabled: Boolean,
    enabledFilters: Set<InterceptFilterType>,
    interceptCount: Int,
    onToggle: (Boolean) -> Unit,
    onToggleFilter: (InterceptFilterType) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "Passive Interception",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            "$interceptCount intercepted",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = NeonGreen,
                        checkedTrackColor = NeonGreen.copy(alpha = 0.5f)
                    )
                )
            }
            
            if (isEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BorderGray)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Filter chips
                Text(
                    "Active Filters:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InterceptFilterType.values().filter { it != InterceptFilterType.ALL }.forEach { filter ->
                        FilterChip(
                            selected = enabledFilters.contains(filter),
                            onClick = { onToggleFilter(filter) },
                            label = { 
                                Text(
                                    filter.name,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberBlue,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceGrayDark,
                                labelColor = TextGray
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Clear button
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AlertRed
                    ),
                    border = BorderStroke(1.5.dp, AlertRed)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Intercepts")
                }
            }
        }
    }
}

@Composable
fun InfoBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CyberCyan.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(20.dp)
            )
            Text(
                "For authorized testing only. This mode only displays observed traffic and does not modify or block requests.",
                style = MaterialTheme.typography.bodySmall,
                color = TextWhite
            )
        }
    }
}

@Composable
fun InterceptCard(
    intercept: InterceptItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Protocol badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getFilterColor(intercept.filterType).copy(alpha = 0.3f)
                    ) {
                        Text(
                            intercept.protocol,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = getFilterColor(intercept.filterType),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Filter type badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyberBlue.copy(alpha = 0.2f)
                    ) {
                        Text(
                            intercept.filterType,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Text(
                    formatTimestamp(intercept.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Summary
            Text(
                intercept.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Connection info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    intercept.sourceIp,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(":", color = TextWhite, fontFamily = FontFamily.Monospace)
                Text(
                    intercept.sourcePort.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
                Text(" → ", color = TextWhite)
                Text(
                    intercept.destIp,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(":", color = TextWhite, fontFamily = FontFamily.Monospace)
                Text(
                    intercept.destPort.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterceptDetailSheet(
    intercept: InterceptItem,
    onDismiss: () -> Unit,
    onForwardModified: ((ByteArray) -> Unit)? = null,
    onForwardOriginal: (() -> Unit)? = null,
    onDrop: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Original, 1 = Modified
    var showHex by remember { mutableStateOf(true) }
    var modifiedText by remember { mutableStateOf("") }
    var isEditable by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHttp = intercept.protocol == "HTTP" || intercept.filterType == "HTTP"
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceGray,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Intercept Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextGray)
                }
            }
            
            Divider(color = BorderGray)
            
            // Metadata
            DetailRow("Protocol", intercept.protocol, CyberCyan)
            DetailRow("Filter Type", intercept.filterType, CyberBlue)
            DetailRow("Summary", intercept.summary, TextWhite)
            DetailRow("Source", "${intercept.sourceIp}:${intercept.sourcePort}", CyberCyan)
            DetailRow("Destination", "${intercept.destIp}:${intercept.destPort}", CyberCyan)
            DetailRow("Timestamp", formatTimestampIntercept(intercept.timestamp), TextGray)
            
            // Hash display
            if (intercept.originalHash.isNotEmpty()) {
                DetailRow("Original Hash (SHA-256)", intercept.originalHash.take(16) + "...", TextGray)
            }
            
            // Payload preview with Original/Modified tabs
            if (intercept.payloadPreview != null && intercept.payloadPreview.isNotEmpty()) {
                Divider(color = BorderGray)
                
                // Tab selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Original") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceGrayDark,
                            labelColor = TextGray
                        )
                    )
                    if (isHttp) {
                        FilterChip(
                            selected = selectedTab == 1,
                            onClick = { 
                                selectedTab = 1
                                if (modifiedText.isEmpty()) {
                                    modifiedText = intercept.payloadAscii
                                }
                            },
                            label = { Text("Modified") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceGrayDark,
                                labelColor = TextGray
                            )
                        )
                    }
                }
                
                // Content based on selected tab
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundBlack)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        if (selectedTab == 0) {
                            // Original (read-only)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TextButton(
                                    onClick = { showHex = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (showHex) CyberCyan else TextGray
                                    )
                                ) {
                                    Text("Hex")
                                }
                                TextButton(
                                    onClick = { showHex = false },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = if (!showHex) CyberCyan else TextGray
                                    )
                                ) {
                                    Text("ASCII")
                                }
                            }
                            
                            Text(
                                if (showHex) intercept.payloadHex else intercept.payloadAscii,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        } else {
                            // Modified (editable for HTTP)
                            if (isHttp) {
                                androidx.compose.material3.OutlinedTextField(
                                    value = modifiedText,
                                    onValueChange = { modifiedText = it },
                                    label = { Text("Edit HTTP Request", color = TextGray) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = TextWhite,
                                        unfocusedTextColor = TextWhite,
                                        focusedBorderColor = CyberCyan,
                                        unfocusedBorderColor = BorderGray
                                    ),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                
                                val modifiedHash = if (modifiedText.isNotEmpty()) {
                                    try {
                                        AuditLogEntry.computeHash(modifiedText.toByteArray()).take(16) + "..."
                                    } catch (e: Exception) {
                                        "N/A"
                                    }
                                } else {
                                    "N/A"
                                }
                                Text(
                                    "Modified Hash: $modifiedHash",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Copy button
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = if (selectedTab == 0) {
                                if (showHex) intercept.payloadHex else intercept.payloadAscii
                            } else {
                                modifiedText
                            }
                            val clip = ClipData.newPlainText("Intercept Payload", text)
                            clipboard.setPrimaryClip(clip)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberBlue)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy")
                    }
                    
                    // Forward/Drop buttons (if callbacks provided - for paused intercepts)
                    if (onForwardOriginal != null || onForwardModified != null || onDrop != null) {
                        if (selectedTab == 1 && isHttp && modifiedText.isNotEmpty() && onForwardModified != null) {
                            Button(
                                onClick = {
                                    onForwardModified(modifiedText.toByteArray())
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text("Forward Modified")
                            }
                        }
                        if (onForwardOriginal != null) {
                            Button(
                                onClick = {
                                    onForwardOriginal()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Text("Forward")
                            }
                        }
                        if (onDrop != null) {
                            Button(
                                onClick = {
                                    onDrop()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                            ) {
                                Text("Drop")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun EmptyInterceptsState(isEnabled: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.BugReport,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(64.dp)
            )
            Text(
                if (isEnabled) "No packets intercepted yet" else "Interception is disabled",
                style = MaterialTheme.typography.titleMedium,
                color = TextGray
            )
            Text(
                if (isEnabled) "Matching packets will appear here when detected" else "Enable interception to start capturing matching packets",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

fun getFilterColor(filterType: String): Color {
    return when (filterType) {
        "HTTP" -> ProtocolHTTP
        "DNS" -> ProtocolDNS
        "TLS" -> ProtocolHTTPS
        "ICMP" -> ProtocolICMP
        else -> CyberCyan
    }
}

private fun formatTimestampIntercept(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun ActiveBreakpointHeader(
    isEnabled: Boolean,
    enabledFilters: Set<InterceptFilterType>,
    pausedCount: Int,
    onToggle: (Boolean) -> Unit,
    onToggleFilter: (InterceptFilterType) -> Unit,
    onClearPaused: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title and toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PauseCircle,
                        contentDescription = null,
                        tint = AlertOrange,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text(
                            "Active Breakpoint Mode",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            "$pausedCount paused",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = AlertOrange,
                        checkedTrackColor = AlertOrange.copy(alpha = 0.5f)
                    )
                )
            }
            
            if (isEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Pause and manually forward or drop filtered requests.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                Divider(color = BorderGray)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Filter chips
                Text(
                    "Breakpoint Filters:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InterceptFilterType.values().filter { it != InterceptFilterType.ALL }.forEach { filter ->
                        FilterChip(
                            selected = enabledFilters.contains(filter),
                            onClick = { onToggleFilter(filter) },
                            label = { 
                                Text(
                                    filter.name,
                                    style = MaterialTheme.typography.labelSmall
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AlertOrange,
                                selectedLabelColor = Color.White,
                                containerColor = SurfaceGrayDark,
                                labelColor = TextGray
                            )
                        )
                    }
                }
                
                if (pausedCount > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onClearPaused,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AlertRed
                        ),
                        border = BorderStroke(1.5.dp, AlertRed)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Paused")
                    }
                }
            }
        }
    }
}

@Composable
fun PausedInterceptCard(
    paused: PausedIntercept,
    onForward: () -> Unit,
    onDrop: () -> Unit,
    timerTick: Int
) {
    val remainingSeconds = paused.getRemainingSeconds()
    val isExpired = paused.isExpired()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) AlertRed.copy(alpha = 0.2f) else AlertOrange.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(2.dp, if (isExpired) AlertRed else AlertOrange)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Protocol badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = getFilterColor(paused.filterType).copy(alpha = 0.3f)
                    ) {
                        Text(
                            paused.protocol,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = getFilterColor(paused.filterType),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    // Filter type badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AlertOrange.copy(alpha = 0.3f)
                    ) {
                        Text(
                            paused.filterType,
                            style = MaterialTheme.typography.labelSmall,
                            color = AlertOrange,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Countdown timer
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isExpired) AlertRed else AlertOrange
                ) {
                    Text(
                        if (isExpired) "EXPIRED" else "${remainingSeconds}s",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Summary
            Text(
                paused.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextWhite
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Connection info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    paused.sourceIp,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(":", color = TextWhite, fontFamily = FontFamily.Monospace)
                Text(
                    paused.sourcePort.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
                Text(" → ", color = TextWhite)
                Text(
                    paused.destIp,
                    style = MaterialTheme.typography.bodySmall,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace
                )
                Text(":", color = TextWhite, fontFamily = FontFamily.Monospace)
                Text(
                    paused.destPort.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onForward,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Forward")
                }
                
                Button(
                    onClick = onDrop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Drop")
                }
            }
        }
    }
}

@Composable
fun BreakpointHistoryCard(history: BreakpointHistory) {
    val actionColor = when (history.action) {
        BreakpointAction.FORWARDED -> NeonGreen
        BreakpointAction.FORWARDED_MODIFIED -> CyberCyan
        BreakpointAction.DROPPED -> AlertRed
        BreakpointAction.AUTO_FORWARDED -> AlertOrange
    }
    
    val actionIcon = when (history.action) {
        BreakpointAction.FORWARDED -> Icons.Default.PlayArrow
        BreakpointAction.FORWARDED_MODIFIED -> Icons.Default.Edit
        BreakpointAction.DROPPED -> Icons.Default.Close
        BreakpointAction.AUTO_FORWARDED -> Icons.Default.Schedule
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGrayDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    actionIcon,
                    contentDescription = null,
                    tint = actionColor,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        history.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextWhite,
                        maxLines = 1
                    )
                    Text(
                        "${history.sourceIp}:${history.sourcePort} → ${history.destIp}:${history.destPort}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = actionColor.copy(alpha = 0.2f)
            ) {
                Text(
                    history.action.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = actionColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyPausedState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.PauseCircle,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(48.dp)
            )
            Text(
                "No packets paused",
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
            Text(
                "Matching packets will pause here for manual review",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

