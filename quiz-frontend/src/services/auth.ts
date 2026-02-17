import api from './api'

export const login = (data: {username: string, password: string}) => {
  return api.post('/quiz/auth/login', data)
}

export const register = (data: any) => {
  return api.post('/quiz/auth/register', data)
}
