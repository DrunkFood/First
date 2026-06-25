import request from '@/utils/request'
import type { ReviewItemTree } from '@/types/review'

export const reviewApi = {
  getTree(projectId: number) {
    return request.get<any, ReviewItemTree[]>(`/core-api/v1/review-items/${projectId}`)
  },
  create(data: any) {
    return request.post('/core-api/v1/review-items', data)
  },
  update(id: number, data: any) {
    return request.put(`/core-api/v1/review-items/${id}`, data)
  },
  deleteById(id: number) {
    return request.delete(`/core-api/v1/review-items/${id}`)
  },
  /** 提交AI生成评审项 */
  generate(projectId: number, params: Record<string, any>) {
    return request.post<any, any>(`/core-api/v1/review-items/${projectId}/generate`, params)
  },
  /** 批量创建 */
  batchCreate(items: any[]) {
    return request.post('/core-api/v1/review-items/batch', items)
  },
  /** 批量更新 */
  batchUpdate(items: any[]) {
    return request.put('/core-api/v1/review-items/batch', items)
  },
  /** 替换项目全部评审项 */
  replaceAll(projectId: number, items: any[]) {
    return request.put(`/core-api/v1/review-items/${projectId}/replace`, items)
  },
}
