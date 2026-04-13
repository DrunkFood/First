import request from '@/utils/request'
import type { ReviewItemTree } from '@/types/review'

export const reviewApi = {
  getTree(projectId: number) {
    return request.get<any, ReviewItemTree[]>(`/core-api/v1/review-items/tree/${projectId}`)
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
}
