export const REPLACEABLE_CONTENT_START = '【可替换正文开始】'
export const REPLACEABLE_CONTENT_END = '【可替换正文结束】'

export function extractReplaceableContent(content: string): string | null {
  return extractReplaceableContents(content)[0] || null
}

export function extractReplaceableContents(content: string): string[] {
  const replaceableContents: string[] = []
  let searchStart = 0

  while (searchStart < content.length) {
    const startIndex = content.indexOf(REPLACEABLE_CONTENT_START, searchStart)
    if (startIndex === -1) break

    const contentStart = startIndex + REPLACEABLE_CONTENT_START.length
    const endIndex = content.indexOf(REPLACEABLE_CONTENT_END, contentStart)
    if (endIndex === -1) break

    const replaceableContent = content.slice(contentStart, endIndex).trim()
    if (replaceableContent) {
      replaceableContents.push(replaceableContent)
    }
    searchStart = endIndex + REPLACEABLE_CONTENT_END.length
  }

  return replaceableContents
}
