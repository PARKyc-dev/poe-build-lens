import { createBuildInsight } from './buildInsight'
import type { BuildAnalysisResult } from '../api/analysis'
import type { BrowserInspectResult } from '../pob/browserPob'

function formatNumber(value: number | null | undefined) {
  return typeof value === 'number' ? Math.round(value).toLocaleString('ko-KR') : '계산 중'
}

function ActiveEntry({ values, activeId }: {
  values: BrowserInspectResult['specs']
  activeId: number
}) {
  return <>{values.find((value) => value.id === activeId)?.title ?? '기본 설정'}</>
}

const slotLabels: Record<string, string> = {
  'Weapon 1': '주무기',
  'Weapon 2': '보조 무기',
  Helmet: '투구',
  'Body Armour': '갑옷',
  Gloves: '장갑',
  Boots: '장화',
  Amulet: '목걸이',
  'Ring 1': '반지 1',
  'Ring 2': '반지 2',
  Belt: '허리띠',
  'Flask 1': '플라스크 1',
  'Flask 2': '플라스크 2',
  'Flask 3': '플라스크 3',
  'Flask 4': '플라스크 4',
  'Flask 5': '플라스크 5',
}

const defenceLabels: Record<string, { label: string; unit?: string }> = {
  life: { label: '생명력' },
  'energy-shield': { label: '에너지 보호막' },
  mana: { label: '마나' },
  armour: { label: '방어도' },
  evasion: { label: '회피' },
  'fire-resistance': { label: '화염 저항', unit: '%' },
  'cold-resistance': { label: '냉기 저항', unit: '%' },
  'lightning-resistance': { label: '번개 저항', unit: '%' },
  'chaos-resistance': { label: '카오스 저항', unit: '%' },
  block: { label: '막기 확률', unit: '%' },
  'spell-block': { label: '주문 막기 확률', unit: '%' },
  'spell-suppression': { label: '주문 억제 확률', unit: '%' },
  guard: { label: '가드 활성화' },
  ward: { label: '와드' },
  'attack-dodge': { label: '공격 회피 확률', unit: '%' },
  'spell-dodge': { label: '주문 회피 확률', unit: '%' },
  'damage-avoidance': { label: '피해 회피 확률', unit: '%' },
}

const activeConfigurationKinds = new Set([
  'life', 'energy-shield', 'fire-resistance', 'cold-resistance', 'lightning-resistance',
  'chaos-resistance', 'block', 'spell-block', 'spell-suppression',
])

function AnalysisSection({ title, mechanics, emptyMessage }: {
  title: string
  mechanics: BuildAnalysisResult['offence']
  emptyMessage: string
}) {
  return <section className="analysis-section" aria-label={title}>
    <h3>{title}</h3>
    {mechanics.length > 0 ? mechanics.map((mechanic) => <article className="insight-card mechanic-card" key={`${mechanic.title}-${mechanic.explanation}`}>
      <h4>{mechanic.title}</h4>
      <p>{mechanic.explanation}</p>
    </article>) : <p className="analysis-empty">{emptyMessage}</p>}
  </section>
}

export function BuildDetailPage({ result, analysis, onNewInspection }: {
  result: BrowserInspectResult
  analysis: BuildAnalysisResult
  onNewInspection: () => void
}) {
  const insight = createBuildInsight({ summary: result.summary })

  return (
    <main className="build-detail" aria-label="빌드 상세">
      <header className="detail-header">
        <div>
          <p className="eyebrow">POE LENS · BUILD INSIGHT</p>
          <h1>{result.activeSkillName ?? '분석 중인 빌드'} <span>Insight</span></h1>
          <p className="detail-subtitle"><ActiveEntry values={result.specs} activeId={result.activeSpec} /> · PoB 헤드리스 분석</p>
        </div>
        <button type="button" className="secondary-button" onClick={onNewInspection}>새 빌드 검사</button>
      </header>

      <div className="detail-grid">
        <section className="insight-panel" aria-labelledby="insight-title">
          <p className="section-kicker">BUILD INSIGHT</p>
          <h2 id="insight-title">이 빌드에서 먼저 볼 것</h2>

          <div className="analysis-sections">
            <AnalysisSection title="공격 기재 분석" mechanics={analysis.offence} emptyMessage="분석할 공격 기재가 없습니다." />
            <AnalysisSection title="방어 기재 분석" mechanics={analysis.defence} emptyMessage="분석할 방어 기재가 없습니다." />
            <AnalysisSection title="유틸리티·버프 분석" mechanics={analysis.buffs} emptyMessage="현재 활성화된 유틸리티·버프 기재가 없습니다." />
            <AnalysisSection title="핵심 패시브 분석" mechanics={analysis.passives} emptyMessage="분석 가능한 핵심 패시브가 없습니다." />
          </div>

          {analysis.unverified.length > 0 && <section className="analysis-section" aria-label="미검증 항목">
            <h3>미검증 항목</h3>
            {analysis.unverified.map((message) => <p className="analysis-empty" key={message}>{message}</p>)}
          </section>}

          {analysis.evidence.length > 0 && <section className="analysis-section" aria-label="근거">
            <h3>근거</h3>
            {analysis.evidence.length > 0 && <ul>
              {analysis.evidence.map((evidence) => <li key={evidence.sourceUrl}>
                <a href={evidence.sourceUrl}>{evidence.name}</a>
              </li>)}
            </ul>}
          </section>}

          <section className="analysis-section priorities-section" aria-label="강화 우선순위">
            <h3>강화 우선순위</h3>
            <ol className="priority-list">
              {insight.priorities.map((item, index) => <li key={item.title}>
                <span>{index + 1}</span><div><h4>{item.title}</h4><p>{item.description}</p></div>
              </li>)}
            </ol>
          </section>
        </section>

        <aside className="build-sidebar" aria-label="빌드 구성">
          <section>
            <p className="section-kicker">ACTIVE CONFIGURATION</p>
            <h2>주요 수치</h2>
            <dl aria-label="방어 수치">
              {result.buildFacts.defence.filter((fact) => activeConfigurationKinds.has(fact.kind)).map((fact) => {
                const detail = defenceLabels[fact.kind] ?? { label: fact.kind }
                return <div key={fact.kind}><dt>{detail.label}</dt><dd>{formatNumber(fact.value)}{detail.unit}</dd></div>
              })}
            </dl>
          </section>
        </aside>
      </div>

      <section className="equipment-panel" aria-label="장비 상세">
        <p className="section-kicker">EQUIPMENT</p>
        <h2>장비 상세</h2>
        {result.equipment.length === 0 ? <p>활성 장비 세트에 장착된 아이템이 없습니다.</p> : <ul className="equipment-list">
          {result.equipment.map((item) => <li key={item.slot} className={`rarity-${item.rarity.toLowerCase()}`}>
            <div className="equipment-icon" aria-label={`${slotLabels[item.slot] ?? item.slot} 아이콘`}>
              {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span>{slotLabels[item.slot] ?? item.slot}</span>}
            </div>
            <div className="equipment-copy">
              <small>{slotLabels[item.slot] ?? item.slot}</small>
              <strong>{item.name}</strong>
              {item.baseName && <span>{item.baseName}</span>}
              {item.modifiers.map((modifier) => <em key={modifier}>{modifier}</em>)}
            </div>
          </li>)}
        </ul>}
      </section>
    </main>
  )
}
