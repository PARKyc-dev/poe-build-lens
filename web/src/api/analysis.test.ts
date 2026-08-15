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
  buildFacts: { offence: [], defence: [], buffs: [], mobility: [], passives: [], passiveTags: [], items: [] },
  summary: { life: 4500, totalDps: 123456 },
  equipment: [],
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
        offence: [],
        defence: [],
        buffs: [],
        passives: [],
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
        buildFacts: { offence: [], defence: [], buffs: [], mobility: [], passives: [], passiveTags: [], items: [] },
      }),
    })
  })
})
