import { ref, onBeforeUnmount, type Ref } from 'vue'
import { ElMessageBox } from 'element-plus'

/**
 * 自动保存组合式函数
 * 每120秒自动保存编辑器内容
 */
export function useAutoSave(
  id: Ref<number>,
  content: Ref<string>,
  saveApi: (id: number, data: { content: string }) => Promise<any>,
  getApi: (id: number) => Promise<string | null>,
  clearApi: (id: number) => Promise<any>,
  interval = 120000
) {
  const isSaving = ref(false)
  const lastSaveTime = ref<string | null>(null)
  let timer: ReturnType<typeof setInterval> | null = null

  const save = async () => {
    if (!id.value || !content.value) return
    isSaving.value = true
    try {
      await saveApi(id.value, { content: content.value })
      lastSaveTime.value = new Date().toLocaleTimeString()
    } catch {
      // 自动保存失败不阻塞用户操作
    } finally {
      isSaving.value = false
    }
  }

  const startAutoSave = () => {
    if (timer) return
    timer = setInterval(save, interval)
  }

  const stopAutoSave = () => {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  /** 检查并恢复草稿 */
  const recoverDraft = async (): Promise<boolean> => {
    if (!id.value) return false
    try {
      const draft = await getApi(id.value)
      if (draft) {
        const confirmed = await ElMessageBox.confirm(
          '检测到未保存的草稿，是否恢复？',
          '草稿恢复',
          { confirmButtonText: '恢复', cancelButtonText: '丢弃', type: 'warning' }
        ).catch(() => false)

        if (confirmed) {
          content.value = draft
          return true
        } else {
          await clearApi(id.value)
        }
      }
    } catch {
      // 忽略恢复失败
    }
    return false
  }

  onBeforeUnmount(() => {
    stopAutoSave()
  })

  return {
    isSaving,
    lastSaveTime,
    startAutoSave,
    stopAutoSave,
    recoverDraft,
    saveNow: save,
  }
}
