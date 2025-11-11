#include "packet_parser.h"
#include <sstream>
#include <iomanip>
#include <cstring>
#include <arpa/inet.h>
#include <android/log.h>

#define LOG_TAG "PacketParser"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

PacketParser::PacketParser() {
    LOGD("PacketParser initialized");
}

PacketParser::~PacketParser() {
}

ParsedPacket PacketParser::parsePacket(const uint8_t* data, size_t length) {
    ParsedPacket result;
    
    if (length < 20) {
        LOGE("Packet too small: %zu bytes", length);
        return result;
    }
    
    // Parse IPv4 header (assuming IP version 4)
    parseIPv4(data, length, result);
    
    return result;
}

void PacketParser::parseIPv4(const uint8_t* data, size_t length, ParsedPacket& result) {
    if (length < sizeof(IPHeader)) {
        return;
    }
    
    const IPHeader* ip_header = reinterpret_cast<const IPHeader*>(data);
    uint8_t version = (ip_header->version_ihl >> 4) & 0x0F;
    uint8_t ihl = (ip_header->version_ihl & 0x0F) * 4; // Header length in bytes
    
    if (version != 4) {
        LOGD("Not IPv4 packet, version: %d", version);
        return;
    }
    
    result.source_ip = ipToString(ntohl(ip_header->source_ip));
    result.dest_ip = ipToString(ntohl(ip_header->dest_ip));
    result.length = ntohs(ip_header->total_length);
    
    // Parse transport layer based on protocol
    const uint8_t* transport_data = data + ihl;
    size_t transport_length = length - ihl;
    
    switch (ip_header->protocol) {
        case 6: // TCP
            result.protocol = "TCP";
            parseTCP(transport_data, transport_length, result);
            break;
        case 17: // UDP
            result.protocol = "UDP";
            parseUDP(transport_data, transport_length, result);
            break;
        case 1: // ICMP
            result.protocol = "ICMP";
            break;
        default:
            result.protocol = "OTHER";
            break;
    }
}

void PacketParser::parseTCP(const uint8_t* data, size_t length, ParsedPacket& result) {
    if (length < sizeof(TCPHeader)) {
        return;
    }
    
    const TCPHeader* tcp_header = reinterpret_cast<const TCPHeader*>(data);
    result.source_port = ntohs(tcp_header->source_port);
    result.dest_port = ntohs(tcp_header->dest_port);
    
    // Parse TCP flags
    std::stringstream flags_ss;
    if (tcp_header->flags & 0x01) flags_ss << "FIN ";
    if (tcp_header->flags & 0x02) flags_ss << "SYN ";
    if (tcp_header->flags & 0x04) flags_ss << "RST ";
    if (tcp_header->flags & 0x08) flags_ss << "PSH ";
    if (tcp_header->flags & 0x10) flags_ss << "ACK ";
    if (tcp_header->flags & 0x20) flags_ss << "URG ";
    result.flags = flags_ss.str();
    
    // Calculate payload offset
    uint8_t data_offset = ((tcp_header->data_offset_reserved >> 4) & 0x0F) * 4;
    if (length > data_offset) {
        const uint8_t* payload = data + data_offset;
        size_t payload_length = length - data_offset;

        // Store payload
        result.payload.assign(payload, payload + payload_length);
        result.payload_preview = extractPayloadPreview(payload, payload_length);

        // Detect application-layer protocol
        bool protocol_detected = false;

        // Check for TLS/HTTPS first (most common on port 443)
        if (payload_length > 0) {
            // TLS record starts with content type (0x14-0x18) followed by version
            if (payload[0] >= 0x14 && payload[0] <= 0x18 && payload_length >= 3) {
                // Check for TLS version (0x03 0x00 to 0x03 0x03)
                if (payload[1] == 0x03 && payload[2] <= 0x03) {
                    parseTLS(payload, payload_length, result);
                    result.protocol = "TLS";
                    protocol_detected = true;
                }
            }
        }

        // Check for HTTP (plain text)
        if (!protocol_detected && payload_length > 4) {
            std::string start(reinterpret_cast<const char*>(payload), std::min(size_t(4), payload_length));
            if (start == "GET " || start == "POST" || start == "PUT " || start == "HEAD" ||
                start == "HTTP") {
                parseHTTP(payload, payload_length, result);
                result.protocol = "HTTP";
                protocol_detected = true;
            }
        }

        // Port-based detection as fallback
        if (!protocol_detected) {
            if (result.dest_port == 443 || result.source_port == 443) {
                result.protocol = "HTTPS";
            } else if (result.dest_port == 80 || result.source_port == 80 ||
                       result.dest_port == 8080 || result.source_port == 8080) {
                result.protocol = "HTTP";
            } else if (result.dest_port == 22 || result.source_port == 22) {
                result.protocol = "SSH";
            } else if (result.dest_port == 21 || result.source_port == 21) {
                result.protocol = "FTP";
            } else if (result.dest_port == 25 || result.source_port == 25) {
                result.protocol = "SMTP";
            } else if (result.dest_port == 110 || result.source_port == 110) {
                result.protocol = "POP3";
            } else if (result.dest_port == 143 || result.source_port == 143) {
                result.protocol = "IMAP";
            }
        }
    }
}

void PacketParser::parseUDP(const uint8_t* data, size_t length, ParsedPacket& result) {
    if (length < sizeof(UDPHeader)) {
        return;
    }

    const UDPHeader* udp_header = reinterpret_cast<const UDPHeader*>(data);
    result.source_port = ntohs(udp_header->source_port);
    result.dest_port = ntohs(udp_header->dest_port);

    // Parse payload
    if (length > sizeof(UDPHeader)) {
        const uint8_t* payload = data + sizeof(UDPHeader);
        size_t payload_length = length - sizeof(UDPHeader);

        result.payload.assign(payload, payload + payload_length);
        result.payload_preview = extractPayloadPreview(payload, payload_length);

        // Check for DNS
        if (result.dest_port == 53 || result.source_port == 53) {
            parseDNS(payload, payload_length, result);
            result.protocol = "DNS";
        }
        // Check for QUIC (HTTP/3)
        else if (result.dest_port == 443 || result.source_port == 443 ||
                 result.dest_port == 80 || result.source_port == 80) {
            // QUIC packets start with specific flags
            if (payload_length > 0) {
                uint8_t first_byte = payload[0];
                // Check for QUIC long header (0x80 bit set) or short header
                if ((first_byte & 0x80) || (first_byte & 0x40)) {
                    result.protocol = "QUIC";
                }
            }
        }
        // Check for DHCP
        else if (result.dest_port == 67 || result.dest_port == 68 ||
                 result.source_port == 67 || result.source_port == 68) {
            result.protocol = "DHCP";
        }
        // Check for NTP
        else if (result.dest_port == 123 || result.source_port == 123) {
            result.protocol = "NTP";
        }
    }
}

void PacketParser::parseHTTP(const uint8_t* data, size_t length, ParsedPacket& result) {
    if (length < 4) return;
    
    std::string payload_str(reinterpret_cast<const char*>(data), std::min(length, size_t(500)));
    
    // Check for HTTP methods
    if (payload_str.find("GET ") == 0) {
        result.http_method = "GET";
        size_t url_start = 4;
        size_t url_end = payload_str.find(" HTTP/", url_start);
        if (url_end != std::string::npos) {
            result.http_url = payload_str.substr(url_start, url_end - url_start);
        }
    } else if (payload_str.find("POST ") == 0) {
        result.http_method = "POST";
        size_t url_start = 5;
        size_t url_end = payload_str.find(" HTTP/", url_start);
        if (url_end != std::string::npos) {
            result.http_url = payload_str.substr(url_start, url_end - url_start);
        }
    } else if (payload_str.find("PUT ") == 0) {
        result.http_method = "PUT";
    } else if (payload_str.find("DELETE ") == 0) {
        result.http_method = "DELETE";
    } else if (payload_str.find("HTTP/") == 0) {
        result.http_method = "RESPONSE";
    }
}

void PacketParser::parseDNS(const uint8_t* data, size_t length, ParsedPacket& result) {
    if (length < 12) return;
    
    // Simple DNS query name extraction (simplified)
    // Skip DNS header (12 bytes)
    size_t offset = 12;
    std::stringstream query;
    
    while (offset < length && offset < 100) {
        uint8_t label_len = data[offset];
        if (label_len == 0) break;
        if (label_len > 63) break; // Invalid
        
        offset++;
        if (offset + label_len > length) break;
        
        if (query.tellp() > 0) query << ".";
        query << std::string(reinterpret_cast<const char*>(data + offset), label_len);
        offset += label_len;
    }
    
    result.dns_query = query.str();
}

void PacketParser::parseTLS(const uint8_t* data, size_t length, ParsedPacket& result) {
    if (length < 5) return;
    
    // Check for TLS handshake (ContentType = 22, Version = 3.x)
    if (data[0] == 0x16 && data[1] == 0x03) {
        // Check for ClientHello (HandshakeType = 1)
        if (length > 5 && data[5] == 0x01) {
            // Try to extract SNI (simplified)
            // This is a complex parsing task, for now just mark it
            result.tls_sni = "TLS_CLIENT_HELLO";
        }
    }
}

std::string PacketParser::extractPayloadPreview(const uint8_t* data, size_t length, int maxBytes) {
    std::stringstream ss;
    size_t preview_len = std::min(length, static_cast<size_t>(maxBytes));
    
    for (size_t i = 0; i < preview_len; i++) {
        if (data[i] >= 32 && data[i] <= 126) {
            ss << static_cast<char>(data[i]);
        } else {
            ss << '.';
        }
    }
    
    if (length > preview_len) {
        ss << "...";
    }
    
    return ss.str();
}

std::string PacketParser::ipToString(uint32_t ip) {
    std::stringstream ss;
    ss << ((ip >> 24) & 0xFF) << "."
       << ((ip >> 16) & 0xFF) << "."
       << ((ip >> 8) & 0xFF) << "."
       << (ip & 0xFF);
    return ss.str();
}
