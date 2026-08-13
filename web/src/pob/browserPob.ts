import type { PassiveTree } from './passiveTree'
import type { BuildSummary } from '../build/buildInsight'

export type BrowserInspectEntry = { id: number; title: string }
export type BrowserEquipmentItem = {
  slot: string
  name: string
  baseName: string | null
  rarity: string
  modifiers: string[]
  imageUrl?: string | null
}
export type BrowserInspectResult = {
  specs: BrowserInspectEntry[]
  skillSets: BrowserInspectEntry[]
  itemSets: BrowserInspectEntry[]
  activeSpec: number
  activeSkillSet: number
  activeItemSet: number
  activeSkillName: string | null
  summary: BuildSummary
  equipment: BrowserEquipmentItem[]
  tree: PassiveTree
}

type WorkerResponse =
  | { type: 'analyzed'; requestId: number; result: BrowserInspectResult }
  | { type: 'error'; requestId: number; message: string }

let worker: Worker | undefined
let nextRequestId = 1

function getWorker() {
  worker ??= new Worker(new URL('./pob.worker.ts', import.meta.url), { type: 'module' })
  return worker
}

export function inspectBuildInBrowser(input: string): Promise<BrowserInspectResult> {
  const requestId = nextRequestId++
  return new Promise((resolve, reject) => {
    const runtime = getWorker()
    const onMessage = ({ data }: MessageEvent<WorkerResponse>) => {
      if (data.requestId !== requestId) return
      runtime.removeEventListener('message', onMessage)
      if (data.type === 'analyzed') resolve(data.result)
      else reject(new Error(data.message))
    }
    runtime.addEventListener('message', onMessage)
    runtime.postMessage({ type: 'inspect', requestId, input })
  })
}
