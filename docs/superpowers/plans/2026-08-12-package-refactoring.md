# 패키지 리팩터링 구현 계획

> **에이전트 작업자용:** 이 계획은 작업별로 `superpowers:subagent-driven-development` 또는 `superpowers:executing-plans`를 사용해 구현한다. 단계는 체크박스(`- [ ]`) 형식으로 관리한다.

**목표:** 분석 동작을 유지하면서 PoB 분석을 기능 패키지로 옮기고 표준 성공·오류 API 본문을 반환한다.

**구조:** `build` 컨트롤러는 `BuildAnalysisService` 인터페이스를 호출한다. 구현체는 빌드를 파싱하고 메커니즘 서비스 인터페이스로 로컬 JPA 카탈로그를 조회한다. 응답 조언은 성공한 컨트롤러 본문만 감싸고, 예외 조언은 공통 오류 DTO를 직접 반환한다.

**기술 구성:** Java 21, Spring Boot Web MVC, Spring Data JPA, Jakarta Validation, H2, MockMvc.

## 공통 제약

- `/api/analyses`, H2 설정, `data.sql`, 원본 XML 입력, Base64 압축 입력 동작을 유지한다.
- `MISSING_BUILD_INPUT`, `INVALID_POB_INPUT`, `UNSUPPORTED_GAME_VERSION`은 HTTP 400 코드로 유지한다.
- 관련 없는 사용자 변경은 수정하지 않는다.

---

### 작업 1: 실패하는 테스트로 HTTP 경계를 명세화한다

**파일:**
- 수정: `src/test/java/com/parkyc/poelens/BuildAnalysisControllerTest.java`

**인터페이스:**
- 입력: `POST /api/analyses` JSON 요청 `{ "pobInput": "..." }`.
- 출력: 성공 본문 `CommonDTO.Response<AnalysisResult>`와 오류 본문 `CommonDTO.Exception`.

- [ ] **단계 1: 실패하는 응답 계약 테스트를 작성한다**

`$.code == "OK"`, `$.message == "SUCCESS"`, `$.returnObject` 아래의 모든 분석 필드를 검증하고, 빈 입력·형식이 잘못된 XML·지원하지 않는 게임 버전이 기존 코드와 HTTP 400을 반환하는지 검증한다.

- [ ] **단계 2: 래퍼 검증이 실패하는지 컨트롤러 테스트를 실행한다**

실행: `./gradlew test --no-daemon --tests com.parkyc.poelens.BuildAnalysisControllerTest`

- [ ] **단계 3: 최소한의 패키지·서비스·응답 변경을 구현한다**

기능 패키지와 `common`·`config` 클래스를 만들고, 파서·카탈로그 의미를 바꾸지 않고 기존 소스 파일을 옮기며, 정적 화면이 `body.returnObject`를 읽도록 갱신한다.

- [ ] **단계 4: 새 계약이 통과하는지 컨트롤러 테스트를 실행한다**

실행: `./gradlew test --no-daemon --tests com.parkyc.poelens.BuildAnalysisControllerTest`

### 작업 2: 애플리케이션 동작을 유지하고 기록을 갱신한다

**파일:**
- 수정: `AGENTS.md`, `WORKFLOW.md`, `memory/INDEX.md`
- 이동·생성: `memory/completed/` 아래의 완료 작업 기록

**인터페이스:**
- 입력: 완료된 패키지 리팩터링.
- 출력: `build`, `mechanics`, `common`, `config` 책임 경계를 설명하는 문서.

- [ ] **단계 1: 전체 테스트 모음을 실행한다**

실행: `./gradlew test --no-daemon`

- [ ] **단계 2: 실행 가능한 아카이브를 생성한다**

실행: `./gradlew bootJar --no-daemon`

- [ ] **단계 3: 작업 흐름 메모리와 프로젝트 지침을 갱신한다**

새 구조, 검증·오류 경계, 검증 결과, 완료 작업 기록을 남기되 관련 없는 지침은 바꾸지 않는다.
