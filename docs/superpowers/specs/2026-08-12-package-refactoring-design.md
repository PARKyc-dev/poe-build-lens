# PoE Lens 패키지 리팩터링 설계

## 목표

역할별 `analysis` 구현을 기능 우선 패키지로 옮기고, 서비스 계약과 구현체를 분리하며, PoB 분석 동작을 바꾸지 않고 API 성공·오류 본문을 표준화한다.

## 구조

`build`는 `/api/analyses` 엔드포인트, PoB 파서, 분석 DTO를 담당한다. `BuildAnalysisService`는 컨트롤러가 의존하는 인터페이스이고, `BuildAnalysisServiceImpl`은 파싱과 메커니즘 서비스 조합을 담당한다.

`mechanics`는 로컬 메커니즘 카탈로그, JPA 엔티티, 리포지터리를 담당한다. `MechanicService`는 카탈로그 버전과 대소문자를 구분하지 않는 조회를 제공하고, `MechanicServiceImpl`은 JPA와 설정된 버전에 의존한다.

`common`은 응답·오류 코드와 공통 성공·오류 DTO를 담당한다. `config`는 `PoeLensException`, 전역 예외 변환, 자동 성공 응답 래핑을 담당한다.

## 패키지 구조

```
com.parkyc.poelens
├── build
│   ├── controller
│   ├── service
│   ├── parser
│   └── domain/dto
├── mechanics
│   ├── service
│   ├── repository
│   └── domain/entity
├── common
│   ├── code
│   └── dto
└── config
    ├── exception
    └── response
```

## 요청·응답·오류 흐름

컨트롤러는 비어 있지 않은 `pobInput`을 가진 `@Valid` 요청 DTO를 받는다. 성공한 분석 결과는 `AnalysisResult`를 유지하되 `PoeLensResponseAdvice`가 감싼다.

```json
{
  "code": "OK",
  "message": "SUCCESS",
  "returnObject": { "gameVersion": "3.27" }
}
```

`PoeLensException`은 HTTP 상태와 메시지를 가진 `ErrorCode`를 보관한다. `MISSING_BUILD_INPUT`, `INVALID_POB_INPUT`, `UNSUPPORTED_GAME_VERSION`은 HTTP 400 오류로 유지한다. 빈 입력의 Bean Validation 오류는 기존 공개 오류 코드와 같은 `MISSING_BUILD_INPUT` 공통 오류 본문을 반환한다. 예외 본문은 성공 응답으로 감싸지 않는다.

## 데이터와 호환성

H2 설정, `MechanicEntity` 매핑, `data.sql`의 Fireball·Arc 행은 의미상 변경하지 않는다. 원본 XML과 Base64 압축 PoB 파싱도 유지한다. 새 엔드포인트, 데이터베이스 마이그레이션, 카탈로그 데이터는 추가하지 않는다.

## 테스트와 문서

컨트롤러 테스트는 `$.returnObject` 아래의 성공 데이터, 빈 입력·형식이 잘못된 입력·지원하지 않는 버전의 공통 오류 코드와 메시지, 기존 원본 XML·압축 입력·JPA 카탈로그 사례를 검증한다. 프로젝트 작업 흐름과 메모리 기록은 새 책임 경계와 완료된 리팩터링을 설명한다.
