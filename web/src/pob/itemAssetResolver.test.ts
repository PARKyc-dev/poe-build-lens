import { describe, expect, it } from 'vitest'

import { resolveItemImage } from './itemAssetResolver'

describe('resolveItemImage', () => {
  it('resolves a rare item through its base name', () => {
    expect(resolveItemImage({ name: 'Woe Chant', baseName: 'Imbued Wand' }))
      .toBe('https://web.poecdn.com/image/Art/2DItems/Weapons/OneHandWeapons/Wands/Wand3.png')
  })

  it('resolves a unique item through its base name', () => {
    expect(resolveItemImage({ name: 'Dawnbreaker', baseName: 'Colossal Tower Shield' }))
      .toBe('https://web.poecdn.com/image/Art/2DItems/Armours/Shields/ShieldStr6.png')
  })

  it('falls back to the item name when no base name is available', () => {
    expect(resolveItemImage({ name: 'Crimson Jewel', baseName: null }))
      .toBe('https://web.poecdn.com/image/Art/2DItems/Jewels/basicstr.png')
  })

  it('returns null for an item outside the asset catalogs', () => {
    expect(resolveItemImage({ name: 'Unknown', baseName: 'Unknown Base' })).toBeNull()
  })
})
