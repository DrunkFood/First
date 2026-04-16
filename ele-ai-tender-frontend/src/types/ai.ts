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
