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

interface ApiSuccess<T> {
  code: string
  message: string
  returnObject: T
}

interface ApiError {
  code: string
  message: string
}

const connectionError = 'Unable to connect to the analysis API.'

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

  let body: ApiSuccess<AnalysisResult> | ApiError
  try {
    body = await response.json() as ApiSuccess<AnalysisResult> | ApiError
  } catch {
    throw new Error(connectionError)
  }

  if (!response.ok) {
    throw new Error(body.message || connectionError)
  }
  if (!('returnObject' in body)) {
    throw new Error(connectionError)
  }
  return body.returnObject
}
