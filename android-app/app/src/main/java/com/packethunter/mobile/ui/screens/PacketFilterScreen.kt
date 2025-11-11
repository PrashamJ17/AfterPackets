package com.packethunter.mobile.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.*
import com.packethunter.mobile.filtering.*
import com.packethunter.mobile.ui.theme.*
import com.packethunter.mobile.ui.utils.HexUtils
import java.text.SimpleDateFormat
import java.util.*

/**
 * Enhanced Packet List Screen with comprehensive filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketFilterScreen(
    packets: List<PacketInfo>,
    filteredResult: FilteredResult,
    activeFilters: ActiveFilters,
    onPacketClick: (PacketInfo) -> Unit,
    onToggleSecurityFilter: (SecurityFilter) -> Unit,
    onAddAdvancedRule: (FilterRule) -> Unit,
    onRemoveAdvancedRule: (FilterRule) -> Unit,
    onClearFilters: () -> Unit,
    selectedPacket: PacketInfo?
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedField by remember { mutableStateOf(FilterField.PROTOCOL) }
    
    // Use LazyColumn for the entire screen to enable scrolling
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Filter Header with Stats
        item {
            FilterHeaderCard(
                totalCaptured = filteredResult.totalCaptured,
                totalFiltered = filteredResult.totalFiltered,
                activeFilters = activeFilters,
                onClearFilters = onClearFilters
            )
        }
        
        // Predefined Security Filters with Horizontal Scroll
        item {
            SecurityFilterChips(
                activeFilters = activeFilters.securityFilters,
                onToggleFilter = onToggleSecurityFilter
            )
        }
        
        // Advanced Filtering Controls
        item {
            AdvancedFilterControls(
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                selectedField = selectedField,
                onFieldSelected = { selectedField = it },
                onSearch = {
                    if (searchQuery.isNotEmpty()) {
                        val rule = FilterRule(
                            field = selectedField,
                            operator = FilterOperator.CONTAINS,
                            value = searchQuery
                        )
                        onAddAdvancedRule(rule)
                        searchQuery = ""
                    }
                },
                onClear = onClearFilters
            )
        }
        
        // Suspicious Alerts Banner
        if (filteredResult.suspiciousAlerts.isNotEmpty()) {
            item {
                SuspiciousAlertsBanner(alerts = filteredResult.suspiciousAlerts)
            }
        }
        
        // Packet List
        if (filteredResult.filteredPackets.isEmpty()) {
            item {
                EmptyFilterState()
            }
        } else {
            items(
                items = filteredResult.filteredPackets,
                key = { it.id }
            ) { packet ->
                val isSuspicious = filteredResult.suspiciousAlerts.any { it.packetId == packet.id }
                PacketCard(
                    packet = packet,
                    onClick = { onPacketClick(packet) },
                    isSelected = packet == selectedPacket,
                    isSuspicious = isSuspicious,
                    suspiciousAlert = filteredResult.suspiciousAlerts.firstOrNull { it.packetId == packet.id }
                )
            }
        }
    }
}

@Composable
fun FilterHeaderCard(
    totalCaptured: Int,
    totalFiltered: Int,
    activeFilters: ActiveFilters,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CyberCyan.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.DataUsage,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxSize()
                        )
                    }
                    Column {
                        Text(
                            "Captured Packets",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Filtered:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Text(
                                "$totalFiltered",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (totalFiltered < totalCaptured) CyberCyan else NeonGreen,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "/ $totalCaptured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                        }
                    }
                }
                
                // Large count display
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CyberCyan.copy(alpha = 0.15f),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "$totalFiltered",
                            style = MaterialTheme.typography.headlineLarge,
                            color = CyberCyan,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "packets",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                    }
                }
            }
            
            // Active Filter Chips with better styling
            if (activeFilters.securityFilters.isNotEmpty() || activeFilters.advancedRules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = BorderGray, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Active:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // Security filter chips
                    activeFilters.securityFilters.forEach { filter ->
                        FilterChip(
                            label = { 
                                Text(
                                    getFilterLabel(filter), 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                ) 
                            },
                            selected = true,
                            onClick = { onClearFilters() },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberBlue,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    
                    // Advanced rule chips
                    activeFilters.advancedRules.forEach { rule ->
                        FilterChip(
                            label = { 
                                Text(
                                    "${getFieldLabel(rule.field)}: ${rule.value}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            selected = true,
                            onClick = { },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonPurple,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Clear all button
                    TextButton(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AlertRed
                        )
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Clear All",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityFilterChips(
    activeFilters: Set<SecurityFilter>,
    onToggleFilter: (SecurityFilter) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        "Security Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                }
                
                // Active filters count badge
                if (activeFilters.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CyberBlue.copy(alpha = 0.3f)
                    ) {
                        Text(
                            "${activeFilters.size} active",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Horizontal scrolling row for filter chips
            val scrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SecurityFilter.values().filter { it != SecurityFilter.ALL }.forEach { filter ->
                    val isSelected = activeFilters.contains(filter)
                    FilterChip(
                        label = { 
                            Text(
                                getFilterLabel(filter),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        selected = isSelected,
                        onClick = { onToggleFilter(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberBlue,
                            selectedLabelColor = Color.White,
                            containerColor = SurfaceGrayDark,
                            labelColor = TextGray
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .padding(vertical = 4.dp),
                        leadingIcon = if (isSelected) {
                            { 
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.White
                                )
                            }
                        } else null,
                        trailingIcon = {
                            // Add icon based on filter type
                            val icon = when (filter) {
                                SecurityFilter.HTTP -> Icons.Default.Web
                                SecurityFilter.HTTPS_TLS -> Icons.Default.Lock
                                SecurityFilter.DNS -> Icons.Default.NetworkCheck
                                SecurityFilter.ICMP -> Icons.Default.Speed
                                SecurityFilter.SUSPICIOUS_PORTS -> Icons.Default.Warning
                                SecurityFilter.LARGE_OUTBOUND -> Icons.Default.Upload
                                else -> Icons.Default.FilterList
                            }
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) Color.White.copy(alpha = 0.8f) else TextGray
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        border = if (isSelected) {
                            FilterChipDefaults.filterChipBorder(
                                borderWidth = 2.dp,
                                selectedBorderWidth = 2.dp,
                                borderColor = CyberCyan,
                                selectedBorderColor = CyberCyan
                            )
                        } else null
                    )
                }
            }
        }
    }
}

@Composable
fun AdvancedFilterControls(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedField: FilterField,
    onFieldSelected: (FilterField) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    "Advanced Filtering",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Field dropdown
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = getFieldLabel(selectedField),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Filter Field", color = TextGray) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextGray,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(SurfaceGray)
                ) {
                    FilterField.values().forEach { field ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    getFieldLabel(field),
                                    color = TextWhite
                                ) 
                            },
                            onClick = {
                                onFieldSelected(field)
                                expanded = false
                            },
                            colors = MenuDefaults.itemColors(
                                textColor = TextWhite
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Search text field with action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { 
                        Text(
                            "Search by ${getFieldLabel(selectedField).lowercase()} (e.g. ${getFieldExample(selectedField)})",
                            color = TextGray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = CyberCyan
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextGray,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = BorderGray
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                // Search button
                Button(
                    onClick = onSearch,
                    enabled = searchQuery.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberBlue,
                        disabledContainerColor = SurfaceGrayDark
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // Clear button
                OutlinedButton(
                    onClick = onClear,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AlertRed
                    ),
                    border = BorderStroke(1.5.dp, AlertRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "Clear",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}


@Composable
fun SuspiciousAlertsBanner(alerts: List<SuspiciousTrafficAlert>) {
    val severityColor = when (alerts.firstOrNull()?.severity) {
        "high" -> AlertRed
        "medium" -> AlertOrange
        else -> CyberCyan
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = severityColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "⚠️ ${alerts.size} Suspicious Traffic Alert${if (alerts.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = severityColor
                )
                Text(
                    alerts.firstOrNull()?.message ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextWhite
                )
            }
        }
    }
}

@Composable
fun PacketCard(
    packet: PacketInfo,
    onClick: () -> Unit,
    isSelected: Boolean,
    isSuspicious: Boolean = false,
    suspiciousAlert: SuspiciousTrafficAlert? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSuspicious) {
                    Modifier.border(
                        width = 2.dp,
                        color = when (suspiciousAlert?.severity) {
                            "high" -> AlertRed
                            "medium" -> AlertOrange
                            else -> CyberCyan
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurfaceGrayLight else SurfaceGray
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        packet.protocol,
                        style = MaterialTheme.typography.labelLarge,
                        color = getProtocolColorFilter(packet.protocol),
                        fontWeight = FontWeight.Bold
                    )
                    if (isSuspicious) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Suspicious",
                            tint = when (suspiciousAlert?.severity) {
                                "high" -> AlertRed
                                "medium" -> AlertOrange
                                else -> CyberCyan
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    formatTimestampFilter(packet.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Connection info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    packet.sourceIp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onClick() }
                )
                Text(":", color = TextWhite, fontFamily = FontFamily.Monospace)
                Text(
                    packet.sourcePort.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
                Text(" → ", color = TextWhite)
                Text(
                    packet.destIp,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { onClick() }
                )
                Text(":", color = TextWhite, fontFamily = FontFamily.Monospace)
                Text(
                    packet.destPort.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite,
                    fontFamily = FontFamily.Monospace
                )
            }
            
            // Suspicious alert message
            if (isSuspicious && suspiciousAlert != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (suspiciousAlert.severity) {
                            "high" -> AlertRed.copy(alpha = 0.2f)
                            "medium" -> AlertOrange.copy(alpha = 0.2f)
                            else -> CyberCyan.copy(alpha = 0.2f)
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            suspiciousAlert.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (suspiciousAlert.severity) {
                                "high" -> AlertRed
                                "medium" -> AlertOrange
                                else -> CyberCyan
                            },
                            fontWeight = FontWeight.Bold
                        )
                        if (suspiciousAlert.recommendation != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "💡 ${suspiciousAlert.recommendation}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Payload preview
            if (packet.payload != null && packet.payload.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundBlack)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "Payload Preview:",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val previewSize = minOf(32, packet.payload.size)
                        val previewBytes = packet.payload.sliceArray(0 until previewSize)
                        Text(
                            HexUtils.bytesToHex(previewBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            maxLines = 2
                        )
                    }
                }
            }
            
            // Flags and length
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (packet.flags.isNotEmpty()) {
                    Text(
                        packet.flags,
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberBlue
                    )
                }
                Text(
                    "${packet.length} bytes",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
        }
    }
}

@Composable
fun EmptyFilterState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
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
                Icons.Default.FilterList,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "No packets match the current filters",
                style = MaterialTheme.typography.titleMedium,
                color = TextGray
            )
            Text(
                "Try adjusting your filter criteria",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}


// Helper functions
fun getFilterLabel(filter: SecurityFilter): String {
    return when (filter) {
        SecurityFilter.HTTP -> "HTTP"
        SecurityFilter.HTTPS_TLS -> "HTTPS/TLS"
        SecurityFilter.DNS -> "DNS"
        SecurityFilter.ICMP -> "ICMP"
        SecurityFilter.SUSPICIOUS_PORTS -> "Suspicious Ports"
        SecurityFilter.LARGE_OUTBOUND -> "Large Outbound"
        SecurityFilter.ALL -> "All"
    }
}

fun getFieldLabel(field: FilterField): String {
    return when (field) {
        FilterField.SOURCE_IP -> "Source IP"
        FilterField.DESTINATION_IP -> "Destination IP"
        FilterField.SOURCE_PORT -> "Source Port"
        FilterField.DESTINATION_PORT -> "Destination Port"
        FilterField.PROTOCOL -> "Protocol"
    }
}

fun getFieldExample(field: FilterField): String {
    return when (field) {
        FilterField.SOURCE_IP -> "192.168.1.1"
        FilterField.DESTINATION_IP -> "8.8.8.8"
        FilterField.SOURCE_PORT -> "8080"
        FilterField.DESTINATION_PORT -> "443"
        FilterField.PROTOCOL -> "HTTP"
    }
}

fun getProtocolColorFilter(protocol: String): Color {
    return when (protocol.uppercase()) {
        "TCP" -> ProtocolTCP
        "UDP" -> ProtocolUDP
        "ICMP" -> ProtocolICMP
        "HTTP" -> ProtocolHTTP
        "HTTPS" -> ProtocolHTTPS
        "DNS" -> ProtocolDNS
        else -> TextGray
    }
}

fun formatTimestampFilter(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

