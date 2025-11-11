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
import com.packethunter.mobile.ui.theme.*
import com.packethunter.mobile.ui.formatBytes

@Composable
fun AppDataDonutChart(apps: List<AppTalker>) {
    val totalBytes = apps.sumOf { it.totalBytes }.toFloat()
    if (totalBytes == 0f) return
    
    val colors = listOf(CyberBlue, NeonGreen, CyberCyan, NeonPurple, AlertOrange)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Donut Chart
        Canvas(
            modifier = Modifier.size(150.dp)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2
            val strokeWidth = 40f
            val innerRadius = radius - strokeWidth
            
            var startAngle = -90f
            
            apps.forEachIndexed { index, app ->
                val sweepAngle = (app.totalBytes / totalBytes) * 360f
                
                drawArc(
                    color = colors[index % colors.size],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(canvasSize - strokeWidth, canvasSize - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            apps.forEachIndexed { index, app ->
                val percentage = (app.totalBytes / totalBytes * 100).toInt()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[index % colors.size], CircleShape)
                    )
                    Column {
                        Text(
                            app.appName.take(15),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            "$percentage% • ${formatBytes(app.totalBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
            }
        }
    }
}

