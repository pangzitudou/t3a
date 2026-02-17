import { create } from 'zustand'
import { QuizSession, Question } from '../services/quiz'

interface QuizState {
  sessionKey: string | null
  session: QuizSession | null
  questions: Question[]
  currentQuestionIndex: number
  answers: Map<number, any>
  timeRemaining: number
  isSubmitted: boolean
  isConnected: boolean

  setSession: (session: QuizSession) => void
  setQuestions: (questions: Question[]) => void
  setCurrentQuestion: (index: number) => void
  setAnswer: (questionIndex: number, answer: any) => void
  setTimeRemaining: (time: number) => void
  submitQuiz: () => void
  resetQuiz: () => void
  setConnected: (connected: boolean) => void
}

export const useQuizStore = create<QuizState>((set) => ({
  // Initial state
  sessionKey: null,
  session: null,
  questions: [],
  currentQuestionIndex: 0,
  answers: new Map(),
  timeRemaining: 0,
  isSubmitted: false,
  isConnected: false,

  // Actions
  setSession: (session) =>
    set({
      session,
      sessionKey: session.sessionKey,
      timeRemaining: session.timeRemaining,
    }),

  setQuestions: (questions) =>
    set({
      questions,
      answers: new Map(),
      currentQuestionIndex: 0,
      isSubmitted: false,
    }),

  setCurrentQuestion: (index) =>
    set({ currentQuestionIndex: index }),

  setAnswer: (questionIndex, answer) =>
    set((state) => {
      const newAnswers = new Map(state.answers)
      newAnswers.set(questionIndex, answer)
      return { answers: newAnswers }
    }),

  setTimeRemaining: (time) =>
    set({ timeRemaining: time }),

  submitQuiz: () =>
    set({ isSubmitted: true }),

  resetQuiz: () =>
    set({
      sessionKey: null,
      session: null,
      questions: [],
      currentQuestionIndex: 0,
      answers: new Map(),
      timeRemaining: 0,
      isSubmitted: false,
      isConnected: false,
    }),

  setConnected: (connected) =>
    set({ isConnected: connected }),
}))

// Helper to get the number of answered questions
export const getAnsweredCount = (answers: Map<number, any>): number => {
  return Array.from(answers.values()).filter(
    (answer) => answer !== undefined && answer !== null
  ).length
}
