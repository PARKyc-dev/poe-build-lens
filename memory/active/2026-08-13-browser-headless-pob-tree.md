# 브라우저 headless PoB와 패시브 트리 렌더링

## 목표

PoB 내보내기 코드 또는 `pobb.in` 입력을 브라우저 `Web Worker`에서 원본 PoB `HeadlessWrapper.lua`로 불러오고, React가 계산 결과를 바탕으로 빌드 인사이트를 렌더링한다.

## 범위

- 원본 PoB `HeadlessWrapper.lua`의 실행 경로를 웹 애플리케이션에 최소 범위로 통합한다.
- 이 저장소가 소유하는 Lua/WebAssembly 호스트와 PoB 자산 패커를 사용한다.
- 현재 지원하는 압축 PoB 코드 입력을 브라우저 런타임으로 전달한다.
- 활성 스킬과 계산 요약값으로 규칙형 빌드 인사이트를 표시한다.
- 실제 PoB 데이터로 동작을 검증한다.

`pob.cool`은 이식 기법을 확인하는 참고 구현체일 뿐이며, 코드·실행 파일·자산 CDN·버전 목록에 의존하지 않는다.

PoB 원본 버전 고정과 캐시는 `web/pob/`에서 관리하며, `worker/`는 빈 예약 디렉터리로 유지한다.

## 완료 기준

- PoB 코드 입력이 브라우저 `Web Worker`에서 처리된다.
- React 화면이 검사 성공 후 인사이트 중심 상세 화면으로 전환된다.
- `web` 테스트와 프로덕션 빌드가 통과한다.

## 진행 상황

- 2026-08-13: 기존 `worker`는 브라우저 `Web Worker`가 아닌 Python 프로세스임을 확인했다.
- 2026-08-13: 원본 PoB `HeadlessWrapper.lua`는 표준 Lua 인터프리터에서 실행되는 headless 진입점을 제공하며, 브라우저에는 Lua/WebAssembly 호스트와 가상 파일 시스템이 추가로 필요함을 확인했다.
- 2026-08-13: `pob.cool`은 참고 구현체로만 사용하고, 제품 런타임·자산·업데이트 의존성에서는 제외하기로 했다.
- 2026-08-13: 자체 `pack:pob`가 PoB `v2.67.2` 원본에서 core와 모든 지원 트리 자산을 생성하도록 구현했다. 생성 자산은 약 20MB이며 버전·SHA-256 매니페스트를 포함한다.
- 2026-08-13: 브라우저 Worker의 Lua/WebAssembly 호스트에서 원본 `HeadlessWrapper.lua`를 실행하고, 실제 PoB fixture의 `3_13` 트리에서 할당 노드 132개와 연결 133개를 추출했다.
- 2026-08-13: 검사 성공 시 패시브 트리 대신 빌드 인사이트 중심 상세 화면으로 전환하도록 변경했다. Fireball·Arc는 메커니즘 규칙을 사용하고, 방어 수치 기반 주의점과 강화 우선순위를 표시한다. 아직 분석하지 않는 장비 상세는 `예시`로 명확히 구분한다.
- 2026-08-13: PoB 원본 버전 고정·가져오기·캐시를 `web/pob/`로 이관하고, 기존 Python worker와 native spike 코드를 제거했다. `worker/`에는 예약 디렉터리 표시 파일만 남겼다.
- 2026-08-13: 활성 장비 세트의 슬롯·아이템명·베이스·희귀도·최대 4개 옵션을 Worker에서 반환해 화면의 더미 장비 영역을 실제 데이터로 교체했다.
- 2026-08-13: 장비 상세를 `BUILD INSIGHT` 아래 전폭 영역으로 옮기고, 이후 `imageUrl`만 추가하면 아이콘을 렌더링할 수 있는 카드 구조를 적용했다.

## 검증 결과

- 원본 PoB `HeadlessWrapper.lua`의 headless 초기화·XML 불러오기 진입점 확인.
- `cd web && npm run pack:pob`: PoB `v2.67.2` 자산 생성 통과.
- Lua/WebAssembly 실제 fixture 실행: `3_13`, 할당 노드 132개, 연결 133개, 설정 1개 반환.
- Lua/WebAssembly 실제 fixture의 계산 요약: 활성 스킬 `Vortex`, 생명력 `6,348` 반환.
- `cd web && npm test -- --run`: 3개 파일, 7개 테스트 통과.
- `cd web && npm run build`: 상세 화면 변경 후 통과.
- `cd web && npm run pack:pob`: `web/pob/.cache/PathOfBuilding` 입력으로 자산 생성 통과.
- `cd web && npm test -- --run`: 3개 파일, 8개 테스트 통과.
- `cd web && npm run build`: 통과.
- Lua/WebAssembly 실제 fixture: 장비 10개와 각 장비의 실제 옵션 반환 확인.

## 다음 행동

- 실제 브라우저에서 PoB 검사 완료 후 상세 화면 전환을 장시간 실행 환경에서 확인한다.

## 차단 요소

- 없음
