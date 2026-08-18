# 보조 젬과 전직 노드 분석 입력

## 목표

브라우저 PoB 엔진이 모든 현재 스킬의 보조 젬 세부 정보와 할당된 전직 노드를 추출하고, API 분석과 화면에 반영한다.

## 범위

- 스킬·보조 젬의 이름, 레벨, 품질, 품질 유형, 활성 여부, 각성 여부를 추출한다.
- 할당된 전직 노드의 전직명, 이름, 효과 원문, 태그를 추출한다.
- 웹·API의 `BuildFacts` 계약과 분석 결과를 확장한다.

## 완료 기준

- 실제 PoB fixture에서 Vortex 보조 젬 및 Occultist 전직 노드가 확인된다.
- API 결과가 보조 젬과 전직 노드를 분석 문장으로 반환한다.
- 영향받는 웹·API의 전체 테스트와 빌드가 통과한다.

## 진행 상황

- Worker 통합 테스트와 API 회귀 테스트를 추가했고, 각각 실패 후 구현으로 통과시켰다.
- 모든 현재 스킬의 보조 젬과 모든 할당 전직 노드를 `BuildFacts`로 전달하도록 구현했다.
- 2026-08-18: `passives`의 주요 노드 이름과 효과 원문을 별도 분석 결과로 반환하고 화면에 표시하도록 확장했다.
- 2026-08-18: 장비·주얼 옵션과 PoB 계산 수치를 분석 입력으로 전달하고 별도 분석 섹션에 표시하도록 확장했다.

## 검증 결과

- `cd web && ./node_modules/.bin/vitest run src/App.test.tsx src/api/analysis.test.ts src/build/buildInsight.test.ts src/build/offenceClassification.test.ts src/pob/passiveTree.test.ts --reporter=dot`: 19개 통과.
- `cd web && ./node_modules/.bin/vitest run src/pob/buildFacts.integration.test.ts -t 'marks only mainSkill'`: 통과.
- `cd web && ./node_modules/.bin/vitest run src/pob/buildFacts.integration.test.ts -t 'returns each skill support gem'`: 통과.
- `cd web && ./node_modules/.bin/vitest run src/pob/buildFacts.integration.test.ts -t 'preserves a support gem quality type'`: 통과.
- `cd web && npm run build`: 통과.
- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.
- 주요 패시브 노드 API 회귀 테스트와 웹 화면 테스트: 통과.
- 장비·주얼·PoB 계산 수치 API 회귀 테스트, 실제 PoB 장비·군 주얼 통합 테스트: 통과.

## 다음 행동

- 없음.

## 차단 요소

- 없음.
