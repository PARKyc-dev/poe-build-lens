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

async function inspectFixture(name: string, transform?: (xml: string) => string) {
  const factory = new LuaFactory(`${webRoot}/node_modules/wasmoon/dist/glue.wasm`)
  await mountArchive(factory, 'public/pob/core.zip')
  await mountArchive(factory, 'public/pob/trees/3_13.zip')
  await factory.mountFile('bridge.lua', await readFile(`${webRoot}/src/pob/bridge.lua`))
  const runtime = await factory.createEngine()
  await runtime.doFile('bridge.lua')
  const inspectBuild = runtime.global.get('inspectBuild') as (xml: string) => Promise<string>
  const source = await readFile(`${webRoot}/pob/.cache/PathOfBuilding/spec/TestBuilds/3.13/${name}.xml`, 'utf8')
  const xml = transform ? transform(source) : source
  return JSON.parse(await inspectBuild(xml))
}

describe('PoB BuildFacts bridge', () => {
  it('returns top DPS attacks and allocated passive effects', async () => {
    const result = await inspectFixture('OccVortex')

    expect(result.buildFacts.offence).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: 'Vortex', role: 'primary' }),
      expect.objectContaining({ name: 'Cold Snap', role: 'secondary' }),
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
    expect(result.jewels).toEqual(expect.arrayContaining([
      expect.objectContaining({ socket: '36634', name: 'Cataclysm Stone', baseName: 'Cobalt Jewel', kind: 'jewel' }),
    ]))
    expect(result.buildFacts.items).toEqual(expect.arrayContaining([
      expect.objectContaining({ slot: 'Weapon 2', name: expect.any(String), modifiers: expect.any(Array), tags: expect.arrayContaining(['cold-resistance']) }),
    ]))
    expect(result.buildFacts.performance).toMatchObject({ life: expect.any(Number), totalDps: expect.any(Number) })
  }, 30_000)

  it('ranks the two highest combined DPS skills as primary and secondary attacks', async () => {
    const result = await inspectFixture('OccVortex')
    const attacks = result.buildFacts.offence

    expect(attacks).toHaveLength(2)
    expect(attacks[0]).toMatchObject({ role: 'primary', combinedDps: expect.any(Number) })
    expect(attacks[1]).toMatchObject({ role: 'secondary', combinedDps: expect.any(Number) })
    expect(attacks[0].combinedDps).toBeGreaterThanOrEqual(attacks[1].combinedDps)
  }, 30_000)

  it('returns each skill support gem and allocated ascendancy node details', async () => {
    const result = await inspectFixture('OccVortex')

    expect(result.buildFacts.skills).toEqual(expect.arrayContaining([
      expect.objectContaining({
        name: 'Vortex',
        supports: expect.arrayContaining([
          { name: 'Hypothermia', level: 20, quality: 10, qualityType: 'Default', enabled: true, awakened: false },
          { name: 'Controlled Destruction', level: 20, quality: 20, qualityType: 'Default', enabled: true, awakened: false },
          { name: 'Swift Affliction', level: 20, quality: 0, qualityType: 'Default', enabled: true, awakened: false },
          { name: 'Efficacy', level: 20, quality: 20, qualityType: 'Default', enabled: true, awakened: false },
          { name: 'Concentrated Effect', level: 21, quality: 0, qualityType: 'Default', enabled: true, awakened: false },
        ]),
      }),
    ]))
    expect(result.buildFacts.ascendancies).toEqual(expect.arrayContaining([
      expect.objectContaining({
        ascendancyName: 'Occultist',
        name: 'Void Beacon',
        effects: expect.arrayContaining(['Nearby Enemies have -20% to Cold Resistance']),
        tags: expect.any(Array),
      }),
    ]))
  }, 30_000)

  it('returns installed cluster jewels from the active passive tree', async () => {
    const result = await inspectFixture('Generals Perforate Zerker')

    expect(result.jewels).toEqual(expect.arrayContaining([
      expect.objectContaining({ socket: '49080', name: 'Glyph Star', baseName: 'Medium Cluster Jewel', kind: 'cluster' }),
    ]))
    expect(result.buildFacts.jewels).toEqual(expect.arrayContaining([
      expect.objectContaining({ socket: '49080', name: 'Glyph Star', kind: 'cluster', modifiers: expect.any(Array), tags: expect.any(Array) }),
    ]))
  }, 30_000)

  it('preserves a support gem quality type from the PoB export', async () => {
    const result = await inspectFixture('OccVortex', (xml) => xml.replace(
      'gemId="Metadata/Items/Gems/SupportGemDamageAgainstChilled" skillId="SupportDamageAgainstChilled" enableGlobal1="true" qualityId="Default"',
      'gemId="Metadata/Items/Gems/SupportGemDamageAgainstChilled" skillId="SupportDamageAgainstChilled" enableGlobal1="true" qualityId="Anomalous"',
    ))

    const vortex = result.buildFacts.skills.find((skill: { name: string }) => skill.name === 'Vortex')
    expect(vortex.supports).toEqual(expect.arrayContaining([
      expect.objectContaining({ name: 'Hypothermia', qualityType: 'Anomalous' }),
    ]))
  }, 30_000)
})
