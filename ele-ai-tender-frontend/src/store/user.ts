import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const userRoles = computed(() => userInfo.value?.roles || [])

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    permissions.value = res.data.permissions || []

    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))

    return res
  }

  async function phoneLogin(phone: string, code: string) {
    const res = await authApi.phoneLogin({ phone, code })
    token.value = res.data.token
    userInfo.value = res.data.userInfo
    permissions.value = res.data.permissions || []

    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data.userInfo))

    return res
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }

  async function getUserInfo() {
    if (!token.value) return

    try {
      const res = await authApi.getUserInfo()
      userInfo.value = res.data
      localStorage.setItem('userInfo', JSON.stringify(res.data))
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  return {
    token,
    userInfo,
    permissions,
    isLoggedIn,
    userRoles,
    login,
    phoneLogin,
    logout,
    getUserInfo,
  }
})
