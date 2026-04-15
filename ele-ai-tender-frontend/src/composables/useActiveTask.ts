import { ref, watch, onMounted, type Ref } from 'vue'
import { aiTaskApi } from '@/api/ai-task'
import { useTaskPolling } from '@/composables/useTaskPolling'
import type { AiTaskVO } from '@/types/ai-task'
import { TERMINAL_STATUSES } from '@/types/ai-task'

/**
 * 活跃任务查询组合式函数
 * 页面加载时检查是否有活跃的AI任务，若有则自动启动轮询
 * 用于按钮禁用联动和进度展示
 *
 * @param taskType 任务类型编码
 * @param bizId 业务实体ID（响应式）
 * @param bizType 业务类型
 */
export function useActiveTask(taskType: string, bizId: Ref<number>, bizType: string) {
  const activeTaskId = ref<number | null>(null)
  const isActive = ref(false)
  const activeTask = ref<AiTaskVO | null>(null)
  const checking = ref(false)

  const { task, retry, skip } = useTaskPolling(activeTaskId)

  /** 检查是否有活跃任务 */
  async function checkActiveTask() {
    if (!bizId.value) return
    checking.value = true
    try {
      const result = await aiTaskApi.getActiveTask(taskType, bizId.value, bizType)
      if (result && result.id) {
        activeTaskId.value = result.id
        isActive.value = true
        activeTask.value = result
      } else {
        activeTaskId.value = null
        isActive.value = false
        activeTask.value = null
      }
    } catch {
      // 查询失败不影响页面，仅重置状态
      activeTaskId.value = null
      isActive.value = false
      activeTask.value = null
    } finally {
      checking.value = false
    }
  }

  /** 提交任务成功后调用，设置活跃状态并启动轮询 */
  function setActive(taskId: number) {
    activeTaskId.value = taskId
    isActive.value = true
  }

  // 监听轮询结果，任务完成时自动解除活跃状态
  watch(task, (t) => {
    if (t && TERMINAL_STATUSES.includes(t.status)) {
      isActive.value = false
      activeTaskId.value = null
    }
    if (t) {
      activeTask.value = t
    }
  })

  // 页面加载时自动检查
  onMounted(checkActiveTask)

  return {
    /** 是否有活跃任务（按钮应禁用） */
    isActive,
    /** 活跃任务详情 */
    activeTask,
    /** 活跃任务ID */
    activeTaskId,
    /** 是否正在检查活跃任务 */
    checking,
    /** 手动重新检查活跃任务 */
    checkActiveTask,
    /** 提交任务后设置为活跃 */
    setActive,
    /** 重试活跃任务 */
    retry,
    /** 跳过活跃任务 */
    skip,
  }
}
