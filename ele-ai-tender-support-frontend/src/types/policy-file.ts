// 政策文件信息
export interface PolicyFileInfo {
  id: number
  fileName: string
  fileCategory: string
  applicableCategory?: string
  fileId?: number
  fileSize?: number
  fileType?: string
  description?: string
  status: number
  createTime?: string
  createName?: string
}

// 政策文件查询参数
export interface PolicyFileQueryParams {
  pageNum: number
  pageSize: number
  fileCategory?: string
  applicableCategory?: string
}

// 政策文件创建参数
export interface PolicyFileCreateParams {
  fileName: string
  fileCategory: string
  applicableCategory?: string
  fileId?: number
  fileSize?: number
  fileType?: string
  description?: string
}
