# PoE Lens 모노레포 및 React 전환 설계

## 목표

현재 루트에 함께 있는 Spring Boot API와 정적 화면을 `api`와 `web` 경계로 분리한다. 기존 PoB 분석 기능과 공개 API 계약을 유지하면서 화면을 Vite, React, TypeScript 기반의 독립 앱으로 전환한다. 향후 PoB 엔진 프로세스를 둘 `worker`는 역할을 설명하는 문서만 가진 예약 디렉터리로 둔다.

이번 전환은 애플리케이션 경계를 명확히 하는 작업이다. Docker, 운영 리버스 프록시, PoB 엔진, 작업 큐, 캐시, 인증, 새 분석 기능은 포함하지 않는다.

## 저장소 구조

```text
poe-lens/
├── AGENTS.md
├── WORKFLOW.md
├── README.md
├── memory/
├── harness-and-pob-notes.md
├── web/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
├── api/
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradlew
│   ├── gradle/
│   └── src/
└── worker/
    └── README.md
```

`web`은 화면 렌더링, 사용자 입력 상태, 분석 API 호출과 결과 표시를 담당한다. `api`는 PoB 입력 검증과 파싱, 로컬 메커니즘 조회, 공통 HTTP 응답을 담당한다. `worker`는 향후 PoB 엔진 프로세스 관리를 위한 경계만 예약하며 실행 코드, 의존성, 인터페이스를 갖지 않는다. 루트는 구현 소스를 갖지 않고 저장소 공통 지침, 작업 기록, 설명 문서를 관리한다.

현재 Gradle 프로젝트의 설정, Wrapper, 운영·테스트 소스는 의미를 바꾸지 않고 `api`로 이동한다. Java 패키지 구조와 `/api/analyses` 계약도 유지한다. 기존 `src/main/resources/static/index.html`은 React 화면이 같은 사용자 흐름을 검증한 뒤 제거한다.

## 웹 애플리케이션

`web`은 Vite, React, TypeScript로 구성하며 npm을 사용한다. 상태 관리, API 클라이언트, UI 프레임워크는 추가하지 않는다.

```text
web/src/
├── api/
│   └── analysis.ts
├── App.tsx
├── App.test.tsx
├── main.tsx
└── styles.css
```

`analysis.ts`는 요청·응답 타입과 `POST /api/analyses` 호출을 보관한다. `App.tsx`는 PoB 입력, 분석 요청 상태, 성공 결과, 오류 메시지를 관리한다. 기존 화면과 같이 개요, 핵심 메커니즘, 기여 요소, 방어, 자원 유지, 미확인 항목, 근거를 표시한다.

요청 중에는 중복 전송을 막기 위해 분석 버튼을 비활성화한다. API 오류 응답에 `message`가 있으면 이를 표시하고, 연결 실패나 해석할 수 없는 응답에는 일반적인 연결 오류를 표시한다. 빈 입력에 대한 최종 검증 책임과 공개 오류 코드는 기존처럼 API에 둔다.

## 개발 데이터 흐름

```text
브라우저 → Vite 개발 서버(:5173) → /api 프록시 → Spring Boot(:8080)
```

React는 환경별 서버 주소를 조합하지 않고 상대 경로 `/api/analyses`를 호출한다. Vite 개발 서버가 `/api` 요청을 로컬 Spring Boot 서버로 전달하므로 개발용 CORS 설정은 추가하지 않는다. `web`과 `api`는 각각 독립적으로 실행한다.

운영 배포 결합 방식은 이번 범위에 포함하지 않는다. PoB 워커를 실제로 도입할 때 Docker Compose와 리버스 프록시를 함께 설계한다.

## 하네스와 문서

루트 `AGENTS.md`는 `web`, `api`, `worker`의 책임과 각각의 검증 명령을 설명하도록 갱신한다. `WORKFLOW.md`와 `memory/{active,completed,decisions}` 구조는 이미 규칙, 절차, 상태를 분리하므로 교체하지 않는다.

README는 새 구조, 프론트와 API의 독립 실행 방법, 현재 워커가 예약 상태임을 설명한다. 모노레포 경계 선택 이유는 결정 기록에 남긴다. 작업 중 상태는 `memory/active`에 기록하고 완료 후 `memory/completed`로 이동한다.

루트 통합 빌드, 임시 셸 스크립트, 중첩 `AGENTS.md`, 저장소 전용 스킬은 이번 작업에서 만들지 않는다. 반복 작업이 확인되기 전에는 하네스 자산을 추가하지 않는다.

## 마이그레이션과 호환성

먼저 현재 API 테스트가 이동 전 기준선으로 통과하는지 확인한다. 그다음 기존 Spring 파일을 `api`로 이동하고 새 위치에서 같은 테스트와 패키징이 통과하는지 확인한다. React 화면은 기존 API 계약을 타입으로 표현하고, 실패하는 사용자 흐름 테스트를 먼저 작성한 뒤 최소 구현으로 통과시킨다.

기존 H2 설정과 로컬 파일 데이터베이스 상대 경로의 의미는 `api` 실행 디렉터리를 기준으로 유지한다. 기존 루트의 사용자 데이터 파일이 있다면 자동 이동하거나 삭제하지 않는다. 관련 없는 미추적 파일과 사용자 변경도 수정하지 않는다.

## 검증과 완료 기준

- `web`의 React 사용자 흐름 테스트가 통과한다.
- `web`의 TypeScript 검사와 프로덕션 빌드가 성공한다.
- `api`의 전체 테스트가 새 위치에서 통과한다.
- `api`의 Spring Boot 실행 JAR이 생성된다.
- 실행 중인 두 개발 서버를 통해 실제 PoB 분석 요청과 결과 표시를 확인한다.
- 루트에 이전 Spring 소스와 Gradle 프로젝트 파일이 남지 않는다.
- `worker`에는 예약 목적을 설명하는 문서 외 구현이 없다.
- `/api/analyses` 경로, 성공·오류 JSON 계약, 기존 분석 결과는 바뀌지 않는다.
