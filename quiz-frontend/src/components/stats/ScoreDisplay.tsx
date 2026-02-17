import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface ScoreDisplayProps {
  score: number
  total: number
  percentage: number
  label?: string
  className?: string
}

export default function ScoreDisplay({ score, total, percentage, label = 'Your Score', className }: ScoreDisplayProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn(
        'bg-gradient-to-br from-primary-500 to-secondary-500 rounded-3xl p-12 text-center text-white shadow-2xl',
        className
      )}
    >
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ delay: 0.2 }}
        className="text-2xl font-semibold mb-4"
      >
        {label}
      </motion.div>

      <motion.div
        initial={{ scale: 0, rotate: -180 }}
        animate={{ scale: 1, rotate: 0 }}
        transition={{ type: 'spring', delay: 0.3, duration: 0.8 }}
        className="relative"
      >
        {/* Circular progress ring */}
        <svg className="w-48 h-48 mx-auto" viewBox="0 0 100 100">
          {/* Background circle */}
          <circle
            cx="50"
            cy="50"
            r="45"
            fill="none"
            stroke="rgba(255,255,255,0.2)"
            strokeWidth="8"
          />
          {/* Progress circle */}
          <motion.circle
            cx="50"
            cy="50"
            r="45"
            fill="none"
            stroke="white"
            strokeWidth="8"
            strokeLinecap="round"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: percentage / 100 }}
            transition={{ duration: 1, delay: 0.5 }}
            style={{
              transform: 'rotate(-90deg)',
              transformOrigin: '50% 50%',
            }}
          />
        </svg>

        {/* Score in center */}
        <div className="absolute inset-0 flex items-center justify-center">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.8 }}
            className="text-center"
          >
            <div className="text-7xl font-bold">{percentage}</div>
            <div className="text-2xl opacity-90">
              {score} / {total}
            </div>
          </motion.div>
        </div>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 1 }}
        className="mt-6 text-lg opacity-90"
      >
        {percentage >= 80 ? '🎉 Excellent!' : percentage >= 60 ? '👍 Good job!' : '💪 Keep practicing!'}
      </motion.div>
    </motion.div>
  )
}
