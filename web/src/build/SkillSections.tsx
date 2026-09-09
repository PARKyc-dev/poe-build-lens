import type { FocusEvent, MouseEvent } from 'react'
import type { BuildAnalysisResult } from '../api/analysis'
import type { BrowserInspectResult } from '../pob/browserPob'
import type { ShowDetailTooltip } from './detailTooltip'
import './AnalysisSection.css'
import './SkillSections.css'
import { HighlightedText, type TextHighlight } from './HighlightedText'

const offenceSections = [
  { prefix: '공격이 작동하는 과정: ', label: '작동 과정', number: '01', kind: 'flow' },
  { prefix: '핵심 상호작용: ', label: '핵심 상호작용', number: '02', kind: 'cards' },
  { prefix: '보조젬 연결: ', label: '보조젬 연결', number: '03', kind: 'flow' },
  { prefix: '운용 방식: ', label: '운용 방식', number: '04', kind: 'operation' },
] as const

const defenceKindLabels: Record<string, string> = {
  life: '생명력', 'energy-shield': '에너지 보호막', mana: '마나', armour: '방어도', evasion: '회피', ward: '와드',
  'physical-mitigation': '물리 피해 경감', resistances: '원소·카오스 저항', block: '공격 막기', 'spell-block': '주문 막기',
  'spell-suppression': '주문 피해 억제', 'attack-dodge': '공격 회피', 'spell-dodge': '주문 회피', 'damage-avoidance': '피해 회피',
  'life-regeneration': '생명력 재생', 'energy-shield-recovery': '에너지 보호막 회복',
}

function uniqueByName<T extends { name: string }>(entries: T[], claimed = new Set<string>()) {
  return entries.filter((entry) => !claimed.has(entry.name) && (claimed.add(entry.name), true))
}

function SkillSection({ title, emptyMessage, entries, mechanics, highlights, onShow, onHighlightShow, onHide }: { title: string; emptyMessage: string; entries: Array<{ name: string; details: string[] }>; mechanics: BuildAnalysisResult['offence']; highlights: TextHighlight[]; onShow: (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, entry: { name: string; details: string[] }) => void; onHighlightShow: ShowDetailTooltip; onHide: () => void }) {
  return <section className="analysis-section" aria-label={title}><h3>{title}</h3>{entries.length > 0 ? <ul className="compact-mechanic-list">{entries.map((entry) => <li key={entry.name}><button type="button" aria-label={`${title.replace(' 기재', '')}: ${entry.name}`} onMouseEnter={(event) => onShow(event, entry)} onMouseLeave={onHide} onFocus={(event) => onShow(event, entry)} onBlur={onHide}><span aria-hidden="true">✦</span>{entry.name}</button></li>)}</ul> : <p className="analysis-empty">{emptyMessage}</p>}{mechanics.length > 0 && <div className="mechanism-analysis">{mechanics.map((mechanic) => <article key={mechanic.title}><strong>{mechanic.title}</strong><p><HighlightedText text={mechanic.explanation} highlights={highlights} onShow={onHighlightShow} onHide={onHide} /></p></article>)}</div>}</section>
}

function DetailText({ text, highlights, onShow, onHide }: { text: string; highlights: TextHighlight[]; onShow: ShowDetailTooltip; onHide: () => void }) {
  return <HighlightedText text={text} highlights={highlights} onShow={onShow} onHide={onHide} />
}

function OffenceSectionDetails({ mechanic, kind, highlights, onShow, onHide }: { mechanic: BuildAnalysisResult['offence'][number]; kind: typeof offenceSections[number]['kind']; highlights: TextHighlight[]; onShow: ShowDetailTooltip; onHide: () => void }) {
  const details = mechanic.details ?? []
  if (details.length === 0) return <p className="offence-section-summary"><DetailText text={mechanic.explanation} highlights={highlights} onShow={onShow} onHide={onHide} /></p>
  const steps = details.filter((detail) => detail.type === 'step')
  const conditions = details.filter((detail) => detail.type === 'condition')
  if (kind === 'cards') return <div className="offence-interactions">{details.map((detail) => <article key={`${detail.label}-${detail.explanation}`}><strong>{detail.label}</strong><p><DetailText text={detail.explanation} highlights={highlights} onShow={onShow} onHide={onHide} /></p></article>)}</div>
  return <>
    {steps.length > 0 && <ol className="offence-flow">{steps.map((detail, index) => <li key={`${detail.label}-${detail.explanation}`}><small>{String(index + 1).padStart(2, '0')}</small><strong>{detail.label}</strong><p><DetailText text={detail.explanation} highlights={highlights} onShow={onShow} onHide={onHide} /></p></li>)}</ol>}
    {conditions.length > 0 && <div className="offence-conditions">{conditions.map((detail) => <article key={`${detail.label}-${detail.explanation}`}><strong>{detail.label}</strong><p><DetailText text={detail.explanation} highlights={highlights} onShow={onShow} onHide={onHide} /></p></article>)}</div>}
    <p className="offence-section-summary"><DetailText text={mechanic.explanation} highlights={highlights} onShow={onShow} onHide={onHide} /></p>
  </>
}

function OffenceAnalysis({ entries, mechanics, highlights, onShow, onHighlightShow, onHide }: { entries: Array<{ name: string; details: string[] }>; mechanics: BuildAnalysisResult['offence']; highlights: TextHighlight[]; onShow: (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, entry: { name: string; details: string[] }) => void; onHighlightShow: ShowDetailTooltip; onHide: () => void }) {
  return <section className="analysis-section offence-analysis" aria-label="공격 기재"><h3>공격 기재</h3>{entries.length === 0 ? <p className="analysis-empty">분석할 공격 기재가 없습니다.</p> : entries.map((entry) => {
    const sections = offenceSections.flatMap((section) => {
      const mechanic = mechanics.find((candidate) => candidate.title === `${section.prefix}${entry.name}`)
      return mechanic ? [{ ...section, mechanic }] : []
    })
    return <div className="offence-group" key={entry.name}>
      <button className="offence-name" type="button" aria-label={`공격: ${entry.name}`} onMouseEnter={(event) => onShow(event, entry)} onMouseLeave={onHide} onFocus={(event) => onShow(event, entry)} onBlur={onHide}><span aria-hidden="true">✦</span>{entry.name}</button>
      {sections.length > 0 ? <div className="offence-section-list">{sections.map(({ mechanic, label, number, kind }) => <section className={`offence-detail-card offence-detail-${kind}`} aria-label={`${entry.name} ${label}`} key={mechanic.title}>
        <header><small>SECTION {number}</small><h4>{label}</h4></header>
        <OffenceSectionDetails mechanic={mechanic} kind={kind} highlights={highlights} onShow={onHighlightShow} onHide={onHide} />
      </section>)}</div> : <p className="analysis-empty">표시할 공격 분석이 없습니다.</p>}
    </div>
  })}</section>
}

function defenceTitle(title: string) {
  if (title === '저항 체계' || title === '저항 핵심 상호작용') return title
  const [section, kind] = title.split(': ', 2)
  return kind ? `${section} · ${defenceKindLabels[kind] ?? kind}` : title
}

function DefenceAnalysis({ entries, mechanics, highlights, onShow, onHighlightShow, onHide }: { entries: Array<{ name: string; details: string[] }>; mechanics: BuildAnalysisResult['defence']; highlights: TextHighlight[]; onShow: (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, entry: { name: string; details: string[] }) => void; onHighlightShow: ShowDetailTooltip; onHide: () => void }) {
  return <section className="analysis-section defence-analysis" aria-label="방어 기재"><h3>방어 기재</h3>
    {entries.length > 0 ? <ul className="compact-mechanic-list">{entries.map((entry) => <li key={entry.name}><button type="button" aria-label={`방어: ${entry.name}`} onMouseEnter={(event) => onShow(event, entry)} onMouseLeave={onHide} onFocus={(event) => onShow(event, entry)} onBlur={onHide}><span aria-hidden="true">✦</span>{entry.name}</button></li>)}</ul> : <p className="analysis-empty">분석할 방어 기재가 없습니다.</p>}
    {mechanics.length > 0 && <div className="defence-section-list">{mechanics.map((mechanic, index) => {
      const details = mechanic.details ?? []
      const steps = details.filter((detail) => detail.type === 'step')
      const cards = details.filter((detail) => detail.type === 'interaction')
      const conditions = details.filter((detail) => detail.type === 'condition')
      return <section className="offence-detail-card defence-detail-card" aria-label={defenceTitle(mechanic.title)} key={mechanic.title}>
        <header><small>SECTION {String(index + 1).padStart(2, '0')}</small><h4>{defenceTitle(mechanic.title)}</h4></header>
        {steps.length > 0 && <ol className="offence-flow">{steps.map((detail, stepIndex) => <li key={`${detail.label}-${detail.explanation}`}><small>{String(stepIndex + 1).padStart(2, '0')}</small><strong>{detail.label}</strong><p><DetailText text={detail.explanation} highlights={highlights} onShow={onHighlightShow} onHide={onHide} /></p></li>)}</ol>}
        {cards.length > 0 && <div className="offence-interactions">{cards.map((detail) => <article key={`${detail.label}-${detail.explanation}`}><strong>{detail.label}</strong><p><DetailText text={detail.explanation} highlights={highlights} onShow={onHighlightShow} onHide={onHide} /></p></article>)}</div>}
        {conditions.length > 0 && <div className="offence-conditions">{conditions.map((detail) => <article key={`${detail.label}-${detail.explanation}`}><strong>{detail.label}</strong><p><DetailText text={detail.explanation} highlights={highlights} onShow={onHighlightShow} onHide={onHide} /></p></article>)}</div>}
        <p className="offence-section-summary"><DetailText text={mechanic.explanation} highlights={highlights} onShow={onHighlightShow} onHide={onHide} /></p>
      </section>
    })}</div>}
  </section>
}

export function SkillSections({ result, analysis, onShow, onHide }: { result: BrowserInspectResult; analysis: BuildAnalysisResult; onShow: ShowDetailTooltip; onHide: () => void }) {
  const defenceTags = new Set(['life', 'energy-shield', 'life-regeneration', 'energy-shield-recovery', 'armour', 'evasion', 'ward', 'physical-mitigation', 'fire-resistance', 'cold-resistance', 'lightning-resistance', 'chaos-resistance', 'block', 'spell-block', 'spell-suppression', 'attack-dodge', 'spell-dodge', 'damage-avoidance', 'shock-immunity', 'shock-avoidance', 'freeze-immunity', 'chill-immunity', 'ignite-immunity'])
  const isCurseOrMark = (kind: string) => kind === 'curse' || kind === 'mark'
  const buffs = result.buildFacts.buffs.filter((buff) => buff.kind !== 'flask')
  const curses = buffs.filter((buff) => isCurseOrMark(buff.kind))
  const defenceBuffs = buffs.filter((buff) => !isCurseOrMark(buff.kind) && (buff.kind === 'guard' || buff.tags.some((tag) => defenceTags.has(tag))))
  const tooltipDetails = new Map((result.skillTooltips ?? []).map((tooltip) => [tooltip.name, tooltip.details]))
  const detailsFor = (name: string) => tooltipDetails.get(name) ?? ['PoB에서 이 스킬의 설명을 찾지 못했습니다.']
  const claimed = new Set<string>()
  const attacks = uniqueByName(result.buildFacts.offence.map((skill) => ({ name: skill.name, details: detailsFor(skill.name) })), claimed)
  const defence = uniqueByName(defenceBuffs.map((buff) => ({ name: buff.name, details: detailsFor(buff.name) })), claimed)
  const buffsOnly = uniqueByName(buffs.filter((buff) => !isCurseOrMark(buff.kind) && !defenceBuffs.includes(buff)).map((buff) => ({ name: buff.name, details: detailsFor(buff.name) })), claimed)
  const curseEntries = uniqueByName(curses.map((curse) => ({ name: curse.name, details: detailsFor(curse.name) })), claimed)
  const curseNames = new Set(curses.map((curse) => curse.name))
  const isCurseMechanic = (mechanic: BuildAnalysisResult['buffs'][number]) => [...curseNames].some((name) => mechanic.title === name || mechanic.title.endsWith(`: ${name}`))
  const curseMechanics = analysis.buffs.filter(isCurseMechanic)
  const buffMechanics = analysis.buffs.filter((mechanic) => !isCurseMechanic(mechanic))
  const mobility = uniqueByName(result.buildFacts.mobility.map((skill) => ({ name: skill.name, details: detailsFor(skill.name) })), claimed)
  const ascendancyHighlights = analysis.ascendancies.map((mechanic) => ({ name: mechanic.title.replace(/^전직 노드:\s*/, ''), details: [mechanic.explanation] }))
  return <><OffenceAnalysis entries={attacks} mechanics={analysis.offence} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `attack:${entry.name}`, label: '공격', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><DefenceAnalysis entries={defence} mechanics={analysis.defence} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `defence:${entry.name}`, label: '방어', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="버프 기재" emptyMessage="표시할 버프 기재가 없습니다." entries={buffsOnly} mechanics={buffMechanics} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `buff:${entry.name}`, label: '버프', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="저주/징표 기재" emptyMessage="표시할 저주 또는 징표가 없습니다." entries={curseEntries} mechanics={curseMechanics} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `curse:${entry.name}`, label: '저주/징표', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /><SkillSection title="이동기 기재" emptyMessage="표시할 이동기가 없습니다." entries={mobility} mechanics={[]} highlights={ascendancyHighlights} onShow={(event, entry) => onShow(event, { id: `mobility:${entry.name}`, label: '이동기', title: entry.name, details: entry.details })} onHighlightShow={onShow} onHide={onHide} /></>
}
