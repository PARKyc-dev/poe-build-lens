# BuildFacts 기반 기재 분석

## 목표

PoB가 계산한 공격·방어·버프 기재를 API로 전달하고, 범용 기재 규칙과 버전별 고유 메커니즘으로 분석한다.

## 완료 내용

- 주력/보조 공격 수단과 실제 전달 방식을 추출해 API 분석으로 연결했다.
- `SkillType.Movement`가 있는 스킬은 공격 기재에서 제외하고 이동기로 분석한다.
- Tempest Shield는 PoB가 계산한 감전 면역과 주문 막기 수치가 모두 적용될 때만 해당 방어 분석을 반환한다.
- 노터블·키스톤·마스터리만 핵심 패시브로 전달하며, 3.29의 Tasalio와 Hinekora 인과 규칙을 별도 패시브 분석에 표시한다.
- ACTIVE CONFIGURATION은 생명력·에너지 보호막·저항·막기·주문 억제 수치만 보이고, 플라스크 1~5는 장비 상세에 포함한다.
- 루트 `.gitignore`의 광범위한 `build/` 규칙을 루트/API 산출물에만 적용해 `web/src/build/` 소스가 추적되도록 수정했다.

## 검증 결과

- `cd web && npm test -- --run` — 6 files, 20 tests passed
- `cd web && npm run build` — passed
- `cd api && ./gradlew test --no-daemon` — passed
- `cd api && ./gradlew bootJar --no-daemon` — passed
