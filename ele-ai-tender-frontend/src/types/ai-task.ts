/** AI任务类型 */
export type AiTaskType =
  | 'REQUIREMENT_GENERATE'
  | 'REVIEW_ITEM_GENERATE'
  | 'DETECTION_SENSITIVE_WORD'
  | 'DETECTION_TYPO'
  | 'DETECTION_POLICY_REVIEW'
  | 'DETECTION_FORMAT_CHECK'
  | 'TEXT_OPTIMIZE'

/** AI任务状态 */
export type AiTaskStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'AI_UNAVAILABLE'
  | 'SKIPPED'

/** 终态集合 */
export const TERMINAL_STATUSES: AiTaskStatus[] = ['COMPLETED', 'FAILED', 'AI_UNAVAILABLE', 'SKIPPED']

export interface AiTaskVO {
  id: number
  taskType: AiTaskType
  taskTypeName: string
  projectId: number
  bizId: number
  bizType: string
  status: AiTaskStatus
  statusName: string
  result?: string
  errorMsg?: string
  retryCount: number
  maxRetry: number
  startedAt?: string
  completedAt?: string
  createTime: string
}
