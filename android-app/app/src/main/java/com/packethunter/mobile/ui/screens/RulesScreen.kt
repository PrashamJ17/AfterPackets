package com.packethunter.mobile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.data.DetectionRule
import com.packethunter.mobile.ui.theme.*

@Composable
fun RulesScreen(
    rules: List<DetectionRule>,
    onAddRule: (DetectionRule) -> Unit,
    onUpdateRule: (DetectionRule) -> Unit,
    onDeleteRule: (DetectionRule) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceGray)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "⚖️ Detection Rules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, "Add Rule", tint = CyberBlue)
                }
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(rules) { rule ->
                RuleCard(
                    rule = rule,
                    onToggle = { onUpdateRule(rule.copy(enabled = !rule.enabled)) },
                    onDelete = { onDeleteRule(rule) }
                )
            }
        }
    }
    
    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { rule ->
                onAddRule(rule)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun RuleCard(
    rule: DetectionRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) SurfaceGray else SurfaceGrayDark
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (rule.enabled) TextWhite else TextGray
                )
                Row {
                    Switch(
                        checked = rule.enabled,
                        onCheckedChange = { onToggle() }
                    )
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = AlertRed)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "IF ${rule.metric} ${rule.condition} ${rule.threshold} IN ${rule.timeWindowSeconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = CyberCyan
            )
            
            Text(
                "THEN ${rule.action} [${rule.severity}]",
                style = MaterialTheme.typography.bodySmall,
                color = TextGray
            )
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (DetectionRule) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var metric by remember { mutableStateOf("outbound_bytes") }
    var threshold by remember { mutableStateOf("1000000") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Detection Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Threshold") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(
                    DetectionRule(
                        name = name,
                        metric = metric,
                        condition = ">",
                        threshold = threshold.toDoubleOrNull() ?: 1000000.0,
                        timeWindowSeconds = 10,
                        action = "alert"
                    )
                )
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
