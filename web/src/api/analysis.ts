export interface Mechanic {
  title: string
  explanation: string
}

export interface Evidence {
  name: string
  sourceUrl: string
  collectedAt: string
  reviewed: boolean
}

export interface AnalysisResult {
  gameVersion: string
  overview: string
  interactions: Mechanic[]
  contributors: string[]
  items: string[]
  defences: string[]
  resourceSustain: string[]
  unverified: string[]
  evidence: Evidence[]
}

const connectionError = 'Unable to connect to the analysis API.'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isStringArray(value: unknown): value is string[] {
  return Array.isArray(value) && value.every((item) => typeof item === 'string')
}

function isMechanic(value: unknown): value is Mechanic {
  return isRecord(value) && typeof value.title === 'string' && typeof value.explanation === 'string'
}

function isEvidence(value: unknown): value is Evidence {
  return isRecord(value)
    && typeof value.name === 'string'
    && typeof value.sourceUrl === 'string'
    && typeof value.collectedAt === 'string'
    && typeof value.reviewed === 'boolean'
}

function isAnalysisResult(value: unknown): value is AnalysisResult {
  return isRecord(value)
    && typeof value.gameVersion === 'string'
    && typeof value.overview === 'string'
    && Array.isArray(value.interactions) && value.interactions.every(isMechanic)
    && isStringArray(value.contributors)
    && isStringArray(value.items)
    && isStringArray(value.defences)
    && isStringArray(value.resourceSustain)
    && isStringArray(value.unverified)
    && Array.isArray(value.evidence) && value.evidence.every(isEvidence)
}

export async function analyzeBuild(pobInput: string): Promise<AnalysisResult> {
  let response: Response
  try {
    response = await fetch('/api/analyses', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pobInput }),
    })
  } catch {
    throw new Error(connectionError)
  }

  let body: unknown
  try {
    body = await response.json()
  } catch {
    throw new Error(connectionError)
  }

  if (!response.ok) {
    throw new Error(isRecord(body) && typeof body.message === 'string' ? body.message : connectionError)
  }
  if (!isRecord(body) || !isAnalysisResult(body.returnObject)) {
    throw new Error(connectionError)
  }
  return body.returnObject
}
