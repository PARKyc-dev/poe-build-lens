# API DDD 가독성 리팩터링 (2026-08-23)

## 결과

- `BuildFactsAnalysisService`를 제거하고 `build.domain.analysis`의 목적별 분석기로 규칙 기반 분석을 분리했다.
- `BuildAnalysisServiceImpl`은 분석 결과와 `NarrativeRefiner`를 조합하는 유스케이스로 유지했다.
- OpenAI Responses 연동을 `ai.service`의 `OpenAiNarrativeRefiner`, HTTP 클라이언트, 스키마, 검증기로 분리했고 기존 프롬프트·파서·로그 기록 클래스를 함께 이동했다.
- `/api/analyses`, `AnalysisResult`, `BuildFacts`, 웹 API 타입은 변경하지 않았다.

## 경계

- 일반 `utils`, repository, 데이터베이스는 추가하지 않았다.
- 태그 문구 변환은 `MechanicProfileFormatter`라는 빌드 분석 도메인 전용 책임으로 남겼다.
- `ai.service`만 OpenAI HTTP·JSON 구현을 알고 `build`는 기존 `NarrativeRefiner` 포트에만 의존한다.

## 검증

- `cd api && ./gradlew test --no-daemon`
- `cd api && ./gradlew bootJar --no-daemon`
