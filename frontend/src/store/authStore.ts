import { create } from 'zustand'
import type { UserDto } from '../api/types'
import { tokenStore } from '../api/tokenStore'

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
    tokenStore.set(token)
    set({ token, user })
  },

  clearAuth: () => {
    tokenStore.set(null)
    set({ token: null, user: null })
  },
}))
