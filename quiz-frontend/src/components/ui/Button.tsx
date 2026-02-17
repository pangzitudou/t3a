import { motion, HTMLMotionProps } from 'framer-motion'
import { cn } from '@/services/utils'

export interface ButtonProps extends HTMLMotionProps<'button'> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
}

export default function Button({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  className,
  disabled,
  ...props
}: ButtonProps) {
  const baseStyles = 'inline-flex items-center justify-center rounded-full font-semibold transition-all duration-300'

  const variantStyles = {
    primary: 'bg-gradient-to-r from-primary-500 to-secondary-500 text-white shadow-lg hover:shadow-xl hover:-translate-y-0.5',
    secondary: 'bg-gradient-to-r from-pink-400 to-rose-400 text-white shadow-lg hover:shadow-xl hover:-translate-y-0.5',
    outline: 'bg-white text-primary-600 border-2 border-primary-500 hover:bg-primary-50',
    ghost: 'bg-transparent text-primary-600 hover:bg-primary-50',
  }

  const sizeStyles = {
    sm: 'px-4 py-2 text-sm',
    md: 'px-6 py-3 text-base',
    lg: 'px-10 py-4 text-lg',
  }

  return (
    <motion.button
      whileHover={{ scale: disabled || isLoading ? 1 : 1.02 }}
      whileTap={{ scale: disabled || isLoading ? 1 : 0.98 }}
      className={cn(
        baseStyles,
        variantStyles[variant],
        sizeStyles[size],
        (disabled || isLoading) && 'opacity-50 cursor-not-allowed',
        className
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading ? (
        <>
          <motion.span
            animate={{ rotate: 360 }}
            transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
            className="mr-2"
          >
            ⚡
          </motion.span>
          Loading...
        </>
      ) : (
        children
      )}
    </motion.button>
  )
}
