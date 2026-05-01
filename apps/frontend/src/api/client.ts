import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

const client = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' }
})

client.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

client.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const auth = useAuthStore()
      try {
        await auth.refresh()
        error.config.headers.Authorization = `Bearer ${auth.accessToken}`
        return client(error.config)
      } catch {
        auth.logout()
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
