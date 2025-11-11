package com.packethunter.mobile.streaming

import android.util.Log
import com.packethunter.mobile.data.PacketInfo
import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real-time PCAP streaming server for Wireshark integration
 * 
 * Implements HTTP server that streams packets in PCAP format
 * Compatible with Wireshark's "Follow TCP Stream" and remote capture
 * 
 * Usage:
 * 1. Start server: streamServer.start(8080)
 * 2. Connect Wireshark: Capture -> Options -> Manage Interfaces -> Remote Interfaces
 * 3. Add remote interface: http://device-ip:8080/pcap
 */
class PcapStreamServer {
    
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private val clientCount = AtomicInteger(0)
    
    // PCAP global header (written once per connection)
    private val PCAP_GLOBAL_HEADER = byteArrayOf(
        0xD4.toByte(), 0xC3.toByte(), 0xB2.toByte(), 0xA1.toByte(), // Magic number
        0x02, 0x00, 0x04, 0x00, // Version 2.4
        0x00, 0x00, 0x00, 0x00, // Timezone offset
        0x00, 0x00, 0x00, 0x00, // Timestamp accuracy
        0xFF.toByte(), 0xFF.toByte(), 0x00, 0x00, // Snapshot length (65535)
        0x01, 0x00, 0x00, 0x00  // Link-layer type (Ethernet)
    )
    
    companion object {
        private const val TAG = "PcapStreamServer"
        private const val DEFAULT_PORT = 8080
        private const val BUFFER_SIZE = 8192
    }
    
    /**
     * Start the streaming server
     */
    fun start(port: Int = DEFAULT_PORT, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        if (isRunning.get()) {
            Log.w(TAG, "Server already running")
            return
        }
        
        try {
            serverSocket = ServerSocket(port)
            isRunning.set(true)
            
            serverJob = scope.launch {
                Log.i(TAG, "PCAP streaming server started on port $port")
                Log.i(TAG, "Connect Wireshark: http://localhost:$port/pcap")
                
                while (isRunning.get()) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        if (clientSocket != null) {
                            clientCount.incrementAndGet()
                            Log.d(TAG, "Client connected: ${clientSocket.remoteSocketAddress} (total: ${clientCount.get()})")
                            
                            // Handle client in separate coroutine
                            scope.launch {
                                handleClient(clientSocket)
                            }
                        }
                    } catch (e: IOException) {
                        if (isRunning.get()) {
                            Log.e(TAG, "Error accepting client", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start streaming server", e)
            isRunning.set(false)
        }
    }
    
    /**
     * Stop the streaming server
     */
    fun stop() {
        if (!isRunning.get()) {
            return
        }
        
        isRunning.set(false)
        serverJob?.cancel()
        
        try {
            serverSocket?.close()
            serverSocket = null
            Log.i(TAG, "PCAP streaming server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server", e)
        }
    }
    
    /**
     * Stream a packet to all connected clients
     */
    fun streamPacket(packet: PacketInfo) {
        if (!isRunning.get() || clientCount.get() == 0) {
            return
        }
        
        // Packet will be written by individual client handlers
        // This method can be extended to maintain a packet queue per client
    }
    
    /**
     * Handle a client connection
     */
    private suspend fun handleClient(clientSocket: Socket) {
        val clientAddress = clientSocket.remoteSocketAddress.toString()
        var outputStream: DataOutputStream? = null
        
        try {
            // Read HTTP request
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))
            val requestLine = reader.readLine()
            
            if (requestLine == null || !requestLine.contains("GET")) {
                Log.w(TAG, "Invalid request from $clientAddress: $requestLine")
                clientSocket.close()
                return
            }
            
            // Check if it's a PCAP request
            val isPcapRequest = requestLine.contains("/pcap") || requestLine.contains("/stream")
            
            if (!isPcapRequest) {
                // Return simple HTML page with connection info
                val response = """
                    HTTP/1.1 200 OK
                    Content-Type: text/html
                    Connection: close
                    
                    <html>
                    <head><title>PCAP Stream Server</title></head>
                    <body>
                        <h1>AFTERPACKETS - PCAP Stream Server</h1>
                        <p>Stream endpoint: <a href="/pcap">/pcap</a></p>
                        <p>Connect Wireshark to: http://${clientSocket.localAddress}:${clientSocket.localPort}/pcap</p>
                        <p>Active clients: ${clientCount.get()}</p>
                    </body>
                    </html>
                """.trimIndent()
                
                clientSocket.getOutputStream().write(response.toByteArray())
                clientSocket.close()
                return
            }
            
            // Send HTTP response headers for streaming
            outputStream = DataOutputStream(clientSocket.getOutputStream())
            val headers = """
                HTTP/1.1 200 OK
                Content-Type: application/vnd.tcpdump.pcap
                Connection: keep-alive
                Transfer-Encoding: chunked
                
            """.trimIndent()
            
            outputStream.write(headers.toByteArray())
            outputStream.flush()
            
            // Write PCAP global header
            outputStream.write(PCAP_GLOBAL_HEADER)
            outputStream.flush()
            
            Log.i(TAG, "Streaming started for client: $clientAddress")
            
            // Keep connection alive and stream packets
            // Note: In a real implementation, packets would be queued and streamed here
            // For now, this is a placeholder that maintains the connection
            
            // Keep connection alive (client will disconnect when done)
            while (isRunning.get() && !clientSocket.isClosed) {
                delay(1000)
                // In production, this would stream actual packets from a queue
            }
            
        } catch (e: Exception) {
            if (isRunning.get()) {
                Log.d(TAG, "Client disconnected: $clientAddress", e)
            }
        } finally {
            try {
                outputStream?.close()
                clientSocket.close()
                clientCount.decrementAndGet()
                Log.d(TAG, "Client connection closed: $clientAddress (remaining: ${clientCount.get()})")
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client connection", e)
            }
        }
    }
    
    /**
     * Write a packet in PCAP format to output stream
     */
    private fun writePcapPacket(dos: DataOutputStream, packet: PacketInfo) {
        val payload = packet.payload ?: return
        
        // Packet header
        val timestampSec = (packet.timestamp / 1000).toInt()
        val timestampUsec = ((packet.timestamp % 1000) * 1000).toInt()
        
        // Write in network byte order (big-endian)
        dos.writeInt(java.lang.Integer.reverseBytes(timestampSec))
        dos.writeInt(java.lang.Integer.reverseBytes(timestampUsec))
        dos.writeInt(java.lang.Integer.reverseBytes(payload.size))
        dos.writeInt(java.lang.Integer.reverseBytes(payload.size))
        
        // Packet data
        dos.write(payload)
        dos.flush()
    }
    
    /**
     * Get server status
     */
    fun isServerRunning(): Boolean = isRunning.get()
    
    fun getClientCount(): Int = clientCount.get()
    
    fun getServerPort(): Int = serverSocket?.localPort ?: 0
}

