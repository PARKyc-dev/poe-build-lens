import { describe, expect, it } from 'vitest'

import { createPassiveTreeDrawModel, type PassiveTree } from './passiveTree'

describe('createPassiveTreeDrawModel', () => {
  it('keeps allocated nodes and connects only known nodes inside the canvas', () => {
    const tree: PassiveTree = {
      version: '3_27',
      nodes: [
        { id: '1', x: -100, y: 0, allocated: true },
        { id: '2', x: 100, y: 100, allocated: false },
      ],
      links: [{ from: '1', to: '2' }, { from: '1', to: 'missing' }],
    }

    const model = createPassiveTreeDrawModel(tree, { width: 300, height: 200, padding: 20 })

    expect(model.nodes).toEqual([
      { id: '1', x: 20, y: 20, allocated: true },
      { id: '2', x: 280, y: 180, allocated: false },
    ])
    expect(model.lines).toEqual([{ from: { x: 20, y: 20 }, to: { x: 280, y: 180 } }])
  })

  it('returns an empty draw model for a tree without nodes', () => {
    expect(createPassiveTreeDrawModel({ version: '3_27', nodes: [], links: [] }, {
      width: 300,
      height: 200,
      padding: 20,
    })).toEqual({ nodes: [], lines: [] })
  })
})
