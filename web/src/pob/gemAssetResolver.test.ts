import { describe, expect, it } from 'vitest'
import { resolveGemImage } from './gemAssetResolver'

describe('resolveGemImage', () => {
  it('resolves PoB gem metadata ids to PoE CDN images', () => {
    expect(resolveGemImage('Metadata/Items/Gems/SkillGemFireball')).toBe('https://web.poecdn.com/image/Art/2DItems/Gems/Fireball.png')
    expect(resolveGemImage('Metadata/Items/Gems/SkillGemKineticFusillade')).toBe('https://web.poecdn.com/image/Art/2DItems/Gems/KineticFullisadeSkillGem.png')
    expect(resolveGemImage('Metadata/Items/Gems/SupportGemIncreasedBurningDamage')).toBe('https://web.poecdn.com/image/Art/2DItems/Gems/Support/IncreasedBurnDuration.png')
    expect(resolveGemImage(undefined)).toBeNull()
  })
})
