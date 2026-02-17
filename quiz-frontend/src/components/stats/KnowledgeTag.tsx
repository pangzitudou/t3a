import { motion } from 'framer-motion'
import { cn } from '@/services/utils'
import { TagVariant } from '../ui/Tag'

export interface KnowledgeTagProps {
  name: string
  type: TagVariant
  score?: number
  className?: string
}

export default function KnowledgeTag({ name, type, score, className }: KnowledgeTagProps) {
  const variantStyles = {
    default: 'bg-white border-2 border-primary-500 text-primary-600',
    weakness: 'bg-red-50 border-2 border-danger text-danger',
    strength: 'bg-green-50 border-2 border-success text-success',
  }

  const icons = {
    default: '📚',
    weakness: '⚠️',
    strength: '💪',
  }

  return (
    <motion.span
      initial={{ scale: 0.8, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      whileHover={{ scale: 1.05 }}
      className={cn(
        'inline-flex items-center gap-2 px-4 py-2 rounded-full text-sm font-semibold border shadow-sm',
        variantStyles[type],
        className
      )}
    >
      <span>{icons[type]}</span>
      <span>{name}</span>
      {score !== undefined && (
        <span className="ml-1 px-2 py-0.5 bg-white/50 rounded-full text-xs font-bold">
          {score}%
        </span>
      )}
    </motion.span>
  )
}
