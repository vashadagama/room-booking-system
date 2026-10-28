import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login, register, getMe } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const user = ref(null)
  const loading = ref(false)
  const error = ref(null)

  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')

  async function loginUser(credentials) {
    loading.value = true
    error.value = null
    try {
      const res = await login(credentials)
      token.value = res.data.token
      user.value = res.data
      localStorage.setItem('token', res.data.token)
    } catch (e) {
      error.value = e.response?.data?.message || e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function registerUser(data) {
    loading.value = true
    error.value = null
    try {
      const res = await register(data)
      token.value = res.data.token
      user.value = res.data
      localStorage.setItem('token', res.data.token)
    } catch (e) {
      error.value = e.response?.data?.message || e.message
      throw e
    } finally {
      loading.value = false
    }
  }

  async function fetchUser() {
    if (!token.value) return
    try {
      const res = await getMe()
      user.value = res.data
    } catch (e) {
      logout()
    }
  }

  let userPromise = null

  function ensureUserLoaded() {
    if (!token.value) return Promise.resolve(null)
    if (user.value) return Promise.resolve(user.value)

    if (!userPromise) {
      userPromise = getMe()
        .then((res) => {
          user.value = res.data
          return user.value
        })
        .catch(() => {
          logout()
          return null
        })
        .finally(() => {
          userPromise = null
        })
    }

    return userPromise
  }

  function logout() {
    token.value = null
    user.value = null
    error.value = null
    localStorage.removeItem('token')
  }

  return {
    token, user, loading, error, isAuthenticated, isAdmin,
    loginUser, registerUser, fetchUser, ensureUserLoaded, logout
  }
})
