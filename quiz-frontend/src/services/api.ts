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
    const payload = response.data as ApiResponse<any>
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 200) {
        const apiError: any = new Error(payload.message || 'Request failed')
        apiError.code = payload.code
        apiError.response = response
        return Promise.reject(apiError)
      }
      return {
        ...response,
        data: payload.data,
      }
    }

    return {
      ...response,
      data: response.data,
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
