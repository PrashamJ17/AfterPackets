import { useState } from 'react'
import { Upload, FileJson, Archive, Check, FlaskConical } from 'lucide-react'
import { useStore } from '../store/useStore'
import JSZip from 'jszip'
import type { ExportData, Timeline } from '../types'
import { sampleExportData, sampleTimeline } from '../data/sampleData'

export default function ImportPage() {
  const [isDragging, setIsDragging] = useState(false)
  const [importStatus, setImportStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle')
  const [message, setMessage] = useState('')
  const { setExportData, setTimeline } = useStore()

  const handleFile = async (file: File) => {
    setImportStatus('loading')
    setMessage('Processing file...')

    try {
      if (file.name.endsWith('.json')) {
        const text = await file.text()
        const data = JSON.parse(text) as ExportData
        setExportData(data)
        setImportStatus('success')
        setMessage(`Successfully imported ${data.packets.length} packets`)
      } else if (file.name.endsWith('.zip')) {
        const zip = new JSZip()
        const contents = await zip.loadAsync(file)
        
        const metadataFile = contents.file('metadata.json')
        const timelineFile = contents.file('timeline.json')
        
        if (metadataFile) {
          const metadataText = await metadataFile.async('text')
          const data = JSON.parse(metadataText) as ExportData
          setExportData(data)
        }
        
        if (timelineFile) {
          const timelineText = await timelineFile.async('text')
          const timeline = JSON.parse(timelineText) as Timeline
          setTimeline(timeline)
        }
        
        setImportStatus('success')
        setMessage('Evidence bundle imported successfully')
      } else {
        throw new Error('Unsupported file format')
      }
    } catch (error) {
      setImportStatus('error')
      setMessage(`Import failed: ${error instanceof Error ? error.message : 'Unknown error'}`)
    }
  }

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    const file = e.dataTransfer.files[0]
    if (file) handleFile(file)
  }

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) handleFile(file)
  }

  const loadSampleData = () => {
    setImportStatus('loading')
    setMessage('Loading sample data...')
    
    setTimeout(() => {
      setExportData(sampleExportData)
      setTimeline(sampleTimeline)
      setImportStatus('success')
      setMessage(`Successfully loaded sample data with ${sampleExportData.packets.length} packets`)
    }, 800)
  }

  return (
    <div className="max-w-4xl mx-auto">
      <div className="mb-8">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-cyber-blue mb-2">Import Capture Data</h1>
            <p className="text-gray-400">Import evidence bundles or JSON exports from the Android app</p>
          </div>
          <button
            onClick={loadSampleData}
            className="flex items-center space-x-2 px-4 py-2 bg-cyber-green/20 text-cyber-green border border-cyber-green rounded-lg hover:bg-cyber-green/30 transition-colors"
          >
            <FlaskConical size={20} />
            <span>Load Sample Data</span>
          </button>
        </div>
      </div>

      {/* Drop Zone */}
      <div
        onDrop={handleDrop}
        onDragOver={(e) => { e.preventDefault(); setIsDragging(true) }}
        onDragLeave={() => setIsDragging(false)}
        className={`border-2 border-dashed rounded-lg p-12 text-center transition-colors ${
          isDragging ? 'border-cyber-blue bg-cyber-blue/10' : 'border-gray-700'
        }`}
      >
        <input
          type="file"
          id="file-upload"
          accept=".json,.zip"
          onChange={handleFileSelect}
          className="hidden"
        />
        <label htmlFor="file-upload" className="cursor-pointer">
          <Upload className="w-16 h-16 mx-auto mb-4 text-gray-500" />
          <p className="text-lg mb-2">Drop your file here or click to browse</p>
          <p className="text-sm text-gray-500">Supports .json and .zip evidence bundles</p>
        </label>
      </div>

      {/* Status */}
      {importStatus !== 'idle' && (
        <div className={`mt-6 p-4 rounded-lg border ${
          importStatus === 'success' ? 'bg-cyber-green/10 border-cyber-green' :
          importStatus === 'error' ? 'bg-cyber-red/10 border-cyber-red' :
          'bg-cyber-blue/10 border-cyber-blue'
        }`}>
          <div className="flex items-center">
            {importStatus === 'loading' && <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-cyber-blue mr-3" />}
            {importStatus === 'success' && <Check className="w-5 h-5 text-cyber-green mr-3" />}
            <span>{message}</span>
          </div>
        </div>
      )}

      {/* Format Info */}
      <div className="mt-12 grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
          <FileJson className="w-8 h-8 text-cyber-cyan mb-3" />
          <h3 className="text-lg font-bold mb-2">JSON Export</h3>
          <p className="text-sm text-gray-400">
            Contains packet metadata, statistics, and alerts. Ideal for quick analysis and visualization.
          </p>
        </div>
        <div className="bg-bg-surface border border-gray-800 rounded-lg p-6">
          <Archive className="w-8 h-8 text-cyber-blue mb-3" />
          <h3 className="text-lg font-bold mb-2">Evidence Bundle</h3>
          <p className="text-sm text-gray-400">
            Complete forensic package with PCAP, JSON, timeline data, and alert details. Full-featured analysis.
          </p>
        </div>
      </div>
    </div>
  )
}
