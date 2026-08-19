# OpenAI 프롬프트 로그

## 목표

OpenAI Responses API 호출에 사용한 프롬프트와 받은 응답을 프로젝트 루트 `logs/prompt/`의 텍스트 파일로 저장한다.

## 범위

- OpenAI 호출 경로에 프롬프트·응답 로그 저장을 추가한다.
- API 키와 Authorization 헤더는 로그에서 제외한다.
- 로그 파일은 Git 추적 대상에서 제외한다.

## 완료 기준

- 호출마다 프롬프트와 응답을 포함한 `.txt` 파일이 생성된다.
- 로그 저장 실패가 기존 분석 결과를 실패시키지 않는다.
- API 전체 테스트와 패키지 생성이 통과한다.

## 진행 상황

- 2026-08-19: OpenAI 호출 지점을 `OpenAiNarrativeClient`로 확인했다.
- 2026-08-19: 프롬프트·응답 텍스트 저장기를 추가하고 OpenAI 응답을 받은 직후 기록하도록 연결했다.

## 검증 결과

- `cd api && ./gradlew test --no-daemon --tests com.parkyc.poelens.ai.infrastructure.PromptLogWriterTest`: 통과.
- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.

## 다음 행동

- 없음.

## 차단 요소

- 없음.
