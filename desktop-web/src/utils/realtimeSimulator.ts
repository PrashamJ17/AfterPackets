import type { PacketInfo, ExportData, IpTalker } from '../types'

// Simulate real-time packet capture
export class RealtimeSimulator {
  private interval: NodeJS.Timeout | null = null
  private packetId = 1000
  private callbacks: Array<(data: Partial<ExportData>) => void> = []

  private sampleIPs = [
    { ip: '142.250.185.206', country: 'United States', lat: 37.4223, lon: -122.0840 },
    { ip: '157.240.241.35', country: 'Ireland', lat: 53.3498, lon: -6.2603 },
    { ip: '13.107.42.14', country: 'United States', lat: 47.6062, lon: -122.3321 },
    { ip: '104.244.42.129', country: 'United States', lat: 37.7749, lon: -122.4194 },
    { ip: '52.84.167.25', country: 'Singapore', lat: 1.3521, lon: 103.8198 },
    { ip: '151.101.1.140', country: 'United Kingdom', lat: 51.5074, lon: -0.1278 },
  ]

  private protocols = ['TCP', 'UDP', 'HTTP', 'HTTPS', 'DNS', 'ICMP']

  start(intervalMs: number = 2000) {
    if (this.interval) return

    this.interval = setInterval(() => {
      this.generatePacket()
    }, intervalMs)
  }

  stop() {
    if (this.interval) {
      clearInterval(this.interval)
      this.interval = null
    }
  }

  onData(callback: (data: Partial<ExportData>) => void) {
    this.callbacks.push(callback)
  }

  private generatePacket() {
    const destIP = this.sampleIPs[Math.floor(Math.random() * this.sampleIPs.length)]
    const protocol = this.protocols[Math.floor(Math.random() * this.protocols.length)]
    
    const packet: PacketInfo = {
      id: this.packetId++,
      timestamp: Date.now(),
      protocol,
      sourceIp: '192.168.1.100',
      destIp: destIP.ip,
      sourcePort: 50000 + Math.floor(Math.random() * 15000),
      destPort: protocol === 'DNS' ? 53 : protocol === 'HTTP' ? 80 : 443,
      length: Math.floor(Math.random() * 1500) + 64,
      flags: protocol === 'TCP' ? 'ACK PSH' : '',
      payloadPreview: 'Live captured data...',
      destCountry: destIP.country,
      destLat: destIP.lat,
      destLon: destIP.lon
    }

    // Create updated talker info
    const talker: IpTalker = {
      ip: destIP.ip,
      packetCount: Math.floor(Math.random() * 100) + 1,
      bytes: Math.floor(Math.random() * 500000) + 10000,
      country: destIP.country,
      lat: destIP.lat,
      lon: destIP.lon
    }

    const update: Partial<ExportData> = {
      packets: [packet],
      stats: {
        totalPackets: this.packetId,
        packetsPerSecond: Math.random() * 50 + 20,
        totalBytes: this.packetId * 850,
        bytesPerSecond: Math.random() * 50000 + 10000,
        protocolDistribution: {},
        topTalkers: [talker],
        startTime: Date.now() - 600000,
        lastUpdate: Date.now()
      }
    }

    this.callbacks.forEach(cb => cb(update))
  }
}

export const realtimeSimulator = new RealtimeSimulator()
