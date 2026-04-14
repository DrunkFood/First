import { request } from '@/utils/request'
import type { MessageInfo, MessageQueryParams } from '@/types/message'
import type { ApiResponse, PageResult } from '@/types'

export const messageApi = {
  getList(params: MessageQueryParams): Promise<ApiResponse<PageResult<MessageInfo>>> {
    return request.get<PageResult<MessageInfo>>('/messages', { params })
  },

  getUnreadCount(): Promise<ApiResponse<number>> {
    return request.get<number>('/messages/unread-count')
  },

  markRead(id: number): Promise<ApiResponse<void>> {
    return request.put(`/messages/${id}/read`)
  },

  markAllRead(): Promise<ApiResponse<void>> {
    return request.put('/messages/read-all')
  },

  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/messages/${id}`)
  },
}
