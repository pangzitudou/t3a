import { useEffect, useState } from 'react'
import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface QuizTimerProps {
  timeRemaining: number // seconds
  totalTime: number
  onTimeUp?: () => void
  className?: string
}

export default function QuizTimer({ timeRemaining, totalTime, onTimeUp, className }: QuizTimerProps) {
  const [remaining, setRemaining] = useState(timeRemaining)

  useEffect(() => {
    setRemaining(timeRemaining)
  }, [timeRemaining])

  useEffect(() => {
    if (remaining <= 0 && onTimeUp) {
      onTimeUp()
      return
    }

    const timer = setInterval(() => {
      setRemaining((prev) => Math.max(0, prev - 1))
    }, 1000)

    return () => clearInterval(timer)
  }, [remaining, onTimeUp])

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60)
    const secs = seconds % 60
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
  }

  const percentage = (remaining / totalTime) * 100
  const isWarning = remaining < 60
  const isCritical = remaining < 30

  return (
    <motion.div
      initial={{ scale: 0.9 }}
      animate={{ scale: 1 }}
      className={cn(
        'flex items-center gap-3 px-6 py-3 rounded-xl',
        isCritical ? 'bg-red-100' : isWarning ? 'bg-yellow-100' : 'bg-white',
        className
      )}
    >
      <motion.span
        animate={{ rotate: 360 }}
        transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
        className={cn('text-2xl', isCritical && 'animate-pulse')}
      >
        ⏱️
      </motion.span>
      <div className="text-right">
        <div
          className={cn(
            'text-3xl font-bold font-mono',
            isCritical ? 'text-red-600' : isWarning ? 'text-yellow-600' : 'text-gray-800'
          )}
        >
          {formatTime(remaining)}
        </div>
        <div className="text-xs text-gray-500">Time Remaining</div>
      </div>

      {/* Circular progress indicator */}
      <svg className="w-12 h-12 -rotate-90" viewBox="0 0 36 36">
        <path
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke="#e5e7eb"
          strokeWidth="3"
        />
        <motion.path
          d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
          fill="none"
          stroke={isCritical ? '#dc2626' : isWarning ? '#eab308' : '#667eea'}
          strokeWidth="3"
          strokeDasharray={`${percentage}, 100`}
          initial={{ strokeDasharray: '0, 100' }}
          animate={{ strokeDasharray: `${percentage}, 100` }}
          transition={{ duration: 0.5 }}
        />
      </svg>
    </motion.div>
  )
}
