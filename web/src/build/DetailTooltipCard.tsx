import type { CSSProperties } from 'react'
import type { DetailTooltip } from './detailTooltip'

export function DetailTooltipCard({ tooltip, style, ariaLabel }: { tooltip: DetailTooltip; style: CSSProperties; ariaLabel: string }) {
  const sections = tooltip.sections?.filter((section) => section.details.length > 0)
  return <aside id="item-tooltip" className="detail-tooltip" style={style} role="tooltip" aria-label={ariaLabel}>
    <small>{tooltip.label}</small>
    <header><h3>{tooltip.title}</h3>{tooltip.baseName && tooltip.baseName !== tooltip.title && <p>{tooltip.baseName}</p>}</header>
    {sections?.length ? <div className="tooltip-sections">{sections.map((section) => <section key={section.label} className={`tooltip-section tooltip-${section.label}`}><h4>{section.label}</h4>{section.details.map((detail, index) => <p key={`${detail}:${index}`}>{detail}</p>)}</section>)}</div> : <div>{tooltip.details.length > 0 ? tooltip.details.map((detail, index) => <p key={`${detail}:${index}`}>{detail}</p>) : <p>표시할 정보가 없습니다.</p>}</div>}
  </aside>
}
