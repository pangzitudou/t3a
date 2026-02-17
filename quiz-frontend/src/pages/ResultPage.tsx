import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast, { Toaster } from 'react-hot-toast'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import ScoreDisplay from '../components/stats/ScoreDisplay'
import KnowledgeTag from '../components/stats/KnowledgeTag'
import StatCard from '../components/stats/StatCard'
import { quizApi, QuizResult } from '../services/quiz'
import { aiApi, KnowledgeAnalysis } from '../services/ai'

export default function ResultPage() {
  const { sessionKey } = useParams<{ sessionKey: string }>()
  const navigate = useNavigate()

  const [result, setResult] = useState<QuizResult | null>(null)
  const [analysis, setAnalysis] = useState<KnowledgeAnalysis | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadingAnalysis, setLoadingAnalysis] = useState(false)

  useEffect(() => {
    if (!sessionKey) {
      navigate('/banks')
      return
    }

    loadResult()
  }, [sessionKey, navigate])

  const loadResult = async () => {
    try {
      setLoading(true)
      const resultData = await quizApi.getResult(sessionKey!)
      setResult(resultData)

      // Load AI analysis
      loadAnalysis()
    } catch (error: any) {
      console.error('Failed to load result:', error)
      toast.error('Failed to load quiz results')
    } finally {
      setLoading(false)
    }
  }

  const loadAnalysis = async () => {
    try {
      setLoadingAnalysis(true)
      const res = await aiApi.getAnalysis(sessionKey!)
      setAnalysis(res as any)
    } catch (error: any) {
      console.error('Failed to load analysis:', error)
    } finally {
      setLoadingAnalysis(false)
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

  if (!result) {
    return null
  }

  const percentage = result.percentage
  const timeTaken = Math.floor(result.timeTaken / 60)
  const avgTimePerQuestion = Math.floor(result.timeTaken / result.total)

  return (
    <div className="min-h-screen pb-8">
      <Toaster position="top-center" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-5xl mx-auto px-4 py-8"
      >
        <h1 className="text-4xl font-bold text-white text-center mb-8">Quiz Results</h1>

        {/* Score Display */}
        <ScoreDisplay
          score={result.score}
          total={result.total}
          percentage={percentage}
          className="mb-8"
        />

        {/* Statistics Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatCard
            title="Total Questions"
            value={result.total}
            icon="📝"
            gradient="primary"
          />
          <StatCard
            title="Correct Answers"
            value={result.score}
            icon="✓"
            gradient="secondary"
          />
          <StatCard
            title="Time Taken"
            value={`${timeTaken}m`}
            icon="⏱️"
            gradient="accent"
          />
          <StatCard
            title="Avg. Time/Question"
            value={`${avgTimePerQuestion}s`}
            icon="📊"
            gradient="primary"
          />
        </div>

        {/* AI Analysis Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
        >
          {loadingAnalysis ? (
            <Card className="text-center py-12">
              <motion.div
                animate={{ rotate: 360 }}
                transition={{ duration: 1, repeat: Infinity, ease: 'linear' }}
                className="text-5xl inline-block mb-4"
              >
                🤖
              </motion.div>
              <h3 className="text-xl font-bold text-gray-800 mb-2">AI Analysis in Progress</h3>
              <p className="text-gray-600">Our AI is analyzing your answers...</p>
            </Card>
          ) : analysis ? (
            <>
              {/* Strengths */}
              {analysis.strengths.length > 0 && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    💪 Strengths
                  </h3>
                  <div className="flex flex-wrap gap-3">
                    {analysis.strengths.map((item, index) => (
                      <motion.div
                        key={index}
                        initial={{ scale: 0.8, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        transition={{ delay: index * 0.1 }}
                      >
                        <KnowledgeTag name={item.topic} type="strength" score={item.score} />
                      </motion.div>
                    ))}
                  </div>
                  {analysis.strengths[0]?.recommendations && (
                    <div className="mt-4 p-4 bg-green-50 border-l-4 border-success rounded-r-lg">
                      <p className="text-sm text-success">
                        <strong>Great job!</strong> You've demonstrated strong understanding in these areas.
                      </p>
                    </div>
                  )}
                </Card>
              )}

              {/* Weaknesses */}
              {analysis.weaknesses.length > 0 && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    ⚠️ Areas for Improvement
                  </h3>
                  <div className="flex flex-wrap gap-3">
                    {analysis.weaknesses.map((item, index) => (
                      <motion.div
                        key={index}
                        initial={{ scale: 0.8, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        transition={{ delay: index * 0.1 }}
                      >
                        <KnowledgeTag name={item.topic} type="weakness" score={item.score} />
                      </motion.div>
                    ))}
                  </div>
                  {analysis.weaknesses[0]?.recommendations && (
                    <div className="mt-4 p-4 bg-red-50 border-l-4 border-danger rounded-r-lg">
                      <p className="text-sm text-gray-700">
                        <strong>AI Recommendations:</strong>
                        <ul className="mt-2 space-y-1 list-disc list-inside">
                          {analysis.weaknesses[0].recommendations.map((rec, i) => (
                            <li key={i}>{rec}</li>
                          ))}
                        </ul>
                      </p>
                    </div>
                  )}
                </Card>
              )}

              {/* Overall Feedback */}
              {analysis.overallFeedback && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    🎯 Overall Feedback
                  </h3>
                  <p className="text-gray-700 leading-relaxed">{analysis.overallFeedback}</p>
                </Card>
              )}

              {/* Study Plan */}
              {analysis.studyPlan && analysis.studyPlan.length > 0 && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    📚 Personalized Study Plan
                  </h3>
                  <ul className="space-y-3">
                    {analysis.studyPlan.map((item, index) => (
                      <motion.li
                        key={index}
                        initial={{ x: -20, opacity: 0 }}
                        animate={{ x: 0, opacity: 1 }}
                        transition={{ delay: index * 0.1 }}
                        className="flex items-start gap-3 p-3 bg-primary-50 rounded-lg"
                      >
                        <span className="flex-shrink-0 w-6 h-6 bg-primary-500 text-white rounded-full flex items-center justify-center text-sm font-bold">
                          {index + 1}
                        </span>
                        <span className="text-gray-700">{item}</span>
                      </motion.li>
                    ))}
                  </ul>
                </Card>
              )}
            </>
          ) : (
            <Card className="mb-6">
              <div className="text-center py-8">
                <div className="text-5xl mb-4">🤖</div>
                <h3 className="text-xl font-bold text-gray-800 mb-2">AI Analysis Not Available</h3>
                <p className="text-gray-600 mb-4">
                  The AI analysis is still being processed. Check back later!
                </p>
                <Button onClick={loadAnalysis} variant="outline">
                  Refresh Analysis
                </Button>
              </div>
            </Card>
          )}
        </motion.div>

        {/* Action Buttons */}
        <div className="flex gap-4 justify-center mt-8">
          <Button onClick={() => navigate('/dashboard')} variant="outline" size="lg">
            📊 Dashboard
          </Button>
          <Button onClick={() => navigate('/banks')} size="lg">
            📝 More Quizzes
          </Button>
        </div>
      </motion.div>
    </div>
  )
}
