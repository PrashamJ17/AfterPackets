import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import { BarChart3, Map, PlayCircle, Settings, Wifi, WifiOff } from 'lucide-react'
import Dashboard from './pages/Dashboard'
import MapView from './pages/MapView'
import ReplayPage from './pages/ReplayPage'
import RulesPage from './pages/RulesPage'
import { useStore } from './store/useStore'
import { useEffect } from 'react'

function App() {
  const { isConnected, connectToAndroid } = useStore()

  useEffect(() => {
    // Auto-connect to Android device on startup
    connectToAndroid('192.168.1.100:8080') // User can configure this
  }, [connectToAndroid])

  return (
    <Router>
      <div className="min-h-screen flex flex-col">
        {/* Header */}
        <header className="bg-bg-surface border-b border-gray-800 px-6 py-4">
          <div className="max-w-7xl mx-auto flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="text-3xl">🎯</div>
              <div>
                <h1 className="text-2xl font-bold text-cyber-blue">
                  AFTERPACKETS
                </h1>
                <p className="text-sm text-gray-400">Desktop Analysis Console</p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              {isConnected ? (
                <div className="flex items-center space-x-2 px-3 py-1 bg-cyber-green/20 text-cyber-green text-sm rounded-full">
                  <Wifi size={16} />
                  <span>Live</span>
                </div>
              ) : (
                <div className="flex items-center space-x-2 px-3 py-1 bg-red-500/20 text-red-400 text-sm rounded-full">
                  <WifiOff size={16} />
                  <span>Disconnected</span>
                </div>
              )}
              <div className="px-3 py-1 bg-gray-700 text-gray-300 text-sm rounded-full">
                v1.0.0
              </div>
            </div>
          </div>
        </header>

        {/* Navigation */}
        <nav className="bg-bg-surface border-b border-gray-800">
          <div className="max-w-7xl mx-auto px-6">
            <div className="flex space-x-1">
              <NavLink to="/" icon={<BarChart3 size={18} />} label="Dashboard" />
              <NavLink to="/map" icon={<Map size={18} />} label="Map" />
              <NavLink to="/replay" icon={<PlayCircle size={18} />} label="Replay" />
              <NavLink to="/rules" icon={<Settings size={18} />} label="Rules" />
            </div>
          </div>
        </nav>

        {/* Main Content */}
        <main className="flex-1 max-w-7xl w-full mx-auto p-6">
          <Routes>
            <Route path="/" element={<Dashboard />} />
            <Route path="/map" element={<MapView />} />
            <Route path="/replay" element={<ReplayPage />} />
            <Route path="/rules" element={<RulesPage />} />
          </Routes>
        </main>

        {/* Footer */}
        <footer className="bg-bg-surface border-t border-gray-800 py-4 text-center text-sm text-gray-500">
          AFTERPACKETS © 2024 | Professional Network Analysis Platform
        </footer>
      </div>
    </Router>
  )
}

function NavLink({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <Link
      to={to}
      className="flex items-center space-x-2 px-4 py-3 text-gray-400 hover:text-cyber-blue hover:bg-bg-elevated transition-colors border-b-2 border-transparent hover:border-cyber-blue"
    >
      {icon}
      <span>{label}</span>
    </Link>
  )
}

export default App
