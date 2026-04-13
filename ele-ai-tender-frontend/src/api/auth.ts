import request from '@/utils/request'
import type { LoginRequest, PhoneLoginRequest, SendSmsCodeRequest, LoginResult, ApiResponse } from '@/types/auth'

export const authApi = {
  // 用户名密码登录
  login(params: LoginRequest): Promise<ApiResponse<LoginResult>> {
    return request.post('/api/auth/login', params)
  },

  // 发送手机验证码
  sendSmsCode(params: SendSmsCodeRequest): Promise<ApiResponse<string>> {
    return request.post('/api/auth/send-sms-code', params)
  },

  // 手机验证码登录
  phoneLogin(params: PhoneLoginRequest): Promise<ApiResponse<LoginResult>> {
    return request.post('/api/auth/phone-login', params)
  },

  // 获取当前用户信息
  getUserInfo(): Promise<ApiResponse<LoginResult['userInfo']>> {
    return request.get('/api/auth/info')
  },

  // 获取当前用户菜单
  getUserMenus(): Promise<ApiResponse<any[]>> {
    return request.get('/api/auth/menus')
  },

  // 登出
  logout(): Promise<ApiResponse<void>> {
    return request.post('/api/auth/logout')
  },
}
