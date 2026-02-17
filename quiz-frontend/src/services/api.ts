import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

type ApiResponse<T> = {
  code: number
  message: string
  data: T
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token && !config.url?.includes('/auth/')) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => {
    console.log('[API] Raw response.data:', response.data)
    const unwrappedData = (response.data as ApiResponse<any>)?.data
    console.log('[API] Unwrapped data:', unwrappedData)
    return {
      ...response,
      data: unwrappedData
    }
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
