import { request } from '@/utils/request'
import type { LoginParams, LoginResult, UserInfo, MenuInfo, ApiResponse } from '@/types'

export const authApi = {
  // 登录
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
  
  // 修改密码
  changePassword(oldPassword: string, newPassword: string): Promise<ApiResponse<void>> {
    return request.post('/auth/change-password', { oldPassword, newPassword })
  },
  
  // 登出
  logout(): Promise<ApiResponse<void>> {
    return request.post('/auth/logout')
  },
}
