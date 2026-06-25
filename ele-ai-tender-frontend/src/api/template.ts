import request from '@/utils/request'
import type { PageResult } from '@/types'
import type { TemplateInfo } from '@/types/template'

export const templateApi = {
  getList(params: any) {
    return request.get<any, PageResult<TemplateInfo>>('/core-api/v1/templates', { params })
  },
  getById(id: number) {
    return request.get<any, TemplateInfo>(`/core-api/v1/templates/${id}`)
  },
  getDefault(projectCategory?: string) {
    return request.get<any, TemplateInfo>('/core-api/v1/templates/default', {
      params: { projectCategory },
    })
  },
}
