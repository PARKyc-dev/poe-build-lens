import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'

import { SkillGemSection } from './SkillGemSection'

afterEach(cleanup)

describe('SkillGemSection', () => {
  it('shows active and support gem images', () => {
    render(<SkillGemSection result={{
      buildFacts: {
        skills: [{
          name: 'Fireball', level: 20, quality: 20, qualityType: 'Default', enabled: true, awakened: false,
          imageUrl: 'https://example.com/fireball.png', effects: [],
          supports: [{ name: 'Burning Damage', level: 20, quality: 0, qualityType: 'Default', enabled: true, awakened: false, imageUrl: 'https://example.com/burning-damage.png', effects: [] }],
        }],
        offence: [], buffs: [], mobility: [], operationFacts: [],
      },
    } as never} />)

    const activeGem = screen.getByRole('img', { name: 'Fireball' })
    const supportGem = screen.getByRole('img', { name: 'Burning Damage' })
    expect(activeGem).toHaveAttribute('src', 'https://example.com/fireball.png')
    expect(activeGem.parentElement).toHaveClass('gem-icon', 'active-gem-icon')
    expect(supportGem).toHaveAttribute('src', 'https://example.com/burning-damage.png')
    expect(supportGem.parentElement).toHaveClass('gem-icon')
  })

  it('layers every frame of a horizontal gem sprite', () => {
    const { container } = render(<SkillGemSection result={{
      buildFacts: {
        skills: [{
          name: 'Purity of Fire', level: 20, quality: 20, qualityType: 'Default', enabled: true, awakened: false,
          imageUrl: 'https://example.com/purity.png', effects: [], supports: [],
        }],
        offence: [], buffs: [], mobility: [], operationFacts: [],
      },
    } as never} />)

    const image = screen.getByRole('img', { name: 'Purity of Fire' })
    Object.defineProperty(image, 'naturalWidth', { value: 234 })
    Object.defineProperty(image, 'naturalHeight', { value: 78 })
    fireEvent.load(image)

    expect(container.querySelectorAll('.gem-icon-layer')).toHaveLength(3)
    expect(container.querySelector('.gem-icon-frame-2')).toHaveAttribute('src', 'https://example.com/purity.png')
  })
})
