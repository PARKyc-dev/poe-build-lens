import { useState, type FocusEvent, type MouseEvent } from 'react'
import { createBuildInsight } from './buildInsight'
import { EquipmentSection } from './EquipmentSection'
import { PassiveSection } from './PassiveSection'
import { SkillSections } from './SkillSections'
import type { DetailTooltip } from './detailTooltip'
import type { BuildAnalysisResult } from '../api/analysis'
import type { BrowserInspectResult } from '../pob/browserPob'

const defenceLabels: Record<string, { label: string; unit?: string }> = {
  life: { label: '생명력' }, 'energy-shield': { label: '에너지 보호막' }, mana: { label: '마나' }, armour: { label: '방어도' }, evasion: { label: '회피' }, 'fire-resistance': { label: '화염 저항', unit: '%' }, 'cold-resistance': { label: '냉기 저항', unit: '%' }, 'lightning-resistance': { label: '번개 저항', unit: '%' }, 'chaos-resistance': { label: '카오스 저항', unit: '%' }, block: { label: '막기 확률', unit: '%' }, 'spell-block': { label: '주문 막기 확률', unit: '%' }, 'spell-suppression': { label: '주문 억제 확률', unit: '%' }, guard: { label: '가드 활성화' }, ward: { label: '와드' }, 'attack-dodge': { label: '공격 회피 확률', unit: '%' }, 'spell-dodge': { label: '주문 회피 확률', unit: '%' }, 'damage-avoidance': { label: '피해 회피 확률', unit: '%' },
}

const activeConfigurationKinds = new Set(['life', 'energy-shield', 'fire-resistance', 'cold-resistance', 'lightning-resistance', 'chaos-resistance', 'block', 'spell-block', 'spell-suppression'])

function formatNumber(value: number | null | undefined) {
  return typeof value === 'number' ? Math.round(value).toLocaleString('ko-KR') : '계산 중'
}

function ActiveEntry({ values, activeId }: { values: BrowserInspectResult['specs']; activeId: number }) {
  return <>{values.find((value) => value.id === activeId)?.title ?? '기본 설정'}</>
}

export function BuildDetailPage({ result, analysis, onNewInspection }: {
  result: BrowserInspectResult
  analysis: BuildAnalysisResult
  onNewInspection: () => void
}) {
  const insight = createBuildInsight({ summary: result.summary })
  const [tooltip, setTooltip] = useState<DetailTooltip | null>(null)
  const showTooltip = (event: MouseEvent<HTMLButtonElement> | FocusEvent<HTMLButtonElement>, nextTooltip: Omit<DetailTooltip, 'rect'>) => setTooltip({ ...nextTooltip, rect: event.currentTarget.getBoundingClientRect() })
  const tooltipLeft = tooltip && tooltip.rect.right + 12 + 300 > window.innerWidth ? Math.max(12, tooltip.rect.left - 312) : (tooltip?.rect.right ?? 0) + 12
  const tooltipTop = tooltip ? Math.max(12, Math.min(tooltip.rect.top, window.innerHeight - 180)) : 0

  return <main className="build-detail" aria-label="빌드 상세">
    <header className="detail-header"><div><p className="eyebrow">POE LENS · BUILD INSIGHT</p><h1>{result.activeSkillName ?? '분석 중인 빌드'} <span>Insight</span></h1><p className="detail-subtitle"><ActiveEntry values={result.specs} activeId={result.activeSpec} /> · PoB 헤드리스 분석</p></div><button type="button" className="secondary-button" onClick={onNewInspection}>새 빌드 검사</button></header>
    <div className="detail-grid">
      <section className="insight-panel" aria-labelledby="insight-title">
        <p className="section-kicker">BUILD INSIGHT</p><h2 id="insight-title">이 빌드에서 먼저 볼 것</h2>
        <div className="analysis-sections"><SkillSections result={result} analysis={analysis} onShow={showTooltip} onHide={() => setTooltip(null)} /><PassiveSection analysis={analysis} onShow={showTooltip} onHide={() => setTooltip(null)} /></div>
        {analysis.unverified.length > 0 && <section className="analysis-section" aria-label="미검증 항목"><h3>미검증 항목</h3>{analysis.unverified.map((message) => <p className="analysis-empty" key={message}>{message}</p>)}</section>}
        <section className="analysis-section priorities-section" aria-label="강화 우선순위"><h3>강화 우선순위</h3><ol className="priority-list">{insight.priorities.map((item, index) => <li key={item.title}><span>{index + 1}</span><div><h4>{item.title}</h4><p>{item.description}</p></div></li>)}</ol></section>
      </section>
      <aside className="build-sidebar" aria-label="빌드 구성"><section><p className="section-kicker">ACTIVE CONFIGURATION</p><h2>주요 수치</h2><dl aria-label="방어 수치">{result.buildFacts.defence.filter((fact) => activeConfigurationKinds.has(fact.kind)).map((fact) => { const detail = defenceLabels[fact.kind] ?? { label: fact.kind }; return <div key={fact.kind}><dt>{detail.label}</dt><dd>{formatNumber(fact.value)}{detail.unit}</dd></div> })}</dl></section></aside>
    </div>
    <EquipmentSection result={result} tooltip={tooltip} onShow={showTooltip} onHide={() => setTooltip(null)} />
    {tooltip && <aside id="item-tooltip" className="detail-tooltip" style={{ position: 'fixed', left: tooltipLeft, top: tooltipTop }} role="tooltip" aria-label={`${tooltip.title} ${['공격', '방어', '버프', '패시브', '마스터리', '전직'].includes(tooltip.label) ? '상세 정보' : '장비 정보'}`}><small>{tooltip.label}</small><h3>{tooltip.title}</h3><div>{tooltip.details.length > 0 ? tooltip.details.map((detail) => <p key={detail}>{detail}</p>) : <p>표시할 핵심 메커니즘 옵션이 없습니다.</p>}</div></aside>}
  </main>
}
