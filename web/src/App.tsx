import { useState } from 'react'

import { BuildDetailPage } from './build/BuildDetailPage'
import { BuildLoadingPage } from './build/BuildLoadingPage'
import { inspectBuildInBrowser } from './pob/browserPob'
import type { BrowserInspectResult } from './pob/browserPob'
import './styles.css'

type WorkerStatus = 'checking' | 'ready' | 'unavailable'

const workerStatusLabel: Record<WorkerStatus, string> = {
  checking: 'PoB 엔진 확인 중',
  ready: 'PoB 엔진 준비 완료',
  unavailable: 'PoB 엔진을 사용할 수 없음',
}

const workerStatusDescription: Record<WorkerStatus, string> = {
  checking: '확인 중',
  ready: '준비 완료',
  unavailable: '사용할 수 없음',
}

export default function App() {
  const [inspectInput, setInspectInput] = useState('')
  const [inspectResult, setInspectResult] = useState<BrowserInspectResult | null>(null)
  const [inspectError, setInspectError] = useState<string | null>(null)
  const [isInspecting, setIsInspecting] = useState(false)
  const workerStatus: WorkerStatus = 'ready'

  async function inspect(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsInspecting(true)
    setInspectError(null)
    try {
      const result = await inspectBuildInBrowser(inspectInput)
      setInspectResult(result)
    } catch (reason) {
      setInspectError(reason instanceof Error ? reason.message : 'PoB 검사 워커에 연결할 수 없습니다.')
    } finally {
      setIsInspecting(false)
    }
  }

  if (inspectResult) {
    return <BuildDetailPage result={inspectResult} onNewInspection={() => setInspectResult(null)} />
  }

  if (isInspecting) {
    return <BuildLoadingPage />
  }

  return (
    <main className="app-shell">
      <header className="hero">
        <div className="hero-content">
          <p className="eyebrow">PATH OF BUILDING WORKBENCH</p>
          <h1>PoE <span>Lens</span></h1>
          <p className="hero-copy">빌드 코드를 붙여 넣어 설정과 장비 구성을 확인하세요.</p>
        </div>
        <div className={`engine-status is-${workerStatus}`} aria-label={`PoB 엔진 상태: ${workerStatusDescription[workerStatus]}`} aria-live="polite">
          <span className="status-dot" />
          <div>
            <strong>{workerStatusLabel[workerStatus]}</strong>
            <small>헤드리스 런타임 · v2.67.2</small>
          </div>
        </div>
      </header>

      <section className="inspect-search" aria-label="PoB 검사">
        <form onSubmit={inspect}>
          <label htmlFor="inspectPobXml">검사할 PoB 코드, pobb.in 또는 XML</label>
          <textarea
            id="inspectPobXml"
            value={inspectInput}
            onChange={(event) => setInspectInput(event.target.value)}
            placeholder="eN... · https://pobb.in/... · <PathOfBuilding>...</PathOfBuilding>"
          />
          <button type="submit" disabled={isInspecting}>{isInspecting ? '검사 중…' : 'PoB 검사'}</button>
        </form>
        <div aria-live="polite">
          {inspectError && <p role="alert">{inspectError}</p>}
        </div>
      </section>
    </main>
  )
}
