package com.packethunter.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.packethunter.mobile.capture.PacketCaptureService
import com.packethunter.mobile.interception.AuthorizedTestingMode
import com.packethunter.mobile.ui.MainViewModel
import com.packethunter.mobile.ui.PacketHunterApp
import com.packethunter.mobile.ui.theme.MobilePacketHunterTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var vpnPermissionGranted = false

    private val vpnPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        vpnPermissionGranted = result.resultCode == RESULT_OK
        if (vpnPermissionGranted) {
            startCaptureService()
        } else {
            Toast.makeText(this, "VPN permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                this,
                "Notification permission recommended for alerts",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Toast.makeText(
                this,
                "Storage permission granted - exports will be saved to Downloads",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            Toast.makeText(
                this,
                "Storage permission denied - exports may not be visible in Files app",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private val fileImportLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { fileUri ->
            try {
                val inputStream = contentResolver.openInputStream(fileUri)
                inputStream?.use { stream ->
                    val jsonContent = stream.bufferedReader().use { it.readText() }
                    viewModel.importFilterPresetsFromJson(jsonContent)
                    Toast.makeText(this, "Filter presets imported successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error importing file: ${e.message}", e)
                Toast.makeText(this, "Error importing file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize AuthorizedTestingMode
        AuthorizedTestingMode.initialize(this)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Request storage permission for Android 6.0 to 12
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        // Start observing service data
        startObservingServiceData()

        setContent {
            MobilePacketHunterTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PacketHunterApp(
                        viewModel = viewModel,
                        onStartCapture = { requestVpnPermissionAndStart() },
                        onStopCapture = { stopCaptureService() },
                        onExit = { finish() }
                    )
                }
            }
        }
        
        // Observe shareableUri for file sharing
        observeShareableUri()
    }
    
    private fun startObservingServiceData() {
        // Poll service data every second
        lifecycleScope.launch {
            while (true) {
                delay(1000) // Update every second
                
                val service = PacketCaptureService.getInstance()
                if (service != null) {
                    // Get current stats value
                    val stats = service.getStats().value
                    viewModel.updateStats(stats)
                    
                    // Get app talkers list
                    val appTalkers = service.getAppTalkers()
                    viewModel.updateAppTalkers(appTalkers)
                    
                    // Log for debugging
                    if (stats.totalPackets > 0 && stats.totalPackets % 100 == 0L) {
                        android.util.Log.d("MainActivity", "Observing: ${stats.totalPackets} packets, ${appTalkers.size} apps")
                    }
                } else {
                    // Service not running - ensure UI shows empty state
                    viewModel.updateStats(com.packethunter.mobile.data.CaptureStats())
                    viewModel.updateAppTalkers(emptyList())
                }
            }
        }
    }

    private fun requestVpnPermissionAndStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPermissionLauncher.launch(intent)
        } else {
            // Permission already granted
            vpnPermissionGranted = true
            startCaptureService()
        }
    }

    private fun startCaptureService() {
        val intent = Intent(this, PacketCaptureService::class.java).apply {
            action = PacketCaptureService.ACTION_START
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        
        viewModel.setCapturing(true)
        Toast.makeText(this, "Packet capture started", Toast.LENGTH_SHORT).show()
    }

    private fun stopCaptureService() {
        val intent = Intent(this, PacketCaptureService::class.java).apply {
            action = PacketCaptureService.ACTION_STOP
        }
        startService(intent)
        
        viewModel.setCapturing(false)
        Toast.makeText(this, "Packet capture stopped", Toast.LENGTH_SHORT).show()
    }
    
    fun importFilterPresets() {
        fileImportLauncher.launch("application/json")
    }
    
    private fun observeShareableUri() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.shareableUri?.let { uri ->
                    shareFile(uri)
                    viewModel.clearExportResult() // Clear the URI after sharing
                }
            }
        }
    }
    
    private fun shareFile(uri: Uri) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Filter Presets")
                putExtra(Intent.EXTRA_TEXT, "Shared filter presets from AFTERPACKETS")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            val chooser = Intent.createChooser(shareIntent, "Share Filter Presets")
            startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error sharing file: ${e.message}", e)
            Toast.makeText(this, "Error sharing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
