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
}
