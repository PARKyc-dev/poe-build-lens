# 시작 스크립트 빈 포트·실행 포트 표시

## 목표

루트 시작·종료 스크립트가 비어 있는 포트에서 정상 동작하고, 시작 스크립트가 실제로 열린 API·웹 포트를 출력한다.

## 범위

- `start.sh`, `shutdown.sh`의 빈 리스너 처리 수정
- 서버 준비 로그에서 API·웹의 실제 TCP 수신 포트 추출
- 셸 회귀 테스트 갱신

## 완료 기준

- 빈 포트와 점유 포트 모두에서 셸 테스트 통과
- 실제 개발 서버의 기동 URL과 종료 확인
- API·웹 테스트와 빌드 확인

## 진행 상황

- 2026-08-23: 빈 포트에서 `return`이 상태 1을 반환해 스크립트를 종료하는 문제를 재현했다.
- 2026-08-23: Gradle 데몬의 내부 TCP 리스너가 API 포트로 잘못 감지되는 것을 실제 기동에서 확인했다.
- 2026-08-23: Gradle·Vite 준비 로그에서 포트를 추출하고, 서버 로그도 계속 터미널에 표시하도록 변경했다.

## 검증 결과

- `sh -n start.sh shutdown.sh tests/start.sh.test.sh tests/shutdown.sh.test.sh`: 통과.
- `sh tests/start.sh.test.sh`: 점유 포트 정리, 빈 포트 기동, 준비 로그의 실행 포트 출력 확인.
- `sh tests/shutdown.sh.test.sh`: 점유·빈 포트 모두 정상 종료 확인.
- 실제 `./start.sh`: `API: http://localhost:8080`, `Web: http://localhost:5173` 출력 확인.
- 실제 `./shutdown.sh`: 8080·5173 리스너 종료 확인.
- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.
- `cd web && npm run build`: 통과.
- `cd web && npm test -- --run`: 기존 `buildFacts.integration` 테스트가 Vitest 시작 후 30초 동안 진행 출력 없이 정지해 완료되지 못했다.

## 다음 행동

- 없음

## 차단 요소

- 웹 전체 테스트의 기존 `buildFacts.integration` 정지 원인 확인이 별도 필요하다.
