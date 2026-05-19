import axios from 'axios'

// Axios instance with JWT interceptor.
// Token is injected from the auth store at request time to avoid circular deps.
const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: inject Bearer token from session storage / in-memory store
apiClient.interceptors.request.use((config) => {
  // Token is set on window.__authToken by authStore to avoid circular import
  const token = (window as unknown as Record<string, string>).__authToken
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// Response interceptor: on 401 clear auth and redirect to login
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Clear in-memory token
      ;(window as unknown as Record<string, unknown>).__authToken = null
      // Redirect to login page (React Router is not available here, use window.location)
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
