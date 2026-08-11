# 에이전트 작업 흐름

작업을 시작할 때 이 파일과 `memory/INDEX.md`를 읽는다. 진행 중인 작업이 있으면 `memory/active/`의 해당 문서를 읽고, 필요한 결정은 `memory/decisions/`에서 확인한다.

새 작업은 `memory/active/YYYY-MM-DD-작업-이름.md`에 목표, 범위, 완료 기준, 진행 상황, 검증 결과, 다음 행동, 차단 요소를 기록한다. 완료 시 해당 문서를 `memory/completed/`로 옮기고 `memory/INDEX.md`를 갱신한다. 패키지 책임이나 공통 API 응답 형식이 바뀌면 `AGENTS.md`와 관련 작업 기록에도 같은 경계를 반영한다. `memory/decisions/`에는 구현 내역이 아니라 선택 이유와 대안을 기록한다.
