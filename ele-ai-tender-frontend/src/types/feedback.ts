/** 反馈类型 */
export type FeedbackType = 'LIKE' | 'DISLIKE'

/** 反馈场景 */
export type FeedbackScene = 'GENERATION_CONTENT' | 'CHAT_MESSAGE'

/** 提交反馈请求 */
export interface FeedbackSubmitRequest {
  feedbackType: FeedbackType
  feedbackScene: FeedbackScene
  taskId?: number
  chatMessageId?: string
  chatContent?: string
  reason?: string
}

/** 反馈信息响应 */
export interface FeedbackVO {
  id: number
  feedbackType: FeedbackType
  feedbackScene: FeedbackScene
  reason?: string
}
