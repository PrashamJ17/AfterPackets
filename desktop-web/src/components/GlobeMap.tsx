import { useEffect, useRef } from 'react'
import Globe from 'globe.gl'

interface Location {
  ip: string
  lat: number
  lon: number
  country: string
  packetCount: number
  bytes: number
}

interface GlobeMapProps {
  userLocation: { lat: number; lon: number; label: string }
  destinations: Location[]
  animatedRoutes: number[]
}

export default function GlobeMap({ userLocation, destinations, animatedRoutes }: GlobeMapProps) {
  const globeEl = useRef<HTMLDivElement>(null)
  const globeInstance = useRef<any>(null)

  useEffect(() => {
    if (!globeEl.current) return

    // Initialize globe
    const globe = new Globe(globeEl.current)
      .globeImageUrl('//unpkg.com/three-globe/example/img/earth-night.jpg')
      .bumpImageUrl('//unpkg.com/three-globe/example/img/earth-topology.png')
      .backgroundImageUrl('//unpkg.com/three-globe/example/img/night-sky.png')
      .showAtmosphere(true)
      .atmosphereColor('#00D9FF')
      .atmosphereAltitude(0.15)
      .width(globeEl.current.clientWidth)
      .height(globeEl.current.clientHeight)

    globeInstance.current = globe

    // Configure camera
    const controls = globe.controls()
    controls.autoRotate = true
    controls.autoRotateSpeed = 0.5
    controls.enableZoom = true
    controls.minDistance = 200
    controls.maxDistance = 600

    // Set initial view
    globe.pointOfView({ lat: 20, lng: 20, altitude: 2.5 }, 0)

    return () => {
      if (globeInstance.current) {
        globeInstance.current._destructor?.()
      }
    }
  }, [])

  useEffect(() => {
    if (!globeInstance.current) return

    // Create arcs data for routes
    const arcsData = destinations.map((dest, idx) => ({
      startLat: userLocation.lat,
      startLng: userLocation.lon,
      endLat: dest.lat,
      endLng: dest.lon,
      color: animatedRoutes.includes(idx) ? '#00FFC8' : '#00D9FF',
      ip: dest.ip,
      country: dest.country
    }))

    // Add arcs (routes)
    globeInstance.current
      .arcsData(arcsData)
      .arcColor('color')
      .arcDashLength(0.4)
      .arcDashGap(0.2)
      .arcDashAnimateTime(2000)
      .arcStroke(0.5)
      .arcAltitude(0.3)
      .arcAltitudeAutoScale(0.3)

    // Add points for destinations
    const pointsData = [
      {
        lat: userLocation.lat,
        lng: userLocation.lon,
        size: 1.2,
        color: '#00FFC8',
        label: userLocation.label
      },
      ...destinations.map(dest => ({
        lat: dest.lat,
        lng: dest.lon,
        size: 0.8,
        color: '#00D9FF',
        label: `${dest.ip}\n${dest.country}\n${dest.packetCount} packets`
      }))
    ]

    globeInstance.current
      .pointsData(pointsData)
      .pointAltitude(0.01)
      .pointRadius('size')
      .pointColor('color')
      .pointLabel('label')

  }, [userLocation, destinations, animatedRoutes])

  return (
    <div 
      ref={globeEl} 
      style={{ 
        width: '100%', 
        height: '100%',
        background: 'radial-gradient(circle at center, #0a1628 0%, #010816 100%)'
      }}
    />
  )
}
