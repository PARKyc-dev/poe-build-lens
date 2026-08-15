# 브라우저 PoB 분석 결과 API 연동

## 완료 결과

- `POST /api/analyses`는 원본 PoB 대신 게임 버전, nullable 주력 공격 기재, 요약 수치를 받는다.
- API는 PoB를 재계산하지 않고 `(gameVersion, skillName, delivery)` 카탈로그를 조회한다.
- 3.29 Fireball·Arc 직접 시전 카탈로그와 근거를 추가했다.
- 기존 H2의 두 열 고유 제약을 세 열 제약으로 안전하게 마이그레이션하며 기존 행을 보존한다.
- 웹은 `3_29`를 `3.29`로 정규화해 API를 호출하고, 검토된 메커니즘·근거 또는 미검증 상태를 표시한다.

## 검증

- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.
- `cd web && npm test -- --run`: 5개 파일, 19개 테스트 통과.
- `cd web && npm run build`: 통과.

## 주의 사항

- 루트 `.gitignore`의 `build/` 규칙이 `web/src/build/`를 무시한다. 해당 변경을 커밋하려면 force-add 또는 ignore 규칙 정리가 필요하다.
