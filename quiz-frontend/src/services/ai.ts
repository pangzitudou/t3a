import axios from 'axios'

// Create a separate axios instance for the quiz-ai service
// This service runs on port 8082 and has different endpoints
const aiApiInstance = axios.create({
  baseURL: '/api/ai', // Will be proxied to http://localhost:8082
  timeout: 60000, // AI generation may take longer
})

// Add request interceptor to include auth token if available
aiApiInstance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Add response interceptor to handle errors
aiApiInstance.interceptors.response.use(
  (response) => {
    const payload = response.data as any
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 200) {
        const apiError: any = new Error(payload.message || 'AI request failed')
        apiError.code = payload.code
        apiError.response = response
        return Promise.reject(apiError)
      }
      return payload.data
    }
    return response.data
  },
  (error) => {
    console.error('[AI API] Error:', {
      status: error.response?.status,
      url: error.config?.url,
      message: error.message,
      data: error.response?.data,
    })
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export interface GenerationSettings {
  file: File
  questionCount: number
  typeCounts: {
    single: number
    multiple: number
    short: number
  }
  bankName?: string
  difficulty: 'easy' | 'medium' | 'hard' | 'mixed'
}

export interface GenerationRequest {
  count: number
  difficulty: string
  typeDistribution: string
  bankName?: string
  category?: string
}

export interface GenerationProgress {
  taskId: string
  status: 'pending' | 'processing' | 'completed' | 'failed'
  progress: number
  message: string
  bankId?: string
}

export interface KnowledgeAnalysis {
  sessionKey: string
  strengths: Array<{
    topic: string
    score: number
    recommendations: string[]
  }>
  weaknesses: Array<{
    topic: string
    score: number
    recommendations: string[]
  }>
  overallFeedback: string
  studyPlan: string[]
}

export const aiApi = {
  // Generate questions from uploaded material
  generateQuestions: async (settings: GenerationSettings) => {
    const formData = new FormData()
    formData.append('file', settings.file)

    const typeDistribution: { [key: string]: number } = {
      SINGLE_CHOICE: settings.typeCounts.single,
      MULTIPLE_CHOICE: settings.typeCounts.multiple,
      SHORT_ANSWER: settings.typeCounts.short,
    }

    const request: GenerationRequest = {
      count: settings.questionCount,
      difficulty: settings.difficulty.toUpperCase(),
      typeDistribution: JSON.stringify(typeDistribution),
      bankName: settings.bankName?.trim() || undefined,
    }
    formData.append('request', JSON.stringify(request))

    return aiApiInstance.post<any>('/generation/generate', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  // Check generation progress - updated to match backend path
  getGenerationProgress: (taskId: string) =>
    aiApiInstance.get<string>(`/generation/status/${taskId}`),

  // Get AI analysis for a completed quiz
  getAnalysis: (sessionKey: string) =>
    aiApiInstance.get<KnowledgeAnalysis>(`/ai/analysis/${sessionKey}`),

  // Regenerate analysis
  regenerateAnalysis: (sessionKey: string) =>
    aiApiInstance.post<KnowledgeAnalysis>(`/ai/analysis/${sessionKey}/regenerate`),
}
