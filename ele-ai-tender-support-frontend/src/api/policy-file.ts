import { request } from '@/utils/request'
import type { PolicyFileInfo, PolicyFileQueryParams, PolicyFileCreateParams } from '@/types/policy-file'
import type { ApiResponse, PageResult } from '@/types'

export const policyFileApi = {
  getList(params: PolicyFileQueryParams): Promise<ApiResponse<PageResult<PolicyFileInfo>>> {
    return request.get<PageResult<PolicyFileInfo>>('/v1/policy-files', { params })
  },

  getById(id: number): Promise<ApiResponse<PolicyFileInfo>> {
    return request.get<PolicyFileInfo>(`/v1/policy-files/${id}`)
  },

  create(params: PolicyFileCreateParams): Promise<ApiResponse<PolicyFileInfo>> {
    return request.post<PolicyFileInfo>('/v1/policy-files', params)
  },

  update(id: number, params: Partial<PolicyFileCreateParams>): Promise<ApiResponse<void>> {
    return request.put(`/v1/policy-files/${id}`, params)
  },

  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/policy-files/${id}`)
  },

  setStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/v1/policy-files/${id}/status`, undefined, { params: { status } })
  },
}
