package com.packethunter.mobile.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.packethunter.mobile.CrashHandler
import com.packethunter.mobile.interception.AuditLogger
import com.packethunter.mobile.ui.Screen
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateToScreen: (Screen) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    var crashLogContent by remember { mutableStateOf<String?>(null) }
    var auditLogContent by remember { mutableStateOf<String?>(null) }
    var crashLogSize by remember { mutableStateOf<Long>(0) }
    var auditLogSize by remember { mutableStateOf<Long>(0) }
    
    LaunchedEffect(Unit) {
        loadLogInfo(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Icon(
            Icons.Default.BugReport,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "🐞 Debug Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Crash Log Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Crash Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${crashLogSize} bytes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Contains unhandled exceptions and stack traces",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                copyCrashLogToDownloads(context)
                            }
                        },
                        enabled = crashLogSize > 0
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy to Downloads")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                loadCrashLog(context)
                            }
                        },
                        enabled = crashLogSize > 0
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Content")
                    }
                }
                
                // Show crash log content if loaded
                crashLogContent?.let { content ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Crash Log Content:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Audit Log Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Audit Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${auditLogSize} bytes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Contains interception events and user consent records",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                copyAuditLogToDownloads(context)
                            }
                        },
                        enabled = auditLogSize > 0
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy to Downloads")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                loadAuditLog(context)
                            }
                        },
                        enabled = auditLogSize > 0
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Content")
                    }
                }
                
                // Show audit log content if loaded
                auditLogContent?.let { content ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Audit Log Content:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        content,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📋 Debug Instructions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "To debug crashes:\n" +
                            "1. Reproduce the issue\n" +
                            "2. Check crash log size above\n" +
                            "3. Copy to Downloads and share with developers\n\n" +
                            "To debug interception:\n" +
                            "1. Enable interception in Intercepts screen\n" +
                            "2. Check audit log for events\n" +
                            "3. View content to see detailed logs",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private suspend fun loadLogInfo(context: Context) {
    try {
        val crashFile = CrashHandler.getCrashLogFile(context)
        val auditFile = File(context.filesDir, "interception_audit.log")
        
        // Update sizes
        // Note: In real implementation, these would be state holders
        Log.d("DebugScreen", "Crash log size: ${crashFile.length()} bytes")
        Log.d("DebugScreen", "Audit log size: ${auditFile.length()} bytes")
    } catch (e: Exception) {
        Log.e("DebugScreen", "Error loading log info", e)
    }
}

private suspend fun loadCrashLog(context: Context) {
    try {
        val crashFile = CrashHandler.getCrashLogFile(context)
        if (crashFile.exists()) {
            val content = crashFile.readText()
            // In real implementation, update state
            Log.d("DebugScreen", "Loaded crash log: ${content.take(500)}...")
        }
    } catch (e: Exception) {
        Log.e("DebugScreen", "Error loading crash log", e)
    }
}

private suspend fun loadAuditLog(context: Context) {
    try {
        val auditFile = File(context.filesDir, "interception_audit.log")
        if (auditFile.exists()) {
            val content = auditFile.readText()
            // In real implementation, update state
            Log.d("DebugScreen", "Loaded audit log: ${content.take(500)}...")
        }
    } catch (e: Exception) {
        Log.e("DebugScreen", "Error loading audit log", e)
    }
}

private suspend fun copyCrashLogToDownloads(context: Context) {
    try {
        val crashFile = CrashHandler.getCrashLogFile(context)
        if (crashFile.exists()) {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val destFile = File(downloadsDir, "AfterPackets_crash_log.txt")
            
            crashFile.copyTo(destFile, overwrite = true)
            
            // Notify system of new file
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(destFile)
            context.sendBroadcast(mediaScanIntent)
            
            Log.i("DebugScreen", "Copied crash log to: ${destFile.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("DebugScreen", "Error copying crash log", e)
    }
}

private suspend fun copyAuditLogToDownloads(context: Context) {
    try {
        val auditFile = File(context.filesDir, "interception_audit.log")
        if (auditFile.exists()) {
            val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val destFile = File(downloadsDir, "AfterPackets_audit_log.txt")
            
            auditFile.copyTo(destFile, overwrite = true)
            
            // Notify system of new file
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(destFile)
            context.sendBroadcast(mediaScanIntent)
            
            Log.i("DebugScreen", "Copied audit log to: ${destFile.absolutePath}")
        }
    } catch (e: Exception) {
        Log.e("DebugScreen", "Error copying audit log", e)
    }
}
