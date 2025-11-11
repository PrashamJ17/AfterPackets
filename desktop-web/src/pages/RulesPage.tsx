import { Plus, Trash2, Power } from 'lucide-react'
import { useStore } from '../store/useStore'

export default function RulesPage() {
  const { rules } = useStore()

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-cyber-blue mb-2">Detection Rules</h1>
          <p className="text-gray-400">Manage custom detection rules</p>
        </div>
        <button className="flex items-center space-x-2 px-4 py-2 bg-cyber-blue rounded-lg">
          <Plus size={20} />
          <span>Add Rule</span>
        </button>
      </div>

      <div className="space-y-4">
        {rules.map((rule) => (
          <div key={rule.id} className="bg-bg-surface border border-gray-800 rounded-lg p-6">
            <div className="flex justify-between">
              <div>
                <h3 className="text-lg font-bold mb-2">{rule.name}</h3>
                <div className="text-cyber-cyan font-mono text-sm">
                  IF {rule.metric} {rule.condition} {rule.threshold} THEN {rule.action}
                </div>
              </div>
              <div className="flex space-x-2">
                <button className="p-2"><Power size={20} className="text-cyber-green" /></button>
                <button className="p-2"><Trash2 size={20} className="text-red-400" /></button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
