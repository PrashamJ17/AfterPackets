package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.IpTalker
import com.packethunter.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(talkers: List<IpTalker>) {
    // Map functionality disabled - Geolocation unavailable
    // Future: Add MaxMind GeoLite2 offline DB integration as opt-in
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Map,
            contentDescription = "Map Disabled",
            modifier = Modifier.size(64.dp),
            tint = TextGray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Map Disabled",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = CyberCyan
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Geolocation unavailable.\n" +
            "Map functionality has been disabled.\n\n" +
            "Future: MaxMind GeoLite2 offline database integration will be available as an opt-in feature.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextGray,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (talkers.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Top Talkers (${talkers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    talkers.take(10).forEach { talker ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                talker.ip,
                                color = CyberCyan,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "${talker.packetCount} pkts",
                                color = TextGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
