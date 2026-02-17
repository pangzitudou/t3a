import { motion } from 'framer-motion'
import { cn } from '@/services/utils'

export type QuestionType = 'single' | 'multiple' | 'code'

export interface QuizCardProps {
  question: string
  options: string[]
  selectedOption?: number | number[]
  onOptionSelect: (index: number) => void
  questionNumber: number
  totalQuestions: number
  type?: QuestionType
}

export default function QuizCard({
  question,
  options,
  selectedOption,
  onOptionSelect,
  questionNumber,
  totalQuestions,
  type = 'single',
}: QuizCardProps) {
  const isSelected = (index: number) => {
    if (Array.isArray(selectedOption)) {
      return selectedOption.includes(index)
    }
    return selectedOption === index
  }

  const typeLabel = {
    single: '单选题',
    multiple: '多选题',
    code: 'Code Question',
  }

  const typeStyle = {
    single: 'bg-indigo-100 text-indigo-700 border border-indigo-200',
    multiple: 'bg-emerald-100 text-emerald-700 border border-emerald-200',
    code: 'bg-primary-100 text-primary-700 border border-primary-200',
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      exit={{ opacity: 0, y: -20 }}
      className="bg-bg-light rounded-2xl p-8 shadow-lg"
    >
      <div className="flex items-center justify-between mb-6">
        <div className="text-primary-600 font-bold text-lg">
          Question {questionNumber} of {totalQuestions}
        </div>
        <span className={cn('px-4 py-1.5 rounded-full text-sm font-semibold', typeStyle[type])}>
          {typeLabel[type]}
        </span>
      </div>

      <p className="text-sm mb-4 font-medium text-gray-600">
        {type === 'multiple' ? '可选择多个选项，再切换题目。' : '仅可选择一个选项。'}
      </p>

      <h2 className="text-2xl font-semibold text-gray-800 mb-8 leading-relaxed">
        {question}
      </h2>

      {type !== 'code' && (
        <div className="space-y-3">
          {options.map((option, index) => (
            <motion.button
              key={index}
              whileHover={{ scale: 1.01, x: 5 }}
              whileTap={{ scale: 0.99 }}
              onClick={() => onOptionSelect(index)}
              className={cn(
                'w-full text-left p-5 rounded-xl border-2 transition-all duration-300',
                'bg-white text-gray-900 hover:bg-indigo-50',
                isSelected(index)
                  ? 'border-primary-500 bg-primary-500 text-white shadow-lg'
                  : 'border-gray-200 hover:border-indigo-400'
              )}
            >
              <span className="font-semibold mr-3">{String.fromCharCode(65 + index)}.</span>
              {option}
            </motion.button>
          ))}
        </div>
      )}

    </motion.div>
  )
}
