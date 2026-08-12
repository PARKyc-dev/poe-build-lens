# 웹에서 PoB headless inspect 확인

## 목표

React 화면에 PoB XML을 붙여넣어 headless PoB worker가 추출한 `Spec`, `SkillSet`, `ItemSet`과 활성값을 확인한다.

## 구현 결과

- FastAPI inspect endpoint `POST /v1/builds/inspect` 추가
- worker가 장기 실행 PoB runner 하나를 소유하고 요청을 직렬화
- Vite `/worker` proxy와 React inspect 화면 추가
- 빈·잘못된 XML 및 worker 오류를 안정적인 HTTP/UI 오류로 표시
- 기존 Spring 분석 화면은 변경하지 않음

## 검증 결과

- worker HTTP 테스트 3개 통과
- React 테스트 12개 통과
- React production build 통과
- worker와 Vite proxy를 임시 실행해 실제 fixture의 Spec 3개, SkillSet 2개, ItemSet 2개와 활성값 3·2·2 확인
- 검증용 worker·Vite 프로세스 종료 및 `git diff --check` 통과

## 다음 행동

- 사용자 요청이 있을 때 Spring API를 포함한 운영 worker 통합을 설계

## 차단 요소

- 없음
