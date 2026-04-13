import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo, MenuInfo, AuthUserInfo } from '@/types'
import { storage } from '@/utils/storage'
import { authApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(storage.getToken() || '')
  const userInfo = ref<UserInfo | null>(storage.getUser<UserInfo>())
  const menus = ref<MenuInfo[]>([])
  const permissions = ref<string[]>([])

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const realName = computed(() => userInfo.value?.realName || '')

  function normalizeUserInfo(raw: AuthUserInfo | null | undefined): UserInfo | null {
    if (!raw) return null
    return {
      id: raw.id ?? raw.userId ?? 0,
      username: raw.username,
      realName: raw.realName,
      email: raw.email,
      phone: raw.phone,
      status: 1,
      roleIds: [],
    }
  }

  // 登录
  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    token.value = res.data.token
    userInfo.value = normalizeUserInfo(res.data.userInfo)
    permissions.value = res.data.permissions || []
    storage.setToken(res.data.token)
    storage.setUser(userInfo.value)
    return res
  }

  // 获取用户信息
  async function getUserInfo() {
    const res = await authApi.getUserInfo()
    userInfo.value = normalizeUserInfo(res.data as AuthUserInfo)
    storage.setUser(userInfo.value)
    return res
  }

  // 获取用户菜单
  async function getUserMenus() {
    const res = await authApi.getUserMenus()
    menus.value = res.data || []
    // 提取权限标识
    extractPermissions(menus.value)
    return res
  }

  // 提取权限标识
  function extractPermissions(menuList: MenuInfo[]) {
    const perms: string[] = []
    const extract = (items: MenuInfo[]) => {
      items.forEach(item => {
        if (item.permission) {
          perms.push(item.permission)
        }
        if (item.children && item.children.length > 0) {
          extract(item.children)
        }
      })
    }
    extract(menuList || [])
    permissions.value = perms
  }

  // 检查权限
  function hasPermission(permission: string): boolean {
    return permissions.value.includes(permission)
  }

  // 登出
  function logout() {
    token.value = ''
    userInfo.value = null
    menus.value = []
    permissions.value = []
    storage.clear()
    router.push('/login')
  }

  return {
    token,
    userInfo,
    menus,
    permissions,
    isLoggedIn,
    username,
    realName,
    login,
    getUserInfo,
    getUserMenus,
    hasPermission,
    logout,
  }
})
