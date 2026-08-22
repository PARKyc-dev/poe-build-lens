import type { FocusEvent, MouseEvent } from 'react'
import type { BuildAnalysisResult } from '../api/analysis'
import type { ShowDetailTooltip } from './detailTooltip'

function mechanicName(title: string) { return title.replace(/^(주요 패시브|마스터리|전직 노드):\s*/, '') }
function uniqueMechanics(mechanics: BuildAnalysisResult['offence'], claimed: Set<string>) {
  return mechanics.filter((mechanic) => {
    const name = mechanicName(mechanic.title)
    return !claimed.has(name) && (claimed.add(name), true)
  })
}
function PassiveGroup({ label, kind, mechanics, onShow, onHide }: { label: string; kind: '패시브' | '마스터리' | '전직'; mechanics: BuildAnalysisResult['offence']; onShow: (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, mechanic: BuildAnalysisResult['offence'][number]) => void; onHide: () => void }) {
  if (mechanics.length === 0) return null
  const icon = kind === '패시브' ? '✦' : kind === '마스터리' ? '◆' : '♛'
  return <div className="passive-group"><p className="passive-group-label">[{label}]</p><ul className="compact-mechanic-list" aria-label={`${kind} 목록`}>{mechanics.map((mechanic) => { const name = mechanicName(mechanic.title); return <li key={`${kind}-${mechanic.title}`}><button type="button" aria-label={`${kind}: ${name}`} onMouseEnter={(event) => onShow(event, mechanic)} onMouseLeave={onHide} onFocus={(event) => onShow(event, mechanic)} onBlur={onHide}><span aria-hidden="true">{icon}</span>{name}</button></li> })}</ul></div>
}
export function PassiveSection({ analysis, onShow, onHide }: { analysis: BuildAnalysisResult; onShow: ShowDetailTooltip; onHide: () => void }) {
  const ascendancies = uniqueMechanics(analysis.ascendancies, new Set())
  const claimed = new Set(ascendancies.map((mechanic) => mechanicName(mechanic.title)))
  const passives = uniqueMechanics(analysis.passiveNodes.filter((mechanic) => !mechanic.title.startsWith('마스터리:')), claimed)
  const masteries = uniqueMechanics(analysis.passiveNodes.filter((mechanic) => mechanic.title.startsWith('마스터리:')), claimed)
  return <section className="analysis-section" aria-label="패시브 트리"><h3>패시브 트리</h3><PassiveGroup label="패시브 트리" kind="패시브" mechanics={passives} onShow={(event, mechanic) => onShow(event, { id: `passive:${mechanic.title}`, label: '패시브', title: mechanicName(mechanic.title), details: [mechanic.explanation] })} onHide={onHide} /><PassiveGroup label="마스터리" kind="마스터리" mechanics={masteries} onShow={(event, mechanic) => onShow(event, { id: `mastery:${mechanic.title}`, label: '마스터리', title: mechanicName(mechanic.title), details: [mechanic.explanation] })} onHide={onHide} /><PassiveGroup label="전직 노드" kind="전직" mechanics={ascendancies} onShow={(event, mechanic) => onShow(event, { id: `ascendancy:${mechanic.title}`, label: '전직', title: mechanicName(mechanic.title), details: [mechanic.explanation] })} onHide={onHide} /></section>
}
