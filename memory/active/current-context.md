# Current Context

## Current Goal

실제 브라우저의 장시간 실행 환경에서 PoB 검사 완료 뒤 상세 화면으로 전환되는지 확인한다.

## In Progress

- 브라우저 Web Worker는 원본 PoB `HeadlessWrapper.lua`로 PoB 입력을 분석하고, React는 계산 결과를 상세 화면에 표시한다.

## Blockers

None.

## Next Actions

1. 실제 브라우저에서 PoB 검사와 상세 화면 전환을 확인한다.
2. 확인 결과를 기록하고 이 활성 문맥을 완료 처리한다.

## Relevant Files

- `web/src/pob/browserPob.ts`
- `web/src/pob/bridge.lua`
