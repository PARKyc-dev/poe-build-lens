# Architecture

## Browser PoB Analysis

PoB 내보내기 코드와 `pobb.in` 입력은 브라우저 `Web Worker`에서 원본 PoB `HeadlessWrapper.lua`로 처리한다. React는 계산 결과를 사용해 빌드 인사이트와 상세 화면을 렌더링한다.

PoB 원본 버전 고정, 가져오기, 캐시와 자산 생성 입력은 `web/pob/`에서 관리한다. 이 저장소가 소유하는 Lua/WebAssembly 호스트와 자산 패커를 사용하며, `pob.cool`은 이식 기법을 검토하는 참고 구현체일 뿐 제품 런타임·자산·업데이트 의존성에는 포함하지 않는다.

`worker/`는 향후 확장을 위한 예약 영역이다. 현재 브라우저 Web Worker와 혼동하지 않는다.
