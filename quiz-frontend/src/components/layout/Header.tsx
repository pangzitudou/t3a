import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { cn } from '@/services/utils'
import { useAuthStore } from '@/stores/authStore'

export default function Header() {
  const [showUserMenu, setShowUserMenu] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()

  const { isAuthenticated, user, logout } = useAuthStore()

  const handleLogout = () => {
    logout()
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setShowUserMenu(false)
    navigate('/')
  }

  const navLinks = isAuthenticated
    ? [
        { name: 'Dashboard', path: '/dashboard' },
        { name: 'Question Banks', path: '/banks' },
        { name: 'Generate', path: '/generate' },
      ]
    : [
        { name: 'Home', path: '/' },
        { name: 'Login', path: '/login' },
        { name: 'Register', path: '/register' },
      ]

  return (
    <motion.header
      initial={{ y: -100 }}
      animate={{ y: 0 }}
      className="sticky top-0 z-50 bg-white/80 backdrop-blur-md shadow-md"
    >
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2">
            <motion.div
              whileHover={{ rotate: 360 }}
              transition={{ duration: 0.6 }}
              className="text-3xl"
            >
              🎓
            </motion.div>
            <span className="text-2xl font-bold bg-gradient-to-r from-primary-500 to-secondary-500 bg-clip-text text-transparent">
              T3A
            </span>
          </Link>

          {/* Navigation */}
          <nav className="hidden md:flex items-center gap-1">
            {navLinks.map((link) => (
              <Link
                key={link.path}
                to={link.path}
                className={cn(
                  'px-4 py-2 rounded-full font-medium transition-all duration-300',
                  location.pathname === link.path
                    ? 'bg-gradient-to-r from-primary-500 to-secondary-500 text-white shadow-lg'
                    : 'text-gray-600 hover:text-primary-600 hover:bg-primary-50'
                )}
              >
                {link.name}
              </Link>
            ))}
          </nav>

          {/* User Menu */}
          {isAuthenticated && user && (
            <div className="relative">
              <motion.button
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                onClick={() => setShowUserMenu(!showUserMenu)}
                className="flex items-center gap-3 px-4 py-2 rounded-full bg-gradient-to-r from-primary-500 to-secondary-500 text-white shadow-lg"
              >
                <div className="w-8 h-8 rounded-full bg-white/20 flex items-center justify-center font-bold">
                  {user.nickname?.[0]?.toUpperCase() || 'U'}
                </div>
                <span className="hidden sm:inline font-medium">{user.nickname}</span>
              </motion.button>

              <AnimatePresence>
                {showUserMenu && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.95, y: -10 }}
                    animate={{ opacity: 1, scale: 1, y: 0 }}
                    exit={{ opacity: 0, scale: 0.95, y: -10 }}
                    className="absolute right-0 mt-2 w-48 bg-white rounded-xl shadow-xl py-2 border"
                  >
                    <Link
                      to="/dashboard"
                      className="block px-4 py-2 text-gray-700 hover:bg-primary-50 hover:text-primary-600"
                      onClick={() => setShowUserMenu(false)}
                    >
                      Dashboard
                    </Link>
                    <Link
                      to="/banks"
                      className="block px-4 py-2 text-gray-700 hover:bg-primary-50 hover:text-primary-600"
                      onClick={() => setShowUserMenu(false)}
                    >
                      My Banks
                    </Link>
                    <hr className="my-2" />
                    <button
                      onClick={handleLogout}
                      className="w-full text-left px-4 py-2 text-danger hover:bg-red-50"
                    >
                      Logout
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          )}
        </div>
      </div>
    </motion.header>
  )
}
