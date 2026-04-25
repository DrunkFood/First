import request from '@/utils/request'
import type { DocumentPreviewVO } from '@/types/document'
import type { AiTaskVO } from '@/types/ai-task'

export const documentApi = {
  /** 执行文档集成（异步，返回AI任务） */
  integrate(projectId: number) {
    return request.post<any, AiTaskVO>(`/core-api/v1/documents/integrate/${projectId}`)
  },

  /** 获取集成预览 */
  getPreview(projectId: number) {
    return request.get<any, DocumentPreviewVO>(`/core-api/v1/documents/preview/${projectId}`)
  },

  /** 编辑集成后的文档内容 */
  editContent(projectId: number, markdownContent: string) {
    return request.put(`/core-api/v1/documents/edit/${projectId}`, { markdownContent })
  },
}
