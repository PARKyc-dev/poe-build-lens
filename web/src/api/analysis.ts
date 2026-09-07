import type { BrowserInspectResult } from '../pob/browserPob'

export type Mechanic = {
  title: string
  explanation: string
  details?: MechanicDetail[]
}

export type MechanicDetail = {
  label: string
  explanation: string
  type: 'step' | 'interaction' | 'condition'
}

export type Evidence = {
  name: string
  sourceUrl: string
  collectedAt: string
  reviewed: boolean
}

export type BuildAnalysisResult = {
  gameVersion: string
  summary: string
  offence: Mechanic[]
  defence: Mechanic[]
  buffs: Mechanic[]
  passives: Mechanic[]
  passiveNodes: Mechanic[]
  ascendancies: Mechanic[]
  gear: Mechanic[]
  performance: Mechanic[]
  overrides: Mechanic[]
  unverified: string[]
  evidence: Evidence[]
}

export type BuildAnalysisRequest = {
  gameVersion: string
  buildFacts: BrowserInspectResult['buildFacts']
}

export type AiUsage = {
  used: number
  limit: number
}

type ApiResponse<T> = {
  code: string
  message: string
  returnObject: T
}

async function readApiResponse<T>(response: Response): Promise<ApiResponse<T>> {
  const body = await response.text()
  try {
    return JSON.parse(body) as ApiResponse<T>
  } catch {
    throw new Error(`분석 API가 JSON이 아닌 응답을 반환했습니다. API 서버와 프록시 상태를 확인해 주세요. (HTTP ${response.status})`)
  }
}

function normalizeGameVersion(version: string): string {
  return version.replace(/^3_/, '3.')
}

export async function analyzeBuild(result: BrowserInspectResult): Promise<BuildAnalysisResult> {
  const response = await fetch('/api/analyses', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      gameVersion: normalizeGameVersion(result.tree.version),
      buildFacts: result.buildFacts,
    } satisfies BuildAnalysisRequest),
  })
  const payload = await readApiResponse<BuildAnalysisResult>(response)

  if (!response.ok) throw new Error(payload.message)

  return payload.returnObject
}

export async function getAiUsage(): Promise<AiUsage> {
  const response = await fetch('/api/ai-usage')
  const payload = await readApiResponse<AiUsage>(response)

  if (!response.ok) throw new Error(payload.message)

  return payload.returnObject
}

export async function getPobbInBuild(id: string): Promise<string> {
  const response = await fetch(`/api/pobb-in/${encodeURIComponent(id)}`)
  const payload = await readApiResponse<string>(response)

  if (!response.ok) throw new Error(payload.message)

  return payload.returnObject
}
