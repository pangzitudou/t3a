import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import toast from 'react-hot-toast'
import Button from '../components/ui/Button'
import QuizCard from '../components/quiz/QuizCard'
import QuizTimer from '../components/quiz/QuizTimer'
import QuizProgress from '../components/quiz/QuizProgress'
import CodeEditor from '../components/quiz/CodeEditor'
import LiveIndicator from '../components/ui/LiveIndicator'
import { quizApi, Question } from '../services/quiz'
import { useQuizStore } from '../stores/quizStore'
import { connectQuizWebSocket, disconnectQuizWebSocket, WebSocketMessage } from '../services/websocket'

const calcQuestionScore = (question: Question, answer: any): number => {
  const normalizedAnswer = Array.isArray(answer)
    ? [...answer].sort((a, b) => a - b)
    : answer

  if (question.type === 'single') {
    return typeof normalizedAnswer === 'number' && normalizedAnswer === question.correctAnswer ? 10 : 0
  }

  if (question.type === 'multiple') {
    const correct = Array.isArray(question.correctAnswer) ? [...question.correctAnswer].sort((a, b) => a - b) : []
    if (!Array.isArray(normalizedAnswer)) return 0
    return correct.length === normalizedAnswer.length && correct.every((v, i) => v === normalizedAnswer[i]) ? 10 : 0
  }

  return 0
}

export default function QuizPage() {
  const { sessionKey } = useParams<{ sessionKey: string }>()
  const navigate = useNavigate()

  const {
    session,
    questions,
    currentQuestionIndex,
    answers,
    timeRemaining,
    setSession,
    setQuestions,
    setCurrentQuestion,
    setAnswer,
    setTimeRemaining,
    submitQuiz,
    resetQuiz,
    setConnected,
  } = useQuizStore()

  const [localCurrentQuestion, setLocalCurrentQuestion] = useState<Question | null>(null)
  const [codeValue, setCodeValue] = useState('')
  const [shortAnswerValue, setShortAnswerValue] = useState('')
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!sessionKey) {
      navigate('/banks')
      return
    }

    loadSession()

    // Connect to WebSocket
    const ws = connectQuizWebSocket(sessionKey)
    ws.connect().then(() => {
      setConnected(true)
      ws.onMessage((message: WebSocketMessage) => {
        switch (message.type) {
          case 'time_warning':
            toast(`⏰ Time warning: ${message.data.remaining} seconds remaining!`, {
              icon: '⚠️',
            })
            break
          case 'answer_saved':
            toast.success('✓ Answer saved')
            break
        }
      })
    })

    return () => {
      disconnectQuizWebSocket()
      resetQuiz()
    }
  }, [sessionKey])

  useEffect(() => {
    if (questions.length > 0 && currentQuestionIndex < questions.length) {
      setLocalCurrentQuestion(questions[currentQuestionIndex])
      if (questions[currentQuestionIndex].type === 'code' && questions[currentQuestionIndex].code) {
        setCodeValue(questions[currentQuestionIndex].code.template || '')
      }
      if (questions[currentQuestionIndex].type === 'short') {
        const savedAnswer = answers.get(currentQuestionIndex)
        setShortAnswerValue(typeof savedAnswer === 'string' ? savedAnswer : '')
      }
    }
  }, [questions, currentQuestionIndex, answers])

  const loadSession = async () => {
    try {
      setLoading(true)
      const sessionData = await quizApi.getSession(sessionKey!)
      const bankId = (sessionData as any)?.bankId
      const totalQuestions = (sessionData as any)?.totalQuestions || 10
      const questionData = await quizApi.getRandomQuestions(String(bankId), totalQuestions)

      setSession(sessionData)
      setQuestions(questionData)
      setLocalCurrentQuestion(questionData[0] || null)
      setTimeRemaining((sessionData as any).timeRemaining ?? totalQuestions * 60)
    } catch (error: any) {
      console.error('Failed to load session:', error)
      toast.error('Failed to load quiz session')
      navigate('/banks')
    } finally {
      setLoading(false)
    }
  }

  const handleOptionSelect = (index: number) => {
    if (!localCurrentQuestion) return

    const questionIndex = currentQuestionIndex
    const isMultiple = localCurrentQuestion.type === 'multiple'
    const prev = useQuizStore.getState().answers.get(questionIndex)
    const nextAnswer = isMultiple
      ? (() => {
          const selected = Array.isArray(prev) ? [...prev] : []
          const pos = selected.indexOf(index)
          if (pos >= 0) selected.splice(pos, 1)
          else selected.push(index)
          return selected.sort((a, b) => a - b)
        })()
      : index

    setAnswer(questionIndex, nextAnswer)

    // Auto-save to server
    quizApi.submitAnswer(sessionKey!, {
      questionId: localCurrentQuestion.id,
      answer: nextAnswer,
    }).catch((err) => console.error('Failed to save answer:', err))
  }

  const handleShortAnswerChange = (value: string) => {
    setShortAnswerValue(value)
    setAnswer(currentQuestionIndex, value)
  }

  const handleShortAnswerSave = () => {
    if (!localCurrentQuestion || localCurrentQuestion.type !== 'short') return
    quizApi.submitAnswer(sessionKey!, {
      questionId: localCurrentQuestion.id,
      answer: shortAnswerValue,
    }).catch((err) => console.error('Failed to save answer:', err))
  }

  const persistCurrentAnswer = () => {
    if (!localCurrentQuestion) return
    const questionId = localCurrentQuestion.id
    if (localCurrentQuestion.type === 'short') {
      const value = shortAnswerValue.trim()
      if (!value) return
      quizApi.submitAnswer(sessionKey!, { questionId, answer: value }).catch((err) => console.error('Failed to save answer:', err))
      return
    }
    if (localCurrentQuestion.type === 'code') {
      const value = codeValue.trim()
      if (!value) return
      setAnswer(currentQuestionIndex, value)
      quizApi.submitAnswer(sessionKey!, { questionId, answer: value }).catch((err) => console.error('Failed to save answer:', err))
    }
  }

  const handleNext = () => {
    persistCurrentAnswer()
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestion(currentQuestionIndex + 1)
    }
  }

  const handlePrevious = () => {
    persistCurrentAnswer()
    if (currentQuestionIndex > 0) {
      setCurrentQuestion(currentQuestionIndex - 1)
    }
  }

  const handleSubmit = async () => {
    const answeredCount = Array.from(answers.values()).filter(
      (a) => a !== undefined && a !== null
    ).length

    if (answeredCount < questions.length && !confirm(`You have only answered ${answeredCount} of ${questions.length} questions. Submit anyway?`)) {
      return
    }

    try {
      setSubmitting(true)
      persistCurrentAnswer()

      const review = questions.map((q, idx) => {
        const liveAnswer =
          idx === currentQuestionIndex && q.type === 'short'
            ? shortAnswerValue
            : idx === currentQuestionIndex && q.type === 'code'
            ? codeValue
            : answers.get(idx)
        const userAnswer = liveAnswer
        return {
          questionId: q.id,
          question: q.question,
          questionType: q.type,
          options: q.options || [],
          correctAnswer: Array.isArray(q.correctAnswer) ? q.correctAnswer.join(',') : q.correctAnswer,
          userAnswer,
          isCorrect: calcQuestionScore(q, userAnswer) > 0,
          score: calcQuestionScore(q, userAnswer),
          explanation: q.explanation,
        }
      })
      const score = review.reduce((sum, item) => sum + (item.score || 0), 0)

      sessionStorage.setItem(
        `quiz_result_local_${sessionKey}`,
        JSON.stringify({
          score,
          total: questions.length * 10,
          percentage: questions.length === 0 ? 0 : Math.round((score * 100) / (questions.length * 10)),
          answers: review,
        })
      )

      await quizApi.submitQuiz(sessionKey!, score)
      submitQuiz()
      navigate(`/result/${sessionKey}`)
    } catch (error: any) {
      console.error('Failed to submit quiz:', error)
      toast.error('Failed to submit quiz')
    } finally {
      setSubmitting(false)
    }
  }

  const handleExitQuiz = async () => {
    if (!confirm('确定退出当前测验吗？退出后本次记录不会保留。')) {
      return
    }
    try {
      await quizApi.abandonQuiz(sessionKey!)
      toast.success('已退出当前测验')
      navigate('/banks')
    } catch (error: any) {
      console.error('Failed to abandon quiz:', error)
      toast.error(error?.response?.data?.message || '退出测验失败')
    }
  }

  const handleTimeUp = () => {
    toast('⏰ Time is up! Submitting your quiz...', { icon: '⏰' })
    handleSubmit()
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

  if (!localCurrentQuestion || !session) {
    return null
  }

  const isAnswered = (value: any) => {
    if (value === undefined || value === null) return false
    if (Array.isArray(value)) return value.length > 0
    if (typeof value === 'string') return value.trim().length > 0
    return true
  }

  const answeredCount = Array.from(answers.values()).filter(isAnswered).length
  const answeredIndexes = Array.from(answers.entries())
    .filter(([, value]) => isAnswered(value))
    .map(([index]) => index)

  return (
    <div className="min-h-screen pb-8">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="max-w-4xl mx-auto px-4 py-8"
      >
        {/* Quiz Header */}
        <div className="flex items-center justify-between mb-6">
          <div>
            <h1 className="text-2xl font-bold text-white mb-1">Quiz Session</h1>
            <p className="text-white/80 text-sm">Session Key: {sessionKey}</p>
          </div>
          <div className="flex items-center gap-4">
            <LiveIndicator label="Real-time Sync" />
            <QuizTimer
              timeRemaining={timeRemaining}
              totalTime={(session as any).totalTime ?? ((session as any).totalQuestions || questions.length || 10) * 60}
              onTimeUp={handleTimeUp}
            />
            <Button onClick={handleExitQuiz} variant="outline">
              退出测验
            </Button>
          </div>
        </div>

        {/* Progress */}
        <QuizProgress
          current={currentQuestionIndex + 1}
          total={questions.length}
          answered={answeredCount}
          answeredIndexes={answeredIndexes}
          onQuestionClick={(index) => setCurrentQuestion(index)}
        />

        {/* Question */}
        <AnimatePresence mode="wait">
          <motion.div
            key={currentQuestionIndex}
            initial={{ opacity: 0, x: 50 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -50 }}
            transition={{ duration: 0.3 }}
          >
            {localCurrentQuestion.type === 'code' ? (
              <div className="bg-bg-light rounded-2xl p-8 shadow-lg mb-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="text-primary-600 font-bold text-lg">
                    Question {currentQuestionIndex + 1} of {questions.length}
                  </div>
                  <span className="px-4 py-1.5 bg-primary-100 text-primary-700 rounded-full text-sm font-medium">
                    Code Question
                  </span>
                </div>

                <h2 className="text-xl font-semibold text-gray-800 mb-6 leading-relaxed">
                  {localCurrentQuestion.question}
                </h2>

                <CodeEditor
                  language={localCurrentQuestion.code?.language || 'java'}
                  value={codeValue}
                  onChange={(value) => {
                    setCodeValue(value)
                    setAnswer(currentQuestionIndex, value)
                  }}
                  height="400px"
                />
              </div>
            ) : localCurrentQuestion.type === 'short' ? (
              <div className="bg-bg-light rounded-2xl p-8 shadow-lg mb-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="text-primary-600 font-bold text-lg">
                    Question {currentQuestionIndex + 1} of {questions.length}
                  </div>
                  <span className="px-4 py-1.5 bg-amber-100 text-amber-700 border border-amber-200 rounded-full text-sm font-semibold">
                    问答题
                  </span>
                </div>
                <h2 className="text-xl font-semibold text-gray-800 mb-6 leading-relaxed">
                  {localCurrentQuestion.question}
                </h2>
                <p className="text-sm mb-3 font-medium text-gray-600">请用完整语句作答，提交后会给出参考答案与点评。</p>
                <textarea
                  value={shortAnswerValue}
                  onChange={(e) => handleShortAnswerChange(e.target.value)}
                  onBlur={handleShortAnswerSave}
                  placeholder="Type your answer..."
                  rows={8}
                  className="w-full rounded-xl border-2 border-gray-200 focus:border-primary-500 focus:outline-none p-4 text-gray-800 bg-white"
                />
              </div>
            ) : (
              <QuizCard
                question={localCurrentQuestion.question}
                options={localCurrentQuestion.options || []}
                selectedOption={answers.get(currentQuestionIndex)}
                onOptionSelect={handleOptionSelect}
                questionNumber={currentQuestionIndex + 1}
                totalQuestions={questions.length}
                type={localCurrentQuestion.type}
              />
            )}
          </motion.div>
        </AnimatePresence>

        {/* Navigation */}
        <div className="flex items-center justify-between mt-6">
          <Button
            onClick={handlePrevious}
            disabled={currentQuestionIndex === 0}
            variant="outline"
          >
            ← Previous
          </Button>

          <div className="flex gap-2">
            {currentQuestionIndex === questions.length - 1 ? (
              <Button
                onClick={handleSubmit}
                isLoading={submitting}
                variant="secondary"
                size="lg"
              >
                ✓ Submit Quiz
              </Button>
            ) : (
              <Button onClick={handleNext} size="lg">
                Next →
              </Button>
            )}
          </div>
        </div>
      </motion.div>
    </div>
  )
}
