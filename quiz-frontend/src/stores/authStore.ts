import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface User {
  id: string
  email: string
  nickname: string
  avatar?: string
}

interface AuthState {
  user: User | null
  token: string | null
  isAuthenticated: boolean

  // Actions
  setAuth: (user: User, token: string) => void
  logout: () => void
  updateUser: (user: Partial<User>) => void
}

// Note: Using zustand persist middleware
// If it fails, we'll fall back to localStorage directly
export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      // Initial state
      user: null,
      token: null,
      isAuthenticated: false,

      // Actions
      setAuth: (user, token) =>
        set({
          user,
          token,
          isAuthenticated: true,
        }),

      logout: () =>
        set({
          user: null,
          token: null,
          isAuthenticated: false,
        }),

      updateUser: (userUpdate) =>
        set((state) => ({
          user: state.user ? { ...state.user, ...userUpdate } : null,
        })),
    }),
    {
      name: 'auth-storage',
    }
  )
)
