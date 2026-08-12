# 압축 PoB 코드 inspect 입력 지원

## 목표

웹의 headless inspect 흐름이 원본 XML뿐 아니라 Path of Building 압축 내보내기 코드를 받아 구성 정보를 반환한다.

## 범위

- worker에서 Base64URL·zlib 압축 PoB 코드를 XML로 복원한다.
- 웹 입력 안내를 실제 지원 형식에 맞춘다.
- worker와 웹 테스트·빌드를 실행한다.

## 완료 기준

- 첨부된 형식의 압축 코드가 `PathOfBuilding` XML로 복원되어 inspect 결과를 반환한다.
- 원본 XML 입력도 유지된다.

## 진행 상황

- 2026-08-13: 첨부 코드(44,548자)를 복원해 314,991바이트 `PathOfBuilding` XML임을 확인했다. 현재 worker는 XML만 파싱해 복원 단계가 없다.
- 2026-08-13: 압축 코드 입력 재현 테스트가 기존 worker에서 HTTP 400으로 실패함을 확인했다.
- 2026-08-13: worker에 Base64URL·zlib 복원 단계를 추가하고, 웹 안내 문구를 코드 또는 XML 입력으로 변경했다.

## 검증 결과

- `PYTHONPATH=. python3 -m unittest discover -s worker/tests -v`: 4개 통과.
- 실제 첨부 코드를 endpoint에 전송: HTTP 200, 스펙 10개·스킬 세트 7개·아이템 세트 7개·활성 스펙 6번.
- `cd web && npm test -- --run`: 13개 통과.
- `cd web && npm run build`: 통과.
- `git diff --check`: 통과.

## 다음 행동

- 없음

## 차단 요소

- 없음
