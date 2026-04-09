import { request } from '@/utils/request'
import type { UserInfo, PageParams, PageResult, ApiResponse } from '@/types'

export interface UserQueryParams extends PageParams {
  username?: string
  realName?: string
  status?: number
}

export interface CreateUserParams {
  username: string
  password: string
  realName: string
  email?: string
  phone?: string
  roleIds: number[]
}

export interface UpdateUserParams {
  id: number
  realName?: string
  email?: string
  phone?: string
  status?: number
  roleIds?: number[]
}

export const userApi = {
  // 分页查询用户
  getList(params: UserQueryParams): Promise<ApiResponse<PageResult<UserInfo>>> {
    return request.get<PageResult<UserInfo>>('/users', { params })
  },
  
  // 获取用户详情
  getById(id: number): Promise<ApiResponse<UserInfo>> {
    return request.get<UserInfo>(`/users/${id}`)
  },
  
  // 创建用户
  create(params: CreateUserParams): Promise<ApiResponse<UserInfo>> {
    return request.post<UserInfo>('/users', params)
  },
  
  // 更新用户
  update(params: UpdateUserParams): Promise<ApiResponse<void>> {
    return request.put(`/users/${params.id}`, params)
  },
  
  // 删除用户
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/users/${id}`)
  },
  
  // 重置密码
  resetPassword(id: number, newPassword: string): Promise<ApiResponse<void>> {
    return request.post(`/users/${id}/reset-password`, undefined, { params: { newPassword } })
  },
  
  // 启用/禁用用户
  changeStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/users/${id}/status`, undefined, { params: { status } })
  },
}
