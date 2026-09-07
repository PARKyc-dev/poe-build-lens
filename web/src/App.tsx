import { useEffect, useState } from 'react'

import { BuildDetailPage } from './build/BuildDetailPage'
import { BuildLoadingPage } from './build/BuildLoadingPage'
import { analyzeBuild, getAiUsage } from './api/analysis'
import type { AiUsage, BuildAnalysisResult } from './api/analysis'
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

function SiteFooter() {
  return <footer className="site-footer">
    <p>Path of Exile 및 관련 게임 데이터·이미지·상표의 권리는 <strong>Grinding Gear Games</strong>에 있습니다.</p>
    <p>PoE Lens는 비상업적 비공식 팬 프로젝트이며, Grinding Gear Games와 제휴·승인·후원 관계가 없습니다.</p>
    <p lang="en">This product isn't affiliated with or endorsed by Grinding Gear Games in any way.</p>
    <p>분석 결과는 참고용이며 게임 업데이트에 따라 달라질 수 있습니다. <a href="https://www.pathofexile.com/legal/terms-of-use-and-privacy-policy" target="_blank" rel="noreferrer">Path of Exile 이용약관</a> <span aria-hidden="true">·</span> <a href="https://github.com/PARKyc-dev/poe-build-lens" target="_blank" rel="noreferrer">GitHub 저장소</a></p>
  </footer>
}

function AppLayout({ children }: { children: React.ReactNode }) {
  return <div className="site-layout">{children}<SiteFooter /></div>
}

export default function App() {
  const [inspectInput, setInspectInput] = useState('')
  const [inspectResult, setInspectResult] = useState<BrowserInspectResult | null>(null)
  const [analysisResult, setAnalysisResult] = useState<BuildAnalysisResult | null>(null)
  const [inspectError, setInspectError] = useState<string | null>(null)
  const [isInspecting, setIsInspecting] = useState(false)
  const [aiUsage, setAiUsage] = useState<AiUsage | null>(null)
  const workerStatus: WorkerStatus = 'ready'

  async function refreshAiUsage() {
    try {
      setAiUsage(await getAiUsage())
    } catch {
      setAiUsage(null)
    }
  }

  useEffect(() => {
    void refreshAiUsage()
  }, [])

  async function inspect(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsInspecting(true)
    setInspectError(null)
    setAnalysisResult(null)
    try {
      const result = await inspectBuildInBrowser(inspectInput)
      const analysis = await analyzeBuild(result)
      setInspectResult(result)
      setAnalysisResult(analysis)
    } catch (reason) {
      setInspectError(reason instanceof Error ? reason.message : 'PoB 검사 워커에 연결할 수 없습니다.')
    } finally {
      void refreshAiUsage()
      setIsInspecting(false)
    }
  }

  if (inspectResult && analysisResult) {
    return <AppLayout><BuildDetailPage result={inspectResult} analysis={analysisResult} onNewInspection={() => {
      setInspectResult(null)
      setAnalysisResult(null)
    }} /></AppLayout>
  }

  if (isInspecting) {
    return <AppLayout><BuildLoadingPage /></AppLayout>
  }

  return (
    <AppLayout><main className="app-shell">
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
        {aiUsage && <p className="ai-usage" aria-label="오늘 AI 분석 사용량">AI 분석 사용량 <strong>{aiUsage.used}/{aiUsage.limit}</strong></p>}
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
    </main></AppLayout>
  )
}
