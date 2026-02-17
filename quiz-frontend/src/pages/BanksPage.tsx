import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast, { Toaster } from 'react-hot-toast'
import Card from '../components/ui/Card'
import Button from '../components/ui/Button'
import Tag, { TagVariant } from '../components/ui/Tag'
import { quizApi, QuestionBank } from '../services/quiz'
import { useAuthStore } from '../stores/authStore'

export default function BanksPage() {
  const navigate = useNavigate()
  const { isAuthenticated, user } = useAuthStore()
  const [banks, setBanks] = useState<QuestionBank[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!isAuthenticated) {
      navigate('/login')
      return
    }

    loadBanks()
  }, [isAuthenticated, navigate])

  const loadBanks = async () => {
    try {
      setLoading(true)
      const result = await quizApi.getBanks()
      const banksList = (result as any)?.records || result
      setBanks(Array.isArray(banksList) ? banksList : [])
    } catch (error: any) {
      console.error('Failed to load banks:', error)
      toast.error('Failed to load question banks')
    } finally {
      setLoading(false)
    }
  }

  const handleStartQuiz = async (bankId: string) => {
    try {
      const userId = user?.id || localStorage.getItem('userId')
      if (!userId) {
        toast.error('请重新登录')
        navigate('/login')
        return
      }
      const result = await quizApi.startSession(bankId, userId, 10)
      const session = (result as any)?.data || result
      navigate(`/quiz/${session.sessionKey}`)
    } catch (error: any) {
      console.error('Failed to start quiz:', error)
      toast.error('Failed to start quiz')
    }
  }

  const handleDeleteBank = async (bankId: string, bankName: string) => {
    if (!confirm(`Are you sure you want to delete "${bankName}"?`)) {
      return
    }

    try {
      await quizApi.deleteBank(bankId)
      toast.success('Question bank deleted')
      loadBanks()
    } catch (error: any) {
      console.error('Failed to delete bank:', error)
      toast.error('Failed to delete question bank')
    }
  }

  const getDifficultyColor = (difficulty: string): TagVariant => {
    switch (difficulty) {
      case 'easy':
        return 'default'
      case 'medium':
        return 'default'
      case 'hard':
        return 'weakness'
      default:
        return 'default'
    }
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

  return (
    <div className="min-h-screen pb-8">
      <Toaster position="top-center" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-7xl mx-auto px-4 py-8"
      >
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <h1 className="text-4xl font-bold text-white mb-2">Question Banks</h1>
            <p className="text-white/80">Manage and start quizzes from your question banks</p>
          </div>
          <Button onClick={() => navigate('/generate')} size="lg">
            ➕ Create New Bank
          </Button>
        </div>

        {/* Banks Grid */}
        {banks.length === 0 ? (
          <Card className="text-center py-16">
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: 1 }}
              transition={{ type: 'spring' }}
              className="text-6xl mb-4"
            >
              📚
            </motion.div>
            <h3 className="text-2xl font-bold text-gray-800 mb-2">No Question Banks Yet</h3>
            <p className="text-gray-600 mb-6">Create your first AI-generated question bank to get started!</p>
            <Button onClick={() => navigate('/generate')} size="lg">
              Generate Questions
            </Button>
          </Card>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {banks.map((bank, index) => (
              <motion.div
                key={bank.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
              >
                <Card hover className="h-full flex flex-col">
                  <div className="flex-1">
                    <h3 className="text-xl font-bold text-gray-800 mb-3">{bank.name}</h3>
                    <div className="flex items-center gap-2 text-sm text-gray-600 mb-4">
                      <span>📝</span>
                      <span>{bank.questionCount} questions</span>
                    </div>

                    {/* Tags */}
                    <div className="flex flex-wrap gap-2 mb-4">
                      <Tag variant={getDifficultyColor(bank.difficulty)}>
                        {bank.difficulty}
                      </Tag>
                      {(bank.types || []).map((type) => (
                        <Tag key={type} variant="default">
                          {type}
                        </Tag>
                      ))}
                    </div>

                    {/* Created Date */}
                    <div className="text-xs text-gray-500">
                      Created {new Date(bank.createdAt).toLocaleDateString()}
                    </div>
                  </div>

                  {/* Actions */}
                  <div className="flex gap-2 mt-6">
                    <Button
                      onClick={() => handleStartQuiz(bank.id)}
                      variant="primary"
                      className="flex-1"
                    >
                      Start Quiz
                    </Button>
                    <button
                      onClick={() => handleDeleteBank(bank.id, bank.name)}
                      className="px-4 py-3 rounded-full border-2 border-danger text-danger hover:bg-red-50 transition-colors"
                    >
                      🗑️
                    </button>
                  </div>
                </Card>
              </motion.div>
            ))}
          </div>
        )}
      </motion.div>
    </div>
  )
}
