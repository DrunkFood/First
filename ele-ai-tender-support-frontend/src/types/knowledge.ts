// 知识库文档信息
export interface KnowledgeDocumentInfo {
  id: number
  documentName: string
  documentCategory: string
  fileType: string
  fileSize: number
  fileUrl?: string
  content?: string
  vectorCollection?: string
  vectorIds?: string
  status: number
  remark?: string
  createTime?: string
  updateTime?: string
  createId?: number
  createName?: string
}

// 知识库文档查询参数
export interface KnowledgeDocumentQueryParams {
  pageNum: number
  pageSize: number
  documentName?: string
  documentCategory?: string
  status?: number
}

// 知识库文档创建参数
export interface KnowledgeDocumentCreateParams {
  documentName: string
  documentCategory: string
  fileType: string
  fileUrl?: string
  content?: string
  remark?: string
}

// 知识库文档更新参数
export interface KnowledgeDocumentUpdateParams {
  id: number
  documentName?: string
  documentCategory?: string
  status?: number
  remark?: string
}
