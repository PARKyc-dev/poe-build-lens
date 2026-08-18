import { useState } from 'react'
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

const equipmentSlots = [
  'Weapon 1', 'Weapon 2', 'Helmet', 'Body Armour', 'Gloves', 'Boots', 'Amulet',
  'Ring 1', 'Ring 2', 'Belt', 'Flask 1', 'Flask 2', 'Flask 3', 'Flask 4', 'Flask 5',
]

const slotClasses: Record<string, string> = {
  'Weapon 1': 'weapon-1',
  'Weapon 2': 'weapon-2',
  Helmet: 'helmet',
  'Body Armour': 'body-armour',
  Gloves: 'gloves',
  Boots: 'boots',
  Amulet: 'amulet',
  'Ring 1': 'ring-1',
  'Ring 2': 'ring-2',
  Belt: 'belt',
  'Flask 1': 'flask-1',
  'Flask 2': 'flask-2',
  'Flask 3': 'flask-3',
  'Flask 4': 'flask-4',
  'Flask 5': 'flask-5',
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
  const equipmentBySlot = new Map(result.equipment.map((item) => [item.slot, item]))
  const [selectedEquipmentKey, setSelectedEquipmentKey] = useState<string | null>(null)
  const selectedEquipment = selectedEquipmentKey?.startsWith('equipment:') ? equipmentBySlot.get(selectedEquipmentKey.slice('equipment:'.length)) : undefined
  const selectedJewel = selectedEquipmentKey?.startsWith('jewel:') ? result.jewels.find((item) => item.socket === selectedEquipmentKey.slice('jewel:'.length)) : undefined
  const selectedItem = selectedEquipment ?? selectedJewel
  const selectedItemLabel = selectedEquipment ? slotLabels[selectedEquipment.slot] ?? selectedEquipment.slot : selectedJewel?.kind === 'cluster' ? '군 주얼' : '주얼'
  const jewels = result.jewels.filter((item) => item.kind === 'jewel')
  const clusterJewels = result.jewels.filter((item) => item.kind === 'cluster')

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
            <AnalysisSection title="분석 근거" mechanics={[...analysis.buffs, ...analysis.passives, ...analysis.passiveNodes, ...analysis.ascendancies, ...analysis.gear, ...analysis.performance]} emptyMessage="분석 근거가 없습니다." />
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
        {result.equipment.length === 0 && result.jewels.length === 0 ? <p>활성 장비 세트와 패시브 트리에 장착된 아이템이 없습니다.</p> : <>
        <div className="equipment-stage">
          <ul className="equipment-layout">
            {equipmentSlots.map((slot) => {
              const item = equipmentBySlot.get(slot)
              const label = slotLabels[slot]
              return <li key={slot} className={`equipment-slot ${slotClasses[slot]} ${item ? `rarity-${item.rarity.toLowerCase()}` : 'empty'}`} onMouseEnter={() => setSelectedEquipmentKey(item ? `equipment:${slot}` : null)} onMouseLeave={() => setSelectedEquipmentKey(null)}>
                {item ? <button type="button" aria-label={`${label} 슬롯: ${item.name}`} aria-describedby={selectedEquipmentKey === `equipment:${slot}` ? 'equipment-tooltip' : undefined} onFocus={() => setSelectedEquipmentKey(`equipment:${slot}`)} onBlur={() => setSelectedEquipmentKey(null)}>
                  {item.imageUrl ? <img src={item.imageUrl} alt="" /> : <span className="equipment-slot-label">{label}</span>}
                  <strong>{item.name}</strong>
                </button> : <span aria-label={`${label} 슬롯`} className="equipment-slot-label">{label}</span>}
              </li>
            })}
          </ul>
          {result.jewels.length > 0 && <div className="jewel-groups">
            {jewels.length > 0 && <section aria-label="주얼"><h3>주얼</h3><ul className="jewel-list">{jewels.map((item) => <li key={item.socket} className={`rarity-${item.rarity.toLowerCase()}`} onMouseEnter={() => setSelectedEquipmentKey(`jewel:${item.socket}`)} onMouseLeave={() => setSelectedEquipmentKey(null)}><button type="button" aria-label={`주얼: ${item.name}`} aria-describedby={selectedEquipmentKey === `jewel:${item.socket}` ? 'equipment-tooltip' : undefined} onFocus={() => setSelectedEquipmentKey(`jewel:${item.socket}`)} onBlur={() => setSelectedEquipmentKey(null)}><strong>{item.name}</strong><span>{item.baseName}</span></button></li>)}</ul></section>}
            {clusterJewels.length > 0 && <section aria-label="군 주얼"><h3>군 주얼</h3><ul className="jewel-list">{clusterJewels.map((item) => <li key={item.socket} className={`rarity-${item.rarity.toLowerCase()}`} onMouseEnter={() => setSelectedEquipmentKey(`jewel:${item.socket}`)} onMouseLeave={() => setSelectedEquipmentKey(null)}><button type="button" aria-label={`군 주얼: ${item.name}`} aria-describedby={selectedEquipmentKey === `jewel:${item.socket}` ? 'equipment-tooltip' : undefined} onFocus={() => setSelectedEquipmentKey(`jewel:${item.socket}`)} onBlur={() => setSelectedEquipmentKey(null)}><strong>{item.name}</strong><span>{item.baseName}</span></button></li>)}</ul></section>}
          </div>}
          {selectedItem && <aside id="equipment-tooltip" className={`equipment-tooltip rarity-${selectedItem.rarity.toLowerCase()}`} style={{ position: 'absolute' }} role="tooltip" aria-label={`${selectedItem.name} 장비 정보`}>
            <small>{selectedItemLabel}</small>
            <h3>{selectedItem.name}</h3>
            {selectedItem.baseName && <p>{selectedItem.baseName}</p>}
            <div>{selectedItem.modifiers.map((modifier) => <p key={modifier}>{modifier}</p>)}</div>
          </aside>}
        </div>
        <ul className="equipment-details">
          {result.equipment.map((item) => <li key={item.slot} className={`rarity-${item.rarity.toLowerCase()}`}>
            <small>{slotLabels[item.slot] ?? item.slot}</small>
            <strong>{item.name}</strong>
            {item.baseName && <span>{item.baseName}</span>}
            {item.modifiers.map((modifier) => <em key={modifier}>{modifier}</em>)}
          </li>)}
          {result.jewels.map((item) => <li key={item.socket} className={`rarity-${item.rarity.toLowerCase()}`}>
            <small>{item.kind === 'cluster' ? '군 주얼' : '주얼'}</small>
            <strong>{item.name}</strong>
            {item.baseName && <span>{item.baseName}</span>}
            {item.modifiers.map((modifier) => <em key={modifier}>{modifier}</em>)}
          </li>)}
        </ul>
        </>}
      </section>
    </main>
  )
}
