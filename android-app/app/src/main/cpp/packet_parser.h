#ifndef PACKET_PARSER_H
#define PACKET_PARSER_H

#include <cstdint>
#include <string>
#include <vector>

// IP header structure
struct IPHeader {
    uint8_t version_ihl;
    uint8_t tos;
    uint16_t total_length;
    uint16_t identification;
    uint16_t flags_fragment;
    uint8_t ttl;
    uint8_t protocol;
    uint16_t checksum;
    uint32_t source_ip;
    uint32_t dest_ip;
};

// TCP header structure
struct TCPHeader {
    uint16_t source_port;
    uint16_t dest_port;
    uint32_t seq_number;
    uint32_t ack_number;
    uint8_t data_offset_reserved;
    uint8_t flags;
    uint16_t window;
    uint16_t checksum;
    uint16_t urgent_pointer;
};

// UDP header structure
struct UDPHeader {
    uint16_t source_port;
    uint16_t dest_port;
    uint16_t length;
    uint16_t checksum;
};

// Parsed packet information
struct ParsedPacket {
    std::string protocol;
    std::string source_ip;
    std::string dest_ip;
    int source_port;
    int dest_port;
    int length;
    std::string flags;
    std::vector<uint8_t> payload;
    std::string payload_preview;
    
    // Protocol-specific
    std::string http_method;
    std::string http_url;
    std::string dns_query;
    std::string dns_response;
    std::string tls_sni;
};

class PacketParser {
public:
    PacketParser();
    ~PacketParser();
    
    // Parse raw packet data
    ParsedPacket parsePacket(const uint8_t* data, size_t length);
    
private:
    // Helper functions
    std::string ipToString(uint32_t ip);
    
    // Protocol parsers
    void parseIPv4(const uint8_t* data, size_t length, ParsedPacket& result);
    void parseTCP(const uint8_t* data, size_t length, ParsedPacket& result);
    void parseUDP(const uint8_t* data, size_t length, ParsedPacket& result);
    void parseHTTP(const uint8_t* data, size_t length, ParsedPacket& result);
    void parseDNS(const uint8_t* data, size_t length, ParsedPacket& result);
    void parseTLS(const uint8_t* data, size_t length, ParsedPacket& result);
    
    std::string extractPayloadPreview(const uint8_t* data, size_t length, int maxBytes = 64);
};

#endif // PACKET_PARSER_H
