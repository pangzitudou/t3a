#!/bin/bash
# 批量创建前端文件

cd "$(dirname "$0")"

# 创建服务文件
mkdir -p src/services src/pages

# API服务
cat > src/services/api.ts << 'EOF'
import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080',
  timeout: 30000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
EOF

# 认证服务
cat > src/services/auth.ts << 'EOF'
import api from './api'

export const login = (data: {username: string, password: string}) => {
  return api.post('/auth/login', data)
}

export const register = (data: any) => {
  return api.post('/auth/register', data)
}
EOF

# 首页
cat > src/pages/HomePage.tsx << 'EOF'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'

export default function HomePage() {
  const navigate = useNavigate()
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <motion.div initial={{opacity:0}} animate={{opacity:1}} className="text-center">
        <h1 className="text-6xl font-bold text-white mb-6">TestAgainAndAgain</h1>
        <p className="text-2xl text-white/90 mb-8">AI驱动的智能测验平台</p>
        <div className="flex gap-4 justify-center">
          <button onClick={()=>navigate('/login')} className="btn btn-primary">开始测验</button>
          <button onClick={()=>navigate('/register')} className="btn btn-secondary">创建账号</button>
        </div>
      </motion.div>
    </div>
  )
}
EOF

# 登录页
cat > src/pages/LoginPage.tsx << 'EOF'
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../services/auth'

export default function LoginPage() {
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res: any = await login({username, password})
      if (res.code === 200) {
        localStorage.setItem('token', res.data.accessToken)
        localStorage.setItem('user', JSON.stringify(res.data.userInfo))
        navigate('/dashboard')
      }
    } catch (err) {
      alert('登录失败')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="card max-w-md w-full">
        <h2 className="text-3xl font-bold text-center mb-6">登录</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input type="text" value={username} onChange={(e)=>setUsername(e.target.value)} 
            className="input" placeholder="用户名" required />
          <input type="password" value={password} onChange={(e)=>setPassword(e.target.value)} 
            className="input" placeholder="密码" required />
          <button type="submit" className="btn btn-primary w-full">登录</button>
        </form>
        <p className="mt-4 text-center"><Link to="/register" className="text-primary-600">注册账号</Link></p>
      </div>
    </div>
  )
}
EOF

# 注册页
cat > src/pages/RegisterPage.tsx << 'EOF'
import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../services/auth'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({username:'', password:'', email:'', nickname:''})

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res: any = await register(form)
      if (res.code === 200) {
        localStorage.setItem('token', res.data.accessToken)
        navigate('/dashboard')
      }
    } catch (err) {
      alert('注册失败')
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="card max-w-md w-full">
        <h2 className="text-3xl font-bold text-center mb-6">注册</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input type="text" value={form.username} onChange={(e)=>setForm({...form, username:e.target.value})} 
            className="input" placeholder="用户名" required />
          <input type="text" value={form.nickname} onChange={(e)=>setForm({...form, nickname:e.target.value})} 
            className="input" placeholder="昵称" required />
          <input type="email" value={form.email} onChange={(e)=>setForm({...form, email:e.target.value})} 
            className="input" placeholder="邮箱" />
          <input type="password" value={form.password} onChange={(e)=>setForm({...form, password:e.target.value})} 
            className="input" placeholder="密码" required />
          <button type="submit" className="btn btn-primary w-full">注册</button>
        </form>
        <p className="mt-4 text-center"><Link to="/login" className="text-primary-600">已有账号？登录</Link></p>
      </div>
    </div>
  )
}
EOF

# 仪表板
cat > src/pages/DashboardPage.tsx << 'EOF'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'

export default function DashboardPage() {
  const navigate = useNavigate()
  const [user, setUser] = useState<any>(null)

  useEffect(() => {
    const token = localStorage.getItem('token')
    const userData = localStorage.getItem('user')
    if (!token) {
      navigate('/login')
      return
    }
    if (userData) setUser(JSON.parse(userData))
  }, [navigate])

  const handleLogout = () => {
    localStorage.clear()
    navigate('/')
  }

  if (!user) return null

  return (
    <div className="min-h-screen p-8">
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between items-center mb-8">
          <h1 className="text-3xl font-bold text-white">欢迎，{user.nickname}！</h1>
          <button onClick={handleLogout} className="btn btn-secondary">退出登录</button>
        </div>
        <div className="grid grid-cols-3 gap-6">
          <div className="card"><h3>已完成测验</h3><p className="text-3xl font-bold">0</p></div>
          <div className="card"><h3>学习时长</h3><p className="text-3xl font-bold">0h</p></div>
          <div className="card"><h3>平均分</h3><p className="text-3xl font-bold">-</p></div>
        </div>
      </div>
    </div>
  )
}
EOF

echo "✓ 前端文件创建完成"
