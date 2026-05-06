/** AI对话请求 */
export interface AiChatRequest {
  message: string
  context?: string
  projectId?: number
  requirementId?: number
}

/** AI对话消息 */
export interface AiChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  error?: boolean
  /** 消息唯一标识，用于反馈关联 */
  uid?: string
  /** 是否为欢迎语（不显示反馈按钮、不作为对话历史发送） */
  isGreeting?: boolean
  /** 用户选中的原文，用于后续替换定位。仅用户消息携带 */
  selectedText?: string
}

/** AI优化请求 */
export interface AiOptimizeRequest {
  content: string
  type: 'requirement' | 'review' | 'document'
  projectId?: number
}

/** AI建议请求 */
export interface AiSuggestRequest {
  content: string
  type: 'policy' | 'template' | 'requirement'
  projectId?: number
}

/** AI建议响应 */
export interface AiSuggestResponse {
  suggestions: string[]
}

/** AI匹配请求(自动) */
export interface AiMatchAutoRequest {
  content: string
  projectCategory: string
  projectType: string
}

/** AI匹配请求(手动) */
export interface AiMatchManualRequest {
  projectCategory: string
  projectType: string
  keyword?: string
}

/** AI匹配结果 */
export interface AiMatchResult {
  requirementId: number
  requirementName: string
  similarity: number
  content: string
}

/** AI检测启动请求 */
export interface AiDetectionStartRequest {
  projectId: number
  detectionTypes: string[]
  policyFileIds?: number[]
}

/** AI检测结果 */
export interface AiDetectionResult {
  id: number
  detectionType: string
  status: string
  issues: AiDetectionIssue[]
}

/** AI检测问题 */
export interface AiDetectionIssue {
  id: number
  description: string
  location: string
  suggestion: string
  severity: 'HIGH' | 'MEDIUM' | 'LOW'
}
