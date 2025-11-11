package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.ui.theme.*

@Composable
fun ExportScreen(
    isExporting: Boolean,
    exportResult: String?,
    onExportPcap: () -> Unit,
    onExportJson: () -> Unit,
    onExportBundle: () -> Unit,
    onClearResult: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📤 Export Capture Data",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Export captured packets for analysis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
        }
        
        // Info card about export location
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CyberCyan.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        "Files saved to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(
                        "Downloads/PacketHunter/",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Visible in Files app after export",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }
        
        // Export options
        ExportOption(
            title = "PCAP Format",
            description = "Wireshark-compatible packet capture file",
            icon = Icons.Default.FileDownload,
            onClick = onExportPcap,
            enabled = !isExporting
        )
        
        ExportOption(
            title = "JSON Metadata",
            description = "Detailed packet metadata and statistics",
            icon = Icons.Default.DataObject,
            onClick = onExportJson,
            enabled = !isExporting
        )
        
        ExportOption(
            title = "Evidence Bundle",
            description = "Complete forensic package (PCAP + JSON + Timeline + Alerts)",
            icon = Icons.Default.FolderZip,
            onClick = onExportBundle,
            enabled = !isExporting,
            highlighted = true
        )
        
        if (isExporting) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CyberBlueDark.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Exporting...", color = CyberBlue)
                }
            }
        }
        
        exportResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("✅ Success", color = NeonGreen, fontWeight = FontWeight.Bold)
                        Text(result, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onClearResult) {
                        Icon(Icons.Default.Close, "Clear", tint = TextGray)
                    }
                }
            }
        }
    }
}

@Composable
fun ExportOption(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    enabled: Boolean,
    highlighted: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) CyberBlueDark.copy(alpha = 0.3f) else SurfaceGray
        ),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (highlighted) CyberBlue else TextWhite
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (highlighted) CyberBlue else TextWhite
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextGray)
        }
    }
}
