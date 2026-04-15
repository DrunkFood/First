import { request } from '@/utils/request'
import type { MessageInfo, MessageQueryParams } from '@/types/message'
import type { ApiResponse, PageResult } from '@/types'

export const messageApi = {
  getList(params: MessageQueryParams): Promise<ApiResponse<PageResult<MessageInfo>>> {
    return request.get<PageResult<MessageInfo>>('/v1/messages', { params })
  },

  getUnreadCount(): Promise<ApiResponse<number>> {
    return request.get<number>('/v1/messages/unread-count')
  },

  markRead(id: number): Promise<ApiResponse<void>> {
    return request.put(`/v1/messages/${id}/read`)
  },

  markAllRead(): Promise<ApiResponse<void>> {
    return request.put('/v1/messages/read-all')
  },

  deleteById(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/v1/messages/${id}`)
  },
}
