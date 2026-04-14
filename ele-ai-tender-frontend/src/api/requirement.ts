import request from '@/utils/request'
import type { PageResult } from '@/types'
import type { RequirementInfo, RequirementQueryParams, RequirementCreateParams } from '@/types/requirement'

export const requirementApi = {
  getList(params: RequirementQueryParams) {
    return request.get<any, PageResult<RequirementInfo>>('/core-api/v1/requirements', { params })
  },
  getById(id: number) {
    return request.get<any, RequirementInfo>(`/core-api/v1/requirements/${id}`)
  },
  create(data: RequirementCreateParams) {
    return request.post<any, RequirementInfo>('/core-api/v1/requirements', data)
  },
  update(id: number, data: Partial<RequirementCreateParams>) {
    return request.put(`/core-api/v1/requirements/${id}`, data)
  },
  deleteById(id: number) {
    return request.delete(`/core-api/v1/requirements/${id}`)
  },
  match(id: number, data: any) {
    return request.post(`/core-api/v1/requirements/${id}/match`, data)
  },
  /** 提交AI生成需求 */
  generate(id: number, params: Record<string, any>) {
    return request.post<any, any>(`/core-api/v1/requirements/${id}/generate`, params)
  },
  /** 自动保存 */
  autoSave(id: number, content: string) {
    return request.post(`/core-api/v1/requirements/${id}/auto-save`, { content })
  },
  /** 获取自动保存内容 */
  getAutoSave(id: number) {
    return request.get<any, string | null>(`/core-api/v1/requirements/${id}/auto-save`)
  },
  /** 清除自动保存 */
  clearAutoSave(id: number) {
    return request.delete(`/core-api/v1/requirements/${id}/auto-save`)
  },
  /** 提交需求检测 */
  detect(id: number) {
    return request.post<any, Record<string, number>>(`/core-api/v1/requirements/${id}/detect`)
  },
}
