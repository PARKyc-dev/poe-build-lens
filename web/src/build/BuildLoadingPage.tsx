export function BuildLoadingPage() {
  return (
    <main className="build-detail build-loading" aria-label="빌드 분석 중" aria-live="polite">
      <section className="loading-panel">
        <p className="eyebrow">POE LENS · BUILD INSIGHT</p>
        <h1>PoB 빌드를 <span>분석하고 있습니다</span></h1>
        <p>원본 PoB 엔진에서 빌드 설정과 계산값을 불러오는 중입니다.</p>
        <div className="loading-bar" role="progressbar" aria-label="빌드 분석 진행 중" aria-valuetext="분석 중" />
        <ol className="loading-steps">
          <li><span />PoB 엔진 준비</li>
          <li><span />빌드 계산</li>
          <li><span />인사이트 작성</li>
        </ol>
      </section>
    </main>
  )
}
