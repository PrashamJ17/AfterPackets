package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.PacketInfo
import com.packethunter.mobile.ui.theme.*
import com.packethunter.mobile.ui.utils.HexUtils
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PacketListScreen(
    packets: List<PacketInfo>,
    onPacketClick: (PacketInfo) -> Unit,
    selectedPacket: PacketInfo?
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "📦 Captured Packets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${packets.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CyberCyan
                )
            }
        }
        
        // Packet list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(packets) { packet ->
                PacketCard(
                    packet = packet,
                    onClick = { onPacketClick(packet) },
                    isSelected = packet == selectedPacket
                )
            }
        }
    }
}

@Composable
fun PacketCard(
    packet: PacketInfo,
    onClick: () -> Unit,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                Text(
                    packet.protocol,
                    style = MaterialTheme.typography.labelLarge,
                    color = getProtocolColor(packet.protocol),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatTimestamp(packet.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Connection info (clickable IP addresses)
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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Payload preview (Hex/ASCII)
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
                        
                        // Hex preview (first 32 bytes)
                        val previewSize = minOf(32, packet.payload.size)
                        val previewBytes = packet.payload.sliceArray(0 until previewSize)
                        Text(
                            HexUtils.bytesToHex(previewBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            maxLines = 2
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // ASCII preview
                        Text(
                            HexUtils.bytesToAscii(previewBytes),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = TextGray
                        )
                        
                        if (packet.payload.size > 32) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "... (${packet.payload.size - 32} more bytes) - Tap to view full payload",
                                style = MaterialTheme.typography.labelSmall,
                                color = AlertOrange
                            )
                        }
                    }
                }
            } else if (!packet.payloadPreview.isNullOrEmpty()) {
                // Fallback to text preview if no raw payload
                Text(
                    packet.payloadPreview.take(50) + if (packet.payloadPreview.length > 50) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray,
                    fontFamily = FontFamily.Monospace
                )
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

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
