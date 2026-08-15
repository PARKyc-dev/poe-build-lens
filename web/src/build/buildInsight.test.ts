import { describe, expect, it } from 'vitest'

import { createBuildInsight } from './buildInsight'

describe('createBuildInsight', () => {
  it('highlights low life without selecting a skill-name mechanic rule', () => {
    const insight = createBuildInsight({
      summary: { life: 2800, energyShield: 0, armour: 1200, evasion: 900 },
    })

    expect(insight.cautions.map((item) => item.title)).toContain('생명력 여유가 부족합니다')
    expect(insight.priorities[0].title).toBe('생명력 기반 방어를 먼저 보강하세요')
  })

  it('returns numerical priorities when no numerical weakness is detected', () => {
    const insight = createBuildInsight({
      summary: {},
    })

    expect(insight.priorities[0].title).toBe('피해와 방어의 다음 병목을 확인하세요')
  })
})
