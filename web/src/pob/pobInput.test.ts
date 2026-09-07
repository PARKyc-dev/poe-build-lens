import { beforeEach, describe, expect, it, vi } from 'vitest'

import { resolvePobInput } from './pobInput'

describe('resolvePobInput', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it.each([
    ['https://pobb.in/AbC_123-xy', 'AbC_123-xy'],
    ['pobb.in/AbC_123-xy', 'AbC_123-xy'],
    ['http://www.pobb.in/AbC_123-xy/raw', 'AbC_123-xy'],
  ])('pobb.in 공유 링크 %s의 raw PoB 코드를 API에서 가져온다', async (input, id) => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({
      code: 'OK',
      message: 'Success',
      returnObject: 'eNrawPobCode',
    }), { status: 200, headers: { 'Content-Type': 'application/json' } }))

    await expect(resolvePobInput(`  ${input}  `)).resolves.toBe('eNrawPobCode')
    expect(fetchMock).toHaveBeenCalledWith(`/api/pobb-in/${id}`)
  })

  it.each(['eNcompressedPobCode', '<PathOfBuilding />'])('기존 PoB 입력 %s은 그대로 반환한다', async (input) => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')

    await expect(resolvePobInput(`  ${input}  `)).resolves.toBe(input)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('pobb.in API 오류 메시지를 전달한다', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({
      code: 'POBB_IN_FETCH_FAILED',
      message: 'pobb.in 빌드 코드를 불러올 수 없습니다.',
    }), { status: 502, headers: { 'Content-Type': 'application/json' } }))

    await expect(resolvePobInput('https://pobb.in/not-found')).rejects.toThrow('pobb.in 빌드 코드를 불러올 수 없습니다.')
  })
})
