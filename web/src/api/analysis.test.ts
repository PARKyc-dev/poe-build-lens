import { afterEach, describe, expect, it, vi } from 'vitest'
import { analyzeBuild } from './analysis'
import type { BrowserInspectResult } from '../pob/browserPob'

const inspectedFireball: BrowserInspectResult = {
  specs: [],
  skillSets: [],
  itemSets: [],
  activeSpec: 0,
  activeSkillSet: 0,
  activeItemSet: 0,
  activeSkillName: 'Fireball',
  mainSkillFlags: { isSelfCast: true },
  buildFacts: { offence: [], skills: [], defence: [], buffs: [], mobility: [], passives: [], ascendancies: [], passiveTags: [], items: [], jewels: [], performance: {} },
  summary: { life: 4500, totalDps: 123456 },
  equipment: [],
  jewels: [],
  tree: { version: '3_29', nodes: [], links: [] },
}

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('analyzeBuild', () => {
  it('sends normalized browser PoB data to the analysis API', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({
      code: 'OK',
      message: 'Success',
      returnObject: {
        gameVersion: '3.29',
        summary: 'Fireball을 주력으로 사용하는 빌드입니다.',
        offence: [],
        defence: [],
        buffs: [],
        passives: [],
        passiveNodes: [],
        ascendancies: [],
        gear: [],
        performance: [],
        overrides: [],
        unverified: [],
        evidence: [],
      },
    }), { status: 200 }))
    vi.stubGlobal('fetch', fetchMock)

    await analyzeBuild(inspectedFireball)

    expect(fetchMock).toHaveBeenCalledWith('/api/analyses', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        gameVersion: '3.29',
        buildFacts: { offence: [], skills: [], defence: [], buffs: [], mobility: [], passives: [], ascendancies: [], passiveTags: [], items: [], jewels: [], performance: {} },
      }),
    })
  })
  it('preserves skill mechanics and calculation assumptions in the request', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ code: 'OK', returnObject: {} })))
    vi.stubGlobal('fetch', fetchMock)
    const result = { ...inspectedFireball, buildFacts: {
      ...inspectedFireball.buildFacts,
      conditions: { buffLifetap: true, conditionStationary: 1 },
      skills: [{ name: 'Fire Trap', level: 20, quality: 0, qualityType: 'Default', enabled: true,
        awakened: false, effects: ['Leaves burning ground'], supports: [] }],
    } }
    await analyzeBuild(result)
    const body = JSON.parse(fetchMock.mock.calls[0][1].body)
    expect(body.buildFacts.conditions).toEqual(result.buildFacts.conditions)
    expect(body.buildFacts.skills[0].effects).toEqual(['Leaves burning ground'])
  })

})
