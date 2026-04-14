import { request } from '@/utils/request'
import type { PolicyFileInfo, PolicyFileQueryParams, PolicyFileCreateParams } from '@/types/policy-file'
import type { ApiResponse, PageResult } from '@/types'

export const policyFileApi = {
  getList(params: PolicyFileQueryParams): Promise<ApiResponse<PageResult<PolicyFileInfo>>> {
    return request.get<PageResult<PolicyFileInfo>>('/policy-files', { params })
  },

  getById(id: number): Promise<ApiResponse<PolicyFileInfo>> {
    return request.get<PolicyFileInfo>(`/policy-files/${id}`)
  },

  create(params: PolicyFileCreateParams): Promise<ApiResponse<PolicyFileInfo>> {
    return request.post<PolicyFileInfo>('/policy-files', params)
  },

  update(id: number, params: Partial<PolicyFileCreateParams>): Promise<ApiResponse<void>> {
    return request.put(`/policy-files/${id}`, params)
  },

  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/policy-files/${id}`)
  },

  setStatus(id: number, status: number): Promise<ApiResponse<void>> {
    return request.put(`/policy-files/${id}/status`, undefined, { params: { status } })
  },
}
