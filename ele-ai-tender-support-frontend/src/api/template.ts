import { request } from '@/utils/request'
import axios from 'axios'
import type {
  TemplateInfo,
  TemplateQueryParams,
  TemplateCreateParams,
  TemplateUpdateParams,
} from '@/types/template'
import type { ApiResponse, PageResult } from '@/types'
import { useUserStore } from '@/store/user'

// 文件服务专用 axios 实例（独立 baseURL，不走 /support-api 代理）
const fileService = axios.create({
  baseURL: '/file-api',
  timeout: 60000,
})

fileService.interceptors.request.use((config) => {
  const userStore = useUserStore()
  const token = userStore.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

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

  // 设置模板状态
  setStatus(id: number, status: string): Promise<ApiResponse<void>> {
    return request.put(`/v1/template-configs/${id}/status`, undefined, { params: { status } })
  },
}

// 模板文件上传 API
export const templateFileApi = {
  upload(file: File, bizType: string) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('bizType', bizType)
    return fileService.post<ApiResponse<{ fileId: number; fileName: string }>>(
      '/api/file/upload',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } },
    ).then(res => res.data)
  },
}
