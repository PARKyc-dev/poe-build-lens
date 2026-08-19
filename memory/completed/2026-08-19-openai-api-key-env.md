# OpenAI API 키 로컬 환경 설정

## 목표

OpenAI API 키가 Git 커밋에 포함되지 않도록 로컬 `.env` 파일에서만 읽는다.

## 범위

- `application.yaml`의 평문 키를 환경변수 참조로 교체한다.
- `api/.env`를 자동 로드하고 Git에서 제외한다.
- 키 없는 예시 파일을 제공한다.

## 완료 기준

- 추적되는 설정 파일에 실제 API 키가 없다.
- `api/.env`가 Git 무시 대상이다.
- API 전체 테스트와 패키지 생성이 통과한다.

## 진행 상황

- 2026-08-19: 실제 키를 `api/.env`로 옮기고 `application.yaml`은 `OPENAI_API_KEY` 환경변수를 참조하도록 변경했다.

## 검증 결과

- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.
- `cd api && git check-ignore -v .env`: `.gitignore`의 `api/.env` 규칙으로 제외됨을 확인.

## 다음 행동

- 없음.

## 차단 요소

- 없음.
