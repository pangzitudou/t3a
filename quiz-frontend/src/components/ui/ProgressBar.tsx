import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface ProgressBarProps {
  progress: number // 0-100
  className?: string
  showLabel?: boolean
  height?: 'sm' | 'md' | 'lg'
}

export default function ProgressBar({ progress, className, showLabel = false, height = 'md' }: ProgressBarProps) {
  const heightStyles = {
    sm: 'h-2',
    md: 'h-3',
    lg: 'h-4',
  }

  return (
    <div className={cn('w-full', className)}>
      <div className={cn('bg-gray-200 rounded-full overflow-hidden', heightStyles[height])}>
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${Math.min(100, Math.max(0, progress))}%` }}
          transition={{ duration: 0.5, ease: 'easeOut' }}
          className="h-full bg-gradient-to-r from-primary-500 to-secondary-500 rounded-full"
        />
      </div>
      {showLabel && (
        <div className="text-sm text-gray-600 mt-1 text-center font-medium">
          {Math.round(progress)}%
        </div>
      )}
    </div>
  )
}
