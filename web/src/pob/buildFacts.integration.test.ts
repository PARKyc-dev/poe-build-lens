// @vitest-environment node

import { readFile } from 'node:fs/promises'
import { describe, expect, it } from 'vitest'
import { unzipSync } from 'fflate'
import { LuaFactory } from 'wasmoon'

const webRoot = process.cwd()

async function mountArchive(factory: LuaFactory, relativePath: string) {
  const archive = unzipSync(await readFile(`${webRoot}/${relativePath}`))
  await Promise.all(Object.entries(archive).map(([path, content]) => factory.mountFile(path, content)))
}

async function inspectFixture(name: string) {
  const factory = new LuaFactory(`${webRoot}/node_modules/wasmoon/dist/glue.wasm`)
  await mountArchive(factory, 'public/pob/core.zip')
  await mountArchive(factory, 'public/pob/trees/3_13.zip')
  await factory.mountFile('bridge.lua', await readFile(`${webRoot}/src/pob/bridge.lua`))
  const runtime = await factory.createEngine()
  await runtime.doFile('bridge.lua')
  const inspectBuild = runtime.global.get('inspectBuild') as (xml: string) => Promise<string>
  const xml = await readFile(`${webRoot}/pob/.cache/PathOfBuilding/spec/TestBuilds/3.13/${name}.xml`, 'utf8')
  return JSON.parse(await inspectBuild(xml))
}

describe('PoB BuildFacts bridge', () => {
  it('marks only mainSkill as primary and returns allocated passive effects', async () => {
    const result = await inspectFixture('OccVortex')

    expect(result.buildFacts.offence).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: 'Vortex', role: 'primary' }),
      expect.objectContaining({ name: 'Storm Brand', role: 'secondary' }),
    ]))
    expect(result.buildFacts.offence).not.toEqual(expect.arrayContaining([
      expect.objectContaining({ name: 'Shield Charge' }),
      expect.objectContaining({ name: 'Flame Dash' }),
    ]))
    expect(result.buildFacts.mobility).toEqual(expect.arrayContaining([
      { name: 'Shield Charge' },
      { name: 'Flame Dash' },
    ]))
    expect(result.buildFacts.passives).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: expect.any(String), effects: expect.arrayContaining([expect.any(String)]), tags: expect.any(Array) }),
      expect.objectContaining({ name: 'Growth and Decay', tags: expect.arrayContaining(['life-regeneration']) }),
    ]))
    expect(result.buildFacts.passives.every((passive: { effects: unknown }) => Array.isArray(passive.effects))).toBe(true)
    expect(result.buildFacts.passives.length).toBeLessThan(result.tree.nodes.length)
    expect(result.buildFacts.passiveTags).toEqual(expect.arrayContaining(['life-regeneration']))
    expect(result.buildFacts.passiveTags).toEqual(expect.arrayContaining([
      'life', 'energy-shield', 'armour', 'life-regeneration',
    ]))

    expect(result.buildFacts.offence[0]).toMatchObject({ name: 'Vortex', delivery: 'self-cast' })
    expect(result.buildFacts.defence).toEqual(expect.arrayContaining([
      { kind: 'life', value: expect.any(Number) },
      { kind: 'fire-resistance', value: expect.any(Number) },
    ]))
    expect(result.buildFacts.defence).not.toContainEqual(expect.objectContaining({ value: 0 }))
    expect(result.buildFacts.buffs).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: 'Clarity', kind: 'aura', appliesTo: 'player' }),
    ]))
    expect(result.equipment).toEqual(expect.arrayContaining([
      expect.objectContaining({ slot: 'Flask 1' }),
    ]))
    expect(result.buildFacts.items).toEqual(expect.arrayContaining([
      expect.objectContaining({ slot: 'Weapon 2', tags: expect.arrayContaining(['cold-resistance']) }),
    ]))
  }, 30_000)
})
