export const REPLACEABLE_CONTENT_START = '【可替换正文开始】'
export const REPLACEABLE_CONTENT_END = '【可替换正文结束】'

export interface AiReplacementResult {
  content: string
  found: boolean
  duplicated: boolean
}

export function extractReplaceableContent(content: string): string | null {
  const replaceableContents = extractReplaceableContents(content)
  return replaceableContents.length > 0 ? replaceableContents[0]! : null
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

    replaceableContents.push(content.slice(contentStart, endIndex).trim())
    searchStart = endIndex + REPLACEABLE_CONTENT_END.length
  }

  return replaceableContents
}

export function applyAiReplacement(content: string, selectedText: string, replacement: string): AiReplacementResult {
  if (!selectedText) {
    return { content, found: false, duplicated: false }
  }

  const index = content.indexOf(selectedText)
  if (index === -1) {
    return { content, found: false, duplicated: false }
  }

  const duplicated = content.indexOf(selectedText, index + selectedText.length) !== -1
  const normalizedReplacement = trimDuplicatedLeadingContext(content, selectedText, replacement)
  const range = getReplacementRange(content, selectedText, index, normalizedReplacement)

  return {
    content: content.slice(0, range.start) + normalizedReplacement + content.slice(range.end),
    found: true,
    duplicated,
  }
}

export function trimDuplicatedLeadingContext(content: string, selectedText: string, replacement: string): string {
  const selectedIndex = content.indexOf(selectedText)
  if (selectedIndex === -1) return replacement

  const lineStart = content.lastIndexOf('\n', Math.max(0, selectedIndex - 1)) + 1
  const sameLinePrefix = content.slice(lineStart, selectedIndex).trimStart()
  if (!sameLinePrefix.trim()) return replacement

  const normalizedReplacement = replacement.trimStart()
  const duplicatedPrefix = findLongestDuplicatedPrefix(sameLinePrefix, normalizedReplacement)
  if (!duplicatedPrefix) return replacement

  return normalizedReplacement.slice(duplicatedPrefix.length).trimStart()
}

function getReplacementRange(content: string, selectedText: string, index: number, replacement: string): { start: number; end: number } {
  const defaultRange = { start: index, end: index + selectedText.length }
  if (replacement !== '') return defaultRange

  const lineStart = findLineStart(content, index)
  const lineEnd = findLineEnd(content, defaultRange.end)
  const lineWithoutSelectedText = content.slice(lineStart, index) + content.slice(defaultRange.end, lineEnd)

  if (!isEmptyMarkdownShell(lineWithoutSelectedText)) {
    return defaultRange
  }

  return expandToWholeLine(content, lineStart, lineEnd)
}

function findLineStart(content: string, index: number): number {
  return content.lastIndexOf('\n', Math.max(0, index - 1)) + 1
}

function findLineEnd(content: string, index: number): number {
  const lineEnd = content.indexOf('\n', index)
  return lineEnd === -1 ? content.length : lineEnd
}

function expandToWholeLine(content: string, lineStart: number, lineEnd: number): { start: number; end: number } {
  let start = lineStart
  let end = lineEnd

  if (end < content.length && content[end] === '\n') {
    end += 1
  } else if (start > 0) {
    start -= content[start - 2] === '\r' && content[start - 1] === '\n' ? 2 : 1
  }

  return { start, end }
}

function isEmptyMarkdownShell(value: string): boolean {
  const trimmed = value.trim()
  return trimmed === ''
    || /^#{1,6}\s*$/.test(trimmed)
    || /^>\s*$/.test(trimmed)
    || /^[-*+]\s*$/.test(trimmed)
    || /^\d+(?:[.)]|、)?\s*$/.test(trimmed)
    || /^\d+(?:\.\d+)*[\s.)、．:：-]*$/.test(trimmed)
    || /^[（(]?[一二三四五六七八九十]+[）)、.．:：\s-]*$/.test(trimmed)
    || /^[#>*+\-\s.)、．:：]+$/.test(trimmed)
}

function findLongestDuplicatedPrefix(sourcePrefix: string, replacement: string): string | null {
  const maxLength = Math.min(sourcePrefix.length, replacement.length, 120)
  for (let length = maxLength; length >= 4; length--) {
    const candidate = sourcePrefix.slice(sourcePrefix.length - length)
    if (hasMeaningfulText(candidate) && replacement.startsWith(candidate)) {
      return candidate
    }
  }
  return null
}

function hasMeaningfulText(value: string): boolean {
  return /[\p{L}\p{N}]/u.test(value)
}
