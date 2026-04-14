import { request } from '@/utils/request'
import type { ModelRouteRuleInfo, ModelRouteQueryParams, ModelRouteRuleParams } from '@/types/model-route'
import type { ApiResponse, PageResult } from '@/types'

export const modelRouteApi = {
  getList(params: ModelRouteQueryParams): Promise<ApiResponse<PageResult<ModelRouteRuleInfo>>> {
    return request.get<PageResult<ModelRouteRuleInfo>>('/model-routes', { params })
  },

  create(params: ModelRouteRuleParams): Promise<ApiResponse<ModelRouteRuleInfo>> {
    return request.post<ModelRouteRuleInfo>('/model-routes', params)
  },

  update(id: number, params: ModelRouteRuleParams): Promise<ApiResponse<void>> {
    return request.put(`/model-routes/${id}`, params)
  },

  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/model-routes/${id}`)
  },

  setActive(id: number, isActive: number): Promise<ApiResponse<void>> {
    return request.put(`/model-routes/${id}/active`, undefined, { params: { isActive } })
  },
}
