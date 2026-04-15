import { request } from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo, MenuInfo, ApiResponse } from '@/types'
import { encryptByPublicKey, formatPublicKey } from '@/utils/crypto'

export interface PublicKeyInfo {
  keyId: string
  publicKey: string
}

export const authApi = {
  // 获取RSA公钥（用于密码加密）
  getPublicKey(): Promise<ApiResponse<PublicKeyInfo>> {
    return request.get<PublicKeyInfo>('/auth/public-key')
  },

  // 登录（密码已RSA加密）
  login(params: LoginParams): Promise<ApiResponse<LoginResult>> {
    return request.post<LoginResult>('/auth/login', params)
  },

  // 获取当前用户信息
  getUserInfo(): Promise<ApiResponse<UserInfo>> {
    return request.get<UserInfo>('/auth/info')
  },

  // 获取用户菜单
  getUserMenus(): Promise<ApiResponse<MenuInfo[]>> {
    return request.get<MenuInfo[]>('/auth/menus')
  },

  // 修改密码（密码RSA加密后传输）
  async changePassword(oldPassword: string, newPassword: string): Promise<ApiResponse<void>> {
    // 获取公钥
    const keyRes = await authApi.getPublicKey()
    const { keyId, publicKey } = keyRes.data
    const formattedKey = formatPublicKey(keyId, publicKey)
    
    // 加密密码
    const encryptedOld = encryptByPublicKey(formattedKey, oldPassword)
    const encryptedNew = encryptByPublicKey(formattedKey, newPassword)
    
    if (!encryptedOld || !encryptedNew) {
      throw new Error('密码加密失败')
    }
    
    return request.post('/auth/change-password', { 
      keyId, 
      oldPassword: encryptedOld, 
      newPassword: encryptedNew 
    })
  },

  // 登出
  logout(): Promise<ApiResponse<void>> {
    return request.post('/auth/logout')
  },
}
