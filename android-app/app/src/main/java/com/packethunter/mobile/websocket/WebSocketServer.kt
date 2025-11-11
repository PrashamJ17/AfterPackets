package com.packethunter.mobile.websocket

import android.util.Log
import com.google.gson.Gson
import com.packethunter.mobile.data.CaptureStats
import com.packethunter.mobile.data.PacketInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.*

/**
 * WebSocket server for streaming real-time packet data to desktop app
 */
class WebSocketServer(
    private val port: Int = 8080
) {
    private var serverSocket: ServerSocket? = null
    private val clients = mutableListOf<WebSocketClient>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private var isRunning = false
    
    companion object {
        private const val TAG = "WebSocketServer"
    }
    
    fun start() {
        if (isRunning) return
        
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                Log.d(TAG, "WebSocket server started on port $port")
                
                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept()
                        socket?.let {
                            handleClient(it)
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }
    
    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                
                // Perform WebSocket handshake
                val headers = mutableMapOf<String, String>()
                var line = reader.readLine()
                
                while (!line.isNullOrEmpty()) {
                    if (line.contains(":")) {
                        val parts = line.split(":", limit = 2)
                        headers[parts[0].trim()] = parts[1].trim()
                    }
                    line = reader.readLine()
                }
                
                val key = headers["Sec-WebSocket-Key"]
                if (key != null) {
                    val acceptKey = generateAcceptKey(key)
                    
                    writer.write("HTTP/1.1 101 Switching Protocols\r\n")
                    writer.write("Upgrade: websocket\r\n")
                    writer.write("Connection: Upgrade\r\n")
                    writer.write("Sec-WebSocket-Accept: $acceptKey\r\n")
                    writer.write("\r\n")
                    writer.flush()
                    
                    val client = WebSocketClient(socket, writer)
                    synchronized(clients) {
                        clients.add(client)
                    }
                    Log.d(TAG, "Client connected: ${socket.inetAddress}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling client", e)
                socket.close()
            }
        }
    }
    
    fun broadcastPackets(packets: List<PacketInfo>, stats: CaptureStats) {
        val data = mapOf(
            "packets" to packets.takeLast(100),
            "stats" to stats,
            "timestamp" to System.currentTimeMillis()
        )
        
        val json = gson.toJson(data)
        broadcast(json)
    }
    
    private fun broadcast(message: String) {
        synchronized(clients) {
            clients.removeAll { client ->
                try {
                    client.send(message)
                    false
                } catch (e: Exception) {
                    Log.d(TAG, "Client disconnected")
                    client.close()
                    true
                }
            }
        }
    }
    
    fun stop() {
        isRunning = false
        synchronized(clients) {
            clients.forEach { it.close() }
            clients.clear()
        }
        serverSocket?.close()
        scope.cancel()
        Log.d(TAG, "WebSocket server stopped")
    }
    
    private fun generateAcceptKey(key: String): String {
        val magicString = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
        val digest = MessageDigest.getInstance("SHA-1")
        val hash = digest.digest(("$key$magicString").toByteArray())
        return Base64.getEncoder().encodeToString(hash)
    }
}

class WebSocketClient(
    private val socket: Socket,
    private val writer: BufferedWriter
) {
    fun send(message: String) {
        // Create WebSocket frame
        val payload = message.toByteArray(Charsets.UTF_8)
        val frame = createFrame(payload)
        
        synchronized(writer) {
            socket.getOutputStream().write(frame)
            socket.getOutputStream().flush()
        }
    }
    
    private fun createFrame(payload: ByteArray): ByteArray {
        val payloadLength = payload.size
        val frameSize = when {
            payloadLength <= 125 -> 2 + payloadLength
            payloadLength <= 65535 -> 4 + payloadLength
            else -> 10 + payloadLength
        }
        
        val frame = ByteArray(frameSize)
        var index = 0
        
        // Byte 0: FIN + opcode (text = 1)
        frame[index++] = 0x81.toByte()
        
        // Byte 1: MASK + payload length
        when {
            payloadLength <= 125 -> {
                frame[index++] = payloadLength.toByte()
            }
            payloadLength <= 65535 -> {
                frame[index++] = 126.toByte()
                frame[index++] = (payloadLength shr 8).toByte()
                frame[index++] = payloadLength.toByte()
            }
            else -> {
                frame[index++] = 127.toByte()
                for (i in 7 downTo 0) {
                    frame[index++] = (payloadLength shr (i * 8)).toByte()
                }
            }
        }
        
        // Copy payload
        payload.copyInto(frame, index)
        
        return frame
    }
    
    fun close() {
        try {
            socket.close()
        } catch (e: Exception) {
            // Ignore
        }
    }
}
