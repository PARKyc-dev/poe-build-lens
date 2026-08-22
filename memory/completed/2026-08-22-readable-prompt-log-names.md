# 읽기 쉬운 프롬프트 로그 파일명

## 목표

OpenAI 프롬프트 로그를 날짜와 일별 순번으로 읽기 쉬운 이름으로 저장한다.

## 범위

- `prompt` 로그 폴더에서 당일 파일의 최대 순번을 찾는다.
- 새 로그를 `YYYYMMDD-NN.txt` 형식으로 저장한다.
- 파일명 산정과 저장을 동기화해 동일 프로세스의 동시 요청이 중복되지 않게 한다.

## 완료 기준

- 첫 로그는 날짜-01 형식으로 저장된다.
- 같은 날짜의 기존 파일 뒤에 다음 순번으로 저장된다.
- API 전체 테스트와 패키지 생성이 통과한다.

## 진행 상황

- 2026-08-22: 기존 UUID 기반 파일명과 단일 로그 저장 테스트를 확인했다.
- 2026-08-22: 로그 저장 날짜의 기존 파일을 조회해 최대 순번 다음 번호를 정하고, 파일명 산정과 저장을 동기화했다.

## 검증 결과

- `cd api && ./gradlew test --no-daemon --tests com.parkyc.poelens.ai.infrastructure.PromptLogWriterTest`: 2개 테스트 통과.
- `cd api && ./gradlew test --no-daemon`: 전체 테스트 통과.
- `cd api && ./gradlew bootJar --no-daemon`: 통과.

## 다음 행동

- 없음.

## 차단 요소

- 없음.
