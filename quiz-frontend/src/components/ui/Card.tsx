import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export interface CardProps {
  children: React.ReactNode
  className?: string
  gradient?: boolean
  hover?: boolean
}

export default function Card({ children, className, gradient = false, hover = false }: CardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      whileHover={hover ? { y: -5 } : {}}
      className={cn(
        'rounded-2xl shadow-lg p-6',
        gradient ? 'bg-gradient-to-br from-primary-500 to-secondary-500 text-white' : 'bg-white',
        className
      )}
    >
      {children}
    </motion.div>
  )
}
