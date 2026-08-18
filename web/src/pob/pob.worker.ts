/// <reference lib="webworker" />

import { unzipSync } from 'fflate'
import { LuaFactory } from 'wasmoon'

import bridgeSource from './bridge.lua?raw'
import type { BuildSummary } from '../build/buildInsight'
import type { MainSkillFlags } from '../build/offenceClassification'
import type { PassiveTree } from './passiveTree'
import type { BrowserJewelItem, BuildFacts } from './browserPob'

type InspectEntry = { id: number; title: string }
type EquipmentItem = { slot: string; name: string; baseName: string | null; rarity: string; modifiers: string[]; imageUrl?: string | null }
type BrowserInspectResult = {
  specs: InspectEntry[]
  skillSets: InspectEntry[]
  itemSets: InspectEntry[]
  activeSpec: number
  activeSkillSet: number
  activeItemSet: number
  activeSkillName: string | null
  mainSkillFlags: MainSkillFlags | null
  buildFacts: BuildFacts
  summary: BuildSummary
  equipment: EquipmentItem[]
  jewels: BrowserJewelItem[]
  tree: PassiveTree
}
type Asset = { url: string; byteSize: number; sha256: string }
type Manifest = { core: Asset; trees: Record<string, Asset> }
type Request = { type: 'inspect'; requestId: number; input: string }
type Response =
  | { type: 'analyzed'; requestId: number; result: BrowserInspectResult }
  | { type: 'error'; requestId: number; message: string }

const enginePromises = new Map<string, Promise<Awaited<ReturnType<LuaFactory['createEngine']>>>>()
let manifestPromise: Promise<Manifest> | undefined

async function decodeInput(input: string): Promise<string> {
  const value = input.trim()
  if (value.startsWith('https://pobb.in/')) {
    const response = await fetch(value)
    if (!response.ok) throw new Error('pobb.in 빌드 코드를 불러올 수 없습니다.')
    return decodeInput(await response.text())
  }
  if (value.startsWith('<')) return value
  const padded = value.replace(/-/g, '+').replace(/_/g, '/') + '='.repeat((4 - value.length % 4) % 4)
  const binary = Uint8Array.from(atob(padded), (character) => character.charCodeAt(0))
  return new Response(new Blob([binary]).stream().pipeThrough(new DecompressionStream('deflate'))).text()
}

async function verify(asset: Asset, bytes: Uint8Array) {
  if (bytes.byteLength !== asset.byteSize) throw new Error('PoB 자산 크기가 일치하지 않습니다.')
  const hash = [...new Uint8Array(await crypto.subtle.digest('SHA-256', bytes.slice().buffer))]
    .map((value) => value.toString(16).padStart(2, '0')).join('')
  if (hash !== asset.sha256) throw new Error('PoB 자산 무결성 검증에 실패했습니다.')
}

async function mountArchive(factory: LuaFactory, asset: Asset) {
  const bytes = new Uint8Array(await (await fetch(asset.url)).arrayBuffer())
  await verify(asset, bytes)
  await Promise.all(Object.entries(unzipSync(bytes)).map(([path, content]) => factory.mountFile(path, content)))
}

async function manifest() {
  manifestPromise ??= fetch('/pob/manifest.json').then(async (response) => {
    if (!response.ok) throw new Error('PoB 자산 목록을 불러올 수 없습니다.')
    return response.json() as Promise<Manifest>
  })
  return manifestPromise
}

function treeVersionFromXml(xml: string) {
  const tree = /<Tree\b[^>]*>([\s\S]*?)<\/Tree>/.exec(xml)?.[1]
  const version = tree && /<Spec\b[^>]*\btreeVersion="([^"]+)"/.exec(tree)?.[1]
  if (!version) throw new Error('PoB 내보내기에 패시브 트리 버전이 없습니다.')
  return version
}

async function engine(treeVersion: string) {
  const existing = enginePromises.get(treeVersion)
  if (existing) return existing
  const created = (async () => {
    const factory = new LuaFactory(new URL('../../node_modules/wasmoon/dist/glue.wasm', import.meta.url).href)
    const currentManifest = await manifest()
    await mountArchive(factory, currentManifest.core)
    if (treeVersion !== '3_29') {
      const asset = currentManifest.trees[treeVersion]
      if (!asset) throw new Error(`PoB 트리 데이터 ${treeVersion}을 지원하지 않습니다.`)
      await mountArchive(factory, asset)
    }
    await factory.mountFile('bridge.lua', bridgeSource)
    const created = await factory.createEngine()
    await created.doFile('bridge.lua')
    return created
  })()
  enginePromises.set(treeVersion, created)
  return created
}

async function inspect(input: string): Promise<BrowserInspectResult> {
  const xml = await decodeInput(input)
  if (!xml.includes('<PathOfBuilding')) throw new Error('유효한 PoB 코드 또는 XML을 입력하세요.')
  const runtime = await engine(treeVersionFromXml(xml))
  const load = runtime.global.get('inspectBuild') as (xml: string) => string
  return JSON.parse(await load(xml)) as BrowserInspectResult
}

self.onmessage = async ({ data }: MessageEvent<Request>) => {
  if (data.type !== 'inspect') return
  try {
    const result = await inspect(data.input)
    self.postMessage({ type: 'analyzed', requestId: data.requestId, result } satisfies Response)
  } catch (error) {
    self.postMessage({
      type: 'error',
      requestId: data.requestId,
      message: error instanceof Error ? error.message : '브라우저 PoB 엔진 실행에 실패했습니다.',
    } satisfies Response)
  }
}
