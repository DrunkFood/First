import request from '@/utils/request'
import type { LoginRequest, PhoneLoginRequest, SendSmsCodeRequest, LoginResult } from '@/types/auth'
import { encryptByPublicKey, formatPublicKey } from '@/utils/crypto'

export interface PublicKeyInfo {
  keyId: string
  publicKey: string
}

export const authApi = {
  // 获取RSA公钥（用于密码加密）
  getPublicKey(): Promise<PublicKeyInfo> {
    return request.get<PublicKeyInfo>('/support-api/auth/public-key')
  },

  // 登录（密码RSA加密后传输）
  async login(params: LoginRequest): Promise<LoginResult> {
    // 获取公钥
    const keyRes = await authApi.getPublicKey()
    const { keyId, publicKey } = keyRes
    const formattedKey = formatPublicKey(keyId, publicKey)
    
    // 加密密码
    const encryptedPassword = encryptByPublicKey(formattedKey, params.password)
    if (!encryptedPassword) {
      throw new Error('密码加密失败')
    }
    
    return request.post('/support-api/auth/login', { 
      username: params.username, 
      password: encryptedPassword, 
      keyId 
    })
  },

  // 发送手机验证码
  sendSmsCode(params: SendSmsCodeRequest): Promise<string> {
    return request.post('/support-api/auth/send-sms-code', params)
  },

  // 手机验证码登录
  phoneLogin(params: PhoneLoginRequest): Promise<LoginResult> {
    return request.post('/support-api/auth/phone-login', params)
  },

  // 获取当前用户信息
  getUserInfo(): Promise<LoginResult['userInfo']> {
    return request.get('/support-api/auth/info')
  },

  // 获取当前用户菜单
  getUserMenus(): Promise<any[]> {
    return request.get('/support-api/auth/menus')
  },

  // 修改密码（密码RSA加密后传输）
  async changePassword(oldPassword: string, newPassword: string): Promise<void> {
    // 获取公钥
    const keyRes = await authApi.getPublicKey()
    const { keyId, publicKey } = keyRes
    const formattedKey = formatPublicKey(keyId, publicKey)
    
    // 加密密码
    const encryptedOld = encryptByPublicKey(formattedKey, oldPassword)
    const encryptedNew = encryptByPublicKey(formattedKey, newPassword)
    
    if (!encryptedOld || !encryptedNew) {
      throw new Error('密码加密失败')
    }
    
    return request.post('/support-api/auth/change-password', { 
      keyId, 
      oldPassword: encryptedOld, 
      newPassword: encryptedNew 
    })
  },

  // 登出
  logout(): Promise<void> {
    return request.post('/support-api/auth/logout')
  },
}
