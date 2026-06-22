export const REPLACEABLE_CONTENT_START = '【可替换正文开始】'
export const REPLACEABLE_CONTENT_END = '【可替换正文结束】'

export function extractReplaceableContent(content: string): string | null {
  const startIndex = content.indexOf(REPLACEABLE_CONTENT_START)
  if (startIndex === -1) return null

  const contentStart = startIndex + REPLACEABLE_CONTENT_START.length
  const endIndex = content.indexOf(REPLACEABLE_CONTENT_END, contentStart)
  if (endIndex === -1) return null

  const replaceableContent = content.slice(contentStart, endIndex).trim()
  return replaceableContent || null
}
