/** 政策文件 */
export interface PolicyFileVO {
  id: number
  fileName: string
  fileCategory: string
  applicableCategory: string
  fileId: number
  fileSize: number
  fileType: string
  description?: string
  userId?: number
  status: number
  source: 'SYSTEM' | 'USER'
  createTime: string
}

/** 政策文件请求 */
export interface PolicyFileRequest {
  fileName: string
  fileCategory: string
  applicableCategory: string
  fileId: number
  fileSize: number
  fileType: string
  description?: string
}
