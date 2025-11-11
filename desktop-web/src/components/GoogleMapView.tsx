import { useCallback } from 'react'
import { GoogleMap, useJsApiLoader, Marker, Polyline } from '@react-google-maps/api'

interface Location {
  ip: string
  lat: number
  lon: number
  country: string
  packetCount: number
  bytes: number
}

interface GoogleMapViewProps {
  userLocation: { lat: number; lon: number; label: string }
  destinations: Location[]
  animatedRoutes: number[]
}

const mapContainerStyle = {
  width: '100%',
  height: '100%'
}

const mapOptions = {
  styles: [
    {
      elementType: 'geometry',
      stylers: [{ color: '#0a1628' }]
    },
    {
      elementType: 'labels.text.stroke',
      stylers: [{ color: '#010816' }]
    },
    {
      elementType: 'labels.text.fill',
      stylers: [{ color: '#00D9FF' }]
    },
    {
      featureType: 'administrative',
      elementType: 'geometry.stroke',
      stylers: [{ color: '#00D9FF' }, { weight: 0.5 }]
    },
    {
      featureType: 'administrative.land_parcel',
      elementType: 'labels',
      stylers: [{ visibility: 'off' }]
    },
    {
      featureType: 'landscape',
      elementType: 'geometry',
      stylers: [{ color: '#0f1e2e' }]
    },
    {
      featureType: 'poi',
      stylers: [{ visibility: 'off' }]
    },
    {
      featureType: 'road',
      elementType: 'geometry',
      stylers: [{ color: '#1a2a3e' }]
    },
    {
      featureType: 'road',
      elementType: 'labels.icon',
      stylers: [{ visibility: 'off' }]
    },
    {
      featureType: 'transit',
      stylers: [{ visibility: 'off' }]
    },
    {
      featureType: 'water',
      elementType: 'geometry',
      stylers: [{ color: '#001428' }]
    },
    {
      featureType: 'water',
      elementType: 'labels.text.fill',
      stylers: [{ color: '#00A9D9' }]
    }
  ],
  disableDefaultUI: true,
  zoomControl: true,
  mapTypeControl: false,
  streetViewControl: false,
  fullscreenControl: false,
  backgroundColor: '#010816'
}

export default function GoogleMapView({ userLocation, destinations, animatedRoutes }: GoogleMapViewProps) {
  const { isLoaded, loadError } = useJsApiLoader({
    id: 'google-map-script',
    googleMapsApiKey: 'AIzaSyBdVl-cTICSwYKrZ95SuvNchl4dG3mQms0' // Using a demo key - replace with your own
  })

  if (loadError) {
    return (
      <div className="flex items-center justify-center h-full">
        <div className="text-center">
          <div className="text-red-400 text-xl mb-4">Failed to load Google Maps</div>
          <div className="text-gray-400 text-sm">
            The Google Maps API key may be invalid or restricted.
            <br />
            Please check the console for more details.
          </div>
        </div>
      </div>
    )
  }

  const onLoad = useCallback((map: google.maps.Map) => {
    const bounds = new window.google.maps.LatLngBounds()
    bounds.extend({ lat: userLocation.lat, lng: userLocation.lon })
    destinations.forEach(dest => {
      bounds.extend({ lat: dest.lat, lng: dest.lon })
    })
    map.fitBounds(bounds)
  }, [userLocation, destinations])

  const onUnmount = useCallback(() => {
    // Cleanup if needed
  }, [])

  const formatBytes = (bytes: number): string => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return `${(bytes / Math.pow(k, i)).toFixed(2)} ${sizes[i]}`
  }

  // Create custom marker icons (only if google is loaded)
  const userIcon = isLoaded ? {
    path: google.maps.SymbolPath.CIRCLE,
    fillColor: '#00FFC8',
    fillOpacity: 1,
    strokeColor: '#00FFC8',
    strokeWeight: 4,
    scale: 15
  } : undefined

  const destIcon = isLoaded ? {
    path: google.maps.SymbolPath.CIRCLE,
    fillColor: '#00D9FF',
    fillOpacity: 1,
    strokeColor: '#00D9FF',
    strokeWeight: 3,
    scale: 10
  } : undefined

  return isLoaded ? (
    <GoogleMap
      mapContainerStyle={mapContainerStyle}
      center={{ lat: 20, lng: 20 }}
      zoom={2}
      onLoad={onLoad}
      onUnmount={onUnmount}
      options={mapOptions}
    >
      {/* User location marker */}
      <Marker
        position={{ lat: userLocation.lat, lng: userLocation.lon }}
        icon={userIcon}
        title={userLocation.label}
      />

      {/* Connection lines */}
      {destinations.map((dest, idx) => {
        const isAnimated = animatedRoutes.includes(idx)
        return (
          <Polyline
            key={`route-${idx}`}
            path={[
              { lat: userLocation.lat, lng: userLocation.lon },
              { lat: dest.lat, lng: dest.lon }
            ]}
            options={{
              strokeColor: isAnimated ? '#00FFC8' : '#00D9FF',
              strokeOpacity: isAnimated ? 1 : 0.6,
              strokeWeight: 3,
              geodesic: true
            }}
          />
        )
      })}

      {/* Destination markers */}
      {destinations.map((dest, idx) => (
        <Marker
          key={`marker-${idx}`}
          position={{ lat: dest.lat, lng: dest.lon }}
          icon={destIcon}
          title={`${dest.ip}\n${dest.country}\n${dest.packetCount} packets\n${formatBytes(dest.bytes)}`}
        />
      ))}
    </GoogleMap>
  ) : (
    <div className="flex items-center justify-center h-full">
      <div className="text-cyan-400 text-xl">Loading map...</div>
    </div>
  )
}
