import { create } from 'zustand'
import type { UserDto } from '../api/types'

interface AuthState {
  token: string | null
  user: UserDto | null
  setAuth: (token: string, user: UserDto) => void
  clearAuth: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  user: null,

  setAuth: (token, user) => {
    // Keep token in memory AND expose on window for the Axios interceptor
    // (avoids circular import between store and client.ts)
    ;(window as unknown as Record<string, unknown>).__authToken = token
    set({ token, user })
  },

  clearAuth: () => {
    ;(window as unknown as Record<string, unknown>).__authToken = null
    set({ token: null, user: null })
  },
}))
