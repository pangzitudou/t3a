import api from './api'

export interface QuestionBank {
  id: string
  name: string
  questionCount: number
  difficulty: 'easy' | 'medium' | 'hard'
  types: string[]
  createdAt: string
}

export interface QuizSession {
  sessionKey: string
  bankId: string
  currentQuestionIndex: number
  totalQuestions: number
  timeRemaining: number
  totalTime: number
  status: 'in_progress' | 'completed'
}

export interface Question {
  id: string
  type: 'single' | 'multiple' | 'short' | 'code'
  question: string
  options?: string[]
  correctAnswer?: number | number[]
  code?: {
    language: string
    template: string
  }
  explanation?: string
  difficulty: 'easy' | 'medium' | 'hard'
  tags: string[]
}

interface BackendQuestion {
  id: number | string
  questionType?: string
  content?: string
  options?: string | string[] | null
  correctAnswer?: string | number | number[] | null
  explanation?: string | null
  difficulty?: string | null
  tags?: string | string[] | null
}

const parseOptions = (raw: BackendQuestion['options']): string[] => {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    try {
      const parsed = JSON.parse(raw)
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

const mapQuestionType = (type?: string): Question['type'] => {
  switch ((type || '').toUpperCase()) {
    case 'MULTIPLE_CHOICE':
      return 'multiple'
    case 'SHORT_ANSWER':
      return 'short'
    case 'CODE':
      return 'code'
    case 'SINGLE_CHOICE':
    default:
      return 'single'
  }
}

const mapDifficulty = (difficulty?: string | null): Question['difficulty'] => {
  switch ((difficulty || '').toUpperCase()) {
    case 'EASY':
      return 'easy'
    case 'HARD':
      return 'hard'
    default:
      return 'medium'
  }
}

const toAnswerIndex = (raw: string, options: string[]): number => {
  const value = raw.trim().toUpperCase()
  if (/^[A-Z]$/.test(value)) return value.charCodeAt(0) - 65
  const asNum = Number(value)
  if (Number.isInteger(asNum)) return asNum
  return options.findIndex((opt) => opt === raw)
}

const parseCorrectAnswer = (raw: BackendQuestion['correctAnswer'], options: string[]): number | number[] | undefined => {
  if (raw === null || raw === undefined) return undefined
  if (Array.isArray(raw)) {
    return raw.map((v) => (typeof v === 'number' ? v : toAnswerIndex(String(v), options))).filter((v) => v >= 0)
  }
  if (typeof raw === 'number') return raw
  const value = String(raw).trim()
  if (!value) return undefined
  if (value.includes(',')) {
    return value
      .split(',')
      .map((v) => toAnswerIndex(v, options))
      .filter((v) => v >= 0)
  }
  const idx = toAnswerIndex(value, options)
  return idx >= 0 ? idx : undefined
}

const parseTags = (raw: BackendQuestion['tags']): string[] => {
  if (!raw) return []
  if (Array.isArray(raw)) return raw
  if (typeof raw === 'string') {
    return raw.split(',').map((t) => t.trim()).filter(Boolean)
  }
  return []
}

const mapQuestion = (q: BackendQuestion): Question => {
  const options = parseOptions(q.options)
  return {
    id: String(q.id),
    type: mapQuestionType(q.questionType),
    question: q.content || '',
    options,
    correctAnswer: parseCorrectAnswer(q.correctAnswer, options),
    explanation: q.explanation || undefined,
    difficulty: mapDifficulty(q.difficulty),
    tags: parseTags(q.tags),
  }
}

export interface QuizResult {
  sessionKey: string
  score: number
  total: number
  percentage: number
  timeTaken: number
  totalQuestions?: number
  correctAnswers?: number
  answers: Array<{
    questionId: string
    question?: string
    questionType?: string
    options?: string[]
    correctAnswer?: string
    userAnswer: any
    isCorrect: boolean
    score?: number
    explanation?: string
    aiFeedback?: string
  }>
  completedAt: string
}

export interface DashboardStats {
  quizzesCompleted: number
  averageScore: number
  studyTime: number
  currentStreak: number
  recentQuizzes: QuizResult[]
}

export const quizApi = {
  // Question Banks
  getBanks: async () => {
    const res = await api.get<any>('/quiz/bank/list')
    return res.data
  },
  createBank: async (data: { name: string; description?: string }) => {
    const res = await api.post<QuestionBank>('/quiz/bank/create', data)
    return res.data
  },
  getBank: async (id: string) => {
    const res = await api.get<QuestionBank>(`/quiz/bank/${id}`)
    return res.data
  },
  deleteBank: (id: string) => api.delete(`/quiz/bank/${id}`),

  // Quiz Sessions
  startSession: async (bankId: string, userId?: string, questionCount?: number) => {
    const res = await api.post<QuizSession>('/quiz/start', { userId, bankId, questionCount })
    return res.data
  },
  getSession: async (sessionKey: string) => {
    const res = await api.get<QuizSession>(`/quiz/session/${sessionKey}`)
    return res.data
  },

  // Question management
  getBankQuestions: async (bankId: string) => {
    const res = await api.get<BackendQuestion[]>(`/quiz/question/list?bankId=${bankId}`)
    return (res.data || []).map(mapQuestion)
  },
  getRandomQuestions: async (bankId: string, count: number = 10) => {
    const res = await api.get<BackendQuestion[]>(`/quiz/question/random?bankId=${bankId}&count=${count}`)
    return (res.data || []).map(mapQuestion)
  },

  // Answers
  submitAnswer: (sessionKey: string, data: { questionId: string | number; answer: any }) =>
    api.post<any>(`/quiz/session/${sessionKey}/answer`, data),
  abandonQuiz: (sessionKey: string) =>
    api.post<any>(`/quiz/session/${sessionKey}/abandon`),

  // Submit quiz
  submitQuiz: (sessionKey: string, score?: number) =>
    api.post<any>('/quiz/submit', { sessionKey, userScore: score }),

  // Results
  getResult: async (sessionKey: string) => {
    const res = await api.get<any>(`/quiz/result/${sessionKey}`)
    return res.data
  },
  getUserHistory: async (userId: string) => {
    const res = await api.get<QuizSession[]>(`/quiz/history/${userId}`)
    return res.data
  },

  // Dashboard
  getDashboard: async () => {
    const res = await api.get<any>('/quiz/dashboard')
    return res.data
  },

  // Current question for a session
  getCurrentQuestion: async (sessionKey: string) => {
    const res = await api.get<any>(`/quiz/session/${sessionKey}/current`)
    if (res.data && res.data.content) return mapQuestion(res.data as BackendQuestion)
    return res.data
  },
}
