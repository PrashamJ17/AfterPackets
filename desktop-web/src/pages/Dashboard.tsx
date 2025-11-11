import { useEffect, useState } from 'react'
import { useStore } from '../store/useStore'
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts'
import { Activity, Database, HardDrive, TrendingUp, Radio, Circle } from 'lucide-react'
import { realtimeSimulator } from '../utils/realtimeSimulator'
import { sampleExportData } from '../data/sampleData'

const COLORS = ['#00D9FF', '#00FFC8', '#39FF14', '#BF40BF', '#FF3366']

export default function Dashboard() {
  const { exportData, setExportData } = useStore()
  const [isLiveMode, setIsLiveMode] = useState(false)

  useEffect(() => {
    if (isLiveMode && exportData) {
      realtimeSimulator.onData((update) => {
        setExportData({
          ...exportData,
          packets: [...update.packets!, ...exportData.packets.slice(0, 50)],
          stats: {
            ...exportData.stats,
            totalPackets: exportData.stats.totalPackets + 1,
            lastUpdate: Date.now()
          }
        })
      })
      realtimeSimulator.start(2000)
      return () => realtimeSimulator.stop()
    }
  }, [isLiveMode, exportData, setExportData])

  if (!exportData) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="text-center">
          <Database className="w-16 h-16 mx-auto mb-4 text-gray-600" />
          <p className="text-gray-400 mb-6">Waiting for Android device connection...</p>
          <p className="text-gray-500 text-sm mb-6">OR</p>
          <button
            onClick={() => setExportData(sampleExportData)}
            className="px-6 py-3 bg-cyber-blue text-white rounded-lg hover:bg-cyber-blue/80 transition-colors"
          >
            Load Demo Data
          </button>
          <p className="text-gray-600 text-xs mt-4">Click to see the interface with sample packet data</p>
        </div>
      </div>
    )
  }

  const { stats, packets, alerts } = exportData

  const protocolData = Object.entries(stats.protocolDistribution).map(([name, value]) => ({
    name,
    value,
  }))

  return (
    <div className="space-y-6">
      {/* Live Mode Toggle */}
      {exportData && (
        <div className="flex justify-end">
          <button
            onClick={() => setIsLiveMode(!isLiveMode)}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg border transition-all ${
              isLiveMode
                ? 'bg-cyber-green/20 text-cyber-green border-cyber-green animate-pulse'
                : 'bg-gray-800 text-gray-400 border-gray-700 hover:border-cyber-green'
            }`}
          >
            {isLiveMode ? <Radio size={20} /> : <Circle size={20} />}
            <span>{isLiveMode ? 'Live Mode ON' : 'Enable Live Mode'}</span>
          </button>
        </div>
      )}

      {/* Stats Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={<Activity className="w-6 h-6" />}
          title="Total Packets"
          value={stats.totalPackets.toLocaleString()}
          color="cyber-blue"
        />
        <StatCard
          icon={<TrendingUp className="w-6 h-6" />}
          title="Packets/sec"
          value={stats.packetsPerSecond.toFixed(2)}
          color="cyber-cyan"
        />
        <StatCard
          icon={<HardDrive className="w-6 h-6" />}
          title="Total Data"
          value={formatBytes(stats.totalBytes)}
          color="cyber-green"
        />
        <StatCard
          icon={<Database className="w-6 h-6" />}
          title="Alerts"
          value={alerts.length.toString()}
          color="cyber-red"
        />
      </div>

      {/* Protocol Distribution */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
          <h2 className="text-xl font-bold mb-4 text-cyber-blue">Protocol Distribution</h2>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={protocolData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {protocolData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Top Talkers */}
        <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
          <h2 className="text-xl font-bold mb-4 text-cyber-cyan">Top Talkers</h2>
          <div className="space-y-3">
            {stats.topTalkers.slice(0, 5).map((talker, idx) => (
              <div key={idx} className="flex justify-between items-center p-3 bg-bg-elevated rounded">
                <div>
                  <div className="text-cyber-cyan font-mono">{talker.ip}</div>
                  <div className="text-xs text-gray-500">{talker.country || 'Unknown'}</div>
                </div>
                <div className="text-right">
                  <div className="text-sm">{talker.packetCount} pkts</div>
                  <div className="text-xs text-gray-500">{formatBytes(talker.bytes)}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Packets */}
      <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
        <h2 className="text-xl font-bold mb-4 text-cyber-green">Recent Packets</h2>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-gray-700">
                <th className="text-left py-2">Time</th>
                <th className="text-left py-2">Protocol</th>
                <th className="text-left py-2">Source</th>
                <th className="text-left py-2">Destination</th>
                <th className="text-right py-2">Length</th>
              </tr>
            </thead>
            <tbody>
              {packets.slice(0, 10).map((packet) => (
                <tr key={packet.id} className="border-b border-gray-800 hover:bg-bg-elevated">
                  <td className="py-2 text-gray-400">{new Date(packet.timestamp).toLocaleTimeString()}</td>
                  <td className="py-2">
                    <span className="px-2 py-1 bg-cyber-blue/20 text-cyber-blue rounded text-xs">
                      {packet.protocol}
                    </span>
                  </td>
                  <td className="py-2 font-mono text-xs">{packet.sourceIp}:{packet.sourcePort}</td>
                  <td className="py-2 font-mono text-xs">{packet.destIp}:{packet.destPort}</td>
                  <td className="py-2 text-right text-gray-400">{packet.length} B</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, title, value, color }: any) {
  return (
    <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
      <div className="flex items-center justify-between mb-2">
        <div className={`text-${color}`}>{icon}</div>
      </div>
      <div className="text-2xl font-bold text-white">{value}</div>
      <div className="text-sm text-gray-400">{title}</div>
    </div>
  )
}

function formatBytes(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  if (bytes < 1024 * 1024 * 1024) return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
  return (bytes / (1024 * 1024 * 1024)).toFixed(1) + ' GB'
}
