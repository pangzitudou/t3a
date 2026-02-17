import Editor from '@monaco-editor/react'
import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface CodeEditorProps {
  language: string
  value: string
  onChange: (value: string) => void
  readOnly?: boolean
  height?: string
  className?: string
}

export default function CodeEditor({
  language = 'java',
  value = '',
  onChange,
  readOnly = false,
  height = '400px',
  className,
}: CodeEditorProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn('rounded-xl overflow-hidden shadow-lg border-2 border-gray-200', className)}
    >
      <div className="bg-gray-800 px-4 py-2 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-3 h-3 rounded-full bg-red-500" />
          <div className="w-3 h-3 rounded-full bg-yellow-500" />
          <div className="w-3 h-3 rounded-full bg-green-500" />
          <span className="ml-4 text-gray-300 text-sm font-medium">
            {language.toUpperCase()}
          </span>
        </div>
        {readOnly && (
          <span className="text-gray-400 text-xs">Read-only mode</span>
        )}
      </div>

      <Editor
        height={height}
        language={language}
        value={value}
        onChange={(value) => onChange(value || '')}
        theme="vs-dark"
        options={{
          readOnly,
          minimap: { enabled: false },
          fontSize: 14,
          lineNumbers: 'on',
          scrollBeyondLastLine: false,
          automaticLayout: true,
          tabSize: 2,
          wordWrap: 'on',
          formatOnPaste: true,
          formatOnType: true,
        }}
      />
    </motion.div>
  )
}
