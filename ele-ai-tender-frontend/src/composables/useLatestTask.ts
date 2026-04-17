import { ref, computed, watch, onMounted, type Ref } from 'vue'
import { aiTaskApi } from '@/api/ai-task'
import { useTaskPolling } from '@/composables/useTaskPolling'
import type { AiTaskVO } from '@/types/ai-task'
import { TERMINAL_STATUSES, canCreateNewTask } from '@/types/ai-task'

/**
 * 最新任务查询组合式函数
 * 页面加载时查询最新AI任务（不限状态），非终态自动启动轮询
 * 用于：任务状态展示 + 新任务发起守卫
 *
 * @param taskType 任务类型编码
 * @param bizId 业务实体ID（响应式）
 * @param bizType 业务类型
 * @param onTaskCompleted 任务进入终态时的回调（可选）
 */
export function useLatestTask(
  taskType: string,
  bizId: Ref<number>,
  bizType: string,
  onTaskCompleted?: (task: AiTaskVO) => void
) {
  const latestTaskId = ref<number | null>(null)
  const latestTask = ref<AiTaskVO | null>(null)
  const loading = ref(false)

  const { task, isPolling, skip, startPolling, stopPolling } = useTaskPolling(latestTaskId)

  /** 是否可发起新任务（上一个任务已结束或不存在） */
  const canCreateNew = computed(() => canCreateNewTask(latestTask.value))

  /** 查询最新任务 */
  async function fetchLatest() {
    if (!bizId.value) return
    loading.value = true
    try {
      const result = await aiTaskApi.getLatestTask(taskType, bizId.value, bizType)
      if (result && result.id) {
        latestTaskId.value = result.id
        latestTask.value = result
        // 非终态自动启动轮询（任务进行中，后续变终态应通知用户）
        if (!TERMINAL_STATUSES.includes(result.status)) {
          firstSync = false
          startPolling()
        }
      } else {
        latestTaskId.value = null
        latestTask.value = null
      }
    } catch {
      latestTaskId.value = null
      latestTask.value = null
    } finally {
      loading.value = false
    }
  }

  /** 提交新任务成功后调用，设置活跃状态并启动轮询 */
  function setActive(taskId: number) {
    firstSync = false  // 用户主动创建任务，后续轮询结果应正常触发回调
    latestTaskId.value = taskId
    startPolling()
  }

  /** 重新查询最新任务（用于任务创建后刷新） */
  function refresh() {
    stopPolling()
    fetchLatest()
  }

  // 标记是否为首次同步（页面加载恢复场景），首次同步已完成任务不应触发回调
  let firstSync = true

  // 同步轮询结果
  watch(task, (t, oldT) => {
    if (t) {
      latestTask.value = t
      if (firstSync) {
        // 首次同步：仅记录状态，不触发回调（页面加载恢复已终态的任务）
        firstSync = false
        return
      }
      // 任务从非终态变为终态时触发回调
      if (TERMINAL_STATUSES.includes(t.status) &&
          (!oldT || !TERMINAL_STATUSES.includes(oldT.status))) {
        onTaskCompleted?.(t)
      }
    }
  })

  // bizId 从 0 变为有效值时自动查询（解决异步加载数据后 bizId 延迟赋值的问题）
  watch(bizId, (newVal, oldVal) => {
    if (newVal && newVal !== oldVal) {
      fetchLatest()
    }
  })

  onMounted(fetchLatest)

  return {
    /** 最新任务详情 */
    latestTask,
    /** 是否可发起新任务 */
    canCreateNew,
    /** 是否正在加载 */
    loading,
    /** 是否正在轮询 */
    isPolling,
    /** 重新查询最新任务 */
    refresh,
    /** 提交任务后设置为活跃 */
    setActive,
    /** 跳过任务 */
    skip,
  }
}
