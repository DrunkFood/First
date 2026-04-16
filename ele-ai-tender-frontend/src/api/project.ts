import request from '@/utils/request'
import type { PageResult } from '@/types'
import type { ProjectInfo, ProjectQueryParams, ProjectCreateParams, ProjectVersionInfo } from '@/types/project'

export const projectApi = {
  getList(params: ProjectQueryParams) {
    return request.get<any, PageResult<ProjectInfo>>('/core-api/v1/projects', { params })
  },
  getById(id: number) {
    return request.get<any, ProjectInfo>(`/core-api/v1/projects/${id}`)
  },
  create(data: ProjectCreateParams) {
    return request.post<any, ProjectInfo>('/core-api/v1/projects', data)
  },
  update(id: number, data: Partial<ProjectCreateParams>) {
    return request.put(`/core-api/v1/projects/${id}`, data)
  },
  deleteByIds(ids: number[]) {
    return request.delete('/core-api/v1/projects', { data: { ids } })
  },
  getVersions(projectId: number) {
    return request.get<any, ProjectVersionInfo[]>(`/core-api/v1/projects/${projectId}/versions`)
  },
  export(projectId: number) {
    return request.post(`/core-api/v1/projects/${projectId}/export`)
  },
  /** 推进项目阶段（触发器自动发起下一阶段AI任务） */
  advancePhase(id: number, targetPhase: number, context?: Record<string, any>) {
    return request.put(`/core-api/v1/projects/${id}/phase`, { targetPhase, context })
  },
  /** 变更项目状态 */
  changeStatus(id: number, status: string) {
    return request.put(`/core-api/v1/projects/${id}/status`, { status })
  },
  /** 取消项目 */
  cancel(id: number) {
    return request.post(`/core-api/v1/projects/${id}/cancel`)
  },
  /** 发布项目 */
  publish(id: number) {
    return request.post(`/core-api/v1/projects/${id}/publish`)
  },
  /** 归档项目 */
  archive(id: number) {
    return request.post(`/core-api/v1/projects/${id}/archive`)
  },
}
