import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast from 'react-hot-toast'
import UploadArea from '../components/ui/UploadArea'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import { aiApi } from '../services/ai'
import { useAuthStore } from '../stores/authStore'

export default function GeneratePage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthStore()

  const [file, setFile] = useState<File | null>(null)
  const [bankName, setBankName] = useState('')
  const [singleCount, setSingleCount] = useState(8)
  const [multipleCount, setMultipleCount] = useState(6)
  const [shortCount, setShortCount] = useState(6)
  const [difficulty, setDifficulty] = useState<'easy' | 'medium' | 'hard' | 'mixed'>('medium')
  const [isGenerating, setIsGenerating] = useState(false)
  const [progress, setProgress] = useState(0)
  const questionCount = singleCount + multipleCount + shortCount

  if (!isAuthenticated) {
    navigate('/login')
    return null
  }

  const handleFileSelect = (selectedFile: File) => {
    setFile(selectedFile)
    toast.success(`File selected: ${selectedFile.name}`)
  }

  const handleGenerate = async () => {
    if (!file) {
      toast.error('Please select a file first')
      return
    }

    if (questionCount <= 0 || questionCount > 50) {
      toast.error('题目总数需在 1-50 之间')
      return
    }

    setIsGenerating(true)
    setProgress(0)

    try {
      const submitData = await aiApi.generateQuestions({
        file,
        questionCount,
        typeCounts: {
          single: singleCount,
          multiple: multipleCount,
          short: shortCount,
        },
        bankName,
        difficulty,
      })
      const submitPayload = submitData?.taskId ? submitData : submitData?.data
      const taskId = submitPayload?.taskId
      if (!taskId) {
        throw new Error('任务提交失败：未返回 taskId')
      }

      // Poll real task status from backend
      let completed = false
      for (let i = 0; i < 300; i += 1) {
        await new Promise((resolve) => setTimeout(resolve, 1000))
        const statusRaw = await aiApi.getGenerationProgress(taskId)
        const statusPayload = statusRaw?.status ? statusRaw : statusRaw?.data ?? statusRaw
        const statusData =
          typeof statusPayload === 'string' ? JSON.parse(statusPayload) : statusPayload
        const currentProgress = Number(statusData?.progress ?? 0)
        setProgress(Math.max(0, Math.min(100, currentProgress)))

        const status = String(statusData?.status || '').toUpperCase()
        if (status === 'COMPLETED') {
          completed = true
          break
        }
        if (status === 'FAILED') {
          throw new Error(statusData?.message || '题目生成失败')
        }
      }

      if (!completed) {
        throw new Error('题目生成超时，请稍后到题库页查看结果')
      }

      toast.success('Question bank generated successfully!')
      navigate('/banks')
    } catch (error: any) {
      console.error('Generation failed:', error)
      if (error?.response?.status === 413) {
        toast.error('文件过大，请压缩后重试（当前限制 100MB）')
      } else {
        toast.error(error.response?.data?.message || error.message || 'Failed to generate questions')
      }
      setProgress(0)
    } finally {
      setIsGenerating(false)
    }
  }

  return (
    <div className="min-h-screen pb-8">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="max-w-4xl mx-auto px-4 py-8"
      >
        <div className="text-center mb-8">
          <motion.div
            initial={{ scale: 0 }}
            animate={{ scale: 1 }}
            transition={{ type: 'spring', delay: 0.2 }}
            className="inline-flex items-center gap-2 bg-gradient-to-r from-pink-500 to-rose-500 text-white px-4 py-2 rounded-full text-sm font-semibold mb-4"
          >
            🤖 AI Question Generator
            <span className="text-xs opacity-80">Powered by Spring AI</span>
          </motion.div>
          <h1 className="text-4xl font-bold text-white mb-2">Generate Question Bank</h1>
          <p className="text-white/80">Upload your learning materials and let AI create quizzes for you</p>
        </div>

        <Card>
          {/* File Upload */}
          <div className="mb-8">
            <UploadArea onFileSelect={handleFileSelect} />
            {file && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-4 p-4 bg-green-50 border-2 border-success rounded-xl flex items-center justify-between"
              >
                <div className="flex items-center gap-3">
                  <span className="text-2xl">📄</span>
                  <div>
                    <div className="font-semibold text-gray-800">{file.name}</div>
                    <div className="text-sm text-gray-500">
                      {(file.size / 1024).toFixed(2)} KB
                    </div>
                  </div>
                </div>
                <button
                  onClick={() => setFile(null)}
                  className="text-danger hover:bg-red-100 px-3 py-1 rounded-lg transition-colors"
                >
                  Remove
                </button>
              </motion.div>
            )}
          </div>

          {/* Generation Settings */}
          <div className="space-y-6">
            <h3 className="text-xl font-bold text-gray-800">Generation Settings</h3>

            {/* Question Count */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                题型数量配置（总数 <span className="text-primary-600 text-lg">{questionCount}</span>）
              </label>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <label className="text-sm text-gray-700">
                  单选题
                  <input
                    type="number"
                    min="0"
                    max="50"
                    value={singleCount}
                    onChange={(e) => setSingleCount(Number(e.target.value || 0))}
                    className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-300"
                    disabled={isGenerating}
                  />
                </label>
                <label className="text-sm text-gray-700">
                  多选题
                  <input
                    type="number"
                    min="0"
                    max="50"
                    value={multipleCount}
                    onChange={(e) => setMultipleCount(Number(e.target.value || 0))}
                    className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-300"
                    disabled={isGenerating}
                  />
                </label>
                <label className="text-sm text-gray-700">
                  问答题
                  <input
                    type="number"
                    min="0"
                    max="50"
                    value={shortCount}
                    onChange={(e) => setShortCount(Number(e.target.value || 0))}
                    className="mt-1 w-full px-3 py-2 rounded-lg border border-gray-300"
                    disabled={isGenerating}
                  />
                </label>
              </div>
            </div>

            {/* Bank Name */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                题库名称（可选）:
              </label>
              <input
                value={bankName}
                onChange={(e) => setBankName(e.target.value)}
                placeholder="例如：Redis 进阶题库（留空则用文件名）"
                disabled={isGenerating}
                className="w-full px-4 py-3 rounded-xl border-2 border-gray-200 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 outline-none transition-all"
              />
            </div>

            {/* Difficulty Level */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                Difficulty Level:
              </label>
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value as any)}
                disabled={isGenerating}
                className="w-full px-4 py-3 rounded-xl border-2 border-gray-200 focus:border-primary-500 focus:ring-2 focus:ring-primary-200 outline-none transition-all"
              >
                <option value="easy">Easy</option>
                <option value="medium">Medium</option>
                <option value="hard">Hard</option>
                <option value="mixed">Mixed</option>
              </select>
            </div>

            {/* Generate Button */}
            <Button
              onClick={handleGenerate}
              isLoading={isGenerating}
              disabled={!file || questionCount <= 0 || questionCount > 50}
              size="lg"
              className="w-full"
            >
              🚀 Generate Questions {questionCount > 0 && `(${questionCount})`}
            </Button>

            {/* Progress Bar */}
            {isGenerating && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-6"
              >
                <div className="flex items-center justify-between mb-2">
                  <span className="text-sm font-medium text-gray-700">Generating...</span>
                  <span className="text-sm font-bold text-primary-600">{progress}%</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
                  <motion.div
                    initial={{ width: 0 }}
                    animate={{ width: `${progress}%` }}
                    transition={{ duration: 0.3 }}
                    className="h-full bg-gradient-to-r from-primary-500 to-secondary-500 rounded-full"
                  />
                </div>
                <p className="text-xs text-gray-500 mt-2">
                  This may take 30-60 seconds depending on the file size...
                </p>
              </motion.div>
            )}
          </div>
        </Card>
      </motion.div>
    </div>
  )
}
