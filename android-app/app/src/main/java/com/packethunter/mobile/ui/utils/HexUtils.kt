package com.packethunter.mobile.ui.utils

/**
 * Utility functions for hex and ASCII conversion
 */
object HexUtils {
    
    /**
     * Convert byte array to hex string
     */
    fun bytesToHex(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        
        return bytes.joinToString(" ") { byte ->
            String.format("%02X", byte.toInt() and 0xFF)
        }
    }
    
    /**
     * Convert byte array to hex string with offset and length
     */
    fun bytesToHex(bytes: ByteArray?, offset: Int, length: Int): String {
        if (bytes == null || bytes.isEmpty()) return ""
        if (offset >= bytes.size) return ""
        
        val end = (offset + length).coerceAtMost(bytes.size)
        return bytes.sliceArray(offset until end).joinToString(" ") { byte ->
            String.format("%02X", byte.toInt() and 0xFF)
        }
    }
    
    /**
     * Convert byte to ASCII character (if printable) or '.'
     */
    fun byteToAscii(byte: Byte): Char {
        val value = byte.toInt() and 0xFF
        return if (value >= 32 && value <= 126) {
            value.toChar()
        } else {
            '.'
        }
    }
    
    /**
     * Convert byte array to ASCII string
     */
    fun bytesToAscii(bytes: ByteArray?): String {
        if (bytes == null || bytes.isEmpty()) return ""
        
        return bytes.map { byteToAscii(it) }.joinToString("")
    }
    
    /**
     * Format payload in hex/ASCII view (like hexdump)
     * Returns list of formatted lines
     */
    fun formatHexDump(bytes: ByteArray?, bytesPerLine: Int = 16): List<String> {
        if (bytes == null || bytes.isEmpty()) return listOf("(empty)")
        
        val lines = mutableListOf<String>()
        var offset = 0
        
        while (offset < bytes.size) {
            val lineBytes = bytes.sliceArray(offset until (offset + bytesPerLine).coerceAtMost(bytes.size))
            
            // Offset (8 hex digits)
            val offsetStr = String.format("%08X", offset)
            
            // Hex representation
            val hexStr = lineBytes.joinToString(" ") { byte ->
                String.format("%02X", byte.toInt() and 0xFF)
            }
            // Pad to 3*16 = 48 characters
            val hexPadded = hexStr.padEnd(48, ' ')
            
            // ASCII representation
            val asciiStr = lineBytes.map { byteToAscii(it) }.joinToString("")
            
            lines.add("$offsetStr  $hexPadded  |$asciiStr|")
            offset += bytesPerLine
        }
        
        return lines
    }
    
    /**
     * Get hex representation with line breaks for display
     */
    fun formatHex(bytes: ByteArray?, maxBytes: Int = 512): String {
        if (bytes == null || bytes.isEmpty()) return "(empty)"
        
        val displayBytes = if (bytes.size > maxBytes) {
            bytes.sliceArray(0 until maxBytes)
        } else {
            bytes
        }
        
        return formatHexDump(displayBytes).joinToString("\n") +
                if (bytes.size > maxBytes) "\n... (${bytes.size - maxBytes} more bytes)" else ""
    }
}

