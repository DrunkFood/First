import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { UserInfo } from '@/types/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)
  const userRoles = computed(() => userInfo.value?.roles || [])

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    // request 拦截器已返回 data，res 就是 LoginResult
    token.value = res.token
    userInfo.value = res.userInfo
    permissions.value = res.permissions || []

    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res.userInfo))

    return res
  }

  async function phoneLogin(phone: string, code: string) {
    const res = await authApi.phoneLogin({ phone, code })
    // request 拦截器已返回 data，res 就是 LoginResult
    token.value = res.token
    userInfo.value = res.userInfo
    permissions.value = res.permissions || []

    localStorage.setItem('token', res.token)
    localStorage.setItem('userInfo', JSON.stringify(res.userInfo))

    return res
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    permissions.value = []
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  }

  async function getUserInfo() {
    if (!token.value) return

    try {
      const res = await authApi.getUserInfo()
      // request 拦截器已返回 data，res 就是 UserInfo
      userInfo.value = res
      localStorage.setItem('userInfo', JSON.stringify(res))
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
