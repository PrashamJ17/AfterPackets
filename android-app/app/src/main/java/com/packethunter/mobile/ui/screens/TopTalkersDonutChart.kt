package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.AppTalker
import com.packethunter.mobile.data.IpTalker
import com.packethunter.mobile.ui.theme.*

/**
 * Donut chart showing packet distribution by top talkers (apps or IPs)
 */
@Composable
fun TopTalkersDonutChart(
    appTalkers: List<AppTalker>? = null,
    ipTalkers: List<IpTalker>? = null
) {
    // Determine which data to use
    val data = if (!appTalkers.isNullOrEmpty()) {
        appTalkers.map { 
            TalkerData(
                label = it.appName,
                packetCount = it.sentPackets + it.receivedPackets,
                sentPackets = it.sentPackets,
                receivedPackets = it.receivedPackets
            )
        }
    } else if (!ipTalkers.isNullOrEmpty()) {
        ipTalkers.map { 
            TalkerData(
                label = it.ip,
                packetCount = it.packetCount,
                sentPackets = 0, // IpTalker doesn't have this breakdown
                receivedPackets = 0
            )
        }
    } else {
        emptyList()
    }
    
    if (data.isEmpty()) {
        Text(
            "No packet data available",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    
    val topTalkers = data.sortedByDescending { it.packetCount }.take(5)
    val totalPackets = topTalkers.sumOf { it.packetCount }.toFloat()
    
    if (totalPackets == 0f) {
        Text(
            "No packets captured yet",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            modifier = Modifier.padding(16.dp)
        )
        return
    }
    
    val colors = listOf(
        CyberBlue,
        CyberCyan,
        NeonGreen,
        NeonPurple,
        AlertOrange,
        ProtocolTCP,
        ProtocolUDP,
        ProtocolHTTP
    )
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Chart
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val canvasSize = size.minDimension
            val center = Offset(canvasSize / 2, canvasSize / 2)
            val radius = canvasSize / 2 - 20f
            val innerRadius = radius * 0.5f // Donut hole
            val strokeWidth = radius - innerRadius
            
            var startAngle = -90f
            
            topTalkers.forEachIndexed { index, talker ->
                val sweepAngle = (talker.packetCount / totalPackets) * 360f
                val color = colors[index % colors.size]
                
                // Draw outer arc (donut)
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(
                        width = strokeWidth,
                        cap = StrokeCap.Round
                    )
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            topTalkers.forEachIndexed { index, talker ->
                val percentage = (talker.packetCount / totalPackets * 100).toInt()
                val color = colors[index % colors.size]
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .background(color, CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                talker.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                maxLines = 1
                            )
                            Text(
                                "$percentage% • ${talker.packetCount} packets",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                    
                    // Show sent/received breakdown if available
                    if (talker.sentPackets > 0 || talker.receivedPackets > 0) {
                        Row(
                            modifier = Modifier.padding(start = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (talker.sentPackets > 0) {
                                Text(
                                    "↑ ${talker.sentPackets} sent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                            if (talker.receivedPackets > 0) {
                                Text(
                                    "↓ ${talker.receivedPackets} recv",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Data class for talker information
 */
private data class TalkerData(
    val label: String,
    val packetCount: Int,
    val sentPackets: Int,
    val receivedPackets: Int
)

