import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../services/auth'
import toast from 'react-hot-toast'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({username:'', password:'', email:'', nickname:''})

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()

    try {
      const response = await register(form)
      const data = (response as any).data || response
      
      if (data?.userInfo) {
        toast.success('注册成功')
        navigate('/login')
      } else {
        toast.error('注册失败：响应数据格式错误')
      }
    } catch (err: any) {
      console.error('[RegisterPage] Registration failed:', err)
      const errorMsg = err.response?.data?.message || err.message || '注册失败，请稍后重试'
      toast.error(errorMsg)
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
