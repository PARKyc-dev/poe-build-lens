import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
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
    summary: { life: 2800, energyShield: 0, armour: 1200, evasion: 900, totalDps: 123456 },
    equipment: [{
      slot: 'Weapon 1', name: 'Doom Branch', baseName: 'Sceptre', rarity: 'RARE',
      modifiers: ['+90 to maximum Life', '+42% to Fire Resistance'],
    }],
    tree: {
      version: '3_27',
      nodes: [{ id: '1', x: 0, y: 0, allocated: true }],
      links: [],
    },
  })),
}))

import App from './App'
import { inspectBuildInBrowser } from './pob/browserPob'

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
    expect(screen.getByText('생명력 여유가 부족합니다')).toBeInTheDocument()
    expect(screen.getByText('생명력 기반 방어를 먼저 보강하세요')).toBeInTheDocument()
    expect(screen.getByText('Skill Set B')).toBeInTheDocument()
    expect(screen.getByText('Item Set C')).toBeInTheDocument()
    expect(screen.getByText('Doom Branch')).toBeInTheDocument()
    expect(screen.getByText('+90 to maximum Life')).toBeInTheDocument()
    expect(screen.queryByText('장비 상세 예시')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('할당 패시브 트리 캔버스')).not.toBeInTheDocument()

    const equipment = screen.getByRole('region', { name: '장비 상세' })
    expect(equipment.closest('aside')).toBeNull()
    expect(screen.getByRole('heading', { name: '이 빌드에서 먼저 볼 것' }).compareDocumentPosition(equipment)).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
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
      activeSkillName: null, summary: {}, equipment: [], tree: { version: '3_27', nodes: [], links: [] },
    })
  })

})
