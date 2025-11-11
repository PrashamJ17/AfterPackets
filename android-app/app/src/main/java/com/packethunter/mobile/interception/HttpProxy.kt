package com.packethunter.mobile.interception

import android.net.VpnService
import android.util.Log
import kotlinx.coroutines.*
import java.io.*
import java.net.*
import java.nio.charset.StandardCharsets

/**
 * HTTP Proxy for Edit & Forward functionality
 * 
 * When a packet is paused and user chooses to edit:
 * 1. Hold the client connection (don't forward original)
 * 2. Create new connection to server with modified payload
 * 3. Pipe responses back to client
 * 
 * This works at application layer (HTTP) only, not raw TCP
 */
class HttpProxy(
    private val vpnService: VpnService,
    private val originalRequest: ByteArray,
    private val sourceIp: String,
    private val sourcePort: Int,
    private val destIp: String,
    private val destPort: Int
) {
    companion object {
        private const val TAG = "HttpProxy"
        private const val CONNECT_TIMEOUT_MS = 5000
        private const val READ_TIMEOUT_MS = 30000
    }
    
    private var clientSocket: Socket? = null
    private var serverSocket: Socket? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Forward modified HTTP request to server and pipe response back
     */
    suspend fun forwardModified(
        modifiedRequest: ByteArray,
        responseCallback: (ByteArray) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            // Create protected socket to server
            val serverSocket = Socket().apply {
                vpnService.protect(this)
                connect(InetSocketAddress(destIp, destPort), CONNECT_TIMEOUT_MS)
                soTimeout = READ_TIMEOUT_MS
            }
            
            this@HttpProxy.serverSocket = serverSocket
            
            // Send modified request
            serverSocket.getOutputStream().use { os ->
                os.write(modifiedRequest)
                os.flush()
            }
            
            // Read response
            val responseBuffer = ByteArray(8192)
            val responseLength = serverSocket.getInputStream().read(responseBuffer)
            
            if (responseLength > 0) {
                val response = responseBuffer.copyOfRange(0, responseLength)
                responseCallback(response)
                Result.success(response)
            } else {
                Result.failure(IOException("No response from server"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding modified request", e)
            Result.failure(e)
        } finally {
            cleanup()
        }
    }
    
    /**
     * Forward original request (as-is)
     */
    suspend fun forwardOriginal(
        responseCallback: (ByteArray) -> Unit
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        forwardModified(originalRequest, responseCallback)
    }
    
    /**
     * Drop the request (don't forward)
     */
    fun drop() {
        cleanup()
    }
    
    private fun cleanup() {
        try {
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up sockets", e)
        }
    }
    
    /**
     * Parse HTTP request and extract editable parts
     */
    fun parseHttpRequest(data: ByteArray): HttpRequest? {
        return try {
            val text = String(data, StandardCharsets.UTF_8)
            val lines = text.split("\r\n")
            if (lines.isEmpty()) return null
            
            val requestLine = lines[0]
            val parts = requestLine.split(" ")
            if (parts.size < 3) return null
            
            val method = parts[0]
            val path = parts[1]
            val version = parts[2]
            
            // Parse headers
            val headers = mutableMapOf<String, String>()
            var bodyStart = -1
            for (i in 1 until lines.size) {
                if (lines[i].isEmpty()) {
                    bodyStart = i + 1
                    break
                }
                val headerParts = lines[i].split(":", limit = 2)
                if (headerParts.size == 2) {
                    headers[headerParts[0].trim()] = headerParts[1].trim()
                }
            }
            
            // Extract body
            val body = if (bodyStart > 0 && bodyStart < lines.size) {
                lines.subList(bodyStart, lines.size).joinToString("\r\n").toByteArray()
            } else {
                null
            }
            
            HttpRequest(method, path, version, headers, body, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing HTTP request", e)
            null
        }
    }
    
    /**
     * Reconstruct HTTP request from modified parts
     */
    fun reconstructHttpRequest(request: HttpRequest): ByteArray {
        val sb = StringBuilder()
        
        // Request line
        sb.append("${request.method} ${request.path} ${request.version}\r\n")
        
        // Headers
        request.headers.forEach { (key, value) ->
            sb.append("$key: $value\r\n")
        }
        
        // Update Content-Length if body changed
        if (request.body != null) {
            val contentLength = request.body.size
            if (request.headers.containsKey("Content-Length")) {
                request.headers["Content-Length"] = contentLength.toString()
            } else {
                sb.append("Content-Length: $contentLength\r\n")
            }
        }
        
        sb.append("\r\n")
        
        // Body
        val headerBytes = sb.toString().toByteArray(StandardCharsets.UTF_8)
        return if (request.body != null) {
            headerBytes + request.body
        } else {
            headerBytes
        }
    }
    
    data class HttpRequest(
        val method: String,
        val path: String,
        val version: String,
        val headers: MutableMap<String, String>,
        val body: ByteArray?,
        val originalBytes: ByteArray
    ) {
        fun toEditableText(): String {
            val sb = StringBuilder()
            sb.append("$method $path $version\n")
            headers.forEach { (key, value) ->
                sb.append("$key: $value\n")
            }
            sb.append("\n")
            body?.let {
                sb.append(String(it, StandardCharsets.UTF_8))
            }
            return sb.toString()
        }
    }
}

