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
      'Accept': 'text/event-stream',
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

      let buffer = ''       // 跨 chunk 行缓冲，防止行被截断
      let currentEvent = '' // 当前 SSE 事件名

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // SSE 协议以 \n\n 分隔事件块
        let boundary: number
        while ((boundary = buffer.indexOf('\n\n')) !== -1) {
          const eventBlock = buffer.slice(0, boundary)
          buffer = buffer.slice(boundary + 2)

          // 解析事件块中的每一行
          for (const line of eventBlock.split('\n')) {
            if (line.startsWith('event:')) {
              currentEvent = line.slice(6).trim()
            } else if (line.startsWith('data:')) {
              // 兼容 data:value 和 data: value 两种格式
              const raw = line.slice(5).trimStart()
              handleSseData(currentEvent, raw, onMessage, onError, onComplete)
              if (currentEvent === 'done') return
            }
          }
          currentEvent = '' // 重置事件名
        }
      }
      // 流正常结束（非 [DONE] 信号）
      onComplete?.()
    })
    .catch((err) => {
      if (err.name !== 'AbortError') {
        onError?.(err as Event)
      }
    })

  return () => controller.abort()
}

/**
 * 解析 SSE data 行，提取实际内容
 * 后端格式：event:message → data:{"content":"..."}
 *           event:done    → data:{"content":"[DONE]"}
 *           event:error   → data:{"error":"..."}
 * 同时兼容纯文本格式：data: xxx
 */
function handleSseData(
  eventName: string,
  rawData: string,
  onMessage: (data: string) => void,
  onError?: (error: Event) => void,
  onComplete?: () => void,
) {
  // 完成信号
  if (eventName === 'done' || rawData === '[DONE]') {
    onComplete?.()
    return
  }

  // 错误信号
  if (eventName === 'error') {
    const errorMsg = tryExtractJsonField(rawData, 'error') || rawData
    onError?.(new Event(errorMsg))
    return
  }

  // 消息内容：尝试从 JSON 中提取 content 字段，非 JSON 则当纯文本
  const content = tryExtractJsonField(rawData, 'content')
  if (content === '[DONE]') {
    onComplete?.()
    return
  }
  if (content != null) {
    onMessage(content)
  } else {
    onMessage(rawData)
  }
}

/**
 * 尝试从 JSON 字符串中提取指定字段，失败返回 null
 */
function tryExtractJsonField(jsonStr: string, field: string): string | null {
  if (!jsonStr.startsWith('{')) return null
  try {
    const obj = JSON.parse(jsonStr)
    const value = obj[field]
    return typeof value === 'string' ? value : null
  } catch {
    return null
  }
}
