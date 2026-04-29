import { nextTick, type Ref } from 'vue'
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
  const highlightedMarks: HTMLMarkElement[] = []

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

  function applyHighlights(issues: DetectionIssueVO[]) {
    clearHighlights()
    if (!containerRef.value || issues.length === 0) return

    const container = containerRef.value
    const paragraphs = container.querySelectorAll('p')

    for (const issue of issues) {
      if (issue.handleStatus !== 0 || !issue.original || !issue.locationRef) continue

      const targetPara = findParagraphByElementIndex(paragraphs, issue.locationRef.elementIndex)
      if (!targetPara) continue

      highlightTextInElement(targetPara, issue)
    }
  }

  function findParagraphByElementIndex(
    paragraphs: NodeListOf<HTMLParagraphElement>,
    elementIndex?: number,
  ): HTMLParagraphElement | null {
    if (elementIndex == null || elementIndex < 0 || elementIndex >= paragraphs.length) return null
    return paragraphs[elementIndex]
  }

  function highlightTextInElement(element: HTMLElement, issue: DetectionIssueVO) {
    const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT)
    const textNodes: Text[] = []
    while (walker.nextNode()) {
      textNodes.push(walker.currentNode as Text)
    }

    const fullText = textNodes.map(n => n.textContent || '').join('')
    const matchIdx = fullText.indexOf(issue.original)
    if (matchIdx < 0) return

    let charOffset = 0
    for (const textNode of textNodes) {
      const nodeText = textNode.textContent || ''
      const nodeStart = charOffset
      const nodeEnd = charOffset + nodeText.length

      const matchStart = matchIdx
      const matchEnd = matchIdx + issue.original.length

      if (nodeEnd <= matchStart || nodeStart >= matchEnd) {
        charOffset += nodeText.length
        continue
      }

      const mark = document.createElement('mark')
      const severity = (issue.severity || 'MEDIUM').toUpperCase()
      const colors = SEVERITY_COLORS[severity] || SEVERITY_COLORS.MEDIUM
      mark.className = 'detection-highlight'
      mark.style.backgroundColor = colors.bg
      mark.style.borderBottom = `2px solid ${colors.border}`
      mark.style.borderRadius = '2px'
      mark.style.padding = '0 2px'
      mark.style.cursor = 'pointer'
      mark.dataset.recordId = String(issue.recordId)
      mark.dataset.issueIndex = String(issue.issueIndex)
      mark.title = `${issue.typeName}: ${issue.description}`

      if (nodeStart <= matchStart && nodeEnd >= matchEnd) {
        const before = nodeText.substring(0, matchStart - nodeStart)
        const matched = nodeText.substring(matchStart - nodeStart, matchEnd - nodeStart)
        const after = nodeText.substring(matchEnd - nodeStart)

        mark.textContent = matched
        const parent = textNode.parentNode!
        if (before) parent.insertBefore(document.createTextNode(before), textNode)
        parent.insertBefore(mark, textNode)
        if (after) parent.insertBefore(document.createTextNode(after), textNode)
        parent.removeChild(textNode)
        highlightedMarks.push(mark)
        return
      }

      charOffset += nodeText.length
    }
  }

  async function scrollToAndHighlight(issue: DetectionIssueVO) {
    if (!containerRef.value || !issue.locationRef) return

    await nextTick()
    const container = containerRef.value
    const paragraphs = container.querySelectorAll('p')
    const targetPara = findParagraphByElementIndex(paragraphs, issue.locationRef.elementIndex)
    if (!targetPara) return

    targetPara.scrollIntoView({ behavior: 'smooth', block: 'center' })

    targetPara.style.transition = 'background-color 0.3s'
    targetPara.style.backgroundColor = 'rgba(255, 152, 0, 0.1)'
    setTimeout(() => {
      targetPara.style.backgroundColor = ''
    }, 3000)
  }

  return {
    applyHighlights,
    clearHighlights,
    scrollToAndHighlight,
  }
}
