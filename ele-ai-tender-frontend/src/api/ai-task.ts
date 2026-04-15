import request from '@/utils/request'
import type { AiTaskVO } from '@/types/ai-task'

export const aiTaskApi = {
  /** 查询任务状态 */
  getStatus(taskId: number) {
    return request.get<any, AiTaskVO>(`/core-api/v1/ai-tasks/${taskId}`)
  },

  /** 查询项目所有任务 */
  getByProject(projectId: number) {
    return request.get<any, AiTaskVO[]>(`/core-api/v1/ai-tasks/project/${projectId}`)
  },

  /** 重试任务 */
  retry(taskId: number) {
    return request.post(`/core-api/v1/ai-tasks/${taskId}/retry`)
  },

  /** 跳过任务 */
  skip(taskId: number) {
    return request.post(`/core-api/v1/ai-tasks/${taskId}/skip`)
  },

  /** 查询业务实体的活跃任务（PENDING/PROCESSING） */
  getActiveTask(taskType: string, bizId: number, bizType: string) {
    return request.get<any, AiTaskVO | null>('/core-api/v1/ai-tasks/active', {
      params: { taskType, bizId, bizType },
    })
  },

  /** 查询项目的活跃任务列表 */
  getActiveTasksByProject(projectId: number) {
    return request.get<any, AiTaskVO[]>(`/core-api/v1/ai-tasks/project/${projectId}/active`)
  },
}
