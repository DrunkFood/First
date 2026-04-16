import request from '@/utils/request'
import type { FeedbackSubmitRequest, FeedbackVO } from '@/types/feedback'

export const feedbackApi = {
  /** 提交或更新反馈 */
  submit(data: FeedbackSubmitRequest) {
    return request.post<any, FeedbackVO>('/core-api/v1/feedback', data)
  },

  /** 查询当前用户对某个目标的反馈状态 */
  getUserFeedback(params: { taskId?: number; feedbackScene: string; chatMessageId?: string }) {
    return request.get<any, FeedbackVO | null>('/core-api/v1/feedback', { params })
  },
}
