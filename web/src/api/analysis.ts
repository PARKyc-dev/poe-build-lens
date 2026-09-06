import type { BrowserInspectResult } from '../pob/browserPob'

export type Mechanic = {
  title: string
  explanation: string
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
  const payload = await response.json() as ApiResponse<BuildAnalysisResult>

  if (!response.ok) throw new Error(payload.message)

  return payload.returnObject
}

export async function getAiUsage(): Promise<AiUsage> {
  const response = await fetch('/api/ai-usage')
  const payload = await response.json() as ApiResponse<AiUsage>

  if (!response.ok) throw new Error(payload.message)

  return payload.returnObject
}
