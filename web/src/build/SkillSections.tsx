import type { FocusEvent, MouseEvent } from 'react'
import type { BuildAnalysisResult } from '../api/analysis'
import type { BrowserInspectResult } from '../pob/browserPob'
import type { ShowDetailTooltip } from './detailTooltip'
import { HighlightedText, type TextHighlight } from './HighlightedText'

function uniqueByName<T extends { name: string }>(entries: T[], claimed = new Set<string>()) {
  return entries.filter((entry) => !claimed.has(entry.name) && (claimed.add(entry.name), true))
}

function SkillSection({ title, emptyMessage, entries, mechanics, highlights, onShow, onHighlightShow, onHide }: { title: string; emptyMessage: string; entries: Array<{ name: string; details: string[] }>; mechanics: BuildAnalysisResult['offence']; highlights: TextHighlight[]; onShow: (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, entry: { name: string; details: string[] }) => void; onHighlightShow: ShowDetailTooltip; onHide: () => void }) {
  return <section className="analysis-section" aria-label={title}><h3>{title}</h3>{entries.length > 0 ? <ul className="compact-mechanic-list">{entries.map((entry) => <li key={entry.name}><button type="button" aria-label={`${title.replace(' 기재', '')}: ${entry.name}`} onMouseEnter={(event) => onShow(event, entry)} onMouseLeave={onHide} onFocus={(event) => onShow(event, entry)} onBlur={onHide}><span aria-hidden="true">✦</span>{entry.name}</button></li>)}</ul> : <p className="analysis-empty">{emptyMessage}</p>}{mechanics.length > 0 && <div className="mechanism-analysis">{mechanics.map((mechanic) => <article key={mechanic.title}><strong>{mechanic.title}</strong><p><HighlightedText text={mechanic.explanation} highlights={highlights} onShow={onHighlightShow} onHide={onHide} /></p></article>)}</div>}</section>
}

export function SkillSections({ result, analysis, onShow, onHide }: { result: BrowserInspectResult; analysis: BuildAnalysisResult; onShow: ShowDetailTooltip; onHide: () => void }) {
  const defenceTags = new Set(['life', 'energy-shield', 'life-regeneration', 'energy-shield-recovery', 'armour', 'evasion', 'ward', 'physical-mitigation', 'fire-resistance', 'cold-resistance', 'lightning-resistance', 'chaos-resistance', 'block', 'spell-block', 'spell-suppression', 'attack-dodge', 'spell-dodge', 'damage-avoidance', 'shock-immunity', 'shock-avoidance', 'freeze-immunity', 'chill-immunity', 'ignite-immunity'])
  const buffs = result.buildFacts.buffs.filter((buff) => buff.kind !== 'flask')
  const curses = buffs.filter((buff) => buff.kind === 'curse')
  const defenceBuffs = buffs.filter((buff) => buff.kind !== 'curse' && (buff.kind === 'guard' || buff.tags.some((tag) => defenceTags.has(tag))))
  const tooltipDetails = new Map((result.skillTooltips ?? []).map((tooltip) => [tooltip.name, tooltip.details]))
  const detailsFor = (name: string) => tooltipDetails.get(name) ?? ['PoB에서 이 스킬의 설명을 찾지 못했습니다.']
  const claimed = new Set<string>()
  const attacks = uniqueByName(result.buildFacts.offence.map((skill) => ({ name: skill.name, details: detailsFor(skill.name) })), claimed)
  const defence = uniqueByName(defenceBuffs.map((buff) => ({ name: buff.name, details: detailsFor(buff.name) })), claimed)
  const buffsOnly = uniqueByName(buffs.filter((buff) => buff.kind !== 'curse' && !defenceBuffs.includes(buff)).map((buff) => ({ name: buff.name, details: detailsFor(buff.name) })), claimed)
  const curseEntries = uniqueByName(curses.map((curse) => ({ name: curse.name, details: detailsFor(curse.name) })), claimed)
  const curseNames = new Set(curses.map((curse) => curse.name))
  const isCurseMechanic = (mechanic: BuildAnalysisResult['buffs'][number]) => [...curseNames].some((name) => mechanic.title === name || mechanic.title.endsWith(`: ${name}`))
  const curseMechanics = analysis.buffs.filter(isCurseMechanic)
  const buffMechanics = analysis.buffs.filter((mechanic) => !isCurseMechanic(mechanic))
  const mobility = uniqueByName(result.buildFacts.mobility.map((skill) => ({ name: skill.name, details: detailsFor(skill.name) })), claimed)
  const ascendancyHighlights = analysis.ascendancies.map((mechanic) => ({ name: mechanic.title.replace(/^전직 노드:\s*/, ''), details: [mechanic.explanation] }))
  return <><SkillSection title="공격 기재" emptyMessage="분석할 공격 기재가 없습니다." entries={attacks} mechanics={analysis.offence} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `attack:${entry.name}`, label: '공격', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="방어 기재" emptyMessage="분석할 방어 기재가 없습니다." entries={defence} mechanics={analysis.defence} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `defence:${entry.name}`, label: '방어', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="버프 기재" emptyMessage="표시할 버프 기재가 없습니다." entries={buffsOnly} mechanics={buffMechanics} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `buff:${entry.name}`, label: '버프', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="저주/징표 기재" emptyMessage="표시할 저주 또는 징표가 없습니다." entries={curseEntries} mechanics={curseMechanics} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `curse:${entry.name}`, label: '저주/징표', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="이동기 기재" emptyMessage="표시할 이동기가 없습니다." entries={mobility} mechanics={[]} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `mobility:${entry.name}`, label: '이동기', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /></>
}
