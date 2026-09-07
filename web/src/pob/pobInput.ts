import { getPobbInBuild } from '../api/analysis'

const POBB_IN_PREFIX = /^(?:https?:\/\/)?(?:www\.)?pobb\.in(?:\/|$)/i
const POBB_IN_URL = /^(?:https?:\/\/)?(?:www\.)?pobb\.in\/([A-Za-z0-9_-]+)(?:\/raw)?\/?(?:[?#].*)?$/i

export async function resolvePobInput(input: string): Promise<string> {
  const value = input.trim()
  const match = POBB_IN_URL.exec(value)
  if (!match) {
    if (POBB_IN_PREFIX.test(value)) throw new Error('올바른 pobb.in 공유 링크를 입력해 주세요.')
    return value
  }

  return (await getPobbInBuild(match[1])).trim()
}
