// 系统消息信息
export interface MessageInfo {
  id: number
  userId?: number
  title: string
  content?: string
  messageType: string
  bizId?: number
  bizType?: string
  isRead: number
  readTime?: string
  createTime?: string
}

// 消息查询参数
export interface MessageQueryParams {
  pageNum: number
  pageSize: number
  messageType?: string
  isRead?: number
}
