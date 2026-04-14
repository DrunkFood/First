/** 消息 */
export interface MessageVO {
  id: number
  userId: number
  title: string
  content: string
  messageType: string
  bizId?: number
  bizType?: string
  isRead: number
  readTime?: string
  createTime: string
}
