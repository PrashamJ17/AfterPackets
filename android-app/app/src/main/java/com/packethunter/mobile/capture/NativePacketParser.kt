package com.packethunter.mobile.capture

/**
 * Data class to receive parsed packet data from native code
 */
data class ParsedPacketData(
    var protocol: String = "",
    var sourceIp: String = "",
    var destIp: String = "",
    var sourcePort: Int = 0,
    var destPort: Int = 0,
    var length: Int = 0,
    var flags: String = "",
    var payload: ByteArray? = null,
    var payloadPreview: String = "",
    var httpMethod: String? = null,
    var httpUrl: String? = null,
    var dnsQuery: String? = null,
    var dnsResponse: String? = null,
    var tlsSni: String? = null
)

/**
 * JNI wrapper for native packet parser
 */
class NativePacketParser {
    
    companion object {
        init {
            System.loadLibrary("packethunter")
        }
    }
    
    /**
     * Initialize the native parser
     */
    external fun initParser()
    
    /**
     * Parse a raw packet
     * @param data Raw packet bytes
     * @return Parsed packet data or null on error
     */
    external fun parsePacket(data: ByteArray): ParsedPacketData?
    
    /**
     * Clean up native resources
     */
    external fun destroyParser()
}
