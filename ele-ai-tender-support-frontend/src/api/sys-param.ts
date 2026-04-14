import { request } from '@/utils/request'
import type { SysParamInfo } from '@/types/sys-param'
import type { ApiResponse } from '@/types'

export const sysParamApi = {
  // 按分组查询参数
  getList(paramGroup?: string): Promise<ApiResponse<SysParamInfo[]>> {
    return request.get<SysParamInfo[]>('/sys-params', { params: { paramGroup } })
  },

  // 获取单个参数值
  getValue(paramKey: string): Promise<ApiResponse<string>> {
    return request.get<string>('/sys-params/value', { params: { paramKey } })
  },

  // 批量更新参数
  batchUpdate(params: Record<string, string>): Promise<ApiResponse<void>> {
    return request.put('/sys-params', params)
  },

  // 更新单个参数
  updateByKey(paramKey: string, paramValue: string): Promise<ApiResponse<void>> {
    return request.put(`/sys-params/${paramKey}`, { paramValue })
  },
}
