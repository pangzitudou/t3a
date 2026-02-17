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
  type: 'single' | 'multiple' | 'code'
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

export interface QuizResult {
  sessionKey: string
  score: number
  total: number
  percentage: number
  timeTaken: number
  answers: Array<{
    questionId: string
    userAnswer: any
    isCorrect: boolean
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
    const res = await api.get<Question[]>(`/quiz/question/list?bankId=${bankId}`)
    return res.data
  },
  getRandomQuestions: async (bankId: string, count: number = 10) => {
    const res = await api.get<Question[]>(`/quiz/question/random?bankId=${bankId}&count=${count}`)
    return res.data
  },

  // Answers
  submitAnswer: (sessionKey: string, data: { questionId: string; answer: any }) =>
    api.post<any>(`/quiz/session/${sessionKey}/answer`, data),

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
    const res = await api.get<Question>(`/quiz/session/${sessionKey}/current`)
    return res.data
  },
}
