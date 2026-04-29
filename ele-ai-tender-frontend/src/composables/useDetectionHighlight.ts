import { nextTick, onBeforeUnmount, type Ref } from 'vue'
import type { DetectionIssueVO } from '@/types/detection'

interface HighlightOptions {
  containerRef: Ref<HTMLElement | null>
}

const SEVERITY_COLORS: Record<string, { bg: string; border: string }> = {
  HIGH: { bg: '#ffcdd2', border: '#f44336' },
  MEDIUM: { bg: '#ffe0b2', border: '#ff9800' },
  LOW: { bg: '#bbdefb', border: '#2196f3' },
}

export function useDetectionHighlight({ containerRef }: HighlightOptions) {
  const highlightedMarks: HTMLElement[] = []
  const pendingTimers: ReturnType<typeof setTimeout>[] = []

  onBeforeUnmount(() => {
    clearHighlights()
    for (const t of pendingTimers) clearTimeout(t)
    pendingTimers.length = 0
  })

  function clearHighlights() {
    for (const mark of highlightedMarks) {
      const parent = mark.parentNode
      if (parent) {
        parent.replaceChild(document.createTextNode(mark.textContent || ''), mark)
        parent.normalize()
      }
    }
    highlightedMarks.length = 0
  }

  /**
   * 根据 elementIndex 定位 docx-preview 渲染的 DOM 元素。
   * elementIndex 是文档顶层 IBodyElement 序号（段落和表格共享）。
   * docx-preview 渲染后，.docx-wrapper > section > * 是顶层元素（段落或表格容器），
   * 按 elementIndex 顺序对应。
   */
  function findTopLevelElementByIndex(elementIndex?: number): HTMLElement | null {
    if (elementIndex == null || !containerRef.value) return null

    const sections = containerRef.value.querySelectorAll('.docx-wrapper > section.docx')
    if (sections.length === 0) return null

    // 取第一个 section 下的直接子元素
    const topElements = sections[0]!.children
    if (elementIndex < 0 || elementIndex >= topElements.length) return null
    return topElements[elementIndex] as HTMLElement
  }

  function applyHighlights(issues: DetectionIssueVO[]) {
    clearHighlights()
    if (!containerRef.value || issues.length === 0) return

    for (const issue of issues) {
      if (issue.handleStatus !== 0 || !issue.original || !issue.locationRef) continue

      const targetEl = findTopLevelElementByIndex(issue.locationRef.elementIndex)
      if (!targetEl) continue

      // 段落类型：直接高亮段落内文本
      if (issue.locationRef.type === 'paragraph') {
        highlightTextInElement(targetEl, issue)
      }
      // 表格类型：在表格内查找包含原文的段落并高亮
      else if (issue.locationRef.type === 'table') {
        const cellText = targetEl.textContent || ''
        if (cellText.includes(issue.original)) {
          const allParas = targetEl.querySelectorAll('p')
          for (const para of allParas) {
            if ((para.textContent || '').includes(issue.original)) {
              highlightTextInElement(para as HTMLElement, issue)
              break
            }
          }
        }
      }
    }
  }

  /**
   * 在元素内高亮 original 文本，支持跨 textNode 匹配
   */
  function highlightTextInElement(element: HTMLElement, issue: DetectionIssueVO) {
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT)
    const textNodes: Text[] = []
    while (walker.nextNode()) {
      textNodes.push(walker.currentNode as Text)
    }

    const fullText = textNodes.map(n => n.textContent || '').join('')
    const matchIdx = fullText.indexOf(issue.original)
    if (matchIdx < 0) return

    const matchEnd = matchIdx + issue.original.length

    // 构建每个 textNode 的字符偏移映射
    const nodeRanges: { node: Text; start: number; end: number }[] = []
    let offset = 0
    for (const textNode of textNodes) {
      const len = (textNode.textContent || '').length
      nodeRanges.push({ node: textNode, start: offset, end: offset + len })
      offset += len
    }

    // 找到匹配范围覆盖的所有 textNode
    const severity = (issue.severity || 'MEDIUM').toUpperCase()
    const colors = SEVERITY_COLORS[severity] ?? SEVERITY_COLORS.MEDIUM!

    for (const { node, start, end } of nodeRanges) {
      if (end <= matchIdx || start >= matchEnd) continue

      const nodeText = node.textContent || ''
      const localStart = Math.max(0, matchIdx - start)
      const localEnd = Math.min(nodeText.length, matchEnd - start)

      const before = nodeText.substring(0, localStart)
      const matched = nodeText.substring(localStart, localEnd)
      const after = nodeText.substring(localEnd)

      if (!matched) continue

      // 只为匹配的文本创建 mark，首尾放在各自的 textNode 中
      const mark = document.createElement('mark')
      mark.className = 'detection-highlight'
      mark.style.backgroundColor = colors!.bg
      mark.style.borderBottom = `2px solid ${colors!.border}`
      mark.style.borderRadius = '2px'
      mark.style.padding = '0 2px'
      mark.style.cursor = 'pointer'
      mark.dataset.recordId = String(issue.recordId)
      mark.dataset.issueIndex = String(issue.issueIndex)
      mark.title = `${issue.typeName}: ${issue.description}`

      const parent = node.parentNode!
      const fragment = document.createDocumentFragment()
      if (before) fragment.appendChild(document.createTextNode(before))
      mark.textContent = matched
      fragment.appendChild(mark)
      if (after) fragment.appendChild(document.createTextNode(after))

      parent.replaceChild(fragment, node)
      highlightedMarks.push(mark)
    }
  }

  async function scrollToAndHighlight(issue: DetectionIssueVO) {
    if (!containerRef.value || !issue.locationRef) return

    await nextTick()
    const targetEl = findTopLevelElementByIndex(issue.locationRef.elementIndex)
    if (!targetEl) return

    targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' })

    targetEl.style.transition = 'background-color 0.3s'
    targetEl.style.backgroundColor = 'rgba(255, 152, 0, 0.1)'
    const timer = setTimeout(() => {
      targetEl.style.backgroundColor = ''
    }, 3000)
    pendingTimers.push(timer)
  }

  return {
    applyHighlights,
    clearHighlights,
    scrollToAndHighlight,
  }
}
