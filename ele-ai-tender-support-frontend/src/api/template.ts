import { request } from '@/utils/request'
import type {
  TemplateInfo,
  TemplateQueryParams,
  TemplateCreateParams,
  TemplateUpdateParams,
} from '@/types/template'
import type { ApiResponse, PageResult } from '@/types'

export const templateApi = {
  // 分页查询模板
  getList(params: TemplateQueryParams): Promise<ApiResponse<PageResult<TemplateInfo>>> {
    return request.get<PageResult<TemplateInfo>>('/template-configs', { params })
  },

  // 获取模板详情
  getById(id: number): Promise<ApiResponse<TemplateInfo>> {
    return request.get<TemplateInfo>(`/template-configs/${id}`)
  },

  // 创建模板
  create(params: TemplateCreateParams): Promise<ApiResponse<TemplateInfo>> {
    return request.post<TemplateInfo>('/template-configs', params)
  },

  // 更新模板
  update(params: TemplateUpdateParams): Promise<ApiResponse<void>> {
    return request.put(`/template-configs/${params.id}`, params)
  },

  // 删除模板
  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/template-configs/${id}`)
  },

  // 批量删除模板
  deleteByIds(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/template-configs/batch', { data: ids })
  },

  // 设为默认模板
  setDefault(id: number): Promise<ApiResponse<void>> {
    return request.put(`/template-configs/${id}/set-default`)
  },
}
