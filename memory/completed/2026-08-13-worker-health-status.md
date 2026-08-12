# Worker 실제 상태 표시

## 목표

PoB inspect worker의 실행 상태를 웹 화면에서 실제 health endpoint 결과로 표시한다.

## 범위

- worker health endpoint 추가
- 웹 상태 표시를 5초 주기로 갱신
- worker 종료 확인

## 완료 기준

- worker 실행 시 화면에 준비 상태가 표시된다.
- worker 중단 또는 연결 실패 시 화면에 사용할 수 없음 상태가 표시된다.

## 진행 상황

- 2026-08-13: 8000 포트에 남아 있던 worker(PID 28656)를 사용자 요청으로 종료했다.
- 2026-08-13: worker에 `/v1/health` endpoint를 추가하고, 웹이 페이지 진입 시 및 5초마다 상태를 갱신하도록 변경했다.

## 검증 결과

- 8000 포트 닫힘 확인.
- `PYTHONPATH=. python3 -m unittest discover -s worker/tests -v`: 6개 통과.
- `cd web && npm test -- --run`: 15개 통과.
- `cd web && npm run build`: 통과.

## 다음 행동

- 없음

## 차단 요소

- 없음
