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
   * docx-preview DOM: container > (styles + wrapper) > section > article > (p, table, ...)
   * 用结构选择器 section > article 替代 class 名选择器，解耦 className 配置。
   * 多页文档有多个 section，需遍历所有 article > children 做偏移映射。
   */
  function findTopLevelElementByIndex(elementIndex?: number): HTMLElement | null {
    if (elementIndex == null || !containerRef.value) return null

    const articles = containerRef.value.querySelectorAll('section > article')
    if (articles.length === 0) return null

    let offset = 0
    for (const article of articles) {
      const children = article.children
      if (elementIndex < offset + children.length) {
        return children[elementIndex - offset] as HTMLElement
      }
      offset += children.length
    }
    return null
  }

  /**
   * 获取可滚动的预览容器，优先找 .preview-scroll-area，fallback 到 containerRef
   */
  function getScrollContainer(): HTMLElement | null {
    if (!containerRef.value) return null
    const scrollArea = containerRef.value.closest('.preview-scroll-area')
    return scrollArea as HTMLElement || containerRef.value
  }

  function applyHighlights(issues: DetectionIssueVO[]) {
    clearHighlights()
    if (!containerRef.value || issues.length === 0) return

    for (const issue of issues) {
      if (issue.handleStatus !== 0 || !issue.original || !issue.locationRef) continue

      const targetEl = findTopLevelElementByIndex(issue.locationRef.elementIndex)
      if (!targetEl) continue

      if (issue.locationRef.type === 'paragraph') {
        highlightTextInElement(targetEl, issue)
      } else if (issue.locationRef.type === 'table') {
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

      const mark = document.createElement('mark')
      mark.className = 'detection-highlight'
      mark.style.backgroundColor = colors!.bg
      mark.style.borderBottom = `2px solid ${colors!.border}`
      mark.style.borderRadius = '2px'
      mark.style.padding = '0 2px'
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

    // 先清除旧高亮，再创建文本级 <mark> 高亮
    clearHighlights()
    if (issue.locationRef.type === 'paragraph') {
      highlightTextInElement(targetEl, issue)
    } else if (issue.locationRef.type === 'table') {
      const allParas = targetEl.querySelectorAll('p')
      for (const para of allParas) {
        if ((para.textContent || '').includes(issue.original)) {
          highlightTextInElement(para as HTMLElement, issue)
          break
        }
      }
    }

    // 滚动到目标位置：优先滚动预览容器，fallback 到 scrollIntoView
    const scrollContainer = getScrollContainer()
    if (scrollContainer && scrollContainer !== containerRef.value) {
      const containerRect = scrollContainer.getBoundingClientRect()
      const targetRect = targetEl.getBoundingClientRect()
      const scrollOffset = targetRect.top - containerRect.top + scrollContainer.scrollTop
        - containerRect.height / 2 + targetRect.height / 2
      scrollContainer.scrollTo({ top: scrollOffset, behavior: 'smooth' })
    } else {
      targetEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }

    // 临时背景闪烁提示
    targetEl.style.transition = 'background-color 0.3s'
    targetEl.style.backgroundColor = 'rgba(255, 152, 0, 0.15)'
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
