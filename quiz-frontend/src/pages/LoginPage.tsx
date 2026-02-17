import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { login } from '../services/auth'
import { useAuthStore } from '../stores/authStore'
import toast from 'react-hot-toast'

export default function LoginPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((state) => state.setAuth)
  const [form, setForm] = useState({ username: '', password: '' })
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!form.username || !form.password) {
      toast.error('请输入用户名和密码')
      return
    }

    try {
      setLoading(true)
      const response = await login({ username: form.username, password: form.password })
      const data = (response as any).data || response

      if (data?.userInfo && data?.accessToken) {
        setAuth(data.userInfo, data.accessToken)
        localStorage.setItem('token', data.accessToken)
        localStorage.setItem('user', JSON.stringify(data.userInfo))
        localStorage.setItem('userId', data.userInfo.id)
        toast.dismiss()
        toast.success('登录成功', {
          id: 'login-success',
          duration: 1500,
        })
        navigate('/dashboard', { replace: true })
      } else {
        toast.error('登录失败：响应数据格式错误')
      }
    } catch (err: any) {
      console.error('[LoginPage] Login failed:', err)
      const errorMsg = err.response?.data?.message || err.message || '登录失败，请稍后重试'
      toast.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="card max-w-md w-full">
        <h2 className="text-3xl font-bold text-center mb-6">登录</h2>
        <form onSubmit={handleSubmit} className="space-y-4">
          <input
            type="text"
            value={form.username}
            onChange={(e) => setForm({ ...form, username: e.target.value })}
            className="input"
            placeholder="用户名"
            required
            disabled={loading}
          />
          <input
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            className="input"
            placeholder="密码"
            required
            disabled={loading}
          />
          <button
            type="submit"
            className="btn btn-primary w-full"
            disabled={loading}
          >
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
        <p className="mt-4 text-center">
          <Link to="/register" className="text-primary-600">
            没有账号？注册
          </Link>
        </p>
      </div>
    </div>
  )
}
