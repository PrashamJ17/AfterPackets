import { create } from 'zustand'
import type { ExportData, Timeline, DetectionRule } from '../types'

interface StoreState {
  exportData: ExportData | null
  timeline: Timeline | null
  rules: DetectionRule[]
  currentView: 'packets' | 'timeline' | 'map'
  isConnected: boolean
  androidDevice: string | null
  ws: WebSocket | null
  
  setExportData: (data: ExportData) => void
  setTimeline: (timeline: Timeline) => void
  setRules: (rules: DetectionRule[]) => void
  setCurrentView: (view: 'packets' | 'timeline' | 'map') => void
  clearData: () => void
  connectToAndroid: (address: string) => void
  disconnect: () => void
  updateRealtimeData: (data: Partial<ExportData>) => void
}

export const useStore = create<StoreState>((set, get) => ({
  exportData: null,
  timeline: null,
  rules: [],
  currentView: 'packets',
  isConnected: false,
  androidDevice: null,
  ws: null,
  
  setExportData: (data) => set({ exportData: data }),
  setTimeline: (timeline) => set({ timeline }),
  setRules: (rules) => set({ rules }),
  setCurrentView: (view) => set({ currentView: view }),
  clearData: () => set({ exportData: null, timeline: null }),
  
  connectToAndroid: (address: string) => {
    const currentWs = get().ws
    if (currentWs) {
      currentWs.close()
    }
    
    try {
      const ws = new WebSocket(`ws://${address}/packets`)
      
      ws.onopen = () => {
        console.log('Connected to Android device')
        set({ isConnected: true, androidDevice: address, ws })
      }
      
      ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        get().updateRealtimeData(data)
      }
      
      ws.onerror = (error) => {
        console.error('WebSocket error:', error)
        set({ isConnected: false })
      }
      
      ws.onclose = () => {
        console.log('Disconnected from Android device')
        set({ isConnected: false, ws: null })
      }
    } catch (error) {
      console.error('Failed to connect:', error)
      set({ isConnected: false })
    }
  },
  
  disconnect: () => {
    const ws = get().ws
    if (ws) {
      ws.close()
    }
    set({ isConnected: false, androidDevice: null, ws: null })
  },
  
  updateRealtimeData: (data: Partial<ExportData>) => {
    const current = get().exportData
    if (current && data.packets) {
      // Merge new packets with existing ones
      set({
        exportData: {
          ...current,
          packets: [...data.packets, ...current.packets].slice(0, 1000), // Keep last 1000
          stats: data.stats || current.stats
        }
      })
    } else if (data.packets) {
      // Initialize with new data
      set({ exportData: data as ExportData })
    }
  }
}))
