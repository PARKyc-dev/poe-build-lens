# PoB Worker

이 디렉터리에는 Path of Building headless 스파이크가 있습니다. 스파이크는 HTTP 서비스가 아니며, 운영 worker를 구현하기 전에 PoB 구성 조회·계산·상태 격리를 검증합니다.

시작 전 LuaJIT 2.1과 LuaFileSystem이 필요합니다.

```bash
worker/spike/scripts/check-runtime.sh
worker/spike/scripts/fetch-pob.sh
PYTHONPATH=worker/spike/tests python3 -m unittest discover -s worker/spike/tests -v
python3 worker/spike/scripts/benchmark.py --tests-passed --output worker/spike/reports/latest.json
python3 worker/spike/scripts/render_report.py worker/spike/reports/latest.json docs/pob-headless-spike-results.md
```

공식 PoB 소스와 생성 fixture는 `worker/spike/.cache/`에만 저장하며 Git으로 추적하지 않습니다.

## Local inspect endpoint

웹에서 PoB 구성 정보를 확인하기 위한 로컬 개발용 endpoint를 제공합니다. 원본 XML 또는 압축 PoB 내보내기 코드를 받아 XML로 복원한 뒤, `Spec`, `SkillSet`, `ItemSet`과 활성값만 반환합니다. 이는 운영 Spring API 경계가 아닙니다.

```bash
./worker/start-inspect.sh
```

다른 터미널에서 `cd web && npm run dev`를 실행하면 Vite가 `/worker` 요청을 이 endpoint로 전달합니다.
