package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.packethunter.mobile.ui.theme.*

/**
 * Legal/Confidential warning dialog for payload data
 * Shows when user first accesses payload section
 */
@Composable
fun PayloadWarningDialog(
    onDismiss: () -> Unit,
    onAccept: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = AlertOrange,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Title
                Text(
                    "⚠️ Confidential Data Warning",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AlertOrange
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Warning message
                    Text(
                        "The payload data contains sensitive network traffic information that may include:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Bullet points
                    WarningItem("Personal information and credentials")
                    WarningItem("Private communications and messages")
                    WarningItem("Encrypted data (may be decrypted)")
                    WarningItem("Sensitive business or personal data")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Divider(color = BorderGray)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Legal notice
                    Text(
                        "⚠️ Legal Notice:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed
                    )
                    
                    Text(
                        "By viewing this data, you acknowledge that:\n\n" +
                        "• You have legal authorization to capture and analyze this network traffic\n" +
                        "• You will not misuse or share confidential information\n" +
                        "• Unauthorized access to network data may be illegal in your jurisdiction\n" +
                        "• You are solely responsible for compliance with applicable laws",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // OK Button
                Button(
                    onClick = {
                        onAccept()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlertOrange
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun WarningItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            "•",
            style = MaterialTheme.typography.bodyMedium,
            color = AlertOrange,
            fontWeight = FontWeight.Bold
        )
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = TextGray,
            modifier = Modifier.weight(1f)
        )
    }
}

