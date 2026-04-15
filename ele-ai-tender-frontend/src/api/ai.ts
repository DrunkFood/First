import request from '@/utils/request'
import type {
  AiSuggestRequest,
  AiSuggestResponse,
  AiMatchAutoRequest,
  AiMatchManualRequest,
  AiMatchResult,
  AiDetectionStartRequest,
  AiDetectionResult,
} from '@/types/ai'

export const aiApi = {
  /** AI对话（SSE流式） — URL常量，供createSSEConnection使用 */
  chatUrl: '/ai-api/v1/ai/chat',

  /** 文本优化（SSE流式） — URL常量，供createSSEConnection使用 */
  optimizeUrl: '/ai-api/v1/ai/optimize',

  /** AI建议（同步） */
  suggest(data: AiSuggestRequest) {
    return request.post<any, AiSuggestResponse>('/ai-api/v1/ai/suggest', data)
  },

  /** 自动匹配历史需求 */
  matchAuto(data: AiMatchAutoRequest) {
    return request.post<any, AiMatchResult[]>('/ai-api/v1/ai/match/auto', data)
  },

  /** 手动选择匹配（返回候选列表） */
  matchManual(data: AiMatchManualRequest) {
    return request.post<any, AiMatchResult[]>('/ai-api/v1/ai/match/manual', data)
  },

  /** 启动AI检测 */
  startDetection(data: AiDetectionStartRequest) {
    return request.post<any, Record<string, number>>('/ai-api/v1/detection/start', data)
  },

  /** 获取AI检测结果 */
  getDetectionResult(id: number) {
    return request.get<any, AiDetectionResult>(`/ai-api/v1/detection/${id}/result`)
  },

  /** 确认AI检测结果 */
  confirmDetection(id: number) {
    return request.post(`/ai-api/v1/detection/${id}/confirm`)
  },
}

/**
 * 创建SSE连接的通用工具函数
 * 使用 fetch + ReadableStream 实现SSE（因为EventSource不支持POST+body）
 * @param url 请求URL
 * @param body 请求体
 * @param onMessage 消息回调
 * @param onError 错误回调
 * @param onComplete 完成回调
 * @returns 关闭连接的函数
 */
export function createSSEConnection(
  url: string,
  body: Record<string, any>,
  onMessage: (data: string) => void,
  onError?: (error: Event) => void,
  onComplete?: () => void,
): () => void {
  const token = localStorage.getItem('token')
  const controller = new AbortController()

  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token ? `Bearer ${token}` : '',
    },
    body: JSON.stringify(body),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const reader = response.body?.getReader()
      if (!reader) return
      const decoder = new TextDecoder()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const text = decoder.decode(value, { stream: true })
        // 解析SSE格式: data: xxx\n\n
        const lines = text.split('\n')
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6)
            if (data === '[DONE]') {
              onComplete?.()
              return
            }
            onMessage(data)
          }
        }
      }
      onComplete?.()
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError?.(err as Event)
      }
    })

  return () => controller.abort()
}
