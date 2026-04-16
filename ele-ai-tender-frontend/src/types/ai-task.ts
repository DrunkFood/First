/** AI任务类型 */
export type AiTaskType =
  | 'REQUIREMENT_GENERATE'
  | 'REVIEW_ITEM_GENERATE'
  | 'DETECTION_POLICY_REVIEW'
  | 'DETECTION_FORMAT_CHECK'
  | 'DETECTION_SENSITIVE_WORD'
  | 'DETECTION_TYPO'
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

/** 标准化任务进度百分比 */
export function getTaskProgress(status: AiTaskStatus): number {
  switch (status) {
    case 'PENDING': return 10
    case 'PROCESSING': return 60
    case 'COMPLETED': return 100
    default: return 0
  }
}

/** 标准化进度条样式 */
export function getProgressStatus(status: AiTaskStatus): '' | 'success' | 'warning' | 'exception' {
  switch (status) {
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'exception'
    case 'AI_UNAVAILABLE': return 'warning'
    default: return ''
  }
}

/** 是否可发起新任务（上一个任务已结束或不存在） */
export function canCreateNewTask(latestTask: AiTaskVO | null): boolean {
  if (!latestTask) return true
  return TERMINAL_STATUSES.includes(latestTask.status)
}
