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
  explanation?: string
}

export default function QuizCard({
  question,
  options,
  selectedOption,
  onOptionSelect,
  questionNumber,
  totalQuestions,
  type = 'single',
  explanation,
}: QuizCardProps) {
  const isSelected = (index: number) => {
    if (Array.isArray(selectedOption)) {
      return selectedOption.includes(index)
    }
    return selectedOption === index
  }

  const typeLabel = {
    single: 'Single Choice',
    multiple: 'Multiple Choice',
    code: 'Code Question',
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
        <span className="px-4 py-1.5 bg-primary-100 text-primary-700 rounded-full text-sm font-medium">
          {typeLabel[type]}
        </span>
      </div>

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
                'bg-white hover:bg-primary-50',
                isSelected(index)
                  ? 'border-primary-500 bg-primary-500 text-white shadow-lg'
                  : 'border-gray-200 hover:border-primary-500'
              )}
            >
              <span className="font-semibold mr-3">{String.fromCharCode(65 + index)}.</span>
              {option}
            </motion.button>
          ))}
        </div>
      )}

      {explanation && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: 'auto' }}
          className="mt-6 p-4 bg-blue-50 border-l-4 border-blue-500 rounded-r-lg"
        >
          <p className="text-sm text-blue-800">
            <strong>💡 Explanation:</strong> {explanation}
          </p>
        </motion.div>
      )}
    </motion.div>
  )
}
