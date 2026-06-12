import request from '@/utils/request'
import type { PageResult } from '@/types'
import type { PolicyFileVO, PolicyFileRequest, KnowledgeDocumentPolicyVO } from '@/types/policy-file'

export const policyFileApi = {
  /** 分页查询政策文件 */
  getList(params: { pageNum: number; pageSize: number; fileName?: string; fileCategory?: string }) {
    return request.get<any, PageResult<PolicyFileVO>>('/core-api/v1/policy-files', { params })
  },

  /** 根据ID查询 */
  getById(id: number) {
    return request.get<any, PolicyFileVO>(`/core-api/v1/policy-files/${id}`)
  },

  /** 创建政策文件 */
  create(data: PolicyFileRequest) {
    return request.post<any, PolicyFileVO>('/core-api/v1/policy-files', data)
  },

  /** 删除政策文件 */
  deleteById(id: number) {
    return request.delete(`/core-api/v1/policy-files/${id}`)
  },

  /** 设置状态 */
  setStatus(id: number, status: number) {
    return request.put(`/core-api/v1/policy-files/${id}/status`, undefined, { params: { status } })
  },

  /** 获取所有可用政策文件（平台+用户） */
  getAllAvailable(applicableCategory?: string) {
    return request.get<any, PolicyFileVO[]>('/core-api/v1/policy-files/all', {
      params: { applicableCategory },
    })
  },

  /** 获取知识库中所有政策类文档 */
  getKnowledgePolicyDocuments() {
    return request.get<any, KnowledgeDocumentPolicyVO[]>('/core-api/v1/policy-files/knowledge-policy')
  },
}
