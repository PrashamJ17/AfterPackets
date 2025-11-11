import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Polyline } from 'react-leaflet'
import { useStore } from '../store/useStore'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

// Fix default marker icons
delete (L.Icon.Default.prototype as any)._getIconUrl
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
})

// Create custom red marker icon
const redIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-2x-red.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  popupAnchor: [1, -34],
  shadowSize: [41, 41]
})

// Format bytes helper
const formatBytes = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
}

// Assume user location (or could use geolocation API)
const USER_LOCATION = { lat: 37.7749, lon: -122.4194, label: 'Your Device' } // San Francisco

export default function MapView() {
  const { exportData } = useStore()
  const [animatedRoutes, setAnimatedRoutes] = useState<number[]>([])

  if (!exportData) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <div className="text-gray-400 text-xl mb-4">No data loaded</div>
          <p className="text-gray-500 mb-6">Please load sample data or import a capture file to view the map</p>
          <a 
            href="/import" 
            className="px-6 py-3 bg-cyber-blue text-white rounded-lg hover:bg-cyber-blue/80 transition-colors inline-block"
          >
            Go to Import Page
          </a>
        </div>
      </div>
    )
  }

  // Get locations with coordinates, or use default if none exist
  let locations = exportData.stats.topTalkers.filter(t => t.lat && t.lon)
  
  // If no locations with coordinates, add sample coordinates to existing talkers
  if (locations.length === 0 && exportData.stats.topTalkers.length > 0) {
    locations = exportData.stats.topTalkers.slice(0, 5).map((talker, idx) => ({
      ...talker,
      lat: 20 + (idx * 10) - 20,
      lon: -20 + (idx * 20),
      country: talker.country || 'Unknown'
    }))
  }

  // Animate routes sequentially
  useEffect(() => {
    if (locations.length > 0) {
      const interval = setInterval(() => {
        setAnimatedRoutes(prev => {
          const next = prev.length >= locations.length ? [] : [...prev, prev.length]
          return next
        })
      }, 800)
      return () => clearInterval(interval)
    }
  }, [locations.length])

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-cyber-blue mb-2">Network Geo-Location Map</h1>
        <p className="text-gray-400">Remote hosts plotted by geographic location with live connection tracking</p>
      </div>

      <div className="bg-gradient-to-b from-[#010816] to-[#0a1628] border border-cyan-500/50 rounded-lg overflow-hidden shadow-2xl shadow-cyan-500/30" style={{ height: '700px' }}>
        <MapContainer
          center={[20, 0]}
          zoom={2}
          minZoom={1}
          maxZoom={18}
          style={{ height: '100%', width: '100%' }}
          className="leaflet-map-dark"
        >
          {/* OpenStreetMap tiles - visible and free */}
          <TileLayer
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          />
          
          {/* User location marker */}
          <Marker position={[USER_LOCATION.lat, USER_LOCATION.lon]} icon={redIcon}>
            <Popup>
              <div className="text-center">
                <strong className="text-red-600 text-lg">{USER_LOCATION.label}</strong>
                <br />
                <span className="text-gray-600">Source Location</span>
              </div>
            </Popup>
          </Marker>

          {/* Connection lines */}
          {locations.map((location, idx) => {
            const isAnimated = animatedRoutes.includes(idx)
            return (
              <Polyline
                key={`route-${idx}`}
                positions={[
                  [USER_LOCATION.lat, USER_LOCATION.lon],
                  [location.lat!, location.lon!]
                ]}
                pathOptions={{
                  color: isAnimated ? '#FF0000' : '#DC2626',
                  weight: isAnimated ? 4 : 2,
                  opacity: isAnimated ? 1 : 0.6,
                  dashArray: isAnimated ? '10, 10' : undefined
                }}
              />
            )
          })}

          {/* Destination markers */}
          {locations.map((location, idx) => (
            <Marker
              key={`marker-${idx}`}
              position={[location.lat!, location.lon!]}
              icon={redIcon}
            >
              <Popup>
                <div className="min-w-[200px]">
                  <strong className="text-red-600 text-base block mb-2">{location.ip}</strong>
                  <div className="text-sm text-gray-700 mb-1">📍 {location.country}</div>
                  <div className="text-sm text-gray-700 mb-1">📦 {location.packetCount} packets</div>
                  <div className="text-sm text-gray-700">💾 {formatBytes(location.bytes)}</div>
                </div>
              </Popup>
            </Marker>
          ))}
        </MapContainer>
      </div>

      <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
        <h2 className="text-xl font-bold mb-4 text-cyber-cyan">Location Details</h2>
        <div className="space-y-3">
          {locations.map((loc, idx) => (
            <div key={idx} className="flex justify-between items-center p-3 bg-bg-elevated rounded">
              <div>
                <div className="font-mono text-cyber-cyan">{loc.ip}</div>
                <div className="text-sm text-gray-400">
                  {loc.country} • {loc.lat?.toFixed(2)}, {loc.lon?.toFixed(2)}
                </div>
              </div>
              <div className="text-right">
                <div className="text-sm">{loc.packetCount} packets</div>
                <div className="text-xs text-gray-500">{formatBytes(loc.bytes)}</div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}

