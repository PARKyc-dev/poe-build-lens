# 모노레포 및 React 전환

## 목표

Spring Boot API를 `api`로 분리하고 기존 정적 화면을 `web`의 Vite·React·TypeScript 앱으로 전환한다. `worker`는 향후 PoB 엔진을 위한 예약 디렉터리로 둔다.

## 범위

- 기존 Spring Boot 프로젝트를 `api`로 이동
- 기존 화면을 독립 React 앱으로 전환
- Vite 개발 프록시로 기존 분석 API 연결
- 루트 하네스와 README를 새 경계에 맞게 갱신
- `worker` 역할 문서 추가

Docker, PoB 워커 구현, 운영 배포, 새 분석 기능은 범위에서 제외했다.

## 완료한 사항

- Gradle Wrapper, 설정, Spring 운영·테스트 소스를 `api`로 이동했다.
- 기존 정적 HTML을 제거하고 Vite·React·TypeScript 기반 `web`을 추가했다.
- 분석 요청·응답 타입과 API 오류 처리를 `web/src/api/analysis.ts`에 정의했다.
- 분석 성공, API 오류, 중복 요청 방지를 검증하는 React 사용자 흐름 테스트를 추가했다.
- Vite 개발 서버가 `/api`를 Spring Boot 포트 8080으로 전달하도록 구성했다.
- `worker`를 향후 PoB 엔진 프로세스 관리용 예약 경계로 문서화했다.
- `AGENTS.md`, `WORKFLOW.md`, README와 결정 기록을 모노레포 구조에 맞게 갱신했다.

## 검증 결과

- 이동 전 `./gradlew test --no-daemon` 통과
- 이동 전 `./gradlew bootJar --no-daemon` 통과
- 최종 `cd api && ./gradlew test --no-daemon` 통과
- 최종 `cd api && ./gradlew bootJar --no-daemon` 통과
- 최종 `cd web && npm test -- --run` 통과: 테스트 파일 1개, 테스트 3개
- 최종 `cd web && npm run build` 통과
- `http://127.0.0.1:5173/api/analyses` 프록시 요청이 HTTP 200, `code: OK`, `Level 90 Witch using Fireball`을 반환함
- 통합 검증 후 Spring Boot와 Vite 개발 서버를 종료함

## 후속 사항

- PoB 엔진의 정확성, 초기화 시간, 계산 시간, 메모리, 연속 요청 격리를 독립 스파이크로 측정한다.
- 측정 결과에 따라 `worker` 런타임과 운영 배포 방식을 별도로 설계한다.
