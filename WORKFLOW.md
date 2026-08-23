# 에이전트 작업 흐름

작업을 시작할 때 이 파일과 `memory/README.md`를 읽는다. `active/current-context.md`와 필요하면 `active/current-decisions.md`를 확인한 뒤, 작업에 관련된 `knowledge/`와 `decisions/`만 읽는다. `archive/`는 과거 맥락이 필요한 경우에만 검색한다.

구현 전에 영향받는 `web`, `api`, `worker`의 코드, 설정, 문서, 테스트를 확인한다. API 계약이 바뀌면 `api`의 응답과 `web/src/api/analysis.ts`의 타입·사용처를 같은 작업에서 갱신한다.

완료 전에는 영향받은 애플리케이션의 전체 테스트와 빌드를 실행한다. API가 변경되면 `cd api && ./gradlew test --no-daemon`과 `cd api && ./gradlew bootJar --no-daemon`을 실행하고, 웹이 변경되면 `cd web && npm test -- --run`과 `cd web && npm run build`를 실행한다. 실제 개발 서버를 사용한 검증은 실행 방법과 결과를 작업 기록에 남기고 프로세스를 종료한다.

의미 있는 작업을 완료할 때는 `active/current-context.md`를 갱신하고 완료 항목을 제거한다. 재사용할 사실·규칙은 `knowledge/`에, 중요한 안정된 선택과 대안·근거는 `decisions/` ADR에, 완료 작업의 간결한 이력은 `archive/`에 기록한다. 중복되거나 저가치인 로그는 고유 정보를 보존한 뒤에만 제거한다.

애플리케이션 또는 패키지 책임, 공통 API 응답 형식, 실행·검증 명령이 바뀌면 `AGENTS.md`, `README.md`, 관련 메모리에도 같은 경계를 반영한다.
