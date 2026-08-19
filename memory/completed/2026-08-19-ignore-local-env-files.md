# 로컬 환경 파일 Git 제외

## 목표

프로젝트 어느 위치의 실제 환경설정 파일도 Git에 추가되지 않게 한다.

## 범위

- `.env`와 변형 파일을 전체 프로젝트에서 무시한다.
- 안전한 공유 템플릿인 `.env.example`은 추적 가능하게 유지한다.

## 완료 기준

- 루트·API·웹 하위의 `.env` 파일이 무시된다.
- `api/.env.example`은 무시되지 않는다.

## 검증 결과

- `git check-ignore -v --no-index .env api/.env web/.env api/.env.local`: 모두 무시됨을 확인.
- `git check-ignore -q --no-index api/.env.example`: 무시되지 않음을 확인.
- `git diff --check`: 통과.
