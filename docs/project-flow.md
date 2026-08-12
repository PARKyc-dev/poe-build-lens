# PoE Lens 프로젝트 흐름

## 목적

PoE Lens는 사용자가 입력한 Path of Building(PoB) 빌드를 PoB 엔진으로 해석하고, 사용자가 선택한 구성의 계산 결과와 검토된 로컬 메커니즘을 결합해 설명한다.

현재 구현은 Spring API가 PoB XML을 직접 파싱하는 초기 단계다. 목표 구조에서는 PoB가 `Spec`, `SkillSet`, `ItemSet`과 계산 결과를 해석하는 유일한 기준이며, Spring은 HTTP 경계와 결과 조합을 담당한다.

## 애플리케이션 책임

### Web

- PoB 코드 입력
- PoB 구성 확인 요청
- 단일 구성 자동 분석
- 복수 구성 선택
- 분석 요청과 결과 표시
- 입력·선택·분석 오류 상태 관리

### API

- 외부 HTTP 요청과 공통 응답 계약
- 요청 본문 크기와 선택값 형식 검증
- worker 호출과 오류 변환
- worker 결과 정규화
- 로컬 메커니즘 카탈로그 결합
- 향후 PoB 버전을 포함한 결과 캐시

API는 PoB 의미를 Java로 다시 계산하지 않으며 worker 실패 시 Java 파서로 조용히 대체하지 않는다.

### Worker

- Python FastAPI 내부 인터페이스
- PoB 프로세스 시작·종료·재시작
- 요청 직렬화와 타임아웃
- Lua adapter와 JSON 통신
- 향후 여러 PoB 프로세스에 대한 작업 분배

초기에는 `PobProcessManager` 하나가 PoB 프로세스 하나를 소유한다. 각 요청은 다른 요청의 빌드 상태에 의존하지 않는다.

### Lua adapter와 PoB

- 고정된 공식 PoB 릴리스 사용
- 공식 `HeadlessWrapper.lua`로 빌드 로드
- PoB 내부의 `Spec`, `SkillSet`, `ItemSet`과 활성값 추출
- 선택된 구성 적용
- PoB 계산 결과를 JSON으로 변환

PoB 원본은 수정하지 않는다. 미병합 JSON-RPC 구현은 참고 자료로만 사용하고 런타임에 포함하지 않는다.

## 사용자 흐름

### 1. PoB 입력

사용자가 원본 PoB XML 또는 압축 내보내기 코드를 입력하고 `PoB 불러오기`를 누른다. Web은 사용자가 입력하는 동안 자동 요청하지 않는다.

### 2. 구성 확인

Web은 `POST /api/builds/inspect`로 PoB 코드를 전송한다. API는 worker의 inspect 기능을 호출한다. worker는 PoB로 빌드를 import하고 다음을 반환한다.

- 모든 `Spec`과 `activeSpec`
- 모든 `SkillSet`과 `activeSkillSet`
- 모든 `ItemSet`과 `activeItemSet`
- 캐릭터 레벨, 클래스, 게임 버전
- 선택지를 구분할 수 있는 제목과 최소 요약

구버전 PoB는 PoB가 import한 내부 상태를 그대로 기준으로 한다. API와 Web은 임의의 가상 세트를 만들지 않는다.

### 3. 자동 분석

다음 조건을 모두 만족하면 Web이 유일한 조합으로 분석을 자동 요청한다.

- `Spec`이 정확히 1개
- `SkillSet`이 정확히 1개
- `ItemSet`이 정확히 1개

목록 중 하나라도 비어 있으면 자동 분석하거나 선택 화면을 표시하지 않고 분석 가능한 구성을 찾지 못했다는 오류를 표시한다.

### 4. 사용자 선택

어느 목록이든 2개 이상이면 Web은 세 목록을 모두 표시한다. `activeSpec`, `activeSkillSet`, `activeItemSet`을 기본값으로 선택한다.

사용자는 다음을 각각 하나씩 선택한다.

- `Spec`: 제목, 트리 버전, 할당 노드 수
- `SkillSet`: 제목, 주요 스킬
- `ItemSet`: 제목, 슬롯별 아이템

세 선택값이 모두 유효할 때만 `분석 요청`을 사용할 수 있다.

### 5. 분석

Web은 `POST /api/analyses`로 PoB 코드와 세 선택값을 전송한다.

```json
{
  "pobInput": "...",
  "activeSpec": 1,
  "activeSkillSet": 2,
  "activeItemSet": 1
}
```

기존 호환성을 위해 세 선택값이 모두 없으면 PoB의 활성값을 사용한다. 세 값 중 일부만 있거나 PoB에 존재하지 않는 값이면 요청을 거부한다.

worker는 빌드를 다시 import하고 선택 조합을 적용한 뒤 계산한다. inspect 상태를 서버에 보관하지 않으므로 worker 프로세스가 바뀌어도 분석 요청을 독립적으로 처리할 수 있다.

### 6. 결과

API는 PoB 계산 결과와 로컬 메커니즘 카탈로그를 결합한다. 결과에는 실제 사용한 `activeSpec`, `activeSkillSet`, `activeItemSet`을 포함한다.

분석 실패 시 Web은 inspect 결과와 사용자 선택을 유지한다. 사용자는 같은 조합을 다시 요청하거나 다른 조합을 선택할 수 있다. 새로운 PoB 코드를 입력하면 이전 구성과 결과를 초기화한다.

## 공개 API 목표 계약

### 구성 확인

```http
POST /api/builds/inspect
```

응답은 캐릭터 요약, `specs`, `skillSets`, `itemSets`, 세 활성값과 `selectionRequired`를 포함한다. `selectionRequired`는 세 목록 중 하나라도 2개 이상일 때 참이다.

### 분석

```http
POST /api/analyses
```

기존 `pobInput` 단독 요청과 세 활성값을 모두 포함하는 선택 분석 요청을 지원한다.

## 내부 worker 계약

```http
POST /v1/builds/inspect
POST /v1/builds/analyze
```

Lua adapter의 초기 명령 범위는 다음과 같다.

- `import_build`
- `list_specs`
- `list_skill_sets`
- `list_item_sets`
- `select_loadout`
- `calculate_summary`

## 오류 처리

- 잘못된 PoB 입력: HTTP 400
- 존재하지 않거나 일부만 전달된 선택값: HTTP 400
- 요청 또는 압축 해제 크기 한도 초과: HTTP 413
- 예기치 못한 worker 응답: HTTP 502
- worker 사용 불가: HTTP 503
- PoB 처리 시간 초과: HTTP 504

worker 오류에는 안정적인 내부 오류 코드를 사용한다. PoB나 Python의 원본 예외 메시지는 Web에 직접 노출하지 않는다.

## 병렬 확장

PoB 내부 상태를 공유하는 스레드 병렬화는 사용하지 않는다. 추후 병렬화는 PoB 프로세스를 여러 개 실행하는 방식으로 확장한다.

초기 `PobEngine` 계약과 요청 형식은 프로세스 개수와 무관하게 유지한다. 프로세스 풀 도입 시 manager 목록, 사용 가능한 프로세스를 선택하는 분배기, 프로세스별 상태 관리만 worker 내부에 추가한다. 공개 API 계약은 바꾸지 않는다.

## 단계별 도입

1. PoB headless 스파이크로 정보 추출·계산·성능·상태 격리를 검증한다.
2. 단일 PoB 프로세스를 관리하는 Python worker를 구현한다.
3. Spring inspect·analyze API를 worker에 연결한다.
4. Web에 자동 분석과 복수 구성 선택 흐름을 구현한다.

각 단계는 별도 설계·계획·검증 단위다. 선행 단계가 완료 기준을 만족하지 않으면 다음 단계로 진행하지 않는다.

## 현재 상태

- Web과 API 모노레포 경계 구성 완료
- Spring의 초기 직접 PoB 파서 사용 중
- worker는 예약 문서만 존재
- PoB headless 스파이크 미실행

다음 작업은 PoB headless 스파이크다. 이 단계에서는 운영 worker나 공개 API를 구현하지 않는다.
