import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface LiveIndicatorProps {
  className?: string
  label?: string
}

export default function LiveIndicator({ className, label = 'LIVE' }: LiveIndicatorProps) {
  return (
    <motion.div
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      className={cn(
        'inline-flex items-center gap-2 bg-success text-white px-4 py-1.5 rounded-full text-xs font-semibold',
        className
      )}
    >
      <motion.span
        animate={{ opacity: [1, 0.3, 1] }}
        transition={{ duration: 2, repeat: Infinity }}
        className="w-2 h-2 bg-white rounded-full"
      />
      {label}
    </motion.div>
  )
}
