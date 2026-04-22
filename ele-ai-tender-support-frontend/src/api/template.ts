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
    return request.get<PageResult<TemplateInfo>>('/v1/template-configs', { params })
  },

  // 获取模板详情
  getById(id: number): Promise<ApiResponse<TemplateInfo>> {
    return request.get<TemplateInfo>(`/v1/template-configs/${id}`)
  },

  // 创建模板
  create(params: TemplateCreateParams): Promise<ApiResponse<TemplateInfo>> {
    return request.post<TemplateInfo>('/v1/template-configs', params)
  },

  // 更新模板
  update(params: TemplateUpdateParams): Promise<ApiResponse<void>> {
    return request.put(`/v1/template-configs/${params.id}`, params)
  },

  // 删除模板
  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/template-configs/${id}`)
  },

  // 批量删除模板
  deleteByIds(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/v1/template-configs/batch', { data: ids })
  },

  // 设为默认模板
  setDefault(id: number): Promise<ApiResponse<void>> {
    return request.post(`/v1/template-configs/${id}/set-default`)
  },
}

// 文件上传 API
export const fileApi = {
  upload(file: File, bizType: string) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('bizType', bizType)
    return request.post<{ fileId: number; fileName: string }>('/file-api/api/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
