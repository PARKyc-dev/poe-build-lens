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
  offence: Mechanic[]
  defence: Mechanic[]
  buffs: Mechanic[]
  passives: Mechanic[]
  passiveNodes: Mechanic[]
  ascendancies: Mechanic[]
  overrides: Mechanic[]
  unverified: string[]
  evidence: Evidence[]
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
    }),
  })
  const payload = await response.json() as ApiResponse<BuildAnalysisResult>

  if (!response.ok) throw new Error(payload.message)

  return payload.returnObject
}
