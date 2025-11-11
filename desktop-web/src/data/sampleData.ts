import type { ExportData, Timeline } from '../types'

// Sample data for testing the desktop app
export const sampleExportData: ExportData = {
  version: "1.0",
  exportTime: Date.now(),
  stats: {
    totalPackets: 15420,
    packetsPerSecond: 124.5,
    totalBytes: 8947200,
    bytesPerSecond: 72150,
    protocolDistribution: {
      "TCP": 8234,
      "UDP": 4521,
      "ICMP": 892,
      "HTTP": 1234,
      "HTTPS": 539
    },
    topTalkers: [
      {
        ip: "142.250.185.206",
        packetCount: 2341,
        bytes: 1847200,
        country: "United States",
        lat: 37.4223,
        lon: -122.0840
      },
      {
        ip: "157.240.241.35",
        packetCount: 1823,
        bytes: 1234567,
        country: "Ireland",
        lat: 53.3498,
        lon: -6.2603
      },
      {
        ip: "13.107.42.14",
        packetCount: 1456,
        bytes: 987654,
        country: "United States",
        lat: 47.6062,
        lon: -122.3321
      },
      {
        ip: "104.244.42.129",
        packetCount: 1234,
        bytes: 856432,
        country: "United States",
        lat: 37.7749,
        lon: -122.4194
      },
      {
        ip: "52.84.167.25",
        packetCount: 987,
        bytes: 654321,
        country: "Singapore",
        lat: 1.3521,
        lon: 103.8198
      },
      {
        ip: "151.101.1.140",
        packetCount: 876,
        bytes: 543210,
        country: "United Kingdom",
        lat: 51.5074,
        lon: -0.1278
      },
      {
        ip: "172.217.14.206",
        packetCount: 765,
        bytes: 432109,
        country: "United States",
        lat: 40.7128,
        lon: -74.0060
      },
      {
        ip: "34.107.221.82",
        packetCount: 654,
        bytes: 321098,
        country: "Germany",
        lat: 52.5200,
        lon: 13.4050
      }
    ],
    startTime: Date.now() - 3600000, // 1 hour ago
    lastUpdate: Date.now()
  },
  packets: [
    {
      id: 1,
      timestamp: Date.now() - 3500000,
      protocol: "HTTPS",
      sourceIp: "192.168.1.100",
      destIp: "142.250.185.206",
      sourcePort: 54321,
      destPort: 443,
      length: 1420,
      flags: "ACK PSH",
      payloadPreview: "TLS handshake data...",
      tlsSni: "www.google.com",
      destCountry: "United States",
      destLat: 37.4223,
      destLon: -122.0840
    },
    {
      id: 2,
      timestamp: Date.now() - 3490000,
      protocol: "DNS",
      sourceIp: "192.168.1.100",
      destIp: "8.8.8.8",
      sourcePort: 52341,
      destPort: 53,
      length: 64,
      flags: "",
      payloadPreview: "",
      dnsQuery: "www.facebook.com"
    },
    {
      id: 3,
      timestamp: Date.now() - 3480000,
      protocol: "HTTP",
      sourceIp: "192.168.1.100",
      destIp: "93.184.216.34",
      sourcePort: 54322,
      destPort: 80,
      length: 512,
      flags: "ACK PSH",
      payloadPreview: "GET /api/data HTTP/1.1...",
      httpMethod: "GET",
      httpUrl: "/api/data"
    },
    {
      id: 4,
      timestamp: Date.now() - 3470000,
      protocol: "TCP",
      sourceIp: "192.168.1.100",
      destIp: "157.240.241.35",
      sourcePort: 54323,
      destPort: 443,
      length: 2048,
      flags: "ACK",
      payloadPreview: "Encrypted payload...",
      destCountry: "Ireland",
      destLat: 53.3498,
      destLon: -6.2603
    },
    {
      id: 5,
      timestamp: Date.now() - 3460000,
      protocol: "UDP",
      sourceIp: "192.168.1.100",
      destIp: "8.8.4.4",
      sourcePort: 52342,
      destPort: 53,
      length: 72,
      flags: "",
      payloadPreview: "",
      dnsQuery: "api.twitter.com"
    },
    {
      id: 6,
      timestamp: Date.now() - 3450000,
      protocol: "ICMP",
      sourceIp: "192.168.1.100",
      destIp: "1.1.1.1",
      sourcePort: 0,
      destPort: 0,
      length: 84,
      flags: "",
      payloadPreview: "Echo request"
    },
    {
      id: 7,
      timestamp: Date.now() - 3440000,
      protocol: "HTTPS",
      sourceIp: "192.168.1.100",
      destIp: "13.107.42.14",
      sourcePort: 54324,
      destPort: 443,
      length: 1350,
      flags: "ACK PSH",
      payloadPreview: "TLS application data...",
      tlsSni: "teams.microsoft.com",
      destCountry: "United States",
      destLat: 47.6062,
      destLon: -122.3321
    },
    {
      id: 8,
      timestamp: Date.now() - 3430000,
      protocol: "TCP",
      sourceIp: "192.168.1.100",
      destIp: "104.244.42.129",
      sourcePort: 54325,
      destPort: 443,
      length: 896,
      flags: "SYN ACK",
      payloadPreview: "",
      destCountry: "United States",
      destLat: 37.7749,
      destLon: -122.4194
    },
    {
      id: 9,
      timestamp: Date.now() - 3420000,
      protocol: "DNS",
      sourceIp: "192.168.1.100",
      destIp: "8.8.8.8",
      sourcePort: 52343,
      destPort: 53,
      length: 68,
      flags: "",
      payloadPreview: "",
      dnsQuery: "cdn.jsdelivr.net"
    },
    {
      id: 10,
      timestamp: Date.now() - 3410000,
      protocol: "HTTPS",
      sourceIp: "192.168.1.100",
      destIp: "52.84.167.25",
      sourcePort: 54326,
      destPort: 443,
      length: 3200,
      flags: "ACK PSH",
      payloadPreview: "Large TLS payload...",
      destCountry: "Singapore",
      destLat: 1.3521,
      destLon: 103.8198
    }
  ],
  alerts: [
    {
      id: 1,
      timestamp: Date.now() - 3300000,
      severity: "high",
      type: "data_exfil",
      title: "Large Data Transfer Detected",
      description: "Sent 2.5MB to 52.84.167.25 in 8 seconds",
      acknowledged: false
    },
    {
      id: 2,
      timestamp: Date.now() - 3100000,
      severity: "medium",
      type: "dns_spoof",
      title: "Suspicious DNS Response",
      description: "DNS response for api.twitter.com returned unexpected IP",
      acknowledged: false
    },
    {
      id: 3,
      timestamp: Date.now() - 2900000,
      severity: "critical",
      type: "mitm",
      title: "Possible MITM Attack",
      description: "TLS certificate fingerprint changed for www.google.com",
      acknowledged: true
    },
    {
      id: 4,
      timestamp: Date.now() - 2700000,
      severity: "low",
      type: "custom_rule",
      title: "High Packet Rate",
      description: "Packet rate exceeded 200/sec threshold",
      acknowledged: true
    }
  ],
  metadata: {
    totalPackets: 15420,
    totalAlerts: 4,
    captureStartTime: Date.now() - 3600000,
    captureEndTime: Date.now()
  }
}

export const sampleTimeline: Timeline = {
  buckets: Array.from({ length: 60 }, (_, i) => ({
    timestamp: Date.now() - (60 - i) * 60000,
    packetCount: Math.floor(Math.random() * 300) + 50,
    bytes: Math.floor(Math.random() * 150000) + 20000,
    protocols: {
      "TCP": Math.floor(Math.random() * 150) + 25,
      "UDP": Math.floor(Math.random() * 80) + 10,
      "ICMP": Math.floor(Math.random() * 20) + 2,
      "HTTP": Math.floor(Math.random() * 40) + 5,
      "HTTPS": Math.floor(Math.random() * 30) + 3
    }
  })),
  totalDuration: 3600000,
  stats: sampleExportData.stats
}
