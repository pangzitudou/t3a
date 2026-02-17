import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import toast, { Toaster } from 'react-hot-toast'
import Button from '../components/ui/Button'
import QuizCard from '../components/quiz/QuizCard'
import QuizTimer from '../components/quiz/QuizTimer'
import QuizProgress from '../components/quiz/QuizProgress'
import CodeEditor from '../components/quiz/CodeEditor'
import LiveIndicator from '../components/ui/LiveIndicator'
import { quizApi, Question } from '../services/quiz'
import { useQuizStore } from '../stores/quizStore'
import { connectQuizWebSocket, disconnectQuizWebSocket, WebSocketMessage } from '../services/websocket'

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
    }
  }, [questions, currentQuestionIndex])

  const loadSession = async () => {
    try {
      setLoading(true)
      const [sessionData, questionData] = await Promise.all([
        quizApi.getSession(sessionKey!),
        quizApi.getCurrentQuestion(sessionKey!),
      ])

      setSession(sessionData)
      // Load all questions (in real app, might paginate)
      setQuestions([questionData])
      setLocalCurrentQuestion(questionData)
      setTimeRemaining(sessionData.timeRemaining)
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
    setAnswer(questionIndex, index)

    // Auto-save to server
    quizApi.submitAnswer(sessionKey!, {
      questionId: localCurrentQuestion.id,
      answer: index,
    }).catch((err) => console.error('Failed to save answer:', err))
  }

  const handleNext = () => {
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestion(currentQuestionIndex + 1)
    }
  }

  const handlePrevious = () => {
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
      await quizApi.submitQuiz(sessionKey!)
      submitQuiz()
      navigate(`/result/${sessionKey}`)
    } catch (error: any) {
      console.error('Failed to submit quiz:', error)
      toast.error('Failed to submit quiz')
    } finally {
      setSubmitting(false)
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

  const answeredCount = Array.from(answers.values()).filter(
    (a) => a !== undefined && a !== null
  ).length

  return (
    <div className="min-h-screen pb-8">
      <Toaster position="top-center" />

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
              totalTime={session.totalTime}
              onTimeUp={handleTimeUp}
            />
          </div>
        </div>

        {/* Progress */}
        <QuizProgress
          current={currentQuestionIndex + 1}
          total={questions.length}
          answered={answeredCount}
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
                  onChange={setCodeValue}
                  height="400px"
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
                explanation={localCurrentQuestion.explanation}
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
