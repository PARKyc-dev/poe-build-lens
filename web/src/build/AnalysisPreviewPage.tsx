import type { BuildAnalysisResult } from '../api/analysis'
import type { BrowserInspectResult } from '../pob/browserPob'
import previewAnalysisJson from './analysisPreviewAnalysis.json'
import previewResultJson from './analysisPreviewResult.json'
import { BuildDetailPage } from './BuildDetailPage'

const previewResult = previewResultJson as unknown as BrowserInspectResult
const previewAnalysis = previewAnalysisJson as BuildAnalysisResult

export function AnalysisPreviewPage() {
  return <BuildDetailPage
    result={previewResult}
    analysis={previewAnalysis}
    onNewInspection={() => window.location.assign('/')}
  />
}
