import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast, { Toaster } from 'react-hot-toast'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import StatCard from '../components/stats/StatCard'
import { quizApi, DashboardStats } from '../services/quiz'
import { useAuthStore } from '../stores/authStore'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { user, logout } = useAuthStore()

  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) {
      navigate('/login')
      return
    }

    loadDashboard()
  }, [user, navigate])

  const loadDashboard = async () => {
    try {
      setLoading(true)
      const data = await quizApi.getDashboard()
      setStats(data)
    } catch (error: any) {
      console.error('Failed to load dashboard:', error)
      toast.error('Failed to load dashboard data')
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
          className="text-6xl"
        >
          ⚡
        </motion.div>
      </div>
    )
  }

  const quizzesCompleted = stats?.quizzesCompleted || 0
  const averageScore = stats?.averageScore || 0
  const studyTime = stats?.studyTime || 0
  const currentStreak = stats?.currentStreak || 0
  const recentQuizzes = stats?.recentQuizzes || []

  return (
    <div className="min-h-screen pb-8">
      <Toaster position="top-center" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-7xl mx-auto px-4 py-8"
      >
        {/* Welcome Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-4xl font-bold text-white mb-2">
              Welcome back, {user?.nickname || 'User'}! 👋
            </h1>
            <p className="text-white/80">Ready to continue your learning journey?</p>
          </div>
          <Button onClick={handleLogout} variant="outline">
            Log Out
          </Button>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.1 }}
          >
            <StatCard
              title="Quizzes Completed"
              value={quizzesCompleted}
              icon="📝"
              gradient="primary"
            />
          </motion.div>
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 }}
          >
            <StatCard
              title="Average Score"
              value={averageScore}
              icon="📊"
              gradient="secondary"
            />
          </motion.div>
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.3 }}
          >
            <StatCard
              title="Study Time"
              value={`${studyTime}h`}
              icon="⏱️"
              gradient="accent"
            />
          </motion.div>
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.4 }}
          >
            <StatCard
              title="Current Streak"
              value={currentStreak}
              icon="🔥"
              gradient="primary"
            />
          </motion.div>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.5 }}
          >
            <Card hover className="h-full">
              <div className="text-center py-8">
                <div className="text-5xl mb-4">🚀</div>
                <h3 className="text-2xl font-bold text-gray-800 mb-2">Start a New Quiz</h3>
                <p className="text-gray-600 mb-6">Test your knowledge with AI-generated questions</p>
                <Button onClick={() => navigate('/banks')} size="lg">
                  Browse Question Banks
                </Button>
              </div>
            </Card>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.6 }}
          >
            <Card hover className="h-full">
              <div className="text-center py-8">
                <div className="text-5xl mb-4">🤖</div>
                <h3 className="text-2xl font-bold text-gray-800 mb-2">Generate Questions</h3>
                <p className="text-gray-600 mb-6">Upload your materials and let AI create quizzes</p>
                <Button onClick={() => navigate('/generate')} variant="secondary" size="lg">
                  Create Question Bank
                </Button>
              </div>
            </Card>
          </motion.div>
        </div>

        {/* Recent Quizzes */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.7 }}
        >
          <Card>
            <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
              📚 Recent Quizzes
            </h3>

            {recentQuizzes.length === 0 ? (
              <div className="text-center py-12 text-gray-500">
                <div className="text-4xl mb-2">📭</div>
                <p>No quizzes completed yet. Start your first quiz!</p>
              </div>
            ) : (
              <div className="space-y-3">
                {recentQuizzes.map((quiz, index) => (
                  <motion.div
                    key={quiz.sessionKey}
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.8 + index * 0.1 }}
                    className="flex items-center justify-between p-4 bg-bg-light rounded-xl hover:bg-primary-50 transition-colors cursor-pointer"
                    onClick={() => navigate(`/result/${quiz.sessionKey}`)}
                  >
                    <div className="flex items-center gap-4">
                      <div
                        className={`w-12 h-12 rounded-xl flex items-center justify-center font-bold text-white ${
                          quiz.percentage >= 80
                            ? 'bg-gradient-to-br from-success to-green-600'
                            : quiz.percentage >= 60
                            ? 'bg-gradient-to-br from-primary-500 to-secondary-500'
                            : 'bg-gradient-to-br from-danger to-red-600'
                        }`}
                      >
                        {quiz.percentage}%
                      </div>
                      <div>
                        <div className="font-semibold text-gray-800">
                          Quiz - {new Date(quiz.completedAt).toLocaleDateString()}
                        </div>
                        <div className="text-sm text-gray-500">
                          {quiz.score} / {quiz.total} points • {Math.floor(quiz.timeTaken / 60)} min
                        </div>
                      </div>
                    </div>
                    <div className="text-primary-600">→</div>
                  </motion.div>
                ))}
              </div>
            )}
          </Card>
        </motion.div>

        {/* Knowledge Graph Placeholder */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 1 }}
          className="mt-8"
        >
          <Card>
            <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
              📈 Knowledge Progress
            </h3>
            <div className="h-64 bg-bg-light rounded-xl flex items-center justify-center text-gray-500">
              <div className="text-center">
                <div className="text-5xl mb-3">📊</div>
                <p>Knowledge graph coming soon!</p>
                <p className="text-sm mt-2">Complete more quizzes to see your progress</p>
              </div>
            </div>
          </Card>
        </motion.div>
      </motion.div>
    </div>
  )
}
