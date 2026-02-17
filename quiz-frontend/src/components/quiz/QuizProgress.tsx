import { motion } from 'framer-motion'
import { cn } from '@/services/utils'
import ProgressBar from '../ui/ProgressBar'

export interface QuizProgressProps {
  current: number
  total: number
  answered: number
  answeredIndexes?: number[]
  onQuestionClick?: (index: number) => void
  className?: string
}

export default function QuizProgress({
  current,
  total,
  answered,
  answeredIndexes = [],
  onQuestionClick,
  className,
}: QuizProgressProps) {
  const percentage = (current / total) * 100
  const answeredSet = new Set(answeredIndexes)

  return (
    <motion.div
      initial={{ opacity: 0, y: -10 }}
      animate={{ opacity: 1, y: 0 }}
      className={cn('bg-bg-light rounded-xl p-4 mb-6', className)}
    >
      <div className="flex items-center justify-between mb-3">
        <div className="text-sm text-gray-600">
          <span className="font-semibold text-primary-600">Question {current}</span> of {total}
        </div>
        <div className="text-sm text-gray-600">
          <span className="font-semibold text-success">{answered}</span> answered
        </div>
      </div>

      <ProgressBar progress={percentage} height="sm" />

      {/* Question indicators */}
      <div className="flex gap-2 mt-4 flex-wrap">
        {Array.from({ length: total }, (_, i) => (
          <motion.button
            key={i}
            type="button"
            onClick={() => onQuestionClick?.(i)}
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ delay: i * 0.02 }}
            className={cn(
              'w-8 h-8 rounded-full flex items-center justify-center text-xs font-semibold transition-colors',
              onQuestionClick ? 'cursor-pointer hover:ring-2 hover:ring-primary-300' : 'cursor-default',
              i + 1 === current
                ? 'bg-primary-500 text-white scale-110 shadow-lg'
                : answeredSet.has(i)
                ? 'bg-success text-white'
                : 'bg-gray-200 text-gray-600'
            )}
          >
            {i + 1}
          </motion.button>
        ))}
      </div>
    </motion.div>
  )
}
