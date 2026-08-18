# 장비창 주얼 표시

## 목표

활성 PoB 패시브 트리에 장착된 주얼과 군 주얼을 장비 상세 영역에서 확인할 수 있게 한다.

## 구현

- `build.spec.jewels`의 소켓과 아이템 ID를 읽어 브라우저 PoB 결과에 추가했다.
- 기본 이름에 `Cluster Jewel`이 포함된 아이템을 군 주얼로 분류했다.
- 장비 상세의 주얼/군 주얼 영역에서 PC hover·focus 설명 레이어와 모바일 상세 목록을 제공한다.

## 검증

- `cd web && npm test -- --run src/App.test.tsx` — 6개 통과
- `cd web && npm test -- --run src/pob/buildFacts.integration.test.ts -t 'returns installed cluster jewels'` — 1개 통과
- `cd web && npm run build` — 통과
- 전체 `npm test -- --run --reporter=verbose`는 모든 단위/UI 테스트와 첫 PoB 통합 테스트가 통과한 뒤 기존 Wasmoon/PoB 테스트 프로세스 종료 지연으로 30초 실행 제한에 도달했다.
