package com.packethunter.mobile.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.PacketInfo
import com.packethunter.mobile.ui.theme.*
import com.packethunter.mobile.ui.utils.HexUtils
import com.packethunter.mobile.ui.utils.PreferenceManager
import java.util.Locale

/**
 * Packet Detail Screen - Shows detailed information about a captured packet
 * including hex/ASCII payload view (Deep Packet Inspection)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PacketDetailScreen(
    packet: PacketInfo,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    // Session-based flag (resets when app restarts)
    var hasSeenWarningThisSession by remember { mutableStateOf(false) }
    var showPayloadWarning by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Packet Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceGray
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Protocol Badge
            ProtocolHeader(packet = packet)
            
            // Connection Information
            ConnectionInfoCard(packet = packet)
            
            // Layer Information
            LayerInfoCard(packet = packet)
            
            // Traffic & Statistics
            TrafficStatsCard(packet = packet)
            
            // Protocol-Specific Information
            ProtocolSpecificCard(packet = packet)
            
            // Hex/ASCII Payload View (Deep Packet Inspection)
            PayloadHexView(
                packet = packet,
                onPayloadViewRequested = {
                    // Show warning on first payload view in this session
                    if (!hasSeenWarningThisSession && packet.payload != null && packet.payload.isNotEmpty()) {
                        showPayloadWarning = true
                    }
                }
            )
        }
        
        // Show payload warning dialog when payload is first viewed
        if (showPayloadWarning) {
            PayloadWarningDialog(
                onDismiss = {
                    // Mark as seen for this session
                    hasSeenWarningThisSession = true
                    showPayloadWarning = false
                },
                onAccept = {
                    // Mark as seen for this session and persist preference
                    preferenceManager.setPayloadWarningShown()
                    hasSeenWarningThisSession = true
                    showPayloadWarning = false
                }
            )
        }
    }
}

@Composable
fun ProtocolHeader(packet: PacketInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Protocol",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    packet.protocol,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = getProtocolColor(packet.protocol)
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Length",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${packet.length} bytes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
        }
    }
}

@Composable
fun ConnectionInfoCard(packet: PacketInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Connection Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Source
            InfoRow(
                label = "Source IP",
                value = packet.sourceIp,
                icon = Icons.Default.ArrowUpward
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow(
                label = "Source Port",
                value = packet.sourcePort.toString(),
                icon = Icons.Default.Numbers
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = BorderGray)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Destination
            InfoRow(
                label = "Destination IP",
                value = packet.destIp,
                icon = Icons.Default.ArrowDownward
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow(
                label = "Destination Port",
                value = packet.destPort.toString(),
                icon = Icons.Default.Numbers
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Divider(color = BorderGray)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Timestamp
            InfoRow(
                label = "Timestamp",
                value = formatTimestampDetail(packet.timestamp),
                icon = Icons.Default.Schedule
            )
            
            // Direction
            if (packet.direction.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "Direction",
                    value = packet.direction,
                    icon = Icons.Default.CompareArrows
                )
            }
        }
    }
}

@Composable
fun LayerInfoCard(packet: PacketInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Layer Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = CyberBlue
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Protocol
            InfoRow(
                label = "Protocol",
                value = packet.protocol,
                icon = Icons.Default.Code
            )
            
            // Flags
            if (packet.flags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "Flags",
                    value = packet.flags,
                    icon = Icons.Default.Flag
                )
            }
            
            // Session ID
            if (packet.sessionId.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                InfoRow(
                    label = "Session ID",
                    value = packet.sessionId,
                    icon = Icons.Default.Link
                )
            }
        }
    }
}

@Composable
fun TrafficStatsCard(packet: PacketInfo) {
    // Determine status based on flags
    val status = when {
        packet.flags.contains("SYN") && !packet.flags.contains("ACK") -> "Establishing"
        packet.flags.contains("SYN") && packet.flags.contains("ACK") -> "Established"
        packet.flags.contains("FIN") -> "Closing"
        packet.flags.contains("RST") -> "Reset"
        packet.flags.contains("ACK") -> "Active"
        else -> "Unknown"
    }
    
    val payloadBytes = packet.payload?.size ?: 0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Traffic & Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonPurple
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status
            InfoRow(
                label = "Status",
                value = status,
                icon = Icons.Default.Info
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Total packet size
            InfoRow(
                label = "Packet Size",
                value = "${packet.length} bytes",
                icon = Icons.Default.DataUsage
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Payload bytes
            InfoRow(
                label = "Payload Bytes",
                value = if (payloadBytes > 0) "$payloadBytes bytes" else "No payload",
                icon = Icons.Default.Storage
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Direction
            InfoRow(
                label = "Traffic Direction",
                value = packet.direction.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                icon = Icons.Default.CompareArrows
            )
        }
    }
}

@Composable
fun ProtocolSpecificCard(packet: PacketInfo) {
    val hasProtocolInfo = packet.httpMethod != null || 
                         packet.httpUrl != null || 
                         packet.dnsQuery != null || 
                         packet.dnsResponse != null || 
                         packet.tlsSni != null
    
    if (!hasProtocolInfo) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Protocol-Specific Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonPurple
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // HTTP
            if (packet.httpMethod != null) {
                InfoRow(
                    label = "HTTP Method",
                    value = packet.httpMethod,
                    icon = Icons.Default.Http
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (packet.httpUrl != null) {
                InfoRow(
                    label = "HTTP URL",
                    value = packet.httpUrl,
                    icon = Icons.Default.Language
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // DNS
            if (packet.dnsQuery != null) {
                InfoRow(
                    label = "DNS Query",
                    value = packet.dnsQuery,
                    icon = Icons.Default.Search
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            if (packet.dnsResponse != null) {
                InfoRow(
                    label = "DNS Response",
                    value = packet.dnsResponse,
                    icon = Icons.Default.CheckCircle
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // TLS
            if (packet.tlsSni != null) {
                InfoRow(
                    label = "TLS SNI",
                    value = packet.tlsSni,
                    icon = Icons.Default.Lock
                )
            }
        }
    }
}

@Composable
fun PayloadHexView(
    packet: PacketInfo,
    onPayloadViewRequested: () -> Unit = {}
) {
    // Trigger callback when payload section is rendered
    LaunchedEffect(packet.id) {
        if (packet.payload != null && packet.payload.isNotEmpty()) {
            onPayloadViewRequested()
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Payload (Hex/ASCII)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AlertOrange
                )
                
                if (packet.payload != null) {
                    Text(
                        "${packet.payload.size} bytes",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (packet.payload == null || packet.payload.isEmpty()) {
                Text(
                    "No payload data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                // Hex dump view
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundBlack)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Offset",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray,
                                modifier = Modifier.width(80.dp)
                            )
                            Text(
                                "Hex",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "ASCII",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextGray,
                                modifier = Modifier.width(80.dp)
                            )
                        }
                        
                        Divider(
                            color = BorderGray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        // Hex dump lines
                        val hexLines = HexUtils.formatHexDump(packet.payload)
                        hexLines.forEach { line ->
                            Text(
                                line,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = TextWhite,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = TextGray
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextWhite,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

fun formatTimestampDetail(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}

