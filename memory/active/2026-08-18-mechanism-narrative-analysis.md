# 메커니즘 중심 자연어 분석

## 목표

PoB 선택 주 스킬 대신 활성 피해 스킬별 합산 DPS 상위 2개를 공격 기재로 선정하고, 근거 기반의 공격·방어 설명을 로컬 상주 LLM으로 생성한다.

## 범위

- 브라우저 PoB 브리지의 스킬별 `CombinedDPS` 계산과 상위 2개 선정
- API의 근거 묶음, 단일 FIFO 생성 큐, 로컬 LLM HTTP 호출 및 규칙 기반 대체
- 공격·방어 설명과 근거 중심 화면

## 완료 기준

- mainSkill 선택과 관계없이 합산 DPS 상위 2개가 주력·보조 공격이다.
- LLM 응답이 근거 안의 정보만 사용하며 실패 시 규칙 문장으로 응답한다.
- API/웹 테스트와 빌드가 통과한다.

## 진행 상황

- PoB 통합 테스트를 먼저 추가했고, 활성 피해 스킬의 개별 `CombinedDPS` 순위로 상위 2개를 전달하도록 구현했다.
- 로컬 LLM 큐 및 서술 분석은 진행 중이다.

## 검증

- `web/src/pob/buildFacts.integration.test.ts -t 'ranks the two highest'` 통과
