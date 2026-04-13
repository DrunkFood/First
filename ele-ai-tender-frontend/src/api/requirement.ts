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
}
