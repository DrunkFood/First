/** AI任务类型 */
export type AiTaskType =
  | 'REQUIREMENT_GENERATE'
  | 'PROJECT_REQUIREMENT_GENERATE'
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

/** 终态状态集合（status维度，不含COMPLETED——COMPLETED需看resultSynced） */
export const TERMINAL_STATUSES: AiTaskStatus[] = ['FAILED', 'AI_UNAVAILABLE', 'SKIPPED']

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
  /** 结果是否已同步到业务表: 0-未同步 1-已同步 2-同步失败 */
  resultSynced?: number
}

/** 任务是否真正完成（resultSynced=1 才算成功，可读取业务数据） */
export type RequirementContentStage =
  | 'OUTLINE_GENERATED'
  | 'CHAPTER_GENERATING'
  | 'DRAFT_COMPLETED'
  | 'REVIEWING'
  | 'COMPLETED'

export type RequirementReviewStatus =
  | 'NOT_STARTED'
  | 'PROCESSING'
  | 'COMPLETED'

export type RequirementChapterStatus =
  | 'PENDING'
  | 'COMPLETED'

export interface RequirementGenerationOutlineChapter {
  chapterNo: number
  chapterKey?: string
  chapterTitle: string
  corePoints?: string
  estimatedWords?: number
}

export interface RequirementGenerationProgressChapter extends RequirementGenerationOutlineChapter {
  status?: RequirementChapterStatus
  content?: string
}

export interface RequirementGenerationProgressResult {
  contentStage?: RequirementContentStage
  outline?: {
    projectOverview?: string
    chapters?: RequirementGenerationOutlineChapter[]
  }
  chapters?: RequirementGenerationProgressChapter[]
  content?: string
  completedChapterCount?: number
  totalChapterCount?: number
  reviewStatus?: RequirementReviewStatus
}

export function parseRequirementGenerationProgress(result?: string): RequirementGenerationProgressResult | null {
  if (!result) return null
  try {
    const parsed = JSON.parse(result) as RequirementGenerationProgressResult
    if (!parsed || typeof parsed !== 'object') return null
    if (!parsed.contentStage && !parsed.outline && !parsed.chapters && !parsed.content) return null
    return parsed
  } catch {
    return null
  }
}

export function buildRequirementGenerationProgressMarkdown(progress: RequirementGenerationProgressResult): string {
  if (
    (progress.contentStage === 'DRAFT_COMPLETED'
      || progress.contentStage === 'REVIEWING'
      || progress.contentStage === 'COMPLETED')
    && progress.content?.trim()
  ) {
    return progress.content
  }

  const outlineChapters = progress.outline?.chapters ?? []
  if (outlineChapters.length === 0) {
    return progress.content ?? ''
  }

  const chapterMap = new Map<number, string>()
  for (const chapter of progress.chapters ?? []) {
    if (chapter.chapterNo && chapter.content?.trim()) {
      chapterMap.set(chapter.chapterNo, chapter.content.trim())
    }
  }

  return outlineChapters.map((chapter, index) => {
    const chapterNo = chapter.chapterNo || index + 1
    const title = chapter.chapterTitle || `\u7b2c${chapterNo}\u7ae0`
    const chapterContent = chapterMap.get(chapterNo)
    if (chapterContent) {
      return `## ${title}\n\n${chapterContent}`
    }

    const statusText = progress.contentStage === 'OUTLINE_GENERATED'
      ? '\u7b49\u5f85\u751f\u6210\u6b63\u6587...'
      : '\u751f\u6210\u4e2d...'
    const corePoints = chapter.corePoints?.trim()
      ? `\n\n> \u6838\u5fc3\u8981\u70b9\uff1a${chapter.corePoints}`
      : ''
    return `## ${title}\n\n> ${statusText}${corePoints}`
  }).join('\n\n---\n\n')
}

export function isTaskSucceeded(task: AiTaskVO | null): boolean {
  return task?.status === 'COMPLETED' && task.resultSynced === 1
}

/** 任务是否处于终态（无需继续轮询） */
export function isTaskTerminal(task: AiTaskVO | null): boolean {
  if (!task) return true
  if (TERMINAL_STATUSES.includes(task.status)) return true
  // COMPLETED 需等 resultSynced 有值（0=还在同步，1=成功，2=失败）
  if (task.status === 'COMPLETED') return task.resultSynced !== 0
  return false
}

/** 标准化任务进度百分比 */
export function getTaskProgress(task: AiTaskVO | null): number {
  if (!task) return 0
  switch (task.status) {
    case 'PENDING': return 10
    case 'PROCESSING': return 60
    case 'COMPLETED':
      if (task.resultSynced === 1) return 100
      if (task.resultSynced === 2) return 0
      return 90
    default: return 0
  }
}

/**
 * 基于任务已运行时间计算 PROCESSING 状态的进度
 * 使用指数曲线：前期增长快、后期趋于平缓，上限90%
 *
 * 公式：progress = 90 × (1 - e^(-3 × elapsed / estimatedTotal))
 *
 * 典型值（estimatedTotal = 5分钟）：
 * | 已用时间 | 进度  |
 * |----------|-------|
 * | 0秒      | 0%    |
 * | 30秒     | 23%   |
 * | 1分钟    | 41%   |
 * | 2分钟    | 63%   |
 * | 3分钟    | 75%   |
 * | 5分钟    | 86%   |
 *
 * @param task AI任务（需有 startedAt 字段）
 * @param fallbackStartMs 本地开始时间戳（task.startedAt 为空时的兜底）
 * @param estimatedTotalMs 预估总耗时（毫秒），默认5分钟
 */
export function getProcessingProgressByTime(
  task: AiTaskVO,
  fallbackStartMs: number = 0,
  estimatedTotalMs: number = 5 * 60 * 1000,
): number {
  const startTime = task.startedAt ? new Date(task.startedAt).getTime() : fallbackStartMs
  if (!startTime) return 30 // 无时间信息时返回基础值

  const elapsed = Date.now() - startTime
  if (elapsed <= 0) return 0

  const ratio = Math.min(elapsed / estimatedTotalMs, 1)
  const progress = 90 * (1 - Math.exp(-3 * ratio))
  return Math.min(Math.round(progress), 90)
}

/** 标准化进度条样式 */
export function getProgressStatus(task: AiTaskVO | null): '' | 'success' | 'warning' | 'exception' {
  if (!task) return ''
  if (task.status === 'COMPLETED' && task.resultSynced === 2) return 'exception'
  if (task.status === 'COMPLETED' && task.resultSynced === 1) return 'success'
  if (task.status === 'FAILED') return 'exception'
  if (task.status === 'AI_UNAVAILABLE') return 'warning'
  return ''
}

/** 是否可发起新任务（上一个任务已结束或不存在） */
export function canCreateNewTask(latestTask: AiTaskVO | null): boolean {
  if (!latestTask) return true
  return isTaskTerminal(latestTask)
}
