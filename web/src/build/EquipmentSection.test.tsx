import { cleanup, render } from '@testing-library/react'
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
})
