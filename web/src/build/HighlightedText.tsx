import type { FocusEvent, MouseEvent } from 'react'
import type { ShowDetailTooltip } from './detailTooltip'

export type TextHighlight = { name: string; details: string[] }

function escapePattern(value: string) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function HighlightedText({ text, highlights, onShow, onHide }: { text: string; highlights: TextHighlight[]; onShow: ShowDetailTooltip; onHide: () => void }) {
  const detailsByName = new Map(highlights.filter((highlight) => highlight.name).map((highlight) => [highlight.name, highlight.details]))
  const names = [...detailsByName.keys()].sort((left, right) => right.length - left.length)
  if (names.length === 0) return <>{text}</>
  const parts = text.split(new RegExp(`(${names.map(escapePattern).join('|')})`, 'g'))
  const show = (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, name: string) => onShow(event, {
    id: `ascendancy-reference:${name}`, label: '전직', title: name, details: detailsByName.get(name) ?? [],
  })

  return <>{parts.map((part, index) => detailsByName.has(part)
    ? <button type="button" className="ascendancy-reference ascendancy-highlight" aria-label={`전직 효과: ${part}`} key={`${part}-${index}`} onMouseEnter={(event) => show(event, part)} onMouseLeave={onHide} onFocus={(event) => show(event, part)} onBlur={onHide}>{part}</button>
    : part)}</>
}
