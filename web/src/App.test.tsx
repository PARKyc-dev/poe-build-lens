import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'

import App from './App'

const analysisResult = {
  gameVersion: '3.27',
  overview: 'Level 90 Witch using Fireball',
  interactions: [
    { title: 'Fireball deals fire spell damage', explanation: 'Fireball is a fire spell.' },
  ],
  contributors: ['Fireball'],
  items: ['The Searing Touch'],
  defences: ['No defence interaction is verified by the local catalog yet.'],
  resourceSustain: ['No resource-sustain interaction is verified by the local catalog yet.'],
  unverified: ['Combustion Support'],
  evidence: [
    {
      name: 'Fireball',
      sourceUrl: 'https://www.pathofexile.com/',
      collectedAt: '2026-08-12',
      reviewed: true,
    },
  ],
}

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

describe('build analysis', () => {
  it('submits a PoB build and renders the analysis', async () => {
    vi.stubGlobal('fetch', async (input: RequestInfo | URL, init?: RequestInit) => {
      const request = JSON.parse(String(init?.body)) as { pobInput?: string }
      if (input !== '/api/analyses' || init?.method !== 'POST' || request.pobInput !== '<PathOfBuilding />') {
        throw new Error('Unexpected analysis request')
      }
      return Response.json({ code: 'OK', message: 'SUCCESS', returnObject: analysisResult })
    })

    render(<App />)
    const user = userEvent.setup()
    await user.type(screen.getByLabelText('Path of Building export'), '<PathOfBuilding />')
    await user.click(screen.getByRole('button', { name: 'Analyze build' }))

    expect(await screen.findByText('Level 90 Witch using Fireball')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Core mechanics' })).toBeInTheDocument()
    expect(screen.getByText('Fireball deals fire spell damage: Fireball is a fire spell.')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Evidence' })).toBeInTheDocument()
  })

  it('shows the API error message', async () => {
    vi.stubGlobal('fetch', async () => Response.json(
      { code: 'INVALID_POB_INPUT', message: 'Provide a raw Path of Building XML export.' },
      { status: 400 },
    ))

    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Analyze build' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Provide a raw Path of Building XML export.')
  })

  it('prevents duplicate submissions while analysis is pending', async () => {
    vi.stubGlobal('fetch', () => new Promise<Response>(() => undefined))

    render(<App />)
    await userEvent.click(screen.getByRole('button', { name: 'Analyze build' }))

    expect(screen.getByRole('button', { name: 'Analyzing…' })).toBeDisabled()
  })
})
