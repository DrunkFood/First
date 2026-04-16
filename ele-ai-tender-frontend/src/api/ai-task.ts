import request from '@/utils/request'
import type { AiTaskVO } from '@/types/ai-task'

/**
 * AI任务API
 * 精简后仅保留3个接口：
 * - getStatus  轮询任务状态
 * - skip       跳过卡住的任务
 * - getLatest  查询最新任务（页面加载用）
 */
export const aiTaskApi = {
  /** 查询任务状态 */
  getStatus(taskId: number) {
    return request.get<any, AiTaskVO>(`/core-api/v1/ai-tasks/${taskId}`)
  },

  /** 跳过任务（降级手动） */
  skip(taskId: number) {
    return request.post(`/core-api/v1/ai-tasks/${taskId}/skip`)
  },

  /** 查询业务实体的最新任务（含终态） */
  getLatestTask(taskType: string, bizId: number, bizType: string) {
    return request.get<any, AiTaskVO | null>('/core-api/v1/ai-tasks/latest', {
      params: { taskType, bizId, bizType },
    })
  },
}
