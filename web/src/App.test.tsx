import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
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
    skillTooltips: [
      { name: 'Fireball', details: ['Fireball fires a ball of fire that explodes.'] },
      { name: 'Flame Wall', details: ['Flame Wall creates a wall of fire.'] },
      { name: 'Determination', details: ['Determination grants additional Armour.'] },
      { name: 'Tempest Shield', details: ['Tempest Shield grants spell block chance.'] },
      { name: 'Hatred', details: ['Hatred grants extra cold damage.'] },
      { name: 'Molten Shell', details: ['Molten Shell absorbs damage.'] },
    ],
    buildFacts: {
      offence: [
        { name: 'Fireball', role: 'primary', delivery: 'self-cast', tags: ['spell'] },
        { name: 'Flame Wall', role: 'secondary', delivery: 'self-cast', tags: ['spell'] },
      ],
      skills: [],
      defence: [
        { kind: 'life', value: 2800 },
        { kind: 'fire-resistance', value: 75 },
        { kind: 'block', value: 60 },
      ],
      buffs: [
        { name: 'Determination', kind: 'aura', appliesTo: 'player', tags: ['armour'] },
        { name: 'Tempest Shield', kind: 'aura', appliesTo: 'player', tags: ['spell-block'] },
        { name: 'Hatred', kind: 'aura', appliesTo: 'player', tags: ['cold'] },
        { name: 'Determination', kind: 'buff', appliesTo: 'player', tags: [] },
        { name: 'Molten Shell', kind: 'guard', appliesTo: 'player', tags: [] },
      ],
      mobility: [{ name: 'Shield Charge' }],
      passives: [{ name: 'Tasalio, Cleansing Water', kind: 'notable', effects: ['+100% to Fire Resistance'], tags: ['fire-resistance'] }],
      ascendancies: [],
      passiveTags: ['fire-resistance'],
      items: [],
      jewels: [],
      performance: {},
    },
    summary: { life: 2800, energyShield: 0, armour: 1200, evasion: 900, totalDps: 123456 },
    equipment: [{
      slot: 'Weapon 1', name: 'Doom Branch', baseName: 'Sceptre', rarity: 'RARE',
      modifiers: ['+90 to maximum Life', '+42% to Fire Resistance', '15% reduced Mana Cost of Skills'],
    }, {
      slot: 'Flask 1', name: 'Granite Flask', baseName: 'Granite Flask', rarity: 'MAGIC',
      modifiers: ['+1500 to Armour during Flask effect'],
    }],
    jewels: [{
      socket: '1234', name: 'Crimson Jewel of the Fox', baseName: 'Crimson Jewel', rarity: 'MAGIC',
      modifiers: ['+7% to maximum Life'], kind: 'jewel',
    }, {
      socket: '5678', name: 'Large Cluster Jewel', baseName: 'Large Cluster Jewel', rarity: 'RARE',
      modifiers: ['Added Small Passive Skills grant: 12% increased Fire Damage'], kind: 'cluster',
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
    passiveNodes: [{
      title: '주요 패시브: Growth and Decay',
      explanation: '적용된 효과: Regenerate 1% of Life per second',
    }, {
      title: '주요 패시브: Tasalio, Cleansing Water',
      explanation: '패시브에 중복된 전직 이름입니다.',
    }, {
      title: '마스터리: Arcane Mastery',
      explanation: '적용된 효과: 10% increased Mana Reservation Efficiency',
    }],
    ascendancies: [{
      title: '전직 노드: Aspect of the Cat',
      explanation: 'Occultist 전직의 적용된 효과: 고양이의 위상이 활성화됩니다.',
    }, {
      title: '전직 노드: Tasalio, Cleansing Water',
      explanation: '전직 노드에 표시됩니다.',
    }],
    gear: [{
      title: '장비: Helmet · Mind Crown',
      explanation: '옵션: 15% reduced Mana Cost of Skills',
    }],
    performance: [],
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
    expect(screen.getByRole('heading', { name: '공격 기재' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '방어 기재' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '버프 기재' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '패시브 트리' })).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '분석 근거' })).not.toBeInTheDocument()
    const attack = screen.getByRole('region', { name: '공격 기재' })
    const defence = screen.getByRole('region', { name: '방어 기재' })
    const buffs = screen.getByRole('region', { name: '버프 기재' })
    expect(within(attack).getByRole('button', { name: '공격: Fireball' })).toBeInTheDocument()
    expect(within(attack).getByRole('button', { name: '공격: Flame Wall' })).toBeInTheDocument()
    expect(within(defence).getByRole('button', { name: '방어: Determination' })).toBeInTheDocument()
    expect(within(defence).getByRole('button', { name: '방어: Tempest Shield' })).toBeInTheDocument()
    expect(within(defence).getByRole('button', { name: '방어: Molten Shell' })).toBeInTheDocument()
    expect(within(buffs).getByRole('button', { name: '버프: Hatred' })).toBeInTheDocument()
    expect(within(buffs).queryByRole('button', { name: '버프: Determination' })).not.toBeInTheDocument()
    expect(screen.queryByText('표시할 버프 기재가 없습니다.')).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '발사체 적중과 폭발' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '생명력·저항·막기 기반 방어' })).not.toBeInTheDocument()
    expect(within(attack).getByText('Fireball은 적중 지점에서 폭발 피해를 줍니다.')).toBeInTheDocument()
    expect(within(defence).getByText('생명력으로 피해를 견디고, 원소 저항으로 원소 피해를 줄이며, 막기로 적중 피해의 일부를 막는 방어 구조입니다.')).toBeInTheDocument()
    expect(within(buffs).getByText('감전 면역과 주문 막기 태그가 적용되어 상태 이상과 주문 적중을 함께 대응합니다.')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '패시브: Growth and Decay' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '마스터리: Arcane Mastery' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '전직: Aspect of the Cat' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '전직: Tasalio, Cleansing Water' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '패시브: Tasalio, Cleansing Water' })).not.toBeInTheDocument()
    expect(screen.getByText('[패시브 트리]')).toBeInTheDocument()
    expect(screen.getByText('[마스터리]')).toBeInTheDocument()
    expect(screen.getByText('[전직 노드]')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '주요 패시브: Growth and Decay' })).not.toBeInTheDocument()
    await user.hover(screen.getByRole('button', { name: '패시브: Growth and Decay' }))
    expect(screen.getByRole('tooltip', { name: 'Growth and Decay 상세 정보' })).toHaveTextContent('Regenerate 1% of Life per second')
    await user.unhover(screen.getByRole('button', { name: '패시브: Growth and Decay' }))
    await user.hover(within(attack).getByRole('button', { name: '공격: Fireball' }))
    expect(screen.getByRole('tooltip', { name: 'Fireball 상세 정보' })).toHaveTextContent('Fireball fires a ball of fire that explodes.')
    expect(screen.getByRole('tooltip', { name: 'Fireball 상세 정보' })).not.toHaveTextContent('Fireball은 적중 지점에서 폭발 피해를 줍니다.')
    await user.unhover(within(attack).getByRole('button', { name: '공격: Fireball' }))
    await user.hover(within(defence).getByRole('button', { name: '방어: Determination' }))
    expect(screen.getByRole('tooltip', { name: 'Determination 상세 정보' })).toHaveTextContent('Determination grants additional Armour.')
    await user.unhover(within(defence).getByRole('button', { name: '방어: Determination' }))
    await user.hover(within(buffs).getByRole('button', { name: '버프: Hatred' }))
    expect(screen.getByRole('tooltip', { name: 'Hatred 상세 정보' })).toHaveTextContent('Hatred grants extra cold damage.')
    expect(screen.getByRole('tooltip', { name: 'Hatred 상세 정보' })).not.toHaveTextContent('활성화된 버프 효과입니다.')
    await user.unhover(within(buffs).getByRole('button', { name: '버프: Hatred' }))
    expect(screen.getByText('생명력 기반 방어를 먼저 보강하세요')).toBeInTheDocument()
    expect(screen.getByText('+90 to maximum Life')).toBeInTheDocument()
    expect(screen.queryByText('장비 상세 예시')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('할당 패시브 트리 캔버스')).not.toBeInTheDocument()

    const equipment = screen.getByRole('region', { name: '장비 상세' })
    expect(equipment.closest('aside')).toBeNull()
    expect(screen.getByRole('heading', { name: '이 빌드에서 먼저 볼 것' }).compareDocumentPosition(equipment)).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
    expect(within(screen.getByLabelText('주무기 슬롯: Doom Branch')).getByText('Doom Branch')).toBeInTheDocument()
    expect(within(screen.getByLabelText('플라스크 1 슬롯: Granite Flask')).getByText('Granite Flask')).toBeInTheDocument()
    expect(screen.getByLabelText('투구 슬롯')).toHaveTextContent('투구')
    await user.hover(screen.getByLabelText('주무기 슬롯: Doom Branch'))
    const tooltip = screen.getByRole('tooltip', { name: 'Doom Branch 장비 정보' })
    expect(tooltip).toHaveTextContent('+90 to maximum Life')
    expect(tooltip).toHaveTextContent('+42% to Fire Resistance')
    expect(window.getComputedStyle(tooltip).position).toBe('fixed')
    await user.unhover(screen.getByLabelText('주무기 슬롯: Doom Branch'))
    expect(screen.queryByRole('tooltip', { name: 'Doom Branch 장비 정보' })).not.toBeInTheDocument()
    fireEvent.focus(screen.getByRole('button', { name: '주무기 슬롯: Doom Branch' }))
    expect(screen.getByRole('tooltip', { name: 'Doom Branch 장비 정보' })).toBeInTheDocument()
    fireEvent.blur(screen.getByRole('button', { name: '주무기 슬롯: Doom Branch' }))
    expect(screen.queryByRole('tooltip', { name: 'Doom Branch 장비 정보' })).not.toBeInTheDocument()

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

  it('shows socketed jewels in the equipment panel', async () => {
    render(<App />)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('검사할 PoB 코드, pobb.in 또는 XML'), '<PathOfBuilding />')
    await user.click(screen.getByRole('button', { name: 'PoB 검사' }))

    expect(await screen.findByRole('heading', { name: '주얼' })).toBeInTheDocument()
    const jewel = screen.getByRole('button', { name: '주얼: Crimson Jewel of the Fox' })
    expect(jewel).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '군 주얼' })).toBeInTheDocument()
    const clusterJewel = screen.getByRole('button', { name: '군 주얼: Large Cluster Jewel' })
    expect(clusterJewel).toBeInTheDocument()

    await user.hover(jewel)
    expect(screen.getByRole('tooltip', { name: 'Crimson Jewel of the Fox 장비 정보' })).toHaveTextContent('+7% to maximum Life')
    await user.unhover(jewel)
    fireEvent.focus(clusterJewel)
    expect(screen.getByRole('tooltip', { name: 'Large Cluster Jewel 장비 정보' })).toHaveTextContent('12% increased Fire Damage')
    fireEvent.blur(clusterJewel)
    expect(screen.queryByRole('tooltip', { name: 'Large Cluster Jewel 장비 정보' })).not.toBeInTheDocument()
  })

  it('places equipment tooltips next to the hovered slot', async () => {
    render(<App />)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('검사할 PoB 코드, pobb.in 또는 XML'), '<PathOfBuilding />')
    await user.click(screen.getByRole('button', { name: 'PoB 검사' }))

    const weapon = await screen.findByRole('button', { name: '주무기 슬롯: Doom Branch' })
    const flask = screen.getByRole('button', { name: '플라스크 1 슬롯: Granite Flask' })
    vi.spyOn(weapon, 'getBoundingClientRect').mockReturnValue({ top: 80, right: 120, left: 20 } as DOMRect)
    vi.spyOn(flask, 'getBoundingClientRect').mockReturnValue({ top: 420, right: 600, left: 500 } as DOMRect)

    await user.hover(weapon)
    expect(screen.getByRole('tooltip', { name: 'Doom Branch 장비 정보' })).toHaveStyle({ left: '132px', top: '80px' })
    await user.unhover(weapon)
    await user.hover(flask)
    expect(screen.getByRole('tooltip', { name: 'Granite Flask 장비 정보' })).toHaveStyle({ left: '612px', top: '420px' })
  })

  it('shows the API unverified result instead of the browser-only pending mechanic', async () => {
    vi.mocked(analyzeBuild).mockResolvedValueOnce({
      gameVersion: '3.30',
      offence: [],
      defence: [],
      buffs: [],
      passives: [],
      passiveNodes: [],
      ascendancies: [],
      gear: [],
      performance: [],
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
      activeSkillName: null, mainSkillFlags: null, buildFacts: { offence: [], skills: [], defence: [], buffs: [], mobility: [], passives: [], ascendancies: [], passiveTags: [], items: [], jewels: [], performance: {} }, summary: {}, equipment: [], jewels: [], tree: { version: '3_27', nodes: [], links: [] },
    })

    expect(await screen.findByText('활성 장비 세트와 패시브 트리에 장착된 아이템이 없습니다.')).toBeInTheDocument()
    expect(screen.queryByLabelText('투구 슬롯')).not.toBeInTheDocument()
  })

})
