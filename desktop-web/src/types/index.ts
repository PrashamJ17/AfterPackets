export interface PacketInfo {
  id: number
  timestamp: number
  protocol: string
  sourceIp: string
  destIp: string
  sourcePort: number
  destPort: number
  length: number
  flags: string
  payloadPreview: string
  httpMethod?: string
  httpUrl?: string
  dnsQuery?: string
  tlsSni?: string
  destCountry?: string
  destCity?: string
  destLat?: number
  destLon?: number
}

export interface Alert {
  id: number
  timestamp: number
  severity: 'low' | 'medium' | 'high' | 'critical'
  type: string
  title: string
  description: string
  acknowledged: boolean
}

export interface CaptureStats {
  totalPackets: number
  packetsPerSecond: number
  totalBytes: number
  bytesPerSecond: number
  protocolDistribution: Record<string, number>
  topTalkers: IpTalker[]
  startTime: number
  lastUpdate: number
}

export interface IpTalker {
  ip: string
  packetCount: number
  bytes: number
  country?: string
  lat?: number
  lon?: number
}

export interface ExportData {
  version: string
  exportTime: number
  stats: CaptureStats
  packets: PacketInfo[]
  alerts: Alert[]
  metadata: {
    totalPackets: number
    totalAlerts: number
    captureStartTime: number
    captureEndTime: number
  }
}

export interface Timeline {
  buckets: TimelineBucket[]
  totalDuration: number
  stats: CaptureStats
}

export interface TimelineBucket {
  timestamp: number
  packetCount: number
  bytes: number
  protocols: Record<string, number>
}

export interface DetectionRule {
  id: number
  name: string
  enabled: boolean
  metric: string
  condition: string
  threshold: number
  timeWindowSeconds: number
  action: string
  severity: string
}
