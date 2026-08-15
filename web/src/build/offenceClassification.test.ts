import { describe, expect, it } from 'vitest'

import { classifyOffenceDelivery } from './offenceClassification'

describe('classifyOffenceDelivery', () => {
  it('returns the delivery mechanism with Korean and English labels', () => {
    expect(classifyOffenceDelivery({ isAttack: true, isTotem: true })).toEqual({
      kind: 'totem',
      label: '토템 (Totem)',
    })
  })

  it('uses trigger before every other matching mechanism', () => {
    expect(classifyOffenceDelivery({ isTriggered: true, isAttack: true })).toEqual({
      kind: 'trigger',
      label: '트리거 (Trigger)',
    })
  })

  it.each([
    [{ isMinion: true }, { kind: 'minion', label: '소환수 (Minion)' }],
    [{ isTrap: true }, { kind: 'trap', label: '트랩 (Trap)' }],
    [{ isMine: true }, { kind: 'mine', label: '마인 (Mine)' }],
    [{ isBrand: true }, { kind: 'brand', label: '브랜드 (Brand)' }],
    [{ isAttack: true, isSelfCast: true }, { kind: 'attack', label: '공격 (Attack)' }],
    [{ isSelfCast: true }, { kind: 'self-cast', label: '직접 시전 (Self-Cast)' }],
  ])('returns the matching delivery label for %o', (flags, classification) => {
    expect(classifyOffenceDelivery(flags)).toEqual(classification)
  })

  it('returns unverified when PoB supplies no delivery evidence', () => {
    expect(classifyOffenceDelivery(null)).toEqual({
      kind: 'unverified',
      label: '확인 불가 (Unverified)',
    })
  })
})
