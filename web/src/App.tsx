import { useState } from 'react'

import { analyzeBuild, type AnalysisResult, type Mechanic } from './api/analysis'
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

export default function App() {
  const [pobInput, setPobInput] = useState('')
  const [analysis, setAnalysis] = useState<AnalysisResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)

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

  return (
    <main>
      <h1>PoE Lens</h1>
      <p>Paste a raw Path of Building XML export or compressed export code to explain its verified mechanics.</p>
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
    </main>
  )
}
