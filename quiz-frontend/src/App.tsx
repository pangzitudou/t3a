import { Routes, Route, Navigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { Toaster } from 'react-hot-toast'
import Header from './components/layout/Header'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import DashboardPage from './pages/DashboardPage'
import BanksPage from './pages/BanksPage'
import GeneratePage from './pages/GeneratePage'
import QuizPage from './pages/QuizPage'
import ResultPage from './pages/ResultPage'
import DebugApiPage from './pages/DebugApi'
import { useAuthStore } from './stores/authStore'

// Protected Route Wrapper
function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { isAuthenticated } = useAuthStore()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" replace />
}

function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <Header />

      <motion.main
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.5 }}
        className="flex-1"
      >
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Protected Routes */}
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <DashboardPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/banks"
            element={
              <ProtectedRoute>
                <BanksPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/generate"
            element={
              <ProtectedRoute>
                <GeneratePage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/quiz/:sessionKey"
            element={
              <ProtectedRoute>
                <QuizPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/debug"
            element={
              <ProtectedRoute>
                <DebugApiPage />
              </ProtectedRoute>
            }
          />
          <Route
            path="/result/:sessionKey"
            element={
              <ProtectedRoute>
                <ResultPage />
              </ProtectedRoute>
            }
          />

          {/* Catch all - redirect to home */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </motion.main>

      <Toaster
        position="top-center"
        toastOptions={{
          duration: 3000,
          style: {
            background: 'white',
            color: '#1a1a2e',
            borderRadius: '12px',
            padding: '12px 20px',
            boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)',
          },
        }}
      />
    </div>
  )
}

export default App
