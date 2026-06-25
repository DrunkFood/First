import { ref, watch, onBeforeUnmount, type Ref } from 'vue'
import { aiTaskApi } from '@/api/ai-task'
import type { AiTaskVO } from '@/types/ai-task'
import { isTaskTerminal } from '@/types/ai-task'

/**
 * AI任务轮询组合式函数
 * 自动轮询任务状态，终态时停止轮询
 * 注意：COMPLETED+resultSynced=0/3 仍需继续轮询（等待结果同步）
 */
export function useTaskPolling(taskId: Ref<number | null>, interval = 8000) {
  const task = ref<AiTaskVO | null>(null)
  const isPolling = ref(false)
  const error = ref<string | null>(null)
  let timer: ReturnType<typeof setInterval> | null = null

  const fetchStatus = async () => {
    if (!taskId.value) return
    try {
      const res = await aiTaskApi.getStatus(taskId.value)
      task.value = res
      error.value = null
      if (isTaskTerminal(res)) {
        stopPolling()
      }
    } catch (e: any) {
      error.value = e.message || '查询任务状态失败'
    }
  }

  const startPolling = () => {
    if (isPolling.value) return
    isPolling.value = true
    fetchStatus()
    timer = setInterval(fetchStatus, interval)
  }

  const stopPolling = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
    isPolling.value = false
  }

  const skip = async () => {
    if (!taskId.value) return
    await aiTaskApi.skip(taskId.value)
    await fetchStatus()
  }

  // 当taskId变化时自动开始轮询
  watch(taskId, (newId) => {
    stopPolling()
    if (newId) {
      startPolling()
    }
  })

  // 组件卸载时清理
  onBeforeUnmount(() => {
    stopPolling()
  })

  return {
    task,
    isPolling,
    error,
    startPolling,
    stopPolling,
    skip,
  }
}
