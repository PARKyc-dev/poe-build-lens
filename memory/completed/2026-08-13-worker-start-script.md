# Worker 시작 스크립트

## 목표

PoB inspect worker 실행에 필요한 두 명령을 `./worker/start-inspect.sh` 하나로 제공한다.

## 범위

- POSIX shell 실행 스크립트 추가
- worker 실행 안내 갱신

## 완료 기준

- 저장소 루트에서 단일 스크립트로 의존성 설치 후 Uvicorn을 8000 포트에서 실행한다.
- 셸 문법과 worker 애플리케이션 import를 검증한다.

## 진행 상황

- 2026-08-13: 사용자 승인 및 기존 worker 스크립트 형식을 확인했다.
- 2026-08-13: `worker/start-inspect.sh`를 추가하고 README의 실행 명령을 갱신했다.

## 검증 결과

- `sh -n worker/start-inspect.sh`: 통과.
- `PYTHONPATH=. python3 -m uvicorn worker.inspect_app:app --help`: 통과.
- `./worker/start-inspect.sh` 실행 후 `http://127.0.0.1:8000/openapi.json`: HTTP 200. 확인용 서버 종료.

## 다음 행동

- 없음

## 차단 요소

- 없음
