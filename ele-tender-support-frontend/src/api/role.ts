import { request } from '@/utils/request'
import type { RoleInfo, PageParams, PageResult, ApiResponse } from '@/types'

export interface RoleQueryParams extends PageParams {
  roleName?: string
  roleCode?: string
  status?: number
}

export interface CreateRoleParams {
  roleName: string
  roleCode: string
  description?: string
  menuIds: number[]
}

export interface UpdateRoleParams {
  id: number
  roleName?: string
  description?: string
  status?: number
  menuIds?: number[]
}

export const roleApi = {
  // 分页查询角色
  getList(params: RoleQueryParams): Promise<ApiResponse<PageResult<RoleInfo>>> {
    return request.get<PageResult<RoleInfo>>('/roles', { params })
  },
  
  // 获取所有角色（下拉列表用）
  getAll(): Promise<ApiResponse<RoleInfo[]>> {
    return request.get<RoleInfo[]>('/roles/all')
  },
  
  // 获取角色详情
  getById(id: number): Promise<ApiResponse<RoleInfo>> {
    return request.get<RoleInfo>(`/roles/${id}`)
  },
  
  // 创建角色
  create(params: CreateRoleParams): Promise<ApiResponse<RoleInfo>> {
    return request.post<RoleInfo>('/roles', params)
  },
  
  // 更新角色
  update(params: UpdateRoleParams): Promise<ApiResponse<void>> {
    return request.put(`/roles/${params.id}`, params)
  },
  
  // 删除角色
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/roles/${id}`)
  },
  
  // 获取角色的菜单权限
  getRoleMenus(id: number): Promise<ApiResponse<number[]>> {
    return request.get<number[]>(`/roles/${id}/menus`)
  },
  
  // 分配角色菜单权限
  assignMenus(id: number, menuIds: number[]): Promise<ApiResponse<void>> {
    return request.put(`/roles/${id}/menus`, { menuIds })
  },
}
