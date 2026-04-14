import request from '@/utils/request'
import type { DocumentPreviewVO } from '@/types/document'

export const documentApi = {
  /** 执行文档集成 */
  integrate(projectId: number) {
    return request.post<any, DocumentPreviewVO>(`/core-api/v1/documents/integrate/${projectId}`)
  },

  /** 获取集成预览 */
  getPreview(projectId: number) {
    return request.get<any, DocumentPreviewVO>(`/core-api/v1/documents/preview/${projectId}`)
  },

  /** 导出Word文档 */
  exportWord(projectId: number) {
    return request.get(`/core-api/v1/documents/export/${projectId}`, {
      responseType: 'blob',
    })
  },

  /** 编辑集成后的文档内容 */
  editContent(projectId: number, markdownContent: string) {
    return request.put(`/core-api/v1/documents/edit/${projectId}`, { markdownContent })
  },
}
