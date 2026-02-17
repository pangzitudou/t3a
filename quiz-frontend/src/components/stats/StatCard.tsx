import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export type StatGradient = 'primary' | 'secondary' | 'accent'

export interface StatCardProps {
  title: string
  value: string | number
  icon?: React.ReactNode
  gradient?: StatGradient
  className?: string
}

export default function StatCard({ title, value, icon, gradient = 'primary', className }: StatCardProps) {
  const gradientStyles = {
    primary: 'bg-gradient-to-br from-primary-500 to-secondary-500',
    secondary: 'bg-gradient-to-br from-pink-400 to-rose-400',
    accent: 'bg-gradient-to-br from-accent to-accent-light',
  }

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      whileHover={{ y: -5, scale: 1.02 }}
      className={cn(
        'rounded-2xl p-6 text-white text-center shadow-lg',
        gradientStyles[gradient],
        className
      )}
    >
      {icon && (
        <motion.div
          initial={{ y: -10, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.1 }}
          className="text-4xl mb-2"
        >
          {icon}
        </motion.div>
      )}
      <motion.div
        initial={{ scale: 0 }}
        animate={{ scale: 1 }}
        transition={{ type: 'spring', delay: 0.2 }}
        className="text-4xl font-bold mb-1"
      >
        {value}
      </motion.div>
      <div className="text-sm opacity-90 font-medium">{title}</div>
    </motion.div>
  )
}
