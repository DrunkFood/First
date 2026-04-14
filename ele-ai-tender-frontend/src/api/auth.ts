import request from '@/utils/request'
import type { LoginRequest, PhoneLoginRequest, SendSmsCodeRequest, LoginResult } from '@/types/auth'

export const authApi = {
  // 用户名密码登录 (request拦截器已返回data，这里直接返回LoginResult)
  login(params: LoginRequest): Promise<LoginResult> {
    return request.post('/support-api/auth/login', params)
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

  // 登出
  logout(): Promise<void> {
    return request.post('/support-api/auth/logout')
  },
}
