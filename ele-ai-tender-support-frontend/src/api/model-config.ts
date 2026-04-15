import { request } from '@/utils/request'
import type {
  ModelConfigInfo,
  ModelConfigQueryParams,
  ModelConfigCreateParams,
  ModelConfigUpdateParams,
} from '@/types/model-config'
import type { ApiResponse, PageResult } from '@/types'

export const modelConfigApi = {
  // 分页查询模型配置
  getList(params: ModelConfigQueryParams): Promise<ApiResponse<PageResult<ModelConfigInfo>>> {
    return request.get<PageResult<ModelConfigInfo>>('/v1/model-configs', { params })
  },

  // 获取模型配置详情
  getById(id: number): Promise<ApiResponse<ModelConfigInfo>> {
    return request.get<ModelConfigInfo>(`/v1/model-configs/${id}`)
  },

  // 创建模型配置
  create(params: ModelConfigCreateParams): Promise<ApiResponse<ModelConfigInfo>> {
    return request.post<ModelConfigInfo>('/v1/model-configs', params)
  },

  // 更新模型配置
  update(params: ModelConfigUpdateParams): Promise<ApiResponse<void>> {
    return request.put(`/v1/model-configs/${params.id}`, params)
  },

  // 删除模型配置
  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/model-configs/${id}`)
  },

  // 批量删除模型配置
  deleteByIds(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/v1/model-configs/batch', { data: ids })
  },

  // 激活/停用模型配置
  changeStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/v1/model-configs/${id}/status`, undefined, { params: { status } })
  },

  // 设置为活跃模型
  setActive(id: number): Promise<ApiResponse<void>> {
    return request.put(`/v1/model-configs/${id}/set-active`)
  },
}
