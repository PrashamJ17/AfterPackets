package com.packethunter.mobile.export

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.packethunter.mobile.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Manages export of captured packets to various formats
 */
class ExportManager(private val context: Context) {

    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    companion object {
        private const val TAG = "ExportManager"
        private const val EXPORT_DIR = "exports"
    }

    /**
     * Export packets to PCAP format (Wireshark compatible)
     */
    suspend fun exportToPcap(
        packets: List<PacketInfo>,
        filename: String = "capture_${System.currentTimeMillis()}.pcap"
    ): File = withContext(Dispatchers.IO) {
        val exportDir = getExportDir()
        val file = File(exportDir, filename)

        FileOutputStream(file).use { fos ->
            DataOutputStream(fos).use { dos ->
                // Write PCAP global header
                writePcapGlobalHeader(dos)

                // Write each packet
                for (packet in packets) {
                    writePcapPacket(dos, packet)
                }
            }
        }

        Log.d(TAG, "Exported ${packets.size} packets to PCAP: ${file.absolutePath}")
        
        // Notify MediaScanner so file appears in Files app
        notifyMediaScanner(file)
        
        file
    }

    /**
     * Export packets to JSON format
     */
    suspend fun exportToJson(
        packets: List<PacketInfo>,
        alerts: List<Alert> = emptyList(),
        stats: CaptureStats,
        filename: String = "capture_${System.currentTimeMillis()}.json"
    ): File = withContext(Dispatchers.IO) {
        val exportDir = getExportDir()
        val file = File(exportDir, filename)

        val exportData = ExportData(
            version = "1.0",
            exportTime = System.currentTimeMillis(),
            stats = stats,
            packets = packets.map { it.toExportPacket() },
            alerts = alerts,
            metadata = ExportMetadata(
                totalPackets = packets.size,
                totalAlerts = alerts.size,
                captureStartTime = stats.startTime,
                captureEndTime = stats.lastUpdate
            )
        )

        file.writeText(gson.toJson(exportData))

        Log.d(TAG, "Exported ${packets.size} packets to JSON: ${file.absolutePath}")
        
        // Notify MediaScanner so file appears in Files app
        notifyMediaScanner(file)
        
        file
    }

    /**
     * Export evidence bundle (ZIP containing PCAP, JSON, timeline, and metadata)
     */
    suspend fun exportEvidenceBundle(
        packets: List<PacketInfo>,
        alerts: List<Alert>,
        stats: CaptureStats,
        filename: String = "evidence_${System.currentTimeMillis()}.zip"
    ): File = withContext(Dispatchers.IO) {
        val exportDir = getExportDir()
        val file = File(exportDir, filename)

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // Add PCAP file
            zos.putNextEntry(ZipEntry("capture.pcap"))
            val pcapData = generatePcapBytes(packets)
            zos.write(pcapData)
            zos.closeEntry()

            // Add JSON metadata
            zos.putNextEntry(ZipEntry("metadata.json"))
            val jsonData = generateJsonBytes(packets, alerts, stats)
            zos.write(jsonData)
            zos.closeEntry()

            // Add timeline
            zos.putNextEntry(ZipEntry("timeline.json"))
            val timelineData = generateTimelineBytes(packets, stats)
            zos.write(timelineData)
            zos.closeEntry()

            // Add alerts summary
            zos.putNextEntry(ZipEntry("alerts.json"))
            val alertsData = gson.toJson(alerts).toByteArray()
            zos.write(alertsData)
            zos.closeEntry()

            // Add README
            zos.putNextEntry(ZipEntry("README.txt"))
            val readme = generateReadme(packets, alerts, stats)
            zos.write(readme.toByteArray())
            zos.closeEntry()
        }

        Log.d(TAG, "Exported evidence bundle: ${file.absolutePath}")
        
        // Notify MediaScanner so file appears in Files app
        notifyMediaScanner(file)
        
        file
    }

    private fun writePcapGlobalHeader(dos: DataOutputStream) {
        dos.writeInt(java.lang.Integer.reverseBytes(0xa1b2c3d4.toInt())) // Magic number
        dos.writeInt(java.lang.Integer.reverseBytes(2) shl 16) // Version major (2.0)
        dos.writeInt(java.lang.Integer.reverseBytes(4) shl 16) // Version minor (0.4)
        dos.writeInt(0) // Timezone offset
        dos.writeInt(0) // Timestamp accuracy
        dos.writeInt(java.lang.Integer.reverseBytes(65535)) // Snapshot length
        dos.writeInt(java.lang.Integer.reverseBytes(1)) // Link-layer type (Ethernet)
    }

    private fun writePcapPacket(dos: DataOutputStream, packet: PacketInfo) {
        val payload = packet.payload ?: return

        // Packet header
        val timestampSec = (packet.timestamp / 1000).toInt()
        val timestampUsec = ((packet.timestamp % 1000) * 1000).toInt()

        dos.writeInt(java.lang.Integer.reverseBytes(timestampSec))
        dos.writeInt(java.lang.Integer.reverseBytes(timestampUsec))
        dos.writeInt(java.lang.Integer.reverseBytes(payload.size))
        dos.writeInt(java.lang.Integer.reverseBytes(payload.size))

        // Packet data
        dos.write(payload)
    }

    private fun generatePcapBytes(packets: List<PacketInfo>): ByteArray {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            writePcapGlobalHeader(dos)
            for (packet in packets) {
                writePcapPacket(dos, packet)
            }
        }
        return baos.toByteArray()
    }

    private fun generateJsonBytes(
        packets: List<PacketInfo>,
        alerts: List<Alert>,
        stats: CaptureStats
    ): ByteArray {
        val exportData = ExportData(
            version = "1.0",
            exportTime = System.currentTimeMillis(),
            stats = stats,
            packets = packets.map { it.toExportPacket() },
            alerts = alerts,
            metadata = ExportMetadata(
                totalPackets = packets.size,
                totalAlerts = alerts.size,
                captureStartTime = stats.startTime,
                captureEndTime = stats.lastUpdate
            )
        )
        return gson.toJson(exportData).toByteArray()
    }

    private fun generateTimelineBytes(packets: List<PacketInfo>, stats: CaptureStats): ByteArray {
        // Group packets by time buckets (1 second)
        val buckets = mutableMapOf<Long, TimelineBucket>()

        for (packet in packets) {
            val bucketKey = packet.timestamp / 1000
            val bucket = buckets.getOrPut(bucketKey) {
                TimelineBucket(
                    timestamp = bucketKey * 1000,
                    packetCount = 0,
                    bytes = 0,
                    protocols = mutableMapOf()
                )
            }

            bucket.packetCount++
            bucket.bytes += packet.length
            bucket.protocols[packet.protocol] = (bucket.protocols[packet.protocol] ?: 0) + 1
        }

        val timeline = Timeline(
            buckets = buckets.values.sortedBy { it.timestamp },
            totalDuration = stats.lastUpdate - stats.startTime,
            stats = stats
        )

        return gson.toJson(timeline).toByteArray()
    }

    private fun generateReadme(
        packets: List<PacketInfo>,
        alerts: List<Alert>,
        stats: CaptureStats
    ): String {
        return """
            AFTERPACKETS - Evidence Bundle
            =======================================
            
            Capture Summary:
            - Total Packets: ${packets.size}
            - Total Bytes: ${stats.totalBytes}
            - Duration: ${(stats.lastUpdate - stats.startTime) / 1000}s
            - Packets/sec: ${"%.2f".format(stats.packetsPerSecond)}
            - Bytes/sec: ${"%.2f".format(stats.bytesPerSecond)}
            
            Alerts Generated: ${alerts.size}
            ${alerts.joinToString("\n") { "- [${it.severity.uppercase()}] ${it.title}" }}
            
            Protocol Distribution:
            ${stats.protocolDistribution.entries.joinToString("\n") { "- ${it.key}: ${it.value}" }}
            
            Top Talkers:
            ${stats.topTalkers.take(5).joinToString("\n") { "- ${it.ip}: ${it.packetCount} packets, ${it.bytes} bytes" }}
            
            Files Included:
            - capture.pcap: Wireshark-compatible packet capture
            - metadata.json: Detailed packet metadata and analysis
            - timeline.json: Temporal visualization data
            - alerts.json: Security alerts and detections
            - README.txt: This file
            
            Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(java.util.Date())}
        """.trimIndent()
    }

    private fun getExportDir(): File {
        // Use public Downloads directory so files are accessible
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dir = File(downloadsDir, "PacketHunter")
        if (!dir.exists()) {
            dir.mkdirs()
            Log.i(TAG, "Created export directory: ${dir.absolutePath}")
        }
        Log.d(TAG, "Export directory: ${dir.absolutePath}")
        return dir
    }
    
    /**
     * Notify MediaScanner so exported file appears in Files app
     */
    private fun notifyMediaScanner(file: File) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                null
            ) { path, uri ->
                Log.i(TAG, "✅ File indexed by MediaScanner: $path -> $uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify MediaScanner for ${file.absolutePath}", e)
        }
    }

    private fun PacketInfo.toExportPacket(): ExportPacket {
        return ExportPacket(
            id = id,
            timestamp = timestamp,
            protocol = protocol,
            sourceIp = sourceIp,
            destIp = destIp,
            sourcePort = sourcePort,
            destPort = destPort,
            length = length,
            flags = flags,
            payloadPreview = payloadPreview,
            httpMethod = httpMethod,
            httpUrl = httpUrl,
            dnsQuery = dnsQuery,
            dnsResponse = dnsResponse,
            tlsSni = tlsSni,
            tlsCertFingerprint = tlsCertFingerprint,
            destCountry = destCountry,
            destCity = destCity,
            destLat = destLat,
            destLon = destLon
        )
    }

    /**
     * Get shareable URI for exported file
     */
    fun getShareableUri(file: File): Uri? {
        return try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get shareable URI for ${file.absolutePath}", e)
            null
        }
    }

    /**
     * Export filter presets to JSON format
     */
    suspend fun exportFilterPresets(
        presets: List<FilterPreset>,
        filename: String = "filter_presets_${System.currentTimeMillis()}.json"
    ): File = withContext(Dispatchers.IO) {
        val exportDir = getExportDir()
        val file = File(exportDir, filename)
        
        val exportData = FilterPresetExportData(
            version = "1.0",
            exportTime = System.currentTimeMillis(),
            presetCount = presets.size,
            presets = presets
        )
        
        file.writeText(gson.toJson(exportData))
        
        Log.d(TAG, "Exported ${presets.size} filter presets to: ${file.absolutePath}")
        
        // Notify MediaScanner so file appears in Files app
        notifyMediaScanner(file)
        
        file
    }

    /**
     * Import filter presets from JSON file
     */
    suspend fun importFilterPresets(
        file: File
    ): List<FilterPreset> = withContext(Dispatchers.IO) {
        try {
            val jsonContent = file.readText()
            val exportData = gson.fromJson(jsonContent, FilterPresetExportData::class.java)
            
            if (exportData.version != "1.0") {
                throw IllegalArgumentException("Unsupported preset export version: ${exportData.version}")
            }
            
            Log.d(TAG, "Imported ${exportData.presets.size} filter presets from: ${file.absolutePath}")
            exportData.presets
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import filter presets from ${file.absolutePath}", e)
            throw e
        }
    }

    /**
     * Import filter presets from JSON content string
     */
    suspend fun importFilterPresetsFromJson(
        jsonContent: String
    ): List<FilterPreset> = withContext(Dispatchers.IO) {
        try {
            val exportData = gson.fromJson(jsonContent, FilterPresetExportData::class.java)
            
            if (exportData.version != "1.0") {
                throw IllegalArgumentException("Unsupported preset export version: ${exportData.version}")
            }
            
            Log.d(TAG, "Imported ${exportData.presets.size} filter presets from JSON content")
            exportData.presets
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import filter presets from JSON content", e)
            throw e
        }
    }

    /**
     * Get all exported filter preset files
     */
    suspend fun getExportedPresetFiles(): List<File> = withContext(Dispatchers.IO) {
        val exportDir = getExportDir()
        exportDir.listFiles { file ->
            file.name.startsWith("filter_presets_") && file.name.endsWith(".json")
        }?.toList() ?: emptyList()
    }
    
    /**
     * Export complete session bundle with PCAP, metadata, audit log, and manifest
     * Creates timestamped folder with all files
     */
    suspend fun exportSessionBundle(
        packets: List<PacketInfo>,
        alerts: List<Alert>,
        stats: CaptureStats,
        auditLogEntries: List<com.packethunter.mobile.interception.AuditLogEntry>,
        progressCallback: ((String, Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val sessionFolderName = "session_${dateFormat.format(Date(timestamp))}"
        
        val exportDir = getExportDir()
        val sessionDir = File(exportDir, sessionFolderName)
        if (!sessionDir.exists()) {
            sessionDir.mkdirs()
        }
        
        progressCallback?.invoke("Creating session folder...", 10)
        
        // 1. Generate session.pcap
        progressCallback?.invoke("Generating PCAP file...", 20)
        val pcapFile = File(sessionDir, "session.pcap")
        FileOutputStream(pcapFile).use { fos ->
            DataOutputStream(fos).use { dos ->
                writePcapGlobalHeader(dos)
                for (packet in packets) {
                    writePcapPacket(dos, packet)
                }
            }
        }
        val pcapHash = computeFileHash(pcapFile)
        
        // 2. Generate metadata.json
        progressCallback?.invoke("Generating metadata...", 40)
        val metadataFile = File(sessionDir, "metadata.json")
        val metadata = mapOf(
            "version" to "1.0",
            "exportTime" to timestamp,
            "sessionStartTime" to stats.startTime,
            "sessionEndTime" to stats.lastUpdate,
            "duration" to (stats.lastUpdate - stats.startTime),
            "totalPackets" to packets.size,
            "totalBytes" to stats.totalBytes,
            "packetsPerSecond" to stats.packetsPerSecond,
            "bytesPerSecond" to stats.bytesPerSecond,
            "protocolDistribution" to stats.protocolDistribution,
            "topTalkers" to stats.topTalkers.take(10).map { 
                mapOf(
                    "ip" to it.ip,
                    "packetCount" to it.packetCount,
                    "bytes" to it.bytes,
                    "country" to (it.country ?: "")
                )
            },
            "totalAlerts" to alerts.size,
            "alerts" to alerts.map { 
                mapOf(
                    "severity" to it.severity,
                    "title" to it.title,
                    "description" to it.description,
                    "timestamp" to it.timestamp
                )
            }
        )
        metadataFile.writeText(gson.toJson(metadata))
        val metadataHash = computeFileHash(metadataFile)
        
        // 3. Generate audit.log.json
        progressCallback?.invoke("Generating audit log...", 60)
        val auditFile = File(sessionDir, "audit.log.json")
        val auditData = mapOf(
            "version" to "1.0",
            "exportTime" to timestamp,
            "totalEntries" to auditLogEntries.size,
            "entries" to auditLogEntries.map {
                mapOf(
                    "id" to it.id,
                    "timestamp" to it.timestamp,
                    "connKey" to it.connKey,
                    "protocol" to it.protocol,
                    "action" to it.action,
                    "originalHash" to it.originalHash,
                    "modifiedHash" to (it.modifiedHash ?: ""),
                    "autoForwarded" to it.autoForwarded,
                    "consentVersion" to it.consentVersion,
                    "summary" to it.summary,
                    "metadata" to it.metadata
                )
            }
        )
        auditFile.writeText(gson.toJson(auditData))
        val auditHash = computeFileHash(auditFile)
        
        // 4. Generate manifest.json with checksums
        progressCallback?.invoke("Generating manifest...", 80)
        val manifestFile = File(sessionDir, "manifest.json")
        val manifest = mapOf(
            "version" to "1.0",
            "exportTime" to timestamp,
            "sessionFolder" to sessionFolderName,
            "files" to listOf(
                mapOf(
                    "name" to "session.pcap",
                    "size" to pcapFile.length(),
                    "sha256" to pcapHash,
                    "description" to "Wireshark-compatible packet capture"
                ),
                mapOf(
                    "name" to "metadata.json",
                    "size" to metadataFile.length(),
                    "sha256" to metadataHash,
                    "description" to "Session summary and statistics"
                ),
                mapOf(
                    "name" to "audit.log.json",
                    "size" to auditFile.length(),
                    "sha256" to auditHash,
                    "description" to "Interception audit log"
                )
            ),
            "totalSize" to (pcapFile.length() + metadataFile.length() + auditFile.length())
        )
        manifestFile.writeText(gson.toJson(manifest))
        
        progressCallback?.invoke("Export complete!", 100)
        
        Log.i(TAG, "Exported session bundle to: ${sessionDir.absolutePath}")
        
        // Notify MediaScanner for all files in the session directory
        val filesToScan = listOf(pcapFile, metadataFile, auditFile, manifestFile)
        try {
            MediaScannerConnection.scanFile(
                context,
                filesToScan.map { it.absolutePath }.toTypedArray(),
                null
            ) { path, uri ->
                Log.i(TAG, "✅ Session file indexed: $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to notify MediaScanner for session files", e)
        }
        
        sessionDir
    }
    
    /**
     * Compute SHA-256 hash of file
     */
    private fun computeFileHash(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hash = digest.digest()
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing file hash for ${file.name}", e)
            ""
        }
    }
}

// Export data structures
data class ExportData(
    val version: String,
    val exportTime: Long,
    val stats: CaptureStats,
    val packets: List<ExportPacket>,
    val alerts: List<Alert>,
    val metadata: ExportMetadata
)

data class ExportPacket(
    val id: Long,
    val timestamp: Long,
    val protocol: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val length: Int,
    val flags: String,
    val payloadPreview: String,
    val httpMethod: String?,
    val httpUrl: String?,
    val dnsQuery: String?,
    val dnsResponse: String?,
    val tlsSni: String?,
    val tlsCertFingerprint: String?,
    val destCountry: String?,
    val destCity: String?,
    val destLat: Double?,
    val destLon: Double?
)

data class ExportMetadata(
    val totalPackets: Int,
    val totalAlerts: Int,
    val captureStartTime: Long,
    val captureEndTime: Long
)

data class Timeline(
    val buckets: List<TimelineBucket>,
    val totalDuration: Long,
    val stats: CaptureStats
)

data class TimelineBucket(
    val timestamp: Long,
    var packetCount: Int,
    var bytes: Long,
    val protocols: MutableMap<String, Int>
)

// Export data structure for filter presets
data class FilterPresetExportData(
    val version: String,
    val exportTime: Long,
    val presetCount: Int,
    val presets: List<FilterPreset>
)
