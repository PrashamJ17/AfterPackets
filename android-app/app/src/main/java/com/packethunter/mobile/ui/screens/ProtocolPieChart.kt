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
import com.packethunter.mobile.ui.theme.*

@Composable
fun ProtocolPieChart(protocols: Map<String, Int>) {
    if (protocols.isEmpty()) return
    
    val total = protocols.values.sum().toFloat()
    if (total == 0f) return
    
    val sortedProtocols = protocols.toList().sortedByDescending { it.second }.take(5)
    val colors = listOf(ProtocolTCP, ProtocolUDP, ProtocolHTTP, ProtocolHTTPS, ProtocolDNS, ProtocolICMP, CyberCyan, NeonPurple)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie Chart
        Canvas(
            modifier = Modifier.size(200.dp)
        ) {
            val canvasSize = size.minDimension
            
            var startAngle = -90f
            
            sortedProtocols.forEachIndexed { index, (_, count) ->
                val sweepAngle = (count / total) * 360f
                
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(0f, 0f),
                    size = Size(canvasSize, canvasSize)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Legend
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sortedProtocols.forEachIndexed { index, (protocol, count) ->
                val percentage = (count / total * 100).toInt()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Column {
                        Text(
                            protocol,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            "$percentage% • $count packets",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

