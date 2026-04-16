import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { feedbackApi } from '@/api/feedback'
import type { FeedbackScene, FeedbackVO } from '@/types/feedback'

/**
 * AI内容反馈组合式函数
 * 统一所有组件的反馈行为: 提交反馈（不可撤销，不可修改） + dislike原因收集
 *
 * @param scene 反馈场景
 * @param getTaskId 获取当前AI任务ID的函数
 */
export function useFeedback(
  scene: FeedbackScene,
  getTaskId: () => number | undefined,
) {
  const submitting = ref(false)
  const currentFeedback = ref<FeedbackVO | null>(null)

  /** 是否已反馈（用于 UI 判断按钮是否可点击） */
  const hasFeedback = computed(() => currentFeedback.value !== null)

  /**
   * 查询当前用户对目标的反馈状态（页面加载时调用）
   */
  async function loadFeedback(chatMessageId?: string) {
    const taskId = getTaskId()
    if (!taskId) return
    try {
      const result = await feedbackApi.getUserFeedback({
        taskId,
        feedbackScene: scene,
        chatMessageId,
      })
      currentFeedback.value = result
    } catch {
      currentFeedback.value = null
    }
  }

  /**
   * 提交反馈（每个目标只能反馈一次，不可修改）
   * - 已反馈 → 提示已反馈，不重复提交
   * - 无反馈 → 新增
   */
  async function submitFeedback(
    type: 'like' | 'dislike',
    options?: {
      chatMessageId?: string
      chatContent?: string
    },
  ) {
    // 已反馈，不可再提交
    if (currentFeedback.value) {
      ElMessage.info('已反馈，不可修改')
      return
    }

    // Dislike 时收集原因
    let reason: string | undefined
    if (type === 'dislike') {
      try {
        const { value } = await ElMessageBox.prompt(
          '请说明不满意的原因，帮助我们改进：',
          '反馈',
          {
            confirmButtonText: '提交',
            cancelButtonText: '取消',
            inputPlaceholder: '可选填原因',
          },
        )
        reason = value || undefined
      } catch {
        return // 用户取消
      }
    }

    const taskId = getTaskId()

    try {
      submitting.value = true
      const result = await feedbackApi.submit({
        feedbackType: type === 'like' ? 'LIKE' : 'DISLIKE',
        feedbackScene: scene,
        taskId,
        chatMessageId: options?.chatMessageId,
        chatContent: options?.chatContent,
        reason,
      })
      currentFeedback.value = result
      ElMessage.success(type === 'like' ? '感谢您的反馈' : '我们会持续改进')
    } catch {
      ElMessage.error('提交反馈失败')
    } finally {
      submitting.value = false
    }
  }

  return {
    submitting,
    currentFeedback,
    hasFeedback,
    loadFeedback,
    submitFeedback,
  }
}
