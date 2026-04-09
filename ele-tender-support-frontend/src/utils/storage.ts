const TOKEN_KEY = 'ele_bid_token'
const USER_KEY = 'ele_bid_user'

export const storage = {
  // Token 操作
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY)
  },
  
  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token)
  },
  
  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY)
  },
  
  // 用户信息操作
  getUser<T>(): T | null {
    const userStr = localStorage.getItem(USER_KEY)
    if (userStr) {
      try {
        return JSON.parse(userStr) as T
      } catch {
        return null
      }
    }
    return null
  },
  
  setUser<T>(user: T): void {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  },
  
  removeUser(): void {
    localStorage.removeItem(USER_KEY)
  },
  
  // 清除所有存储
  clear(): void {
    this.removeToken()
    this.removeUser()
  },
}

export default storage
