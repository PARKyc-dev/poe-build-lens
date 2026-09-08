import { describe, expect, it } from 'vitest'

import { resolveItemImage } from './itemAssetResolver'

describe('resolveItemImage', () => {
  it('resolves a rare item through its base name', () => {
    expect(resolveItemImage({ name: 'Woe Chant', baseName: 'Kinetic Wand', rarity: 'RARE' }))
      .toBe('https://assets.pobb.in/1/Kinetic%20Wand.webp')
  })

  it('resolves a unique through its unique name', () => {
    expect(resolveItemImage({ name: 'Dawnbreaker', baseName: 'Colossal Tower Shield', rarity: 'UNIQUE' }))
      .toBe('https://assets.pobb.in/1/Dawnbreaker.webp')
  })

  it('returns null for an item outside the generated catalog', () => {
    expect(resolveItemImage({ name: 'Unknown', baseName: 'Unknown Base', rarity: 'RARE' })).toBeNull()
  })
})
