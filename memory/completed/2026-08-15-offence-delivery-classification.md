# 공격 기재와 운용 방식 분류

## 목표

`pobb.in` 링크 또는 PoB 내보내기 코드에서 브라우저 PoB 엔진이 추출한 데이터를 바탕으로, 현재 선택된 주력 공격 기재와 운용 방식을 분류하고 후속 빌드 분석의 입력으로 사용한다.

## 범위

- 1차 분류는 공격 기재와 운용 방식까지만 다룬다.
- PoB의 `mainSkill`을 주력 공격 기재의 기준으로 사용한다.
- 운용 방식은 직접 시전 (Self-Cast), 공격 (Attack), 토템 (Totem), 트랩 (Trap), 마인 (Mine), 소환수 (Minion), 브랜드 (Brand), 트리거 (Trigger)로 분류하고, 확실한 근거가 없으면 확인 불가로 표시한다.
- 여러 피해 스킬, RF의 지속 피해 같은 피해 특성, 방어 기재와 기타 스킬은 후속 작업 범위다.

## 완료 결과

- PoB Worker가 계산 완료된 `mainSkill`의 공격·토템·트랩·마인·브랜드·직접 시전·소환수·트리거 플래그를 반환한다.
- 트리거는 PoB의 전체 계산 기준(`triggered`, `triggeredByUnique`, Triggered/InbuiltTrigger 타입, 젬·소스 인스턴스 트리거)을 반영한다.
- 소환수는 플레이어의 `haveMinion`과 자식 스킬의 `minion` 플래그를 함께 반영한다.
- 웹 분류 모델은 전달 방식 우선순위와 한글·영문 병기 레이블을 제공한다. 일반 공격에서 PoB가 함께 설정하는 `selfCast`는 공격으로 분류한다.
- BUILD INSIGHT에 주력 공격 기재명과 운용 방식이 표시되며, `mainSkill`이 없으면 확인 불가로 표시한다.
- 기존 메커니즘·방어 주의점·강화 우선순위 규칙은 변경하지 않았다.

## 검증 결과

- `cd web && npm test -- --run src/build/offenceClassification.test.ts`: 9개 테스트 통과.
- `cd web && npm test -- --run src/App.test.tsx src/build/buildInsight.test.ts`: 6개 테스트 통과.
- `cd web && npm test -- --run`: 4개 파일, 17개 테스트 통과.
- `cd web && npm run build`: TypeScript 컴파일 및 Vite 프로덕션 빌드 통과.
- 서브에이전트 기반 구현·작업별 검토·전체 기능 재검토에서 추가 조치가 필요한 항목이 없음을 확인했다.

## 주의 사항

- 루트 `.gitignore`의 `build/` 규칙이 `web/src/build/`도 무시한다. 이번에 추가·수정한 해당 디렉터리 파일은 작업 공간에는 존재하지만, 커밋할 때는 의도적으로 force-add 하거나 ignore 규칙을 별도 정리해야 한다. 이 규칙 정리는 요청 범위 밖이라 변경하지 않았다.
