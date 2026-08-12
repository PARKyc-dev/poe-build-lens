# PoB 중심 분석 계획 및 headless 스파이크

## 목표

PoB를 분석 기준 엔진으로 사용하는 전체 프로젝트 흐름을 문서화하고, 첫 단계인 PoB headless 스파이크를 구현·측정한다.

## 범위

- Web, API, worker, Lua adapter와 PoB 책임 설계
- 단일 구성 자동 분석과 복수 구성 선택 규칙 정의
- 공개 API와 내부 worker 계약 정의
- headless 스파이크 범위와 완료 조건 정의
- stock `HeadlessWrapper.lua`를 이용한 구성 조회·선택 계산·상태 격리·성능 측정

Spring·Web 변경은 제외했다. 스파이크는 `worker/spike` 아래의 검증 도구로만 구현했다.

## 완료 결과

- 전체 설계와 명세 사용자 승인
- 공식 PoB `v2.67.2`(`b32759ab0f31a1c8499a0d420cb0f0633d4fe478`) 고정 checkout 사용
- 실제 단일·복수 PoB export fixture 생성
- 외부 Lua adapter로 inspect·analyze와 선택값 오류 처리 구현
- A → B → A 상태 격리, 반복 startup·inspect·analyze·RSS 측정 완료
- 결과: `docs/pob-headless-spike-results.md`의 `GO`

## 검증 결과

- 전체 계약 테스트 13개 통과
- 공식 PoB checkout 무변경 상태 확인
- `git diff --check` 통과

## 다음 행동

- 단일 프로세스 FastAPI worker의 새 설계와 구현 계획 작성

## 차단 요소

- 없음
