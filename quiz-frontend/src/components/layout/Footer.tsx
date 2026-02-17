import { motion } from 'framer-motion'

export default function Footer() {
  return (
    <motion.footer
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      className="bg-white/50 backdrop-blur-sm mt-auto"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
        <div className="flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="text-2xl">🎓</span>
            <span className="font-semibold text-gray-700">TestAgainAndAgain</span>
          </div>

          <div className="text-sm text-gray-600">
            © 2026 T3A - AI-Powered Quiz Platform
          </div>

          <div className="flex items-center gap-4 text-sm text-gray-600">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 bg-success rounded-full animate-pulse" />
              Spring AI
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 bg-primary-500 rounded-full" />
              React 19
            </span>
          </div>
        </div>
      </div>
    </motion.footer>
  )
}
