package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.Alert
import com.packethunter.mobile.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertsScreen(
    alerts: List<Alert>,
    onAcknowledge: (Long) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                    "⚠️ Security Alerts",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${alerts.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = AlertRed
                )
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(alerts) { alert ->
                AlertCard(alert = alert, onAcknowledge = { onAcknowledge(alert.id) })
            }
        }
    }
}

@Composable
fun AlertCard(alert: Alert, onAcknowledge: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (alert.severity) {
                "critical" -> AlertRedDark.copy(alpha = 0.2f)
                "high" -> AlertOrange.copy(alpha = 0.2f)
                "medium" -> AlertYellow.copy(alpha = 0.2f)
                else -> SurfaceGray
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = alert.severity.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = getSeverityColor(alert.severity),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        formatTimestamp(alert.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray
                    )
                }
                
                if (!alert.acknowledged) {
                    IconButton(onClick = onAcknowledge) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Acknowledge",
                            tint = NeonGreen
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = alert.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray
            )
        }
    }
}

fun getSeverityColor(severity: String): androidx.compose.ui.graphics.Color {
    return when (severity.lowercase()) {
        "critical" -> SeverityCritical
        "high" -> SeverityHigh
        "medium" -> SeverityMedium
        "low" -> SeverityLow
        else -> TextGray
    }
}
