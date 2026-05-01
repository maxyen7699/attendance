import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, refreshToken as apiRefresh } from '@/api/auth'
import { getMe } from '@/api/users'

export interface User {
  id: number
  username: string
  name: string
  email: string
  department: string
  position: string
  role: 'ADMIN' | 'USER'
  active: boolean
  annualLeaveDays: number
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref(localStorage.getItem('accessToken') || '')
  const refreshTokenVal = ref(localStorage.getItem('refreshToken') || '')
  const user = ref<User | null>(null)

  async function login(username: string, password: string) {
    const res = await apiLogin(username, password)
    accessToken.value = res.data.data.accessToken
    refreshTokenVal.value = res.data.data.refreshToken
    localStorage.setItem('accessToken', accessToken.value)
    localStorage.setItem('refreshToken', refreshTokenVal.value)
    await fetchUser()
  }

  async function refresh() {
    const res = await apiRefresh(refreshTokenVal.value)
    accessToken.value = res.data.data.accessToken
    refreshTokenVal.value = res.data.data.refreshToken
    localStorage.setItem('accessToken', accessToken.value)
    localStorage.setItem('refreshToken', refreshTokenVal.value)
  }

  async function fetchUser() {
    const res = await getMe()
    user.value = res.data.data
  }

  function logout() {
    accessToken.value = ''
    refreshTokenVal.value = ''
    user.value = null
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  const isLoggedIn = () => !!accessToken.value

  return { accessToken, refreshToken: refreshTokenVal, user, login, refresh, fetchUser, logout, isLoggedIn }
})
