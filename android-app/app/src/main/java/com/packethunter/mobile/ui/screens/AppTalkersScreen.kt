package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.PacketInfo
import com.packethunter.mobile.ui.theme.*
import com.packethunter.mobile.ui.formatBytes
import com.packethunter.mobile.ui.utils.HexUtils
import kotlin.math.min

/**
 * Data class for IP-based traffic statistics
 */
data class IpTrafficStats(
    val ip: String,
    val packetCount: Int,
    val totalBytes: Long,
    val sentBytes: Long,
    val receivedBytes: Long,
    val sentPackets: Int,
    val receivedPackets: Int,
    val protocols: Set<String>,
    val packets: List<PacketInfo>
)

/**
 * AppTalkersScreen - Now shows IP address-based data usage instead of app names
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTalkersScreen(
    packets: List<PacketInfo>,
    onPacketClick: (PacketInfo) -> Unit = {}
) {
    // Group packets by IP address (source and destination)
    val ipStats = packets.groupBy { it.sourceIp }
        .map { (ip, pkts) ->
            val destPackets = packets.filter { it.destIp == ip }
            val allPackets = (pkts + destPackets).distinctBy { it.id }
            
            IpTrafficStats(
                ip = ip,
                packetCount = allPackets.size,
                totalBytes = allPackets.sumOf { it.length.toLong() },
                sentBytes = pkts.sumOf { it.length.toLong() },
                receivedBytes = destPackets.sumOf { it.length.toLong() },
                sentPackets = pkts.size,
                receivedPackets = destPackets.size,
                protocols = allPackets.map { it.protocol }.toSet(),
                packets = allPackets
            )
        }
        .sortedByDescending { it.totalBytes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.DeviceHub,
                        contentDescription = null,
                        tint = NeonGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            "📦 Packet Data Usage",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreen
                        )
                        Text(
                            "Network traffic by IP address",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray
                        )
                    }
                }
            }
        }

        // Stats Summary
        if (ipStats.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGray)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            ipStats.size.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan
                        )
                        Text(
                            "IP Addresses",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SurfaceGray)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            formatBytes(ipStats.sumOf { it.totalBytes }),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                        Text(
                            "Total Data",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }
        }
        
        // IP Traffic List
        if (ipStats.isEmpty()) {
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
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        "No packet data yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextGray
                    )
                    Text(
                        "Start capturing to see network traffic by IP address",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ipStats) { stat ->
                    IpTrafficCard(
                        stat = stat,
                        onClick = { 
                            // Show first packet as example, or could show all packets
                            stat.packets.firstOrNull()?.let { onPacketClick(it) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun IpTrafficCard(
    stat: IpTrafficStats,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // IP header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // IP icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CyberBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.DeviceHub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            stat.ip,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "${stat.packetCount} packets",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                
                // Total data
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        formatBytes(stat.totalBytes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Text(
                        "Total",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = BorderGray)
            Spacer(modifier = Modifier.height(12.dp))
            
            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    StatItem(
                        icon = Icons.Default.ArrowUpward,
                        label = "Sent",
                        value = "${stat.sentPackets} pkts",
                        sublabel = formatBytes(stat.sentBytes),
                        color = ProtocolHTTPS
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    StatItem(
                        icon = Icons.Default.ArrowDownward,
                        label = "Received",
                        value = "${stat.receivedPackets} pkts",
                        sublabel = formatBytes(stat.receivedBytes),
                        color = ProtocolDNS
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Protocols
            if (stat.protocols.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Protocols: ${stat.protocols.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    sublabel: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
            Text(
                sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}
