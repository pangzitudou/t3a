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
  (response) => response.data,
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
  types: ('single' | 'multiple' | 'code')[]
  difficulty: 'easy' | 'medium' | 'hard' | 'mixed'
}

export interface GenerationRequest {
  count: number
  difficulty: string
  typeDistribution: { [key: string]: number }
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

    // Convert types array to typeDistribution object
    const typeDistribution: { [key: string]: number } = {}
    const typeCount = settings.types.length
    settings.types.forEach(type => {
      const backendType = type === 'single' ? 'SINGLE_CHOICE' : 
                          type === 'multiple' ? 'MULTIPLE_CHOICE' : 'SHORT_ANSWER'
      typeDistribution[backendType] = Math.round(100 / typeCount)
    })

    const request: GenerationRequest = {
      count: settings.questionCount,
      difficulty: settings.difficulty.toUpperCase(),
      typeDistribution,
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
