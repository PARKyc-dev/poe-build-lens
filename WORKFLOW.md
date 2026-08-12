# 에이전트 작업 흐름

작업을 시작할 때 이 파일과 `memory/INDEX.md`를 읽는다. 진행 중인 작업이 있으면 `memory/active/`의 해당 문서를 읽고, 필요한 결정은 `memory/decisions/`에서 확인한다.

새 작업은 `memory/active/YYYY-MM-DD-작업-이름.md`에 목표, 범위, 완료 기준, 진행 상황, 검증 결과, 다음 행동, 차단 요소를 기록한다. 여러 애플리케이션에 걸친 하나의 변경은 작업 기록 하나에서 경계별 상태와 검증 결과를 함께 관리한다.

구현 전에 영향받는 `web`, `api`, `worker`의 코드, 설정, 문서, 테스트를 확인한다. API 계약이 바뀌면 `api`의 응답과 `web/src/api/analysis.ts`의 타입·사용처를 같은 작업에서 갱신한다.

완료 전에는 영향받은 애플리케이션의 전체 테스트와 빌드를 실행한다. API가 변경되면 `cd api && ./gradlew test --no-daemon`과 `cd api && ./gradlew bootJar --no-daemon`을 실행하고, 웹이 변경되면 `cd web && npm test -- --run`과 `cd web && npm run build`를 실행한다. 실제 개발 서버를 사용한 검증은 실행 방법과 결과를 작업 기록에 남기고 프로세스를 종료한다.

완료 시 작업 문서를 `memory/completed/`로 옮기고 `memory/INDEX.md`를 갱신한다. 애플리케이션 또는 패키지 책임, 공통 API 응답 형식, 실행·검증 명령이 바뀌면 `AGENTS.md`, `README.md`, 관련 작업 기록에도 같은 경계를 반영한다. 장기적으로 유지할 선택 이유와 대안은 `memory/decisions/`에 기록한다.
