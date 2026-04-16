import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { feedbackApi } from '@/api/feedback'
import type { FeedbackType, FeedbackScene, FeedbackVO } from '@/types/feedback'

/**
 * AI内容反馈组合式函数
 * 统一所有组件的反馈行为: 提交反馈（不可撤销，可切换类型） + dislike原因收集
 *
 * @param scene 反馈场景
 * @param getTaskId 获取当前AI任务ID的函数
 * @param getProjectId 获取当前项目ID的函数
 */
export function useFeedback(
  scene: FeedbackScene,
  getTaskId: () => number | undefined,
  getProjectId: () => number | undefined,
) {
  const submitting = ref(false)
  const currentFeedback = ref<FeedbackVO | null>(null)

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
   * 提交反馈
   * - 已有同类型反馈 → 幂等，不重复提交
   * - 已有不同类型反馈 → 更新为新类型
   * - 无反馈 → 新增
   */
  async function submitFeedback(
    type: 'like' | 'dislike',
    options?: {
      chatMessageId?: string
      chatContent?: string
    },
  ) {
    const feedbackType: FeedbackType = type === 'like' ? 'LIKE' : 'DISLIKE'

    // 已有同类型反馈，幂等返回
    if (currentFeedback.value && currentFeedback.value.feedbackType === feedbackType) {
      ElMessage.info('已反馈')
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
    const projectId = getProjectId()

    try {
      submitting.value = true
      const result = await feedbackApi.submit({
        feedbackType,
        feedbackScene: scene,
        taskId,
        projectId,
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
    loadFeedback,
    submitFeedback,
  }
}
