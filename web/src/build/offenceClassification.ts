export type MainSkillFlags = {
  isTriggered?: boolean
  isMinion?: boolean
  isTotem?: boolean
  isTrap?: boolean
  isMine?: boolean
  isBrand?: boolean
  isAttack?: boolean
  isSelfCast?: boolean
}

export type OffenceDelivery = 'trigger' | 'minion' | 'totem' | 'trap' | 'mine' | 'brand' | 'attack' | 'self-cast' | 'unverified'

export type OffenceClassification = {
  kind: OffenceDelivery
  label: string
}

const deliveries: Array<[keyof MainSkillFlags, OffenceClassification]> = [
  ['isTriggered', { kind: 'trigger', label: '트리거 (Trigger)' }],
  ['isMinion', { kind: 'minion', label: '소환수 (Minion)' }],
  ['isTotem', { kind: 'totem', label: '토템 (Totem)' }],
  ['isTrap', { kind: 'trap', label: '트랩 (Trap)' }],
  ['isMine', { kind: 'mine', label: '마인 (Mine)' }],
  ['isBrand', { kind: 'brand', label: '브랜드 (Brand)' }],
  ['isAttack', { kind: 'attack', label: '공격 (Attack)' }],
  ['isSelfCast', { kind: 'self-cast', label: '직접 시전 (Self-Cast)' }],
]

const unverified: OffenceClassification = {
  kind: 'unverified',
  label: '확인 불가 (Unverified)',
}

export function classifyOffenceDelivery(flags: MainSkillFlags | null): OffenceClassification {
  if (!flags) return unverified

  return deliveries.find(([flag]) => flags[flag])?.[1] ?? unverified
}
