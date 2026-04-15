import { request } from '@/utils/request'
import type {
  KnowledgeDocumentInfo,
  KnowledgeDocumentQueryParams,
  KnowledgeDocumentCreateParams,
  KnowledgeDocumentUpdateParams,
} from '@/types/knowledge'
import type { ApiResponse, PageResult } from '@/types'

export const knowledgeApi = {
  // 分页查询知识文档
  getList(params: KnowledgeDocumentQueryParams): Promise<ApiResponse<PageResult<KnowledgeDocumentInfo>>> {
    return request.get<PageResult<KnowledgeDocumentInfo>>('/v1/knowledge-configs', { params })
  },

  // 获取知识文档详情
  getById(id: number): Promise<ApiResponse<KnowledgeDocumentInfo>> {
    return request.get<KnowledgeDocumentInfo>(`/v1/knowledge-configs/${id}`)
  },

  // 创建知识文档
  create(params: KnowledgeDocumentCreateParams): Promise<ApiResponse<KnowledgeDocumentInfo>> {
    return request.post<KnowledgeDocumentInfo>('/v1/knowledge-configs', params)
  },

  // 更新知识文档
  update(params: KnowledgeDocumentUpdateParams): Promise<ApiResponse<void>> {
    return request.put(`/v1/knowledge-configs/${params.id}`, params)
  },

  // 删除知识文档
  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/knowledge-configs/${id}`)
  },

  // 批量删除知识文档
  deleteByIds(ids: number[]): Promise<ApiResponse<void>> {
    return request.delete('/v1/knowledge-configs/batch', { data: ids })
  },
}
