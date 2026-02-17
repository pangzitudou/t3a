import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import toast, { Toaster } from 'react-hot-toast'
import UploadArea from '../components/ui/UploadArea'
import Button from '../components/ui/Button'
import Card from '../components/ui/Card'
import { aiApi } from '../services/ai'
import { useAuthStore } from '../stores/authStore'

export default function GeneratePage() {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuthStore()

  const [file, setFile] = useState<File | null>(null)
  const [questionCount, setQuestionCount] = useState(20)
  const [types, setTypes] = useState<string[]>(['single', 'multiple', 'code'])
  const [difficulty, setDifficulty] = useState<'easy' | 'medium' | 'hard' | 'mixed'>('medium')
  const [isGenerating, setIsGenerating] = useState(false)
  const [progress, setProgress] = useState(0)

  if (!isAuthenticated) {
    navigate('/login')
    return null
  }

  const handleFileSelect = (selectedFile: File) => {
    setFile(selectedFile)
    toast.success(`File selected: ${selectedFile.name}`)
  }

  const handleTypeToggle = (type: string) => {
    setTypes((prev) =>
      prev.includes(type) ? prev.filter((t) => t !== type) : [...prev, type]
    )
  }

  const handleGenerate = async () => {
    if (!file) {
      toast.error('Please select a file first')
      return
    }

    if (types.length === 0) {
      toast.error('Please select at least one question type')
      return
    }

    setIsGenerating(true)
    setProgress(0)

    try {
      // Simulate progress for demo
      const progressInterval = setInterval(() => {
        setProgress((prev) => Math.min(90, prev + 10))
      }, 500)

      await aiApi.generateQuestions({
        file,
        questionCount,
        types: types as any,
        difficulty,
      })

      clearInterval(progressInterval)
      setProgress(100)

      toast.success('Question bank generated successfully!')

      // Navigate to the new bank after a short delay
      setTimeout(() => {
        navigate('/banks')
      }, 1500)
    } catch (error: any) {
      console.error('Generation failed:', error)
      toast.error(error.response?.data?.message || 'Failed to generate questions')
      setProgress(0)
    } finally {
      setIsGenerating(false)
    }
  }

  return (
    <div className="min-h-screen pb-8">
      <Toaster position="top-center" />

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
                Number of Questions: <span className="text-primary-600 text-lg">{questionCount}</span>
              </label>
              <input
                type="range"
                min="5"
                max="50"
                value={questionCount}
                onChange={(e) => setQuestionCount(parseInt(e.target.value))}
                className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer accent-primary-500"
                disabled={isGenerating}
              />
              <div className="flex justify-between text-xs text-gray-500 mt-1">
                <span>5</span>
                <span>50</span>
              </div>
            </div>

            {/* Question Types */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-3">
                Question Types:
              </label>
              <div className="space-y-2">
                {[
                  { value: 'single', label: 'Single Choice', emoji: '🔘' },
                  { value: 'multiple', label: 'Multiple Choice', emoji: '🔘🔘' },
                  { value: 'code', label: 'Code Question', emoji: '💻' },
                ].map((type) => (
                  <label
                    key={type.value}
                    className={`flex items-center gap-3 p-4 rounded-xl border-2 cursor-pointer transition-all ${
                      types.includes(type.value)
                        ? 'border-primary-500 bg-primary-50'
                        : 'border-gray-200 hover:border-primary-300'
                    }`}
                  >
                    <input
                      type="checkbox"
                      checked={types.includes(type.value)}
                      onChange={() => handleTypeToggle(type.value)}
                      disabled={isGenerating}
                      className="w-5 h-5 accent-primary-500"
                    />
                    <span className="text-2xl">{type.emoji}</span>
                    <span className="font-medium">{type.label}</span>
                  </label>
                ))}
              </div>
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
              disabled={!file || types.length === 0}
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
