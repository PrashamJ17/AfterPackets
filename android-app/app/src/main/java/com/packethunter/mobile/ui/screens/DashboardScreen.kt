package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.CaptureStats
import com.packethunter.mobile.ui.theme.*
import com.packethunter.mobile.ui.formatBytes
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    stats: CaptureStats,
    isCapturing: Boolean,
    appTalkers: List<com.packethunter.mobile.data.AppTalker> = emptyList()
) {
    androidx.compose.foundation.lazy.LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Status banner
            StatusBanner(isCapturing = isCapturing)
        }
        
        item {
            // Real-time metrics (4 blocks)
            MetricsRow(stats = stats)
        }
        
        item {
            // Protocol distribution with pie chart
            ProtocolDistributionPieChart(protocols = stats.protocolDistribution)
        }
        
        item {
            // App Usage Pie Chart
            if (appTalkers.isNotEmpty()) {
                AppUsageChart(appTalkers = appTalkers)
            }
        }
        
        item {
            // Top Talkers Donut Chart - shows packet distribution
            TopTalkersDonutChartCard(
                appTalkers = if (appTalkers.isNotEmpty()) appTalkers else null,
                ipTalkers = if (appTalkers.isEmpty()) stats.topTalkers else null
            )
        }
        
        item {
            // Top talkers - show IP-based if no app data, otherwise show app-based
            if (appTalkers.isNotEmpty()) {
                TopTalkersByApp(appTalkers = appTalkers.take(5))
            } else {
                TopTalkers(talkers = stats.topTalkers.take(5))
            }
        }
    }
}

@Composable
fun StatusBanner(isCapturing: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCapturing) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isCapturing) Icons.Default.Circle else Icons.Default.Stop,
                    contentDescription = null,
                    tint = if (isCapturing) NeonGreen else TextGray,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = if (isCapturing) "CAPTURING" else "IDLE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isCapturing) {
                Text(
                    text = "⚡ LIVE",
                    style = MaterialTheme.typography.labelMedium,
                    color = NeonGreen
                )
            }
        }
    }
}

@Composable
fun MetricsRow(stats: CaptureStats) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Packets/s",
                value = DecimalFormat("0.0").format(stats.packetsPerSecond),
                icon = Icons.Default.Speed,
                color = CyberBlue,
                modifier = Modifier.weight(1f)
            )
            
            MetricCard(
                title = "Bandwidth",
                value = formatBytes(stats.bytesPerSecond.toLong()) + "/s",
                icon = Icons.Default.NetworkCheck,
                color = CyberCyan,
                modifier = Modifier.weight(1f)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                title = "Total Packets",
                value = stats.totalPackets.toString(),
                icon = Icons.Default.DataUsage,
                color = NeonGreen,
                modifier = Modifier.weight(1f)
            )
            
            MetricCard(
                title = "Total Data",
                value = formatBytes(stats.totalBytes),
                icon = Icons.Default.Storage,
                color = NeonPurple,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(
            containerColor = SurfaceGray
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextGray,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1
            )
        }
    }
}

@Composable
fun ProtocolDistribution(protocols: Map<String, Int>) {
    if (protocols.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Protocol Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            protocols.entries.take(5).forEach { (protocol, count) ->
                ProtocolBar(protocol = protocol, count = count, total = protocols.values.sum())
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun ProtocolDistributionPieChart(protocols: Map<String, Int>) {
    if (protocols.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Protocol Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProtocolPieChart(protocols = protocols)
        }
    }
}

@Composable
fun ProtocolBar(protocol: String, count: Int, total: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = protocol,
                style = MaterialTheme.typography.bodyMedium,
                color = getProtocolColor(protocol)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
        
        val percentage = if (total > 0) count.toFloat() / total else 0f
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceGrayDark)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(getProtocolColor(protocol))
            )
        }
    }
}

@Composable
fun TopTalkers(talkers: List<com.packethunter.mobile.data.IpTalker>) {
    if (talkers.isEmpty()) return
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "🌐 Top Talkers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            talkers.forEach { talker ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = talker.ip,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberCyan
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${talker.packetCount} pkts",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                        Text(
                            text = formatBytes(talker.bytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }
                if (talker != talkers.last()) {
                    Divider(color = BorderGray)
                }
            }
        }
    }
}

fun getProtocolColor(protocol: String): Color {
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

@Composable
fun NetworkActivityCard(
    stats: CaptureStats,
    appTalkers: List<com.packethunter.mobile.data.AppTalker>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Network Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NeonGreen
            )
            
            // Bandwidth display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        formatBandwidth(stats.bytesPerSecond),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Text(
                        "${stats.totalPackets} packets",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
            
            // Donut chart if we have app data
            if (appTalkers.isNotEmpty()) {
                Divider(color = BorderGray, modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Data Usage by App",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                com.packethunter.mobile.ui.screens.AppDataDonutChart(
                    apps = appTalkers.take(5).sortedByDescending { it.totalBytes }
                )
            }
        }
    }
}

@Composable
fun ActiveConnectionsByApp(
    appTalkers: List<com.packethunter.mobile.data.AppTalker>,
    totalCount: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Active Connections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
                Text(
                    "${appTalkers.size} active",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberCyan
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // App connections list
            if (appTalkers.isEmpty()) {
                Text(
                    "No active connections",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                appTalkers.sortedByDescending { it.totalBytes }.take(10).forEach { app ->
                    AppConnectionCard(app = app)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun AppConnectionCard(app: com.packethunter.mobile.data.AppTalker) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BackgroundBlack, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        // App name with colored dot
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(getAppColor(app.appName), CircleShape)
            )
            Text(
                app.appName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Remote hosts info (simulating IP display)
        Text(
            "${app.uniqueRemoteHosts} remote host${if (app.uniqueRemoteHosts != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = TextGray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Network Activity stats
        Text(
            "Network Activity",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "Sent",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Text(
                    formatBytes(app.sentBytes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
            Column {
                Text(
                    "Received",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Text(
                    formatBytes(app.receivedBytes),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = CyberBlue
                )
            }
            Column {
                Text(
                    "Packets",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
                Text(
                    "${app.sentPackets + app.receivedPackets}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple
                )
            }
        }
    }
}

fun getAppColor(appName: String): Color {
    val colors = listOf(CyberBlue, NeonGreen, CyberCyan, NeonPurple, AlertOrange)
    return colors[appName.hashCode().mod(colors.size)]
}

fun formatBandwidth(bps: Double): String {
    return when {
        bps < 1024 -> "${DecimalFormat("#.#").format(bps)} B/s"
        bps < 1024 * 1024 -> "${DecimalFormat("#.#").format(bps / 1024)} KB/s"
        bps < 1024 * 1024 * 1024 -> "${DecimalFormat("#.#").format(bps / (1024 * 1024))} MB/s"
        else -> "${DecimalFormat("#.#").format(bps / (1024 * 1024 * 1024))} GB/s"
    }
}

@Composable
fun AppUsageChart(appTalkers: List<com.packethunter.mobile.data.AppTalker>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "App Data Usage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
            
            if (appTalkers.isEmpty()) {
                Text(
                    "No app data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                com.packethunter.mobile.ui.screens.AppDataDonutChart(
                    apps = appTalkers.sortedByDescending { it.totalBytes }.take(5)
                )
            }
        }
    }
}

@Composable
fun TopTalkersDonutChartCard(
    appTalkers: List<com.packethunter.mobile.data.AppTalker>? = null,
    ipTalkers: List<com.packethunter.mobile.data.IpTalker>? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PieChart,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "📊 Top Talkers by Packets",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CyberCyan
                )
            }
            
            TopTalkersDonutChart(
                appTalkers = appTalkers,
                ipTalkers = ipTalkers
            )
        }
    }
}

@Composable
fun TopTalkersByApp(appTalkers: List<com.packethunter.mobile.data.AppTalker>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "🌐 Top Talkers",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NeonGreen
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (appTalkers.isEmpty()) {
                Text(
                    "No data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val sortedApps = appTalkers.sortedByDescending { it.totalBytes }
                sortedApps.forEachIndexed { index, app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App icon placeholder
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(getAppColor(app.appName)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    app.appName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            Column {
                                Text(
                                    app.appName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = CyberCyan
                                )
                                Text(
                                    "${app.sentPackets + app.receivedPackets} packets • ${app.uniqueRemoteHosts} hosts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextGray
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatBytes(app.totalBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreen
                            )
                            Text(
                                "${((app.totalBytes.toFloat() / sortedApps.sumOf { it.totalBytes }.toFloat()) * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                    if (index < sortedApps.size - 1) {
                        Divider(color = BorderGray, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

