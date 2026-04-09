import { request } from '@/utils/request'
import type { MenuInfo, ApiResponse } from '@/types'

export interface CreateMenuParams {
  parentId: number
  menuName: string
  menuType: number
  path?: string
  component?: string
  permission?: string
  icon?: string
  sortOrder: number
  visible?: number
}

export interface UpdateMenuParams {
  id: number
  parentId?: number
  menuName?: string
  menuType?: number
  path?: string
  component?: string
  permission?: string
  icon?: string
  sortOrder?: number
  visible?: number
  status?: number
}

interface BackendMenu {
  id: number
  parentId: number
  menuName: string
  menuCode?: string
  menuType: number
  menuUrl?: string
  permission?: string
  icon?: string
  sortOrder: number
  status: number
  children?: BackendMenu[]
}

const mapMenu = (item: BackendMenu): MenuInfo => ({
  id: item.id,
  parentId: item.parentId,
  menuName: item.menuName,
  menuType: item.menuType,
  path: item.menuUrl,
  component: item.menuCode,
  permission: item.permission,
  icon: item.icon,
  sortOrder: item.sortOrder,
  visible: item.status === 1 ? 1 : 0,
  status: item.status,
  children: item.children?.map(mapMenu),
})

export const menuApi = {
  // 获取菜单树
  async getTree(): Promise<ApiResponse<MenuInfo[]>> {
    const res = await request.get<BackendMenu[]>('/menus/tree')
    return { ...res, data: (res.data || []).map(mapMenu) }
  },
  
  // 获取菜单列表（平铺）
  async getList(): Promise<ApiResponse<MenuInfo[]>> {
    const res = await request.get<BackendMenu[]>('/menus')
    return { ...res, data: (res.data || []).map(mapMenu) }
  },
  
  // 获取菜单详情
  async getById(id: number): Promise<ApiResponse<MenuInfo>> {
    const res = await request.get<BackendMenu>(`/menus/${id}`)
    return { ...res, data: mapMenu(res.data) }
  },
  
  // 创建菜单
  create(params: CreateMenuParams): Promise<ApiResponse<MenuInfo>> {
    return request.post<MenuInfo>('/menus', {
      parentId: params.parentId,
      menuName: params.menuName,
      menuType: params.menuType === 0 ? 1 : params.menuType,
      menuUrl: params.path,
      menuCode: params.component,
      permission: params.permission,
      icon: params.icon,
      sortOrder: params.sortOrder,
      status: params.visible === 0 ? 0 : 1,
    })
  },
  
  // 更新菜单
  update(params: UpdateMenuParams): Promise<ApiResponse<void>> {
    return request.put(`/menus/${params.id}`, {
      parentId: params.parentId,
      menuName: params.menuName,
      menuType: params.menuType === 0 ? 1 : params.menuType,
      menuUrl: params.path,
      menuCode: params.component,
      permission: params.permission,
      icon: params.icon,
      sortOrder: params.sortOrder,
      status: params.status ?? (params.visible === 0 ? 0 : 1),
    })
  },
  
  // 删除菜单
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/menus/${id}`)
  },
}
