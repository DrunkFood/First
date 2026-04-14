// 操作日志信息
export interface OperationLogInfo {
  id: number
  userId?: number
  userName?: string
  operation?: string
  method?: string
  params?: string
  ip?: string
  executeTime?: number
  createTime?: string
}

// 操作日志查询参数
export interface OperationLogQueryParams {
  pageNum: number
  pageSize: number
  userName?: string
  operation?: string
}
