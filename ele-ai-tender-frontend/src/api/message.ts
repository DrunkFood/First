import request from '@/utils/request'
import type { PageResult } from '@/types'
import type { MessageVO } from '@/types/message'

export const messageApi = {
  /** 获取我的消息列表 */
  getMyMessages(params: { pageNum: number; pageSize: number; messageType?: string }) {
    return request.get<any, PageResult<MessageVO>>('/core-api/v1/messages', { params })
  },

  /** 标记消息已读 */
  markRead(id: number) {
    return request.put(`/core-api/v1/messages/${id}/read`)
  },

  /** 全部已读 */
  markAllRead() {
    return request.put('/core-api/v1/messages/read-all')
  },

  /** 获取未读计数 */
  getUnreadCount() {
    return request.get<any, number>('/core-api/v1/messages/unread-count')
  },

  /** 删除消息 */
  deleteById(id: number) {
    return request.delete(`/core-api/v1/messages/${id}`)
  },
}
