# Current Context

## Current Goal

실제 브라우저의 장시간 실행 환경에서 PoB 검사 완료 뒤 상세 화면으로 전환되는지 확인한다.

## In Progress

- 브라우저 Web Worker는 원본 PoB `HeadlessWrapper.lua`로 PoB 입력을 분석하고, React는 계산 결과를 상세 화면에 표시한다.

## Blockers

None.

## Next Actions

1. 사용자가 실행 중인 API를 재시작한 뒤 실제 브라우저에서 PoB 검사와 상세 화면 전환, 공격 동작·상호작용 설명을 확인한다.
2. 확인 결과를 기록하고 이 활성 문맥을 완료 처리한다.

## Relevant Files

- `web/src/pob/browserPob.ts`
- `web/src/pob/bridge.lua`

## Completed Work

- 2026-08-25: 일반화된 스킬 운용 분석은 스킬·장비·주얼·패시브·전직·버프의 수정자/효과에서 `operationFacts`를 추출해, 소비-획득 순환, 처치/명중/피격 조건, 유지·예약·재사용 대기시간·전환·강화 흐름만 근거와 함께 AI 서술에 전달한다. 생명력·저항·이동·방어도 계열과 `Condition:`·`Multiplier:`·PvP 항목은 제외하며, 근거 없는 아이템 효과·충전 획득·발동 인과관계를 추측하지 않는다. 공개 분석 응답과 웹 API 응답 타입은 유지했다. API 전체 테스트와 `bootJar`, 웹 프로덕션 빌드는 성공했다. 웹 전체 Vitest는 결과 없이 2분간 대기한 뒤 중단되어 별도 원인 조사가 필요하다.
- 2026-08-23: API DDD 가독성 리팩터링을 완료했다. `build.domain.analysis`에 공격·방어·버프·패시브·장비·성능 분석기와 요약 생성기를 분리하고, `BuildAnalysisServiceImpl`은 이들을 조합하는 유스케이스로 유지했다. OpenAI 연동은 `ai.service`의 요청 클라이언트·스키마·섹션 검증기·보정기로 분리했다. 공개 API와 웹 타입은 바꾸지 않았고 `utils` 또는 repository는 추가하지 않았다. API 전체 테스트와 `bootJar` 검증을 통과했다.
- 2026-08-23: 상세 화면의 요약 제목을 `빌드 메커니즘 요약`으로 바꾸고, 요약 문장을 네 칸 그리드가 아닌 전체 폭 카드로 표시했다. 효과 태그가 없는 버프는 AI 기재 결과에서도 제외한다.
