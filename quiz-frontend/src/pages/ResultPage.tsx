import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
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

  const formatAnswer = (value: any, options?: string[]) => {
    if (value === undefined || value === null || value === '') return '未作答'
    const toLabel = (v: any) => {
      if (typeof v === 'number') {
        const optionText = options?.[v]
        return optionText ? `${String.fromCharCode(65 + v)}. ${optionText}` : String.fromCharCode(65 + v)
      }
      if (typeof v === 'string') {
        const normalized = v.trim()
        if (/^\d+$/.test(normalized)) {
          const idx = Number(normalized)
          const optionText = options?.[idx]
          return optionText ? `${String.fromCharCode(65 + idx)}. ${optionText}` : String.fromCharCode(65 + idx)
        }
      }
      return String(v)
    }
    if (Array.isArray(value)) {
      return value.map(toLabel).join(', ')
    }
    if (typeof value === 'string' && value.includes(',')) {
      return value.split(',').map((v) => toLabel(Number.isNaN(Number(v)) ? v : Number(v))).join(', ')
    }
    return toLabel(value)
  }

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
      const localRaw = sessionStorage.getItem(`quiz_result_local_${sessionKey}`)
      const localResult = localRaw ? JSON.parse(localRaw) : null
      if (localResult) {
        const shouldUseLocal =
          !resultData?.answers ||
          resultData.answers.length === 0 ||
          ((resultData?.score ?? 0) === 0 && (localResult.score ?? 0) > 0)
        setResult({
          ...resultData,
          score: shouldUseLocal ? localResult.score : resultData?.score,
          total: shouldUseLocal ? localResult.total : (resultData?.total || localResult.total),
          percentage: shouldUseLocal ? localResult.percentage : (resultData?.percentage || localResult.percentage),
          answers: shouldUseLocal ? localResult.answers : resultData.answers,
        })
      } else {
        setResult(resultData)
      }

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

  const safeAnalysis = {
    strengths: analysis?.strengths || [],
    weaknesses: analysis?.weaknesses || [],
    overallFeedback: analysis?.overallFeedback || ((result?.percentage ?? 0) >= 80
      ? '整体表现优秀，建议继续保持并挑战更高难度题目。'
      : (result?.percentage ?? 0) >= 60
      ? '基础掌握尚可，建议重点复习错题与薄弱知识点。'
      : '当前正确率较低，建议先阅读参考答案和解析后再练习。'),
    studyPlan: analysis?.studyPlan || [],
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
    return (
      <div className="min-h-screen pb-8">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="max-w-3xl mx-auto px-4 py-12"
        >
          <Card className="text-center py-12">
            <div className="text-5xl mb-4">⚠️</div>
            <h2 className="text-2xl font-bold text-gray-800 mb-2">结果加载失败</h2>
            <p className="text-gray-600 mb-6">本次测验结果暂时不可用，请稍后重试。</p>
            <div className="flex justify-center gap-3">
              <Button onClick={loadResult} variant="outline">重试</Button>
              <Button onClick={() => navigate('/banks')}>返回题库</Button>
            </div>
          </Card>
        </motion.div>
      </div>
    )
  }

  const percentage = result.percentage
  const timeTaken = Math.floor(result.timeTaken / 60)
  const totalQuestions = result.totalQuestions ?? result.answers?.length ?? 0
  const correctAnswers = result.correctAnswers ?? (result.answers?.filter((item) => item.isCorrect).length ?? 0)
  const avgTimePerQuestion = totalQuestions > 0 ? Math.floor(result.timeTaken / totalQuestions) : 0

  return (
    <div className="min-h-screen pb-8">
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
            value={totalQuestions}
            icon="📝"
            gradient="primary"
          />
          <StatCard
            title="Correct Answers"
            value={correctAnswers}
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
              {safeAnalysis.strengths.length > 0 && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    💪 Strengths
                  </h3>
                  <div className="flex flex-wrap gap-3">
                    {safeAnalysis.strengths.map((item, index) => (
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
                  {safeAnalysis.strengths[0]?.recommendations && (
                    <div className="mt-4 p-4 bg-green-50 border-l-4 border-success rounded-r-lg">
                      <p className="text-sm text-success">
                        <strong>Great job!</strong> You've demonstrated strong understanding in these areas.
                      </p>
                    </div>
                  )}
                </Card>
              )}

              {/* Weaknesses */}
              {safeAnalysis.weaknesses.length > 0 && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    ⚠️ Areas for Improvement
                  </h3>
                  <div className="flex flex-wrap gap-3">
                    {safeAnalysis.weaknesses.map((item, index) => (
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
                  {safeAnalysis.weaknesses[0]?.recommendations && (
                    <div className="mt-4 p-4 bg-red-50 border-l-4 border-danger rounded-r-lg">
                      <p className="text-sm text-gray-700">
                        <strong>AI Recommendations:</strong>
                        <ul className="mt-2 space-y-1 list-disc list-inside">
                          {safeAnalysis.weaknesses[0].recommendations.map((rec, i) => (
                            <li key={i}>{rec}</li>
                          ))}
                        </ul>
                      </p>
                    </div>
                  )}
                </Card>
              )}

              {/* Overall Feedback */}
              {safeAnalysis.overallFeedback && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    🎯 Overall Feedback
                  </h3>
                  <p className="text-gray-700 leading-relaxed">{safeAnalysis.overallFeedback}</p>
                </Card>
              )}

              {/* Study Plan */}
              {safeAnalysis.studyPlan && safeAnalysis.studyPlan.length > 0 && (
                <Card className="mb-6">
                  <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                    📚 Personalized Study Plan
                  </h3>
                  <ul className="space-y-3">
                    {safeAnalysis.studyPlan.map((item, index) => (
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

        {/* Answer Review */}
        {result.answers && result.answers.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.25 }}
          >
            <Card className="mb-6">
              <h3 className="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
                📘 参考答案与解析
              </h3>
              <div className="space-y-4">
                {result.answers.map((item, idx) => (
                  <div key={`${item.questionId}-${idx}`} className="p-4 rounded-xl border border-gray-200 bg-white">
                    <div className="flex items-center justify-between gap-3 mb-2">
                      <p className="font-semibold text-gray-800">第 {idx + 1} 题</p>
                      <span className={item.isCorrect ? 'text-success font-semibold' : 'text-danger font-semibold'}>
                        {item.isCorrect ? '正确' : '错误'}
                      </span>
                    </div>
                    {item.question && <p className="text-gray-800 mb-2">{item.question}</p>}
                    <p className="text-sm text-gray-600 mb-1">
                      你的答案: <span className="font-medium text-gray-800">{formatAnswer(item.userAnswer, item.options)}</span>
                    </p>
                    <p className="text-sm text-gray-600 mb-1">
                      参考答案: <span className="font-medium text-gray-800">
                        {item.correctAnswer && String(item.correctAnswer).trim()
                          ? formatAnswer(item.correctAnswer, item.options)
                          : (item.explanation ? '见解析要点' : '暂无参考答案')}
                      </span>
                    </p>
                    {item.explanation && (
                      <p className="text-sm text-gray-700 bg-primary-50 rounded-lg p-3 mt-2">
                        解析: {item.explanation}
                      </p>
                    )}
                    {item.aiFeedback && (
                      <p className="text-sm text-gray-700 bg-amber-50 border border-amber-200 rounded-lg p-3 mt-2">
                        点评: {item.aiFeedback}
                      </p>
                    )}
                  </div>
                ))}
              </div>
            </Card>
          </motion.div>
        )}

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
