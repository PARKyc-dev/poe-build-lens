import type { FocusEvent, MouseEvent } from 'react'
import type { BuildAnalysisResult } from '../api/analysis'
import type { BrowserInspectResult } from '../pob/browserPob'
import type { ShowDetailTooltip } from './detailTooltip'

function uniqueByName<T extends { name: string }>(entries: T[], claimed = new Set<string>()) {
  return entries.filter((entry) => !claimed.has(entry.name) && (claimed.add(entry.name), true))
}

function SkillSection({ title, emptyMessage, entries, mechanics, onShow, onHide }: { title: string; emptyMessage: string; entries: Array<{ name: string; details: string[] }>; mechanics: BuildAnalysisResult['offence']; onShow: (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, entry: { name: string; details: string[] }) => void; onHide: () => void }) {
  return <section className="analysis-section" aria-label={title}><h3>{title}</h3>{entries.length > 0 ? <ul className="compact-mechanic-list">{entries.map((entry) => <li key={entry.name}><button type="button" aria-label={`${title.replace(' 기재', '')}: ${entry.name}`} onMouseEnter={(event) => onShow(event, entry)} onMouseLeave={onHide} onFocus={(event) => onShow(event, entry)} onBlur={onHide}><span aria-hidden="true">✦</span>{entry.name}</button></li>)}</ul> : <p className="analysis-empty">{emptyMessage}</p>}{mechanics.length > 0 && <div className="mechanism-analysis">{mechanics.map((mechanic) => <p key={mechanic.title}>{mechanic.explanation}</p>)}</div>}</section>
}

export function SkillSections({ result, analysis, onShow, onHide }: { result: BrowserInspectResult; analysis: BuildAnalysisResult; onShow: ShowDetailTooltip; onHide: () => void }) {
  const defenceTags = new Set(['life', 'energy-shield', 'life-regeneration', 'energy-shield-recovery', 'armour', 'evasion', 'ward', 'physical-mitigation', 'fire-resistance', 'cold-resistance', 'lightning-resistance', 'chaos-resistance', 'block', 'spell-block', 'spell-suppression', 'attack-dodge', 'spell-dodge', 'damage-avoidance', 'shock-immunity', 'shock-avoidance', 'freeze-immunity', 'chill-immunity', 'ignite-immunity'])
  const buffs = result.buildFacts.buffs.filter((buff) => buff.kind !== 'flask')
  const defenceBuffs = buffs.filter((buff) => buff.kind === 'guard' || buff.tags.some((tag) => defenceTags.has(tag)))
  const tooltipDetails = new Map((result.skillTooltips ?? []).map((tooltip) => [tooltip.name, tooltip.details]))
  const detailsFor = (name: string) => tooltipDetails.get(name) ?? ['PoB에서 이 스킬의 설명을 찾지 못했습니다.']
  const claimed = new Set<string>()
  const attacks = uniqueByName(result.buildFacts.offence.map((skill) => ({ name: skill.name, details: detailsFor(skill.name) })), claimed)
  const defence = uniqueByName(defenceBuffs.map((buff) => ({ name: buff.name, details: detailsFor(buff.name) })), claimed)
  const buffsOnly = uniqueByName(buffs.filter((buff) => !defenceBuffs.includes(buff)).map((buff) => ({ name: buff.name, details: detailsFor(buff.name) })), claimed)
  const mobility = uniqueByName(result.buildFacts.mobility.map((skill) => ({ name: skill.name, details: detailsFor(skill.name) })), claimed)
  return <><SkillSection title="공격 기재" emptyMessage="분석할 공격 기재가 없습니다." entries={attacks} mechanics={analysis.offence} onShow={(event, entry) => onShow(event, { id: `attack:${entry.name}`, label: '공격', title: entry.name, details: entry.details })} onHide={onHide} /><SkillSection title="방어 기재" emptyMessage="분석할 방어 기재가 없습니다." entries={defence} mechanics={analysis.defence} onShow={(event, entry) => onShow(event, { id: `defence:${entry.name}`, label: '방어', title: entry.name, details: entry.details })} onHide={onHide} /><SkillSection title="버프 기재" emptyMessage="표시할 버프 기재가 없습니다." entries={buffsOnly} mechanics={analysis.buffs} onShow={(event, entry) => onShow(event, { id: `buff:${entry.name}`, label: '버프', title: entry.name, details: entry.details })} onHide={onHide} /><SkillSection title="이동기 기재" emptyMessage="표시할 이동기가 없습니다." entries={mobility} mechanics={analysis.mobility ?? []} onShow={(event, entry) => onShow(event, { id: `mobility:${entry.name}`, label: '이동기', title: entry.name, details: entry.details })} onHide={onHide} /></>
}
