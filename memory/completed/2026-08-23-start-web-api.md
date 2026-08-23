# 웹·API 통합 시작 스크립트

## 목표

저장소 루트의 단일 셸 스크립트로 API와 웹 개발 서버를 시작한다.

## 범위

- `start.sh`에서 API(`./gradlew bootRun`)와 웹(`npm run dev`)을 함께 실행한다.
- 시작 전 8080·5173 포트를 수신 중인 프로세스를 종료한다.
- 스크립트를 종료하면 이 스크립트가 시작한 하위 서버도 종료한다.

## 완료 기준

- 포트 점유 프로세스 정리와 두 서버 시작을 자동화 테스트로 확인한다.
- 셸 문법 검사와 영향받는 API·웹의 전체 테스트·빌드를 실행한다.

## 진행 상황

- 2026-08-23: 포트 리스너 정리, API·웹 병렬 실행, 종료 트랩을 `start.sh`에 구현했다.

## 검증 결과

- `sh -n start.sh`
- `sh -n tests/start.sh.test.sh`
- `sh tests/start.sh.test.sh`: 가짜 8080·5173 리스너 종료 요청과 API·웹 기동 확인.
- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.
- `cd web && npm test -- --run`: PoB `buildFacts.integration` 테스트가 출력 없이 장시간 정지해 중단.
- 나머지 웹 테스트 5개 파일 21개 테스트: 개별 실행으로 통과.
- `cd web && npm run build`: 통과.

## 다음 행동

- 없음

## 차단 요소

- 웹 PoB `buildFacts.integration` 테스트의 장시간 정지 원인 확인이 별도 필요하다.
