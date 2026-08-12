export interface InspectEntry {
  id: number
  title: string
}

export interface InspectResult {
  specs: InspectEntry[]
  skillSets: InspectEntry[]
  itemSets: InspectEntry[]
  activeSpec: number
  activeSkillSet: number
  activeItemSet: number
}

const connectionError = 'Unable to connect to the PoB inspect worker.'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null
}

function isInspectEntry(value: unknown): value is InspectEntry {
  return isRecord(value) && Number.isInteger(value.id) && typeof value.title === 'string'
}

function isInspectResult(value: unknown): value is InspectResult {
  return isRecord(value)
    && Array.isArray(value.specs) && value.specs.every(isInspectEntry)
    && Array.isArray(value.skillSets) && value.skillSets.every(isInspectEntry)
    && Array.isArray(value.itemSets) && value.itemSets.every(isInspectEntry)
    && Number.isInteger(value.activeSpec)
    && Number.isInteger(value.activeSkillSet)
    && Number.isInteger(value.activeItemSet)
}

export async function inspectBuild(pobXml: string): Promise<InspectResult> {
  let response: Response
  try {
    response = await fetch('/worker/builds/inspect', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pobXml }),
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
    throw new Error(isRecord(body) && typeof body.detail === 'string' ? body.detail : connectionError)
  }
  if (!isInspectResult(body)) {
    throw new Error(connectionError)
  }
  return body
}
