# PoE Lens PoB 중심 분석 설계

## 목표

PoB 빌드의 `Spec`, `SkillSet`, `ItemSet` 해석과 선택 조합 계산을 공식 Path of Building 엔진에 위임한다. Java에서 PoB 의미를 다시 구현하지 않고 Web, Spring API, Python worker, Lua adapter의 책임을 분리한다.

전체 사용자·애플리케이션 흐름의 정본은 `docs/project-flow.md`다. 이 설계는 구현 경계와 단계별 완료 조건을 정의한다.

## 핵심 결정

- 고정된 공식 PoB 릴리스와 stock `HeadlessWrapper.lua`를 사용한다.
- PoB 원본을 패치하지 않고 worker가 소유한 외부 Lua adapter를 사용한다.
- 미병합 JSON-RPC PR #9505는 참고만 하고 런타임에 의존하지 않는다.
- PoB 공식 명칭 `Spec`, `SkillSet`, `ItemSet`, `activeSpec`, `activeSkillSet`, `activeItemSet`을 코드와 API에서 유지한다.
- 초기 worker는 단일 PoB 프로세스에 요청을 직렬화한다.
- 추후 병렬화는 스레드가 아닌 독립 PoB 프로세스 풀로 수행한다.
- inspect 결과를 서버 세션에 저장하지 않으며 analyze 요청은 자기 완결적이다.
- worker 실패 시 Java 직접 파서로 대체하지 않는다.

## 경계

Web은 PoB 입력, 구성 선택, 자동 분석 분기와 결과 표시를 담당한다. Spring API는 공개 계약, 요청 검증, worker 오류 변환, 결과 정규화와 메커니즘 카탈로그 결합을 담당한다. Python worker는 프로세스 수명주기, 직렬화, 타임아웃과 내부 HTTP 계약을 담당한다. Lua adapter는 PoB 객체 접근과 JSON 변환만 담당한다.

`PobEngine`은 worker 내부에서 inspect와 analyze를 제공하는 계약이다. 초기 구현은 `PobProcessManager` 하나로 이 계약을 만족한다. 프로세스 풀은 같은 계약 아래 manager와 분배기를 추가하는 방식으로 확장한다.

## 사용자 선택 규칙

Web은 먼저 inspect를 요청한다. `Spec`, `SkillSet`, `ItemSet`이 각각 정확히 하나일 때만 유일한 조합으로 자동 분석한다. 어느 목록이든 여러 개면 세 목록을 모두 보여주고 PoB 활성값을 기본 선택으로 사용한다. 목록이 비어 있으면 분석 가능한 구성이 없다는 오류를 표시한다.

분석 요청은 세 선택값을 모두 포함하거나 모두 생략해야 한다. 모두 생략하면 기존 호환성을 위해 PoB 활성값을 사용한다. 일부만 전달하거나 존재하지 않는 선택값은 거부한다.

## 입력과 상태

초기 입력은 기존 결정대로 원본 XML과 압축 PoB 코드만 지원한다. Spring은 HTTP 본문 크기를 제한하고 worker는 디코딩·압축 해제 결과 크기를 제한한다. 공유 URL 지원은 별도 기능이다.

inspect와 analyze는 각각 PoB 코드를 포함한다. worker는 이전 요청에 로드된 빌드에 의존하지 않는다. 같은 코드를 두 번 import하는 비용은 headless 스파이크에서 측정하고, 실제 병목으로 확인되기 전에는 세션이나 캐시를 추가하지 않는다.

## 오류 처리

입력·선택 오류, 크기 제한, worker 프로토콜 오류, worker 사용 불가, 처리 시간 초과를 구분한다. Python·Lua·PoB 내부 예외는 공개 API에서 안정적인 코드와 메시지로 변환한다. 실패 시 Java 파서 결과를 반환하지 않는다.

Web은 inspect 실패 시 입력을 유지하고 analyze 실패 시 inspect 결과와 선택값을 유지한다. 자동 재시도는 초기 범위에 포함하지 않는다.

## 단계 1: PoB headless 스파이크

첫 구현 대상은 운영 worker가 아니라 검증용 스파이크다.

### 범위

- 구현 시점의 공식 PoB 릴리스 태그 하나를 선택하고 고정한다.
- 재현 가능한 PoB 취득·실행 방법과 라이선스 정보를 기록한다.
- stock `HeadlessWrapper.lua`를 외부 Lua script에서 실행한다.
- 단일·복수 `Spec`, `SkillSet`, `ItemSet`과 활성값을 추출한다.
- 선택 조합을 적용하고 최소 계산 요약을 반환한다.
- 초기화, import, inspect, analyze 시간과 프로세스 RSS를 측정한다.
- A → B → A 연속 처리 결과를 새 프로세스 기준값과 비교한다.

### 제외

- FastAPI 운영 서비스
- Spring 또는 Web 변경
- 프로세스 풀
- 캐시, 재시도, Docker, 배포
- 핵심 노드 delta 분석

### 완료 조건

- PoB 원본 수정 없이 필요한 구성 정보를 JSON으로 추출한다.
- 실제 단일·복수 구성 fixture에서 활성값과 목록이 PoB UI와 일치한다.
- 선택 조합 계산 결과가 같은 PoB 조합의 기준 결과와 일치한다.
- A → B → A 검사에서 상태 오염 여부를 수치로 기록한다.
- 초기화·처리 시간과 메모리를 반복 가능한 명령으로 측정한다.
- 결과를 근거로 운영 worker 진행 또는 아키텍처 재검토를 명시한다.

## 후속 단계

스파이크가 통과한 뒤에만 단일 프로세스 FastAPI worker를 별도 설계한다. 이후 Spring API 연결과 Web 선택 흐름도 각각 별도 설계·계획으로 진행한다. 선행 단계의 측정 결과 없이 worker 수, 큐, 캐시, 비동기 UX를 결정하지 않는다.

## 테스트 원칙

Lua adapter 결과는 작은 합성 XML만으로 판단하지 않고 실제 PoB export fixture와 비교한다. 구버전과 현재 형식, 단일·복수 구성, 잘못된 선택, 프로세스 재시작과 상태 오염을 다룬다. PoB 버전을 올릴 때 같은 계약 테스트를 다시 실행한다.

## 참고

- [Path of Building HeadlessWrapper](https://github.com/PathOfBuildingCommunity/PathOfBuilding/blob/dev/src/HeadlessWrapper.lua)
- [미병합 headless JSON-RPC PR #9505](https://github.com/PathOfBuildingCommunity/PathOfBuilding/pull/9505)
