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

/**
 * 规范化文本：移除所有空白字符（空格/换行/制表符/全角空格/不间断空格）。
 * 与后端 TextNormalizeUtil 对齐，用于跨空白差异的文本匹配。
 */
function normalizeText(text: string): string {
  // \s 已涵盖空格/换行/制表符/不间断空格( )/全角空格(　)等
  return (text || '').replace(/\s/g, '')
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
   * 按 original 文本定位顶层段落。
   *
   * 不使用 elementIndex 索引 article.children：docx-preview 在 breakPages 下会拆分
   * 含分页符 <w:br w:type="page"/> 的段落，使 DOM child 数量 > POI bodyElements 数量，
   * 两个 index 空间不一致，直接用 elementIndex 取 child 会产生累积偏移。
   * 改用规范化文本匹配，彻底绕开 index 偏移。
   */
  function findParagraphByText(original: string): HTMLElement | null {
    if (!containerRef.value || !original) return null
    const normOrig = normalizeText(original)
    if (!normOrig) return null

    const paras = containerRef.value.querySelectorAll('section > article > p')
    for (const para of Array.from(paras)) {
      if (normalizeText(para.textContent || '').includes(normOrig)) {
        return para as HTMLElement
      }
    }
    return null
  }

  /**
   * 按 tableIndex/rowIndex/cellIndex 定位表格单元格。
   * docx-preview 把每个 <w:tbl> 渲染成 1 个 article child（不拆分），
   * 与 POI bodyElements 的表格序号一致，可安全用序号索引。
   */
  function findTableCell(tableIndex: number, rowIndex: number, cellIndex: number): HTMLElement | null {
    if (!containerRef.value) return null
    const tables = containerRef.value.querySelectorAll('section > article > table')
    const table = tables[tableIndex] as HTMLTableElement | undefined
    if (!table) return null
    const row = table.rows[rowIndex]
    if (!row) return null
    const cell = row.cells[cellIndex]
    return (cell as HTMLElement) || null
  }

  /**
   * 在容器内找包含 original 的段落（表格 cell 内可能有多段）。
   * 找不到则返回容器本身，交给 highlightTextInElement 做跨 textNode 匹配。
   */
  function findSubElementByText(container: HTMLElement, original: string): HTMLElement {
    const normOrig = normalizeText(original)
    if (normOrig) {
      const paras = container.querySelectorAll('p')
      for (const para of Array.from(paras)) {
        if (normalizeText(para.textContent || '').includes(normOrig)) {
          return para as HTMLElement
        }
      }
    }
    return container
  }

  /**
   * 根据 issue.locationRef 定位目标 DOM 元素：
   * - paragraph：按 original 文本匹配顶层段落
   * - table：按 tableIndex/rowIndex/cellIndex 定位 cell，再在 cell 内按文本匹配段落
   */
  function locateTargetElement(issue: DetectionIssueVO): HTMLElement | null {
    const ref = issue.locationRef
    if (!ref) return null

    if (ref.type === 'table') {
      const cell = findTableCell(ref.tableIndex ?? 0, ref.rowIndex ?? 0, ref.cellIndex ?? 0)
      if (!cell) return null
      return findSubElementByText(cell, issue.original)
    }
    return findParagraphByText(issue.original)
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

      const targetEl = locateTargetElement(issue)
      if (!targetEl) continue

      highlightTextInElement(targetEl, issue)
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

    // 先清除旧高亮，再定位并创建文本级 <mark> 高亮
    clearHighlights()

    const targetEl = locateTargetElement(issue)
    if (!targetEl) return

    highlightTextInElement(targetEl, issue)

    // 滚动到目标位置：优先滚动预览容器，fallback 到 scrollIntoView
    const scrollContainer = getScrollContainer()
    if (scrollContainer && scrollContainer !== containerRef.value) {
      const containerRect = scrollContainer.getBoundingClientRect()
      const targetRect = targetEl.getBoundingClientRect()
      const scrollOffset = targetRect.top - containerRect.top + scrollContainer.scrollTop
        - containerRect.height / 2 + targetRect.height / 2
      scrollContainer.scrollTo({ top: scrollOffset, behavior: 'instant' })
    } else {
      targetEl.scrollIntoView({ behavior: 'instant', block: 'center' })
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
