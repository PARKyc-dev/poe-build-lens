# OpenAI 응답 파싱 수정

## 목표

Responses API가 reasoning 항목을 먼저 반환해도 실제 `output_text`의 분석 문장을 파싱한다.

## 범위

- OpenAI Responses API 응답에서 message/output_text를 찾아 파싱한다.
- reasoning 항목이 앞서는 실제 응답 구조를 테스트한다.
- 테스트 환경에서는 OpenAI 호출을 차단한다.

## 완료 기준

- reasoning 뒤의 output_text에 든 공격·방어 문장을 읽는다.
- API 전체 테스트와 패키지 생성이 통과한다.

## 진행 상황

- 2026-08-19: 실행 로그에서 reasoning 항목이 첫 output이고 실제 JSON은 뒤의 message/output_text에 있음을 확인했다.
- 2026-08-19: message/output_text를 탐색하는 응답 파서를 추가했다.
- 2026-08-19: 테스트 설정에서 OpenAI를 비활성화해 외부 API 호출과 비용 발생을 차단했다.

## 검증 결과

- `cd api && ./gradlew test --no-daemon --tests com.parkyc.poelens.ai.infrastructure.OpenAiResponseParserTest`: 통과.
- `cd api && ./gradlew test --no-daemon`: 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.

## 다음 행동

- 없음.

## 차단 요소

- 없음.
