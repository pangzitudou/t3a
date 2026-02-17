import { useCallback, useState } from 'react'
import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface UploadAreaProps {
  onFileSelect: (file: File) => void
  accept?: string
  className?: string
}

export default function UploadArea({ onFileSelect, accept = '.pdf,.txt,.md,.docx', className }: UploadAreaProps) {
  const [isDragging, setIsDragging] = useState(false)

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
  }, [])

  const handleDragIn = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.dataTransfer.items && e.dataTransfer.items.length > 0) {
      setIsDragging(true)
    }
  }, [])

  const handleDragOut = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsDragging(false)
  }, [])

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      e.stopPropagation()
      setIsDragging(false)

      if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
        const file = e.dataTransfer.files[0]
        onFileSelect(file)
      }
    },
    [onFileSelect]
  )

  const handleFileInput = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      if (e.target.files && e.target.files.length > 0) {
        const file = e.target.files[0]
        onFileSelect(file)
      }
    },
    [onFileSelect]
  )

  return (
    <motion.div
      onDragEnter={handleDragIn}
      onDragLeave={handleDragOut}
      onDragOver={handleDrag}
      onDrop={handleDrop}
      whileHover={{ scale: 1.01 }}
      className={cn(
        'border-3 border-dashed border-primary-500 rounded-2xl p-16 text-center cursor-pointer transition-all duration-300',
        'bg-bg-light hover:bg-primary-50',
        isDragging && 'border-secondary-500 bg-primary-50 scale-105',
        className
      )}
    >
      <input
        type="file"
        accept={accept}
        onChange={handleFileInput}
        className="hidden"
        id="file-upload"
      />
      <label htmlFor="file-upload" className="cursor-pointer block">
        <motion.div
          animate={isDragging ? { scale: 1.1, rotate: [0, -5, 5, -5, 0] } : { scale: 1 }}
          transition={{ duration: 0.3 }}
          className="text-6xl mb-4"
        >
          📄
        </motion.div>
        <h3 className="text-xl font-bold text-gray-800 mb-2">
          {isDragging ? 'Drop your file here' : 'Drag & Drop Upload'}
        </h3>
        <p className="text-gray-500">Supports PDF, TXT, MD, DOCX formats</p>
        <p className="text-sm text-primary-600 mt-2">Or click to browse</p>
      </label>
    </motion.div>
  )
}
