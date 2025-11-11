import { useState, useEffect } from 'react'
import { Play, Pause, SkipBack, SkipForward } from 'lucide-react'
import { useStore } from '../store/useStore'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'

export default function ReplayPage() {
  const { timeline, exportData } = useStore()
  const [isPlaying, setIsPlaying] = useState(false)
  const [currentIndex, setCurrentIndex] = useState(0)

  useEffect(() => {
    if (!isPlaying || !timeline) return

    const interval = setInterval(() => {
      setCurrentIndex((prev) => {
        if (prev >= timeline.buckets.length - 1) {
          setIsPlaying(false)
          return prev
        }
        return prev + 1
      })
    }, 500)

    return () => clearInterval(interval)
  }, [isPlaying, timeline])

  if (!timeline || !exportData) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-400">No timeline data available</p>
      </div>
    )
  }

  const chartData = timeline.buckets.slice(0, currentIndex + 1).map((bucket) => ({
    time: new Date(bucket.timestamp).toLocaleTimeString(),
    packets: bucket.packetCount,
    bytes: bucket.bytes / 1024, // KB
  }))

  return (
    <div className="space-y-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold text-cyber-blue mb-2">Session Replay</h1>
        <p className="text-gray-400">Replay captured session with timeline visualization</p>
      </div>

      {/* Timeline Chart */}
      <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
        <h2 className="text-xl font-bold mb-4 text-cyber-cyan">Packet Rate Over Time</h2>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#333" />
            <XAxis dataKey="time" stroke="#666" />
            <YAxis stroke="#666" />
            <Tooltip
              contentStyle={{ backgroundColor: '#1A1F2E', border: '1px solid #333' }}
              labelStyle={{ color: '#fff' }}
            />
            <Line type="monotone" dataKey="packets" stroke="#00D9FF" strokeWidth={2} dot={false} />
            <Line type="monotone" dataKey="bytes" stroke="#00FFC8" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Playback Controls */}
      <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-xl font-bold text-cyber-green">Playback Controls</h2>
          <div className="text-sm text-gray-400">
            Frame {currentIndex + 1} / {timeline.buckets.length}
          </div>
        </div>

        <div className="flex items-center justify-center space-x-4 mb-6">
          <button
            onClick={() => setCurrentIndex(0)}
            className="p-3 bg-bg-elevated hover:bg-gray-700 rounded-lg transition-colors"
          >
            <SkipBack size={24} />
          </button>
          <button
            onClick={() => setIsPlaying(!isPlaying)}
            className="p-4 bg-cyber-blue hover:bg-cyber-blue/80 rounded-lg transition-colors"
          >
            {isPlaying ? <Pause size={28} /> : <Play size={28} />}
          </button>
          <button
            onClick={() => setCurrentIndex(Math.min(currentIndex + 1, timeline.buckets.length - 1))}
            className="p-3 bg-bg-elevated hover:bg-gray-700 rounded-lg transition-colors"
          >
            <SkipForward size={24} />
          </button>
        </div>

        <input
          type="range"
          min={0}
          max={timeline.buckets.length - 1}
          value={currentIndex}
          onChange={(e) => setCurrentIndex(parseInt(e.target.value))}
          className="w-full"
        />
      </div>

      {/* Current Frame Info */}
      {timeline.buckets[currentIndex] && (
        <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
          <h2 className="text-xl font-bold mb-4 text-cyber-cyan">Current Frame</h2>
          <div className="grid grid-cols-3 gap-4">
            <div>
              <div className="text-sm text-gray-400">Timestamp</div>
              <div className="text-lg font-mono">
                {new Date(timeline.buckets[currentIndex].timestamp).toLocaleTimeString()}
              </div>
            </div>
            <div>
              <div className="text-sm text-gray-400">Packets</div>
              <div className="text-lg text-cyber-blue">{timeline.buckets[currentIndex].packetCount}</div>
            </div>
            <div>
              <div className="text-sm text-gray-400">Bytes</div>
              <div className="text-lg text-cyber-cyan">{formatBytes(timeline.buckets[currentIndex].bytes)}</div>
            </div>
          </div>
          <div className="mt-4">
            <div className="text-sm text-gray-400 mb-2">Protocols</div>
            <div className="flex flex-wrap gap-2">
              {Object.entries(timeline.buckets[currentIndex].protocols).map(([proto, count]) => (
                <span key={proto} className="px-3 py-1 bg-cyber-blue/20 text-cyber-blue rounded-full text-sm">
                  {proto}: {count}
                </span>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}
