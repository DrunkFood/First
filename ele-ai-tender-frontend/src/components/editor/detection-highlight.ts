import { Extension, type CommandProps } from '@tiptap/core'
import { Plugin, PluginKey } from '@tiptap/pm/state'
import { Decoration, DecorationSet } from '@tiptap/pm/view'
import type { DetectionIssueVO } from '@/types/detection'

const pluginKey = new PluginKey('detectionHighlight')

export const DetectionHighlight = Extension.create({
  name: 'detectionHighlight',

  addStorage() {
    return {
      highlights: [] as DetectionIssueVO[],
    }
  },

  addCommands() {
    return {
      setDetectionHighlights: (highlights: DetectionIssueVO[]) => ({ tr, dispatch }: CommandProps) => {
        if (dispatch) {
          dispatch(tr.setMeta(pluginKey, { highlights }))
        }
        return true
      },
    }
  },

  addProseMirrorPlugins() {
    const storage = this.storage

    return [
      new Plugin({
        key: pluginKey,
        state: {
          init: () => DecorationSet.empty,
          apply: (tr, oldSet) => {
            const meta = tr.getMeta(pluginKey)
            if (meta?.highlights !== undefined) {
              storage.highlights = meta.highlights
              return buildDecorations(tr.doc, meta.highlights)
            }
            if (tr.docChanged) {
              return buildDecorations(tr.doc, storage.highlights)
            }
            return oldSet.map(tr.mapping, tr.doc)
          },
        },
        props: {
          decorations: (state) => pluginKey.getState(state),
        },
      }),
    ]
  },
})

function buildDecorations(doc: any, highlights: DetectionIssueVO[]): DecorationSet {
  const active = highlights.filter(h => h.handleStatus === 0 && h.original)
  if (active.length === 0) return DecorationSet.empty

  const decorations: Decoration[] = []

  doc.descendants((node: any, pos: number) => {
    if (!node.isText) return
    const text = node.text || ''

    for (const item of active) {
      const term = item.original
      let idx = text.indexOf(term)
      while (idx !== -1) {
        const from = pos + idx
        const to = from + term.length
        const severity = (item.severity || 'MEDIUM').toLowerCase()
        const tooltip = item.description

        decorations.push(
          Decoration.inline(from, to, {
            class: `detection-highlight detection-${severity}`,
            'data-tooltip': tooltip,
            'data-record-id': String(item.recordId),
            'data-issue-index': String(item.issueIndex),
          }),
        )
        idx = text.indexOf(term, idx + 1)
      }
    }
  })

  return decorations.length > 0
    ? DecorationSet.create(doc, decorations)
    : DecorationSet.empty
}
