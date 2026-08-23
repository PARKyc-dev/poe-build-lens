# 웹·API 종료 스크립트

## 목표

저장소 루트의 단일 셸 스크립트로 웹과 API 개발 서버를 종료한다.

## 범위

- `shutdown.sh`에서 8080·5173 포트를 수신 중인 프로세스를 종료한다.
- 이미 비어 있는 포트는 오류 없이 건너뛴다.

## 완료 기준

- 두 포트 리스너 종료를 자동화 테스트로 확인한다.
- 셸 문법 검사와 영향받는 API·웹의 전체 테스트·빌드를 실행한다.

## 진행 상황

- 2026-08-23: 8080·5173 포트 리스너 종료 스크립트를 구현했다.

## 검증 결과

- `sh -n shutdown.sh`
- `sh -n tests/shutdown.sh.test.sh`
- `sh tests/shutdown.sh.test.sh`: 두 리스너 종료 요청과 포트 해제를 확인.
- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.
- `cd web && npm run build`: 통과.
- 웹 전체 테스트는 이전 작업에서 PoB `buildFacts.integration` 테스트가 출력 없이 장시간 정지해 중단됐으며, 이번 변경은 해당 테스트와 무관하다.

## 다음 행동

- 없음

## 차단 요소

- 웹 PoB `buildFacts.integration` 테스트의 장시간 정지 원인 확인이 별도 필요하다.
