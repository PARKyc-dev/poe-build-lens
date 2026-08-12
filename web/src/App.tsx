import { useEffect, useState } from 'react'

import { analyzeBuild, type AnalysisResult, type Mechanic } from './api/analysis'
import { inspectBuild, type InspectEntry, type InspectResult } from './api/workerInspect'
import './styles.css'

function TextSection({ title, values }: { title: string; values: string[] }) {
  return (
    <section>
      <h2>{title}</h2>
      <ul>{values.map((value) => <li key={value}>{value}</li>)}</ul>
    </section>
  )
}

function MechanicSection({ values }: { values: Mechanic[] }) {
  return (
    <section>
      <h2>Core mechanics</h2>
      <ul>{values.map((value) => <li key={value.title}>{value.title}: {value.explanation}</li>)}</ul>
    </section>
  )
}

function Analysis({ value }: { value: AnalysisResult }) {
  const evidence = value.evidence.map((item) => (
    `${item.name} — ${item.sourceUrl} (${item.collectedAt}, reviewed: ${item.reviewed})`
  ))

  return (
    <>
      <p className="overview"><strong>{value.overview}</strong><small>PoE {value.gameVersion}</small></p>
      <MechanicSection values={value.interactions} />
      <TextSection title="Contributors" values={value.contributors} />
      <TextSection title="Defence" values={value.defences} />
      <TextSection title="Resource sustain" values={value.resourceSustain} />
      <TextSection title="Unverified" values={value.unverified} />
      <TextSection title="Evidence" values={evidence} />
    </>
  )
}

function InspectList({ title, values, activeId }: {
  title: string
  values: InspectEntry[]
  activeId: number
}) {
  return (
    <section>
      <h3>{title}</h3>
      <ul>{values.map((value) => (
        <li key={value.id}>{value.title}{value.id === activeId ? ' (active)' : ''}</li>
      ))}</ul>
    </section>
  )
}

function InspectResultView({ value }: { value: InspectResult }) {
  return (
    <div className="inspect-result">
      <InspectList title="Specs" values={value.specs} activeId={value.activeSpec} />
      <InspectList title="Skill sets" values={value.skillSets} activeId={value.activeSkillSet} />
      <InspectList title="Item sets" values={value.itemSets} activeId={value.activeItemSet} />
    </div>
  )
}

type WorkerStatus = 'checking' | 'ready' | 'unavailable'

const workerStatusLabel: Record<WorkerStatus, string> = {
  checking: 'PoB engine checking',
  ready: 'PoB engine ready',
  unavailable: 'PoB engine unavailable',
}

export default function App() {
  const [pobInput, setPobInput] = useState('')
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [inspectInput, setInspectInput] = useState('')
  const [inspectResult, setInspectResult] = useState<InspectResult | null>(null)
  const [inspectError, setInspectError] = useState<string | null>(null)
  const [isInspecting, setIsInspecting] = useState(false)
  const [workerStatus, setWorkerStatus] = useState<WorkerStatus>('checking')

  useEffect(() => {
    let isMounted = true

    async function refreshWorkerStatus() {
      try {
        const response = await fetch('/worker/health')
        const body: unknown = await response.json()
        if (isMounted) {
          setWorkerStatus(response.ok && typeof body === 'object' && body !== null
            && 'status' in body && body.status === 'ready' ? 'ready' : 'unavailable')
        }
      } catch {
        if (isMounted) setWorkerStatus('unavailable')
      }
    }

    void refreshWorkerStatus()
    const timer = window.setInterval(refreshWorkerStatus, 5000)
    return () => {
      isMounted = false
      window.clearInterval(timer)
    }
  }, [])

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsLoading(true)
    setError(null)
    setAnalysis(null)
    try {
      setAnalysis(await analyzeBuild(pobInput))
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Unable to connect to the analysis API.')
    } finally {
      setIsLoading(false)
    }
  }

  async function inspect(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsInspecting(true)
    setInspectError(null)
    try {
      setInspectResult(await inspectBuild(inspectInput))
    } catch (reason) {
      setInspectError(reason instanceof Error ? reason.message : 'Unable to connect to the PoB inspect worker.')
    } finally {
      setIsInspecting(false)
    }
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <div>
          <p className="eyebrow">PATH OF BUILDING WORKBENCH</p>
          <h1>PoE <span>Lens</span></h1>
          <p className="hero-copy">Bring a build into focus. Inspect its loadouts or trace the mechanics that shape it.</p>
        </div>
        <div className={`engine-status is-${workerStatus}`} aria-label={`PoB engine status: ${workerStatus}`} aria-live="polite">
          <span className="status-dot" />
          <div>
            <strong>{workerStatusLabel[workerStatus]}</strong>
            <small>Headless runtime · v2.67.2</small>
          </div>
        </div>
      </header>

      <div className="workflow-grid">
        <section className="workflow-card inspect-card" aria-labelledby="inspect-heading">
          <div className="card-heading">
            <span className="step-number">01</span>
            <div>
              <p className="eyebrow">ENGINE CHECK</p>
              <h2 id="inspect-heading">PoB headless inspect</h2>
              <p>Paste an export code or XML to read the Specs, Skill sets, and Item sets loaded by the local PoB engine.</p>
            </div>
          </div>
          <form onSubmit={inspect}>
            <label htmlFor="inspectPobXml">PoB export code or XML for headless inspect</label>
            <textarea
              id="inspectPobXml"
              value={inspectInput}
              onChange={(event) => setInspectInput(event.target.value)}
              placeholder="eN... or <PathOfBuilding>...</PathOfBuilding>"
            />
            <button type="submit" disabled={isInspecting}>{isInspecting ? 'Inspecting…' : 'Inspect PoB'}</button>
          </form>
          <div aria-live="polite">
            {inspectError && <p role="alert">{inspectError}</p>}
            {inspectResult && <InspectResultView value={inspectResult} />}
          </div>
        </section>

        <section className="workflow-card analysis-card" aria-labelledby="analysis-heading">
          <div className="card-heading">
            <span className="step-number">02</span>
            <div>
              <p className="eyebrow">BUILD READING</p>
              <h2 id="analysis-heading">Build analysis</h2>
              <p>Review verified mechanics, contributors, and defensive considerations.</p>
            </div>
          </div>
          <form onSubmit={submit}>
            <label htmlFor="pobInput">Path of Building export</label>
            <textarea
              id="pobInput"
              value={pobInput}
              onChange={(event) => setPobInput(event.target.value)}
              placeholder="<PathOfBuilding>...</PathOfBuilding>"
            />
            <button type="submit" disabled={isLoading}>{isLoading ? 'Analyzing…' : 'Analyze build'}</button>
          </form>
          <div aria-live="polite">
            {error && <p role="alert">{error}</p>}
            {analysis && <Analysis value={analysis} />}
          </div>
        </section>
      </div>
    </main>
  )
}
