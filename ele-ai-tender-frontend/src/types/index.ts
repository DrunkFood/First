export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageParams {
  pageNum: number
  pageSize: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
