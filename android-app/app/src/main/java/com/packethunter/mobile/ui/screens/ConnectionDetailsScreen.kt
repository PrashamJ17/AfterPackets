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
import com.packethunter.mobile.ui.formatBytes
import com.packethunter.mobile.ui.utils.HexUtils
import com.packethunter.mobile.ui.utils.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*

/**
 * Connection Details Screen - Shows aggregated information about a connection
 * Similar to PCAPdroid's connection details view
 * 
 * Aggregates all packets for a connection (same source/dest IP:port pair)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionDetailsScreen(
    connectionPackets: List<PacketInfo>,
    onBack: () -> Unit,
    onPacketClick: (PacketInfo) -> Unit = {}
) {
    if (connectionPackets.isEmpty()) {
        // Empty state
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Connection Details", fontWeight = FontWeight.Bold) },
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No packets found for this connection", color = TextGray)
            }
        }
        return
    }
    
    // Get first packet for connection info
    val firstPacket = connectionPackets.first()
    val lastPacket = connectionPackets.last()
    
    // Calculate connection statistics
    val sentPackets = connectionPackets.filter { it.direction == "outbound" || it.sourceIp == firstPacket.sourceIp }
    val receivedPackets = connectionPackets.filter { it.direction == "inbound" || it.destIp == firstPacket.sourceIp }
    
    val sentBytes = sentPackets.sumOf { it.length.toLong() }
    val receivedBytes = receivedPackets.sumOf { it.length.toLong() }
    val totalBytes = sentBytes + receivedBytes
    
    val payloadBytes = connectionPackets.sumOf { (it.payload?.size ?: 0).toLong() }
    
    val duration = lastPacket.timestamp - firstPacket.timestamp
    val durationText = when {
        duration < 1000 -> "< 1 s"
        duration < 60000 -> "${duration / 1000} s"
        else -> "${duration / 60000} m ${(duration % 60000) / 1000} s"
    }
    
    // Determine connection status
    val status = when {
        connectionPackets.any { it.flags.contains("FIN") || it.flags.contains("RST") } -> "Closed"
        connectionPackets.any { it.flags.contains("SYN") && !it.flags.contains("ACK") } -> "Establishing"
        connectionPackets.any { it.flags.contains("SYN") && it.flags.contains("ACK") } -> "Established"
        else -> "Active"
    }
    
    var selectedTab by remember { mutableStateOf(0) }
    
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }
    
    // Session-based flag (resets when app restarts)
    var hasSeenWarningThisSession by remember { mutableStateOf(false) }
    var showPayloadWarning by remember { mutableStateOf(false) }
    
    // Show warning when PAYLOAD tab is first selected in this session
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && !hasSeenWarningThisSession) {
            val hasPayload = connectionPackets.any { it.payload != null && it.payload.isNotEmpty() }
            if (hasPayload) {
                showPayloadWarning = true
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connection Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Copy connection info */ }) {
                        Icon(Icons.Default.ContentCopy, "Copy")
                    }
                    IconButton(onClick = { /* Share connection info */ }) {
                        Icon(Icons.Default.Share, "Share")
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
            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("OVERVIEW") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("PAYLOAD") }
                )
            }
            
            when (selectedTab) {
                0 -> OverviewTab(
                    firstPacket = firstPacket,
                    lastPacket = lastPacket,
                    sentPackets = sentPackets.size,
                    receivedPackets = receivedPackets.size,
                    sentBytes = sentBytes,
                    receivedBytes = receivedBytes,
                    totalBytes = totalBytes,
                    payloadBytes = payloadBytes,
                    duration = durationText,
                    status = status,
                    connectionPackets = connectionPackets
                )
                1 -> PayloadTab(connectionPackets = connectionPackets)
            }
        }
        
        // Show payload warning dialog when PAYLOAD tab is first viewed
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
fun OverviewTab(
    firstPacket: PacketInfo,
    lastPacket: PacketInfo,
    sentPackets: Int,
    receivedPackets: Int,
    sentBytes: Long,
    receivedBytes: Long,
    totalBytes: Long,
    payloadBytes: Long,
    duration: String,
    status: String,
    connectionPackets: List<PacketInfo>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // App (if available)
        DetailCard(
            title = "App",
            items = listOf(
                DetailItem("Package", "Unknown (-1)", Icons.Default.Apps)
            )
        )
        
        // Protocol
        DetailCard(
            title = "Protocol",
            items = listOf(
                DetailItem(
                    "Protocol",
                    "${firstPacket.protocol} (${if (firstPacket.protocol == "TCP") "TCP" else "UDP"})",
                    Icons.Default.Code
                )
            )
        )
        
        // Query/URL (if available)
        val query = firstPacket.dnsQuery ?: firstPacket.httpUrl ?: firstPacket.tlsSni
        if (query != null) {
            DetailCard(
                title = "Query",
                items = listOf(
                    DetailItem("Query", query, Icons.Default.Search)
                )
            )
        }
        
        // Source
        DetailCard(
            title = "Source",
            items = listOf(
                DetailItem(
                    "Source",
                    "${firstPacket.sourceIp}:${firstPacket.sourcePort}",
                    Icons.Default.ArrowUpward,
                    fontFamily = FontFamily.Monospace
                )
            )
        )
        
        // Destination
        DetailCard(
            title = "Destination",
            items = listOf(
                DetailItem(
                    "Destination",
                    "${firstPacket.destIp}:${firstPacket.destPort}",
                    Icons.Default.ArrowDownward,
                    fontFamily = FontFamily.Monospace
                )
            )
        )
        
        // Status
        DetailCard(
            title = "Status",
            items = listOf(
                DetailItem("Status", status, Icons.Default.Info)
            )
        )
        
        // Traffic
        DetailCard(
            title = "Traffic",
            items = listOf(
                DetailItem(
                    "Traffic",
                    "${formatBytes(receivedBytes)} received - ${formatBytes(sentBytes)} sent",
                    Icons.Default.CompareArrows
                )
            )
        )
        
        // Packets
        DetailCard(
            title = "Packets",
            items = listOf(
                DetailItem(
                    "Packets",
                    "$receivedPackets received - $sentPackets sent",
                    Icons.Default.DataUsage
                )
            )
        )
        
        // Payload
        DetailCard(
            title = "Payload",
            items = listOf(
                DetailItem(
                    "Payload",
                    formatBytes(payloadBytes),
                    Icons.Default.Storage
                )
            )
        )
        
        // Duration
        DetailCard(
            title = "Duration",
            items = listOf(
                DetailItem("Duration", duration, Icons.Default.Schedule)
            )
        )
        
        // Timestamps
        DetailCard(
            title = "Timestamps",
            items = listOf(
                DetailItem(
                    "First seen",
                    formatConnectionTimestamp(firstPacket.timestamp),
                    Icons.Default.AccessTime,
                    fontFamily = FontFamily.Monospace
                ),
                DetailItem(
                    "Last seen",
                    formatConnectionTimestamp(lastPacket.timestamp),
                    Icons.Default.AccessTime,
                    fontFamily = FontFamily.Monospace
                )
            )
        )
    }
}

@Composable
fun PayloadTab(connectionPackets: List<PacketInfo>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Show payload for each packet in the connection
        connectionPackets.forEachIndexed { index, packet ->
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
                            "Packet ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            formatConnectionTimestamp(packet.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (packet.payload != null && packet.payload.isNotEmpty()) {
                        Text(
                            "Payload: ${formatBytes(packet.payload.size.toLong())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Hex dump
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BackgroundBlack)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
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
                    } else {
                        Text(
                            "No payload data available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailCard(
    title: String,
    items: List<DetailItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        item.icon,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextGray
                        )
                        Text(
                            item.value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = TextWhite,
                            fontFamily = item.fontFamily
                        )
                    }
                }
                
                if (item != items.last()) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

data class DetailItem(
    val label: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val fontFamily: androidx.compose.ui.text.font.FontFamily? = null
)

fun formatConnectionTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM/dd/yy HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

