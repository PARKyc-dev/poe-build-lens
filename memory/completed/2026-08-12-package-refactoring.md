# 패키지 구조 및 공통 응답 리팩터링

## 목표

역할별 `analysis` 패키지를 기능별 `build`·`mechanics` 패키지로 옮기고, 서비스 인터페이스/구현체와 공통 API 응답·예외 처리를 도입한다.

## 완료한 사항

- PoB 분석 컨트롤러, 파서, DTO와 분석 서비스를 `build`로 이동했다.
- 메커니즘 카탈로그의 서비스, JPA 엔티티와 리포지터리를 `mechanics`로 이동했다.
- `BuildAnalysisService`·`MechanicService` 인터페이스와 각각의 `Impl` 구현체를 분리했다.
- `common`에 성공·오류 코드와 공통 DTO를 추가하고, 성공 응답을 `code`, `message`, `returnObject` 형식으로 통일했다.
- `config`에 `PoeLensException`, 전역 예외 처리, 성공 응답 래핑을 추가했다.
- 빈 PoB 입력은 Bean Validation으로 검증하고 `MISSING_BUILD_INPUT` 공통 오류 응답으로 반환한다.
- `INVALID_POB_INPUT`, `UNSUPPORTED_GAME_VERSION`을 포함한 기존 분석 오류 코드를 HTTP 400으로 유지했다.
- 정적 분석 화면이 래핑된 성공 응답을 읽도록 갱신했다.
- 원본 XML, 압축 PoB 코드, Fireball·Arc JPA 카탈로그 조회 및 성공·오류 응답 계약 테스트를 갱신했다.

## 검증

- 2026-08-12 `./gradlew test --no-daemon` 통과
- 2026-08-12 `./gradlew bootJar --no-daemon` 통과

## 후속 사항

- 검토된 메커니즘 카탈로그 항목을 추가하고 PoB 내보내기 형식을 더 지원한다.
