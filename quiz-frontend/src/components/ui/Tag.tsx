import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export type TagVariant = 'default' | 'weakness' | 'strength'

export interface TagProps {
  children: React.ReactNode
  variant?: TagVariant
  className?: string
  score?: number
}

export default function Tag({ children, variant = 'default', className, score }: TagProps) {
  const variantStyles = {
    default: 'bg-white border-2 border-primary-500 text-primary-600',
    weakness: 'bg-red-50 border-2 border-danger text-danger',
    strength: 'bg-green-50 border-2 border-success text-success',
  }

  return (
    <motion.span
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      className={cn(
        'inline-block px-4 py-2 rounded-full text-sm font-medium border',
        variantStyles[variant],
        className
      )}
    >
      {children}
      {score !== undefined && (
        <span className="ml-2 px-2 py-0.5 bg-white/50 rounded-full text-xs">
          {score}%
        </span>
      )}
    </motion.span>
  )
}
