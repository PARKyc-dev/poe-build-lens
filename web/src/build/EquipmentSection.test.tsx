import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'

import { EquipmentSection } from './EquipmentSection'

afterEach(cleanup)

describe('EquipmentSection', () => {
  it('uses unique React keys when jewels share a socket id', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
    render(<EquipmentSection result={{
      equipment: [],
      jewels: [
        { socket: '26196', name: 'First Jewel', baseName: 'Crimson Jewel', rarity: 'RARE', modifiers: [], kind: 'jewel' },
        { socket: '26196', name: 'Second Jewel', baseName: 'Large Cluster Jewel', rarity: 'RARE', modifiers: [], kind: 'cluster' },
      ],
    }} tooltip={null} onShow={() => {}} onHide={() => {}} />)

    expect(consoleError.mock.calls.flat().join(' ')).not.toContain('Encountered two children with the same key')
    consoleError.mockRestore()
  })

  it('keeps item properties separated for the game-style tooltip', () => {
    const onShow = vi.fn()
    render(<EquipmentSection result={{
      equipment: [{
        slot: 'Amulet', name: 'Vengeance Charm', baseName: 'Focused Amulet', rarity: 'RARE',
        modifiers: ['Allocates Sovereignty', '+2 to Level of all Skill Gems'],
        properties: ['Quality (Critical Modifiers): +20%', 'Item Level: 85'],
        requirements: ['Level 60'],
        enchantModifiers: ['Allocates Sovereignty'],
        implicitModifiers: ['-1 Prefix Modifier allowed'],
        explicitModifiers: ['+2 to Level of all Skill Gems'],
        influences: ['Shaper'],
        status: ['Corrupted'],
      }],
      jewels: [],
    }} tooltip={null} onShow={onShow} onHide={() => {}} />)

    fireEvent.mouseEnter(screen.getByRole('button', { name: '목걸이 슬롯: Vengeance Charm' }))
    expect(onShow).toHaveBeenCalledWith(expect.anything(), expect.objectContaining({
      baseName: 'Focused Amulet',
      sections: [
        { label: '기본 정보', details: ['Quality (Critical Modifiers): +20%', 'Item Level: 85'] },
        { label: '요구사항', details: ['Level 60'] },
        { label: '인챈트', details: ['Allocates Sovereignty'] },
        { label: '고정 속성', details: ['-1 Prefix Modifier allowed'] },
        { label: '일반 속성', details: ['+2 to Level of all Skill Gems'] },
        { label: '영향력', details: ['Shaper'] },
        { label: '상태', details: ['Corrupted'] },
      ],
    }))
  })
})
