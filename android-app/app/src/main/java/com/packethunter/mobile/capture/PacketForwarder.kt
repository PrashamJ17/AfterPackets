package com.packethunter.mobile.capture

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.IOException
import java.net.*
import java.nio.ByteBuffer
import java.nio.channels.DatagramChannel
import java.nio.channels.SocketChannel
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * PCAPdroid-style packet forwarder that maintains network connectivity
 * 
 * This forwarder:
 * 1. Reads packets from TUN interface
 * 2. Parses IP/TCP/UDP headers
 * 3. Creates protected sockets to real network
 * 4. Forwards data and writes responses back to TUN
 * 5. Processes packets in parallel for analysis
 */
class PacketForwarder(
    private val vpnService: VpnService,
    private val vpnInterface: ParcelFileDescriptor,
    private val packetProcessor: (ByteArray) -> Unit
) {
    private val TAG = "PacketForwarder"
    
    private val isRunning = AtomicBoolean(false)
    private var forwarderJob: Job? = null
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private val tcpSessions = ConcurrentHashMap<String, TCPSession>()
    
    // UDP session tracking: (srcIP:srcPort, dstIP:dstPort) -> DatagramChannel
    private val udpSessions = ConcurrentHashMap<String, DatagramChannel>()
    
    // Stats
    private val packetsForwarded = AtomicLong(0)
    private val bytesForwarded = AtomicLong(0)
    private val packetsReceived = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)
    
    fun start() {
        if (isRunning.get()) {
            Log.w(TAG, "Forwarder already running")
            return
        }
        
        isRunning.set(true)
        Log.i(TAG, "Starting packet forwarder")
        
        forwarderJob = scope.launch {
            runForwarder()
        }
    }
    
    fun stop() {
        if (!isRunning.get()) {
            return
        }
        
        Log.i(TAG, "Stopping packet forwarder")
        isRunning.set(false)
        job.cancel() // Cancels all coroutines started by this scope
        forwarderJob = null
        
        // Close all connections
        tcpSessions.values.forEach { it.close() }
        tcpSessions.clear()
        
        udpSessions.values.forEach { channel ->
            try {
                channel.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing UDP socket", e)
            }
        }
        udpSessions.clear()
        
        Log.i(TAG, "Packet forwarder stopped")
    }
    
    fun isRunning(): Boolean = isRunning.get()
    
    fun getStats(): ForwarderStats {
        return ForwarderStats(
            packetsForwarded = packetsForwarded.get(),
            bytesForwarded = bytesForwarded.get(),
            tcpConnections = tcpSessions.size,
            udpSessions = udpSessions.size
        )
    }
    
    private suspend fun runForwarder() {
        val inputStream = ParcelFileDescriptor.AutoCloseInputStream(vpnInterface)
        val buffer = ByteArray(32767)

        Log.i(TAG, "Forwarder loop started - reading from TUN")

        // Use withContext to ensure streams are closed even on cancellation
        withContext(Dispatchers.IO) {
            while (isRunning.get()) {
                if (!isActive) break

                try {
                    val length = inputStream.read(buffer)

                    if (length > 0) {
                        val packetData = buffer.copyOfRange(0, length)

                        // Process packet in parallel (non-blocking)
                        scope.launch {
                            try {
                                packetProcessor(packetData)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error in packet processor", e)
                            }
                        }

                        // Forward packet immediately
                        forwardPacket(packetData)

                        packetsReceived.incrementAndGet()
                        bytesReceived.addAndGet(length.toLong())
                    } else if (length == 0) {
                        delay(1) // Small delay to prevent busy-waiting
                    } else {
                        // End of stream or error
                        if (length < 0) {
                            Log.w(TAG, "TUN read returned $length")
                        }
                        break
                    }
                } catch (e: IOException) {
                    if (isRunning.get()) {
                        Log.e(TAG, "I/O error in forwarder", e)
                        delay(100) // Wait before retrying
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in forwarder loop", e)
                    delay(100)
                }
            }
        }

        Log.i(TAG, "Forwarder loop ended")
    }
    
    private fun forwardPacket(packet: ByteArray) {
        if (packet.size < 20) {
            return // Too small to be a valid IP packet
        }
        
        try {
            val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
            
            // Parse IP header
            val versionAndIhl = buffer.get(0).toInt() and 0xFF
            val version = (versionAndIhl shr 4) and 0x0F
            val ipHeaderLength = (versionAndIhl and 0x0F) * 4
            
            if (version != 4 || ipHeaderLength < 20 || packet.size < ipHeaderLength) {
                return // Not a valid IPv4 packet
            }
            
            val protocol = buffer.get(9).toInt() and 0xFF
            val srcIp = InetAddress.getByAddress(
                byteArrayOf(
                    (buffer.get(12).toInt() and 0xFF).toByte(),
                    (buffer.get(13).toInt() and 0xFF).toByte(),
                    (buffer.get(14).toInt() and 0xFF).toByte(),
                    (buffer.get(15).toInt() and 0xFF).toByte()
                )
            )
            val dstIp = InetAddress.getByAddress(
                byteArrayOf(
                    (buffer.get(16).toInt() and 0xFF).toByte(),
                    (buffer.get(17).toInt() and 0xFF).toByte(),
                    (buffer.get(18).toInt() and 0xFF).toByte(),
                    (buffer.get(19).toInt() and 0xFF).toByte()
                )
            )
            
            when (protocol) {
                6 -> { // TCP
                    if (packet.size >= ipHeaderLength + 20) {
                        val srcPort = buffer.getShort(ipHeaderLength).toInt() and 0xFFFF
                        val dstPort = buffer.getShort(ipHeaderLength + 2).toInt() and 0xFFFF
                        handleTcpPacket(packet, srcIp, dstIp, srcPort, dstPort, ipHeaderLength)
                    }
                }
                17 -> { // UDP
                    if (packet.size >= ipHeaderLength + 8) {
                        val srcPort = buffer.getShort(ipHeaderLength).toInt() and 0xFFFF
                        val dstPort = buffer.getShort(ipHeaderLength + 2).toInt() and 0xFFFF
                        handleUdpPacket(packet, srcIp, dstIp, srcPort, dstPort, ipHeaderLength)
                    }
                }
                else -> {
                    // For other protocols like ICMP, we don't forward them in this simple model.
                    // A full implementation would require raw sockets or a more complex setup.
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing/forwarding packet", e)
        }
    }
    
    private fun handleTcpPacket(packet: ByteArray, srcIp: InetAddress, dstIp: InetAddress, srcPort: Int, dstPort: Int, ipHeaderLen: Int) {
        val connectionKey = "${srcIp.hostAddress}:$srcPort-${dstIp.hostAddress}:$dstPort"
        val tcpHeader = TCPHeader(packet, ipHeaderLen)

        if (tcpHeader.isSYN) {
            val session = TCPSession(vpnService, srcIp, srcPort, dstIp, dstPort)
            tcpSessions[connectionKey] = session
            session.start()
        }

        val session = tcpSessions[connectionKey]
        if (session == null) {
            // Packet for a session that doesn't exist (or was closed), ignore.
            return
        }

        if (tcpHeader.isFIN || tcpHeader.isRST) {
            session.close()
            tcpSessions.remove(connectionKey)
            return
        }

        val payloadSize = packet.size - ipHeaderLen - tcpHeader.headerLength
        if (payloadSize > 0) {
            val payload = ByteBuffer.wrap(packet, ipHeaderLen + tcpHeader.headerLength, payloadSize)
            session.send(payload)
            packetsForwarded.addAndGet(1)
            bytesForwarded.addAndGet(payloadSize.toLong())
        }
    }

    private fun handleUdpPacket(packet: ByteArray, srcIp: InetAddress, dstIp: InetAddress, srcPort: Int, dstPort: Int, ipHeaderLen: Int) {
        val sessionKey = "${srcIp.hostAddress}:$srcPort-${dstIp.hostAddress}:$dstPort"
        
        try {
            val channel = udpSessions.getOrPut(sessionKey) {
                val newChannel = DatagramChannel.open()
                // CRITICAL: Protect socket from VPN routing loop
                if (!vpnService.protect(newChannel.socket())) {
                    Log.e(TAG, "Failed to protect UDP socket for $sessionKey")
                    newChannel.close()
                    throw IOException("Failed to protect UDP socket")
                }
                newChannel.connect(InetSocketAddress(dstIp, dstPort))
                newChannel.configureBlocking(false)

                // Start a listener for this UDP session
                scope.launch {
                    val responseBuffer = ByteBuffer.allocate(32767)
                    try {
                        while (isActive && newChannel.isConnected) {
                            responseBuffer.clear()
                            val read = newChannel.read(responseBuffer)
                            if (read > 0) {
                                responseBuffer.flip()
                                val responseData = ByteArray(read)
                                responseBuffer.get(responseData)

                                val responseIpPacket = createUdpResponsePacket(dstIp, srcIp, dstPort, srcPort, responseData, packet)
                                writeToVpn(responseIpPacket)
                                bytesReceived.addAndGet(read.toLong())
                            } else {
                                delay(10) // Avoid busy-looping
                            }
                        }
                    } catch (e: IOException) {
                        Log.d(TAG, "UDP session $sessionKey closed or errored.", e)
                    } finally {
                        udpSessions.remove(sessionKey)
                        newChannel.close()
                    }
                }
                newChannel
            }

            val dataOffset = ipHeaderLen + 8
            val dataLen = packet.size - dataOffset
            
            if (dataLen > 0) {
                val dataBuffer = ByteBuffer.wrap(packet, dataOffset, dataLen)
                channel.write(dataBuffer)
                packetsForwarded.incrementAndGet()
                bytesForwarded.addAndGet(dataLen.toLong())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling UDP packet: $sessionKey", e)
            udpSessions.remove(sessionKey)
        }
    }
    
    // Reusable output stream for writing to VPN
    private val vpnOutputStream by lazy {
        ParcelFileDescriptor.AutoCloseOutputStream(vpnInterface)
    }

    @Synchronized
    private fun writeToVpn(data: ByteArray) {
        try {
            vpnOutputStream.write(data)
            vpnOutputStream.flush()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write to VPN interface", e)
        }
    }

    // Inner class to manage a single TCP session
    inner class TCPSession(
        private val vpnService: VpnService,
        private val sourceIp: InetAddress,
        private val sourcePort: Int,
        private val destIp: InetAddress,
        private val destPort: Int
    ) {
        private var channel: SocketChannel? = null
        private val isRunning = AtomicBoolean(true)

        fun start() {
            scope.launch {
                try {
                    channel = SocketChannel.open()
                    // CRITICAL: Protect socket from VPN routing loop
                    if (!vpnService.protect(channel!!.socket())) {
                        Log.e(TAG, "Failed to protect TCP socket")
                        close()
                        return@launch
                    }
                    channel!!.connect(InetSocketAddress(destIp, destPort))
                    channel!!.configureBlocking(false)

                    Log.i(TAG, "TCP session started: ${sourceIp.hostAddress}:$sourcePort -> ${destIp.hostAddress}:$destPort")

                    // Listener loop
                    val buffer = ByteBuffer.allocate(32767)
                    while (isRunning.get() && channel!!.isConnected) {
                        buffer.clear()
                        val bytesRead = channel!!.read(buffer)
                        if (bytesRead > 0) {
                            buffer.flip()
                            val responseData = ByteArray(bytesRead)
                            buffer.get(responseData)

                            // This is a simplified response. A real implementation needs to manage SEQ/ACK numbers.
                            val responsePacket = createTcpResponsePacket(destIp, sourceIp, destPort, sourcePort, responseData, bytesRead, ByteArray(0))
                            writeToVpn(responsePacket)
                            bytesReceived.addAndGet(bytesRead.toLong())
                        } else if (bytesRead == -1) {
                            // Remote end closed connection
                            close()
                        } else {
                            delay(10) // Avoid busy-loop
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "TCP session error", e)
                } finally {
                    close()
                }
            }
        }

        fun send(data: ByteBuffer) {
            scope.launch {
                try {
                    channel?.write(data)
                } catch (e: IOException) {
                    Log.e(TAG, "TCP send error", e)
                    close()
                }
            }
        }

        fun close() {
            if (isRunning.compareAndSet(true, false)) {
                try {
                    channel?.close()
                } catch (e: IOException) {
                    // Ignore
                }
                tcpSessions.remove("${sourceIp.hostAddress}:$sourcePort-${destIp.hostAddress}:$destPort")
                Log.i(TAG, "TCP session closed: ${sourceIp.hostAddress}:$sourcePort -> ${destIp.hostAddress}:$destPort")
            }
        }
    }

    // Helper to parse TCP header flags
    class TCPHeader(packet: ByteArray, ipHeaderOffset: Int) {
        private val flags: Int
        val headerLength: Int

        init {
            headerLength = ((packet[ipHeaderOffset + 12].toInt() and 0xF0) shr 4) * 4
            flags = packet[ipHeaderOffset + 13].toInt() and 0xFF
        }
        val isSYN: Boolean get() = (flags and 0x02) != 0
        val isFIN: Boolean get() = (flags and 0x01) != 0
        val isRST: Boolean get() = (flags and 0x04) != 0
    }

    private fun createTcpResponsePacket(
        srcIp: InetAddress,
        dstIp: InetAddress,
        srcPort: Int,
        dstPort: Int,
        data: ByteArray,
        dataLen: Int,
        originalPacket: ByteArray
    ): ByteArray {
        // This is a highly simplified packet creation. A robust implementation
        // needs to track sequence and acknowledgment numbers.
        val ipHeaderLen = 20
        val tcpHeaderLen = 20
        val totalLen = ipHeaderLen + tcpHeaderLen + dataLen
        
        val packet = ByteArray(totalLen)
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        
        // Simplified IP Header
        buffer.put(0x45.toByte()) // Version 4, IHL 5
        buffer.put(0.toByte()) // DSCP/ECN
        buffer.putShort(totalLen.toShort())
        buffer.putShort(0) // Identification
        buffer.putShort(0x4000.toShort()) // Flags
        buffer.put(64.toByte()) // TTL
        buffer.put(6.toByte()) // Protocol TCP
        buffer.putShort(0) // Checksum (kernel will fill)
        buffer.put(srcIp.address)
        buffer.put(dstIp.address)

        // Simplified TCP Header
        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        buffer.putInt(0) // Sequence Number (needs state)
        buffer.putInt(0) // Ack Number (needs state)
        buffer.putShort((0x5018).toShort()) // Header length, Flags (ACK, PSH)
        buffer.putShort(8192.toShort()) // Window
        buffer.putShort(0) // Checksum (kernel will fill)
        buffer.putShort(0) // Urgent pointer
        
        // Copy data
        System.arraycopy(data, 0, packet, 40, dataLen)
        
        return packet
    }
    
    private fun createUdpResponsePacket(
        srcIp: InetAddress,
        dstIp: InetAddress,
        srcPort: Int,
        dstPort: Int,
        data: ByteArray,
        originalPacket: ByteArray
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val totalLen = ipHeaderLen + udpHeaderLen + data.size
        
        val packet = ByteArray(totalLen)
        val buffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN)
        
        // IP Header
        buffer.put(0x45.toByte())
        buffer.put(0.toByte())
        buffer.putShort(totalLen.toShort())
        buffer.putShort(0)
        buffer.putShort(0x4000.toShort())
        buffer.put(64.toByte())
        buffer.put(17.toByte()) // Protocol UDP
        buffer.putShort(0)
        buffer.put(srcIp.address)
        buffer.put(dstIp.address)
        
        // UDP Header
        buffer.putShort(srcPort.toShort())
        buffer.putShort(dstPort.toShort())
        buffer.putShort((udpHeaderLen + data.size).toShort())
        buffer.putShort(0) // Checksum (kernel will fill)
        
        // Copy data
        System.arraycopy(data, 0, packet, 28, data.size)
        
        return packet
    }

    data class ForwarderStats(
        val packetsForwarded: Long,
        val bytesForwarded: Long,
        val tcpConnections: Int,
        val udpSessions: Int
    )
}
