import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

vi.mock('./pob/browserPob', () => ({
  inspectBuildInBrowser: vi.fn(async () => ({
    specs: [{ id: 1, title: 'Spec A' }],
    skillSets: [{ id: 2, title: 'Skill Set B' }],
    itemSets: [{ id: 3, title: 'Item Set C' }],
    activeSpec: 1,
    activeSkillSet: 2,
    activeItemSet: 3,
    activeSkillName: 'Fireball',
    mainSkillFlags: { isAttack: false, isSelfCast: true },
    buildFacts: {
      offence: [
        { name: 'Fireball', role: 'primary', delivery: 'self-cast', tags: ['spell'] },
        { name: 'Flame Wall', role: 'secondary', delivery: 'self-cast', tags: ['spell'] },
      ],
      defence: [
        { kind: 'life', value: 2800 },
        { kind: 'fire-resistance', value: 75 },
        { kind: 'block', value: 60 },
      ],
      buffs: [{ name: 'Determination', kind: 'aura', appliesTo: 'player', tags: [] }],
      mobility: [{ name: 'Shield Charge' }],
      passives: [{ name: 'Tasalio, Cleansing Water', effects: ['+100% to Fire Resistance'], tags: ['fire-resistance'] }],
      passiveTags: ['fire-resistance'],
      items: [],
    },
    summary: { life: 2800, energyShield: 0, armour: 1200, evasion: 900, totalDps: 123456 },
    equipment: [{
      slot: 'Weapon 1', name: 'Doom Branch', baseName: 'Sceptre', rarity: 'RARE',
      modifiers: ['+90 to maximum Life', '+42% to Fire Resistance'],
    }, {
      slot: 'Flask 1', name: 'Granite Flask', baseName: 'Granite Flask', rarity: 'MAGIC',
      modifiers: ['+1500 to Armour during Flask effect'],
    }],
    tree: {
      version: '3_27',
      nodes: [{ id: '1', x: 0, y: 0, allocated: true }],
      links: [],
    },
  })),
}))

vi.mock('./api/analysis', () => ({
  analyzeBuild: vi.fn(async () => ({
    gameVersion: '3.29',
    offence: [{
      title: '발사체 적중과 폭발',
      explanation: 'Fireball은 적중 지점에서 폭발 피해를 줍니다.',
    }],
    defence: [{
      title: '생명력·저항·막기 기반 방어',
      explanation: '생명력으로 피해를 견디고, 원소 저항으로 원소 피해를 줄이며, 막기로 적중 피해의 일부를 막는 방어 구조입니다.',
    }],
    buffs: [{
      title: '상태 이상·주문 방어 유틸리티',
      explanation: '감전 면역과 주문 막기 태그가 적용되어 상태 이상과 주문 적중을 함께 대응합니다.',
    }],
    passives: [{
      title: '저항 핵심 패시브',
      explanation: '패시브 효과 태그가 원소 저항을 방어 축으로 보강합니다.',
    }],
    overrides: [],
    unverified: [],
    evidence: [{
      name: 'Path of Exile Wiki',
      sourceUrl: 'https://www.poewiki.net/wiki/Fireball',
      collectedAt: '2026-08-15',
      reviewed: true,
    }],
  })),
}))

import App from './App'
import { inspectBuildInBrowser } from './pob/browserPob'
import { analyzeBuild } from './api/analysis'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('build analysis', () => {
  it('shows the browser PoB engine as ready without a worker HTTP request', () => {
    render(<App />)

    expect(screen.getByLabelText('PoB 엔진 상태: 준비 완료')).toBeInTheDocument()
    expect(screen.getByText('PoB 엔진 준비 완료')).toBeInTheDocument()
  })

  it('shows the Korean inspect entry point without a separate inspect heading', () => {
    render(<App />)

    expect(screen.getByRole('heading', { name: 'PoE Lens', level: 1 })).toBeInTheDocument()
    expect(screen.getByText('PoB 엔진 준비 완료')).toBeInTheDocument()
    expect(screen.getByLabelText('검사할 PoB 코드, pobb.in 또는 XML')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'PoB 검사' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'PoB headless inspect' })).not.toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Build analysis' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Analyze build' })).not.toBeInTheDocument()
  })

  it('sends a PoB export to the browser engine and changes to the insight detail view', async () => {
    render(<App />)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('검사할 PoB 코드, pobb.in 또는 XML'), '<PathOfBuilding />')
    await user.click(screen.getByRole('button', { name: 'PoB 검사' }))

    expect(await screen.findByRole('main', { name: '빌드 상세' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Fireball Insight' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '이 빌드에서 먼저 볼 것' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '공격 기재 분석' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '방어 기재 분석' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '유틸리티·버프 분석' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '핵심 패시브 분석' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '발사체 적중과 폭발' })).toBeInTheDocument()
    expect(screen.getByText('Fireball은 적중 지점에서 폭발 피해를 줍니다.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '생명력·저항·막기 기반 방어' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '상태 이상·주문 방어 유틸리티' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '저항 핵심 패시브' })).toBeInTheDocument()
    expect(screen.getByText('생명력 기반 방어를 먼저 보강하세요')).toBeInTheDocument()
    expect(screen.getByText('Doom Branch')).toBeInTheDocument()
    expect(screen.getAllByText('Granite Flask')).toHaveLength(2)
    expect(screen.getAllByText('플라스크 1')).toHaveLength(2)
    expect(screen.getByText('+90 to maximum Life')).toBeInTheDocument()
    expect(screen.queryByText('장비 상세 예시')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('할당 패시브 트리 캔버스')).not.toBeInTheDocument()

    const equipment = screen.getByRole('region', { name: '장비 상세' })
    expect(equipment.closest('aside')).toBeNull()
    expect(screen.getByRole('heading', { name: '이 빌드에서 먼저 볼 것' }).compareDocumentPosition(equipment)).toBe(Node.DOCUMENT_POSITION_FOLLOWING)

    const configuration = screen.getByRole('complementary', { name: '빌드 구성' })
    expect(screen.queryByRole('region', { name: '핵심 수치' })).not.toBeInTheDocument()
    expect(configuration).not.toHaveTextContent('주력 공격: Fireball')
    expect(configuration).not.toHaveTextContent('보조 공격: Flame Wall')
    expect(configuration).not.toHaveTextContent('오라: Determination')
    expect(configuration).not.toHaveTextContent('패시브: Tasalio, Cleansing Water')
    expect(within(configuration).getByText('생명력')).toBeInTheDocument()
    expect(within(configuration).getByText('2,800')).toBeInTheDocument()
    expect(within(configuration).getByText('화염 저항')).toBeInTheDocument()
    expect(configuration).toHaveTextContent('75%')
    expect(within(configuration).getByText('막기 확률')).toBeInTheDocument()
    expect(configuration).toHaveTextContent('60%')
    expect(configuration).not.toHaveTextContent('전직 설정')
    expect(configuration).not.toHaveTextContent('스킬 세트')
    expect(configuration).not.toHaveTextContent('장비 세트')
    expect(screen.queryByText('주력 공격 기재')).not.toBeInTheDocument()
  })

  it('shows the API unverified result instead of the browser-only pending mechanic', async () => {
    vi.mocked(analyzeBuild).mockResolvedValueOnce({
      gameVersion: '3.30',
      offence: [],
      defence: [],
      buffs: [],
      passives: [],
      overrides: [],
      unverified: ['3.30 / Fireball / self-cast 조합은 아직 검증되지 않았습니다.'],
      evidence: [],
    })
    render(<App />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('검사할 PoB 코드, pobb.in 또는 XML'), '<PathOfBuilding />')
    await user.click(screen.getByRole('button', { name: 'PoB 검사' }))

    expect(await screen.findByText('3.30 / Fireball / self-cast 조합은 아직 검증되지 않았습니다.')).toBeInTheDocument()
    expect(screen.queryByText('활성 스킬 메커니즘 준비 중')).not.toBeInTheDocument()
  })

  it('changes to the detail loading screen before the browser engine returns', async () => {
    let resolveInspection: (value: Awaited<ReturnType<typeof inspectBuildInBrowser>>) => void
    vi.mocked(inspectBuildInBrowser).mockImplementationOnce(() => new Promise((resolve) => {
      resolveInspection = resolve
    }))
    render(<App />)
    const user = userEvent.setup()

    await user.type(screen.getByLabelText('검사할 PoB 코드, pobb.in 또는 XML'), '<PathOfBuilding />')
    await user.click(screen.getByRole('button', { name: 'PoB 검사' }))

    expect(screen.getByRole('main', { name: '빌드 분석 중' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'PoB 빌드를 분석하고 있습니다' })).toBeInTheDocument()
    expect(screen.getByText('빌드 계산')).toBeInTheDocument()

    resolveInspection!({
      specs: [], skillSets: [], itemSets: [], activeSpec: 0, activeSkillSet: 0, activeItemSet: 0,
      activeSkillName: null, mainSkillFlags: null, buildFacts: { offence: [], defence: [], buffs: [], mobility: [], passives: [], passiveTags: [], items: [] }, summary: {}, equipment: [], tree: { version: '3_27', nodes: [], links: [] },
    })
  })

})
