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

/** 消息类型常量 */
export const MESSAGE_TYPES = [
  { value: 'SYSTEM', label: '系统通知' },
  { value: 'DETECTION', label: '检测通知' },
  { value: 'PROJECT', label: '项目通知' },
] as const
