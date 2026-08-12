# Codex 하네스 & PoB 프로젝트 설계 노트

> 조사·결정 정리 · 2026-08-12
> 하네스 부분은 **OpenAI Codex 기준**으로만 작성됨

---

# Part 1. 하네스 엔지니어링 (Codex)

## 1.1 폴더 관례

Codex가 실제로 읽는 경로는 아래가 전부다.

| 경로 | 용도 |
|---|---|
| `AGENTS.md` | 지침 정본. repo 루트 및 각 디렉터리 |
| `AGENTS.override.md` | 상위 지침을 **대체**(누적 아님). Codex 확장 |
| `~/.codex/AGENTS.md` | 머신 전역 개인 지침 |
| `.agents/skills/<name>/SKILL.md` | 스킬 (repo 공유) |
| `$HOME/.agents/skills` | 개인 스킬 |
| `/etc/codex/skills` | 조직 배포 스킬 (ADMIN) |
| `.codex/config.toml` | 프로젝트 설정 오버라이드 |
| `~/.codex/config.toml` | 개인 설정 |
| `~/.codex/prompts/*.md` | 커스텀 슬래시 커맨드 — **deprecated** |

`.codex/skills/` 라고 쓴 블로그가 많으나 **공식 경로는 `.agents/skills/`** 다.

`~/.codex/prompts/` 는 공식 폐기됐다. 로컬 홈에만 있어 repo 공유가 안 되고 명시적 호출만 가능하기 때문. 팀 자산은 전부 스킬로 간다.

## 1.2 AGENTS.md 로딩 규칙

세션 시작 시 **딱 한 번** 체인을 조립한다.

```
1. ~/.codex/AGENTS.override.md  (없으면 ~/.codex/AGENTS.md)
2. git root → CWD 방향으로 각 디렉터리마다
   AGENTS.override.md → AGENTS.md → project_doc_fallback_filenames
   순으로 확인, 디렉터리당 최대 1개
3. git repo가 아니면 현재 디렉터리만 확인
```

- 루트가 앞, 리프가 뒤로 concat → **가까운 파일이 우선**
- `project_doc_max_bytes` **기본 32 KiB**, 최대 65536. 한도에 닿으면 탐색 즉시 중단
- 빈 파일은 skip
- **glob / path-scoped 룰 미지원.** 분리 축은 디렉터리뿐

## 1.3 가장 중요한 함정 — CWD 의존

중첩 `AGENTS.md` 는 **"그 파일을 편집할 때"가 아니라 "그 폴더에서 실행했을 때"** 로드된다.

```
$ cd my-app && codex          → my-app/AGENTS.md 만 로드
  이 상태에서 web/page.tsx 를 고쳐도 web/AGENTS.md 는 안 읽힘

$ cd my-app/web && codex      → my-app/AGENTS.md + my-app/web/AGENTS.md
```

파일 읽기가 지침 로딩을 트리거하지 않는다. 지연 로딩 요청은 [openai/codex#17239](https://github.com/openai/codex/issues/17239) 에 미해결로 열려 있다.

**따라서 중첩 AGENTS.md는 다음 두 조건이 동시에 만족될 때만 값어치가 있다.**
1. 하나의 git repo 안에 규칙이 실제로 다른 영역이 여러 개
2. 팀이 그 하위 폴더로 `cd` 해서 작업

## 1.4 규칙을 어디에 둘 것인가

| 로드 조건 | 평소 토큰 | |
|---|---|---|
| 루트 `AGENTS.md` | 항상 | 소비 |
| 중첩 `AGENTS.md` | **실행 위치**가 그 폴더 이하일 때 | 소비 |
| 스킬 | **작업 내용**이 description과 맞을 때 | 0 |

| 규칙 성격 | 위치 |
|---|---|
| 항상, 어디서나 | 루트 `AGENTS.md` |
| 특정 패키지 + 팀이 그 안에서 작업 | 중첩 `AGENTS.md` |
| 특정 패키지 + 루트에서 작업 | 루트 `AGENTS.md` 에 "web/ 수정 시…" 문장으로 |
| 특정 작업 유형 (테스트, 마이그레이션) | 스킬 |
| 상위 규칙이 아예 안 맞는 레거시 | `AGENTS.override.md` |

작업 내용 기반으로 규칙이 붙는 유일한 메커니즘은 **스킬**이다. `.agents/skills` 는 CWD에서 repo 루트까지 전 층을 스캔하고 심볼릭 링크를 따라간다.

## 1.5 스킬 구조

```
my-skill/
├── SKILL.md              # 필수 (frontmatter: name, description)
├── scripts/              # 선택
├── references/           # 선택
├── assets/               # 선택
└── agents/openai.yaml    # 선택 — Codex 전용 확장
```

이름이 겹쳐도 병합하지 않고 셀렉터에 둘 다 노출된다.

## 1.6 작업 연속성 (memory)

세션 간 연속성은 **성격이 다른 3층**으로 나뉜다. Codex에는 auto-memory가 없으므로 **repo 안에 직접 만들어야 한다.**

조사한 사례들의 공통 원칙:

- **항상 로드되는 건 인덱스 한 장뿐.** 나머지는 on-demand
- **시간 티어링.** HOT(최근 원본) → WARM(날짜별) → COLD(월별 요약)
- **append-only 로그와 상태 스냅샷을 분리.** 한 파일에 섞으면 둘 다 망가짐
- **다음 액션을 명시.** 완료 내역만 적으면 다음 세션이 판단부터 다시 함
- **폴더 위치 = 상태.** `active/` ↔ `completed/` 이동이 상태 필드보다 잘 동작

권장 구조:

```
docs/
├── STATE.md                 # 현재 작업 스냅샷 (덮어씀, 30줄 이내)
├── journal/
│   ├── work.log             # append-only
│   └── 2026-08.md           # 월별 다이제스트
├── plans/{active,completed}/
└── decisions/               # ADR
```

**폴더만 만들면 에이전트가 안 읽는다.** 읽고 쓰는 프로토콜을 `AGENTS.md` 에 명시하는 것이 폴더 이름보다 결정적이다.

## 1.7 루트 AGENTS.md 예시

```markdown
# AGENTS.md

poe-lens. PoB 빌드 분석 웹앱.
React(`web/`) + Spring Boot(`backend/`) + Python worker(`worker/`) 모노레포.
셀프호스팅 배포물 하나. `docker compose up` 으로 전부 뜬다.

## 세션 시작 시
1. `docs/STATE.md` 를 읽는다.
2. `docs/plans/active/` 에 파일이 있으면 그것부터 읽는다.
3. 작업 종료 시 `docs/STATE.md` 를 갱신하고
   `docs/journal/work.log` 에 한 줄 append 한다.

## 절대 금지
- `main` 에 직접 push 금지. 항상 `feature/*` 브랜치.
- `.env*` 를 읽거나 출력하지 않는다. `.env.example` 만 수정한다.
- PoB 체크아웃 버전을 임의로 올리지 않는다. 태그 고정이며,
  올리면 계산 결과가 바뀐다. 변경은 반드시 물어본다.
- `docker-compose.yml` 의 포트 매핑을 바꾸지 않는다.
- 라이브러리를 새로 추가하기 전에 먼저 물어본다.

## 공통 규칙
- 커밋 메시지는 Conventional Commits. 한글 본문 허용.
- API 스키마를 바꾸면 `web/` 의 타입도 같은 커밋에서 고친다.
- 새 환경변수를 추가하면 `.env.example` 과 README 에 함께 반영한다.
- 사용자 입력(PoB 코드)을 파싱하는 코드는 XXE·압축폭탄·SSRF 방어를
  반드시 유지한다. 자세한 내용은 `pob-parsing` 스킬 참고.
- 주석은 "무엇"이 아니라 "왜"를 쓴다.

## 명령어
| 목적 | 명령 |
|---|---|
| 백엔드 빌드·테스트 | `cd backend && ./gradlew build` |
| 전체 실행 | `docker compose up --build` |
| 프론트 개발 | `cd web && npm run dev` |

## 영역별 규칙
작업 영역에 따라 해당 폴더로 이동한 뒤 Codex 를 실행하거나 `--cd` 를 쓴다.
- `backend/AGENTS.md` — Spring Boot
- `web/AGENTS.md` — React
- `worker/AGENTS.md` — Python (PoB 프로세스 관리)
루트에서 작업하며 위 영역을 건드릴 때는 수정 전에 해당 파일을 직접 읽는다.

## 상세 규칙 (스킬)
- PoB 코드 파싱 → `pob-parsing`
- PoB 워커·프로세스 관리 → `pob-worker`
- 테스트 작성 → `testing-convention`
```

혼자 루트에서 작업한다면 위 "영역별 규칙"의 중첩 파일들은 만들지 말고
섹션으로만 나눠 이 파일 하나에 두는 편이 낫다 (1.3 CWD 함정 참고).

**넣을 것 / 뺄 것**

| 넣는다 | 뺀다 |
|---|---|
| 브랜치·커밋·PR 규칙 | 디렉터리 트리 나열 |
| 절대 금지 사항 | 의존성 목록 |
| repo 전체 빌드/테스트 명령 | 스택별 상세 → 하위 or 스킬 |
| 보안·데이터 취급 | 다단계 절차 → 스킬 |
| 하위 AGENTS.md 위치 안내 | 진행 상황 → `docs/STATE.md` |
| 세션 프로토콜 | 일반적 좋은 코딩 습관 |

판단 기준: **"이 줄을 지우면 에이전트가 하지 않았을 실수를 하는가?"** 아니면 뺀다.

## 1.8 3분할 원칙

```
규칙(AGENTS.md)  /  절차(skills)  /  상태(docs)
```

규칙 파일에 절차나 진행상황이 섞이는 것이 가장 흔한 실패 패턴이다. 32 KiB 예산을 잡아먹고, 매 세션 로드되는 자리에 어제 한 일이 들어앉는다.

---

# Part 2. PoE PoB 프로젝트

## 2.1 프로젝트 전제

- 프론트: **React**
- 백엔드: **Spring Boot**
- 나중에 **Python worker** 추가 예정
- 개인 VM에 셀프호스팅, 웹으로 접속
- **GitHub 공개 공유** 예정
- 대상: **PoE 1 전용**

## 2.2 결정 — repo는 1개

| | 판단 |
|---|---|
| 결론 | **모노레포 1개** |
| 이유 | 배포물이 하나. `git clone` → `docker compose up` 으로 끝나야 함 |

repo 2개면 사용자가 clone 2번, 버전 호환 판단, `.env` 2벌, 포트 2개, CORS 설정을 떠안는다. 더 나쁜 건 **"api를 먼저 띄워라" 같은 설명을 쓸 자리가 없어진다**는 점이다. `AGENTS.md` 도 마찬가지로 "API 스키마 바꾸면 프론트 타입도 고쳐라" 규칙을 둘 곳이 사라진다.

나누는 게 맞는 경우는 ① 백엔드를 여러 클라이언트가 씀 ② 배포 주기가 다름 ③ 관리 주체가 다름 — 셋 다 해당 없음.

mono → multi 분리는 쉽고 반대는 귀찮다. 모노로 시작한다.

```
my-app/
├── README.md            # 5줄 안에 실행되게
├── AGENTS.md            # 루트 하나. 중첩 만들지 않음(혼자 루트에서 작업)
├── docker-compose.yml   # 이거 하나로 전부 뜨게
├── .env.example
├── web/                 # React
├── api/                 # Spring Boot
└── docs/STATE.md
```

리버스 프록시를 넣어 **포트 하나로** 접속되게 할 것. VM 방화벽 포트 2개 여는 것이 실제 이탈 지점이 된다.

## 2.3 PoB 코드 포맷

```
공유 코드 → URL-safe base64 → zlib inflate → XML
```

## 2.4 기존 라이브러리 조사 결과

| 언어 | 라이브러리 | 상태 |
|---|---|---|
| **Java** | **없음** | Maven Central에 PoB 파서 없음 |
| JS/TS | `pob-parser` | v1.0.2 (2026-06), 의존성 0, pobb.in URL 처리. **XML 문자열까지만** |
| Rust | `pob-parser` | 동일 |
| Python | `pobapi` | v0.6.0, **2021년 이후 정지**. 객체까지 만들어주나 최신 리그 대응 안 됨 |
| Python | `pob_wrapper` | PoB 본체를 Python에서 제어 |

→ Spring Boot에서는 직접 구현하거나 PoB 본체를 쓰는 수밖에 없다.

## 2.5 PoB XML 스키마 (소스에서 검증)

> **2.5 ~ 2.8 은 대비책이다.** 2.9 에서 PoB 엔진 사용으로 결정했으므로 지금은 쓰지 않는다.
> `import+calc` 가 느려 하이브리드로 가야 할 때(요약 화면만 직접 파싱) 이 절들을 쓴다.

```xml
<PathOfBuilding>
  <Build level className ascendClassName bandit mainSocketGroup>
    <PlayerStat stat value/>
  </Build>

  <Skills activeSkillSet sortGemsByDPS defaultGemLevel defaultGemQuality>
    <SkillSet id title>
      <Skill enabled label slot source mainActiveSkill includeInFullDPS groupCount>
        <Gem nameSpec skillId gemId variantId level quality enabled count
             skillPart skillMinion skillMinionSkill .../>
      </Skill>
    </SkillSet>
  </Skills>

  <Tree activeSpec>
    <Spec title treeVersion classId ascendClassId secondaryAscendClassId
          nodes masteryEffects clusterHashFormatVersion>
      <URL>...</URL>
      <Sockets><Socket nodeId itemId/></Sockets>
      <Overrides><Override nodeId icon dn/></Overrides>
    </Spec>
  </Tree>

  <Items activeItemSet useSecondWeaponSet>
    <Item id variant variantAlt ModRange>평문 텍스트</Item>
    <ItemSet id title useSecondWeaponSet>
      <Slot name itemId active itemPbURL/>
    </ItemSet>
  </Items>

  <Config><Input name boolean number string/></Config>
  <Notes>...</Notes>
</PathOfBuilding>
```

**주의사항**

- `<Skill>` 이 구버전은 `<Skills>` 직속, 신버전은 `<SkillSet>` 안. **둘 다 처리해야** 옛 코드가 안 깨짐
- `activeSpec` / `activeItemSet` / `activeSkillSet` 은 **1-based**
- `nodes` = 노드 ID 콤마 구분
- `masteryEffects` = `{노드ID,이펙트ID},{...}`
- `<Item>` 안은 **XML이 아니라 게임 복사 평문**
- 슬롯명: `Weapon 1/2`, `Weapon 1/2 Swap`, `Helmet`, `Body Armour`, `Gloves`, `Boots`, `Belt`, `Amulet`, `Ring 1/2/3`, `Flask 1`~`5`, `... Abyssal Socket N`. 주얼은 `Spec/Sockets/Socket` 에 별도
- `getElementsByTagName` 대신 **직계 자식만** 순회할 것 (SkillSet 여러 개일 때 섞임)

## 2.6 아이템 평문 포맷

```
Rarity: RARE
Corpse Coil          ← 이름
Vaal Regalia         ← 베이스
Unique ID: ...
Item Level: 84
Quality: 20
Sockets: B-B-B B-B-B
LevelReq: 68
Implicits: 1         ← 이 숫자만큼이 implicit, 나머지가 explicit
{tags:defences}+15% to maximum Energy Shield
{range:0.5}+(80-100) to maximum Energy Shield
{crafted}+20 to Intelligence
```

인라인 토큰: `{variant:1,2}`, `{version:1}`, `{group:1}`, `{range:0.5}`, `{tags:...}`, `{crafted}`, `{fractured}`, `{implicit}`

**`{variant:...}` 필터링은 필수.** 유니크는 한 `<Item>` 안에 모든 변형 모드가 다 들어있고 `variant` 속성이 가리키는 것만 유효하다. 빼먹으면 Watcher's Eye 같은 아이템에 없는 옵션이 표시된다.

`{range:x}` 는 `(a-b)` 를 `a + (b-a)*x` 로 계산해 표시한다.

## 2.7 보안 (직접 파싱 시)

사용자 입력을 XML 파싱하므로 필수.

- **XXE 차단**: `disallow-doctype-decl=true`, external entities 전부 false
- **압축 폭탄 방어**: inflate 출력 바이트 상한 (예: 8MB)
- **SSRF 방어**: pobb.in URL에서 ID 추출 시 `[A-Za-z0-9_-]{1,64}` 정규식 검증

## 2.8 CORS — 디코딩은 반드시 백엔드에서

- **pobb.in은 CORS를 열어주지 않는다.** 브라우저 fetch 차단됨 → 백엔드 프록시 필수
- 브라우저엔 zlib이 없어 `pako` 를 추가 번들해야 함

→ Spring Boot가 pobb.in을 대신 호출하고 디코딩까지 끝내 JSON을 내려준다.

## 2.9 핵심 결정 — 직접 파싱 vs PoB 본체 사용

직접 파싱은 **"빌드에 뭐가 들어있는지"** 만 알려준다. 사용자가 보고 싶은 건 대부분 **계산 결과**(DPS, EHP, 저항)이고, 그건 PoB 계산 엔진 없이 재현이 사실상 불가능하다.

### PoB를 엔진으로 쓰는 3가지 방법

| 방법 | 내용 | 상태 |
|---|---|---|
| **HeadlessWrapper.lua** | PoB 내장. GUI 없이 계산 엔진만 구동. LuaJIT 필요 | **가장 검증됨.** 디스코드 봇들이 오래 사용 |
| **Headless JSON-RPC** | `--stdio` 로 JSON-RPC 서버. stats/tree/items/what-if 전부 노출 | PR #9505 **미머지**. 참조 구현으로만 |
| **pob-web (WASM)** | PoB Lua 전체를 Emscripten으로 WASM화. 브라우저 클라이언트 실행 | `atty303/pob-web`, pob.cool 이 실물 |

### 비교

| | 직접 파싱 | Headless PoB | pob-web |
|---|---|---|---|
| 아이템/스킬/트리 파싱 | 직접 구현 | **공짜** | 공짜 |
| DPS·방어 계산 | **사실상 불가** | 정확 | 정확 |
| 트리 노드 이름 | 별도 데이터 필요 | **공짜** | 공짜 |
| 리그 업데이트 대응 | 매번 수작업 | `git pull` | 상류 따라감 |
| 런타임 의존성 | 없음 | LuaJIT + PoB 체크아웃 | 없음(브라우저) |
| 응답 속도 | ms | 워밍업 후 빠름(미측정) | 클라이언트 부담 |

### 결정: Headless PoB 우선

**가장 큰 이유는 트리 데이터다.**

`nodes` 는 숫자 ID 목록일 뿐이라 "Elemental Overload를 찍었다"를 알려면 노드 ID → 이름 매핑이 필요하다. PoB는 `src/TreeData/<버전>/tree.lua` 로 갖고 있는데 **JSON이 아니라 Lua 테이블**이라 Java에서 직접 못 읽는다. 게다가 `treeVersion` 이 빌드마다 달라 **버전별로 여러 벌** 필요하다.

PoB 본체를 쓰면 이 작업이 통째로 사라진다. 리그가 바뀌면 `git pull` 로 끝.

## 2.10 성능 — 미측정, 직접 재야 함

공개된 헤드리스 벤치마크는 **없다.** 간접 근거만 있다.

| | 예상 | 근거 |
|---|---|---|
| 프로세스 기동 + 데이터 로딩 | 초 단위 | 데이터 볼륨, pob-mcp 기본 타임아웃 10초 |
| 워밍업 후 빌드 1건 계산 | 수십~수백ms | GUI가 노드 클릭마다 전체 재계산하며 인터랙티브 유지 |

**"느리다"의 실체는 계산이 아니라 기동이다.** 워커를 상주시키면 대부분 사라진다.

### 측정 방법 (5분)

```bash
git clone --depth 1 https://github.com/PathOfBuildingCommunity/PathOfBuilding
cd PathOfBuilding/src
apt install luajit   # 또는 brew install luajit
```

```lua
-- bench.lua (src/ 에 두고 실행)
local t0 = os.clock()
dofile("HeadlessWrapper.lua")
print(("init: %.2fs"):format(os.clock() - t0))

local t1 = os.clock()
build:ImportFromCode(code)   -- 실제 API 이름은 HeadlessWrapper 확인 필요
print(("import+calc: %.3fs"):format(os.clock() - t1))
```

`init` 과 `import+calc` 두 숫자로 설계 판단이 끝난다. `import+calc` 가 100ms 안쪽이면 고민할 게 없다.

## 2.11 실제로 걸리는 제약 (속도 아님)

- **메모리** — PoB 인스턴스 하나가 데이터를 통째로 올린다. 프로세스당 수백MB 예상. VM 사양 확인 필요. 워커 4개면 GB 단위
- **셀프호스팅 진입장벽** — GitHub 공유 대상자가 LuaJIT 설치 + PoB clone을 해야 하면 이탈한다. **PoB 포함 Docker 이미지 필수**
- **버전 고정** — `dev` 브랜치 추적 금지. 태그 체크아웃 후 의도적으로 올릴 것. 안 그러면 어제와 다른 DPS가 나옴
- **stdio 브릿지 유지보수** — JSON-RPC PR이 미머지라 브릿지를 직접 들고 있어야 함. pob-mcp처럼 **stock 체크아웃 + 패치 없음** 방식을 따를 것
- **라이선스** — PoB는 MIT. 배포 형태(번들 vs 런타임 clone) 결정하고 저작자 표기

## 2.12 목표 아키텍처

```
[React] → [Spring Boot: 캐싱·정규화·API] → [PoB 워커 (상주, Docker)]
```

```yaml
# docker-compose.yml
services:
  web:            # React
  api:            # Spring Boot
  pob:            # LuaJIT + PoB checkout + 얇은 HTTP/stdio 래퍼
```

- 워커 **상주**. 요청마다 기동하지 않음
- 결과는 **PoB 코드 해시를 키로 캐싱**. 빌드 코드는 불변이라 적중률이 매우 높음
- 워커 크래시 대비 재시작, 요청 타임아웃 10초
- PoB 인스턴스 하나는 빌드 하나만 다룸 → 워커 풀 또는 큐 직렬화

**Python worker의 실제 자리**: Lua 프로세스 관리·타임아웃·재시작은 Python이 Java보다 편하다. `subprocess` + FastAPI 로 충분. 디코딩 자체를 Python으로 뺄 이유는 없다.

## 2.13 게임 데이터 — 별도 보관 불필요 (결론)

한때 "PoE 데이터를 VM에 버전별로 보관" 을 검토했으나 **전부 불필요**로 결론.

### 이유 1 — PoB 체크아웃이 곧 데이터다

```
src/Data/          젬·스킬·유니크·베이스·모드 전체
                   (explicit/implicit/flask/jewel/map/eldritch/delve/
                    crafting/synthesis/essence/enchant/cluster/pantheon/
                    tattoo/minion/spectre...), 통화, 글로벌 스탯
src/TreeData/      버전별 패시브 트리 (2_6, 3_6 ~ 3_29, ruthless/alternate 변형)
```

전부 Lua 포맷(일부 JSONC). Java가 직접 못 읽지만, **PoB 엔진을 통해 쓸 거라 상관없다.**

### 이유 2 — 버전별 체크아웃조차 불필요할 가능성이 높다

- `src/TreeData/` 는 **전 버전이 한 체크아웃에 다 있다** → 옛 `treeVersion` 빌드도 현재 체크아웃 하나로 처리
- `src/Data/` 는 **버전 구분이 없다** → PoB는 어떤 빌드든 항상 현재 리그 데이터로 계산

즉 "3.25 당시 수치 재현" 은 **PoB로도 원래 안 된다.** 실제 PoB에서 옛 빌드를 임포트해도 현재 수치가 나오는 것과 같다. 그걸 포기하면 체크아웃 한 벌로 끝.

```
/opt/pob        ← 태그 하나 고정. 이게 전부
```

### 조사한 외부 데이터 소스 (현재로선 불필요, 기록용)

**패시브 트리**

| 소스 | 버전별 | 비고 |
|---|---|---|
| `poe-tool-dev/passive-skill-tree-json` | ✅ 폴더 = 버전 | 0.9.6~3.25.0, `-atlas`/`-ruthless` 변형. raw URL로 직접 접근 |
| `grindinggear/skilltree-export` | ❌ master 덮어씀 | **GGG 공식**. 과거는 커밋 히스토리 |

**게임 데이터 (젬·모드·베이스)**

| 소스 | 상태 | 버전별 |
|---|---|---|
| `repoe-fork/repoe` | 활발 (2026-05) | ❌ |
| `SnosMe/poe-dat-viewer` | 활발, 사실상 표준 | 직접 추출 |
| `jchantrell/exiledb` | 활발. SQLite/JSON CLI | 직접 추출 |
| `erosson/poedat` | **중단** | 6개월 보관했었음 |
| `erosson/pypoe-json` | **아카이브(2021)** | — |

**버전별 아카이브를 제공하는 곳이 사실상 없다.** 직접 추출 계열은 게임 설치본(GGPK/Steam depot)이 필요하다.

### 다시 필요해지는 유일한 조건

**패시브 트리를 화면에 그림으로 렌더링할 때** — 노드 좌표·그룹 배치·스프라이트가 필요하며 `poe-tool-dev/passive-skill-tree-json` 을 쓴다. 단 이건 **프론트 자산**이지 VM 데이터 저장소가 아니다.

→ 현재 계획은 트리 렌더링을 하지 않으므로 **해당 없음.**

## 2.14 핵심 노드 분석 (주요 기능)

### 목표

트리 전체를 보여주는 대신, **분석 후 "이 노드가 핵심이다" 를 골라서 제시**한다.

### 방법 — what-if delta

```
노드 기여도 = (현재 스탯) − (그 노드를 뺐을 때 스탯)
```

**게임 데이터를 아무리 갖고 있어도 계산 불가. PoB 엔진만 할 수 있다.**
→ PoB로 가기로 한 결정이 이 기능에서 특히 잘 맞는다.

PR #9505 의 `calc_with` (what-if) 가 정확히 이 패턴:
```
원래 상태 저장 → 수정 적용 → 스탯 계산 → 원상 복구
```

### PoB 내장 Node Power는 쓰지 말 것

- Full DPS 기준 Node Power가 **말이 안 되는 값**을 낸다는 이슈 (#4982)
- 캐릭터가 아니라 **미니언 기준**으로 계산되는 경우 (#4522)
- 애초에 "**안 찍은 노드를 찍으면 얼마나 이득인가**" 방향 → 우리가 원하는 것과 **반대**

→ delta를 직접 돌린다.

### 반드시 걸리는 문제 — 연결성

노드를 하나 빼면 **트리가 끊어진다.** 패시브 트리는 시작점부터 연결돼야 하고, 중간 경로 노드를 빼면 그 뒤가 통째로 해제된다. 단순 "한 개씩 빼보기" 는 성립하지 않는다.

**대응 3가지**

1. **대상을 노터블·키스톤·마스터리로 한정**
   경로용 잡템 노드는 애초에 "핵심" 이 아니다. 노터블은 대개 경로 끝단이라 연결이 잘 안 끊긴다.
   대상 120개 → 30~40개. 성능 문제도 같이 해결.

2. **끊어지면 그 결과는 버린다**
   해제된 노드 수를 함께 받아, 1개(자기 자신)가 아니면 제외하거나 별도 표시.

3. **키스톤은 특별 취급**
   페널티가 있어 **델타가 음수로 나올 수 있다.** "빼면 DPS가 오르는데 왜 찍었나" 같은 인사이트가 여기서 나온다. 버리지 말 것.

### 비용

노터블 40개 → 재계산 40회. **한 빌드 분석 시간 = `import+calc` 실측값 × 40.**

| `import+calc` | 40회 | 판단 |
|---|---|---|
| 50ms | 2초 | 동기 처리 가능 |
| 300ms | 12초 | 비동기 + 진행률 표시 필요 |

**벤치 숫자가 여기서 실제로 설계를 가른다.** (Part 3 #1)

캐싱 필수. 같은 빌드 코드면 결과가 완전히 동일 → 코드 해시 캐싱으로 2회차부터 즉시 응답.

### 파이프라인

```
PoB 코드
  → PoB 임포트 + 기준 스탯 계산
  → 노터블/키스톤/마스터리 목록 추출
  → 각각 해제 후 재계산 (연결성 깨진 건 제외)
  → 델타 정렬 → 상위 N개 = "핵심 노드"
  → 노드 이름·설명도 PoB에서 함께 반환
  → 결과 캐싱
```

노드 이름·설명을 PoB가 갖고 있으므로 **외부 트리 데이터가 필요한 유일한 이유(노드 ID→이름 매핑)조차 사라진다.**

### 배치

- **Python worker** — 분석 로직. 반복 호출·타임아웃·부분 실패 처리가 필요하고 Java보다 다루기 편하다
- **Spring Boot** — 결과 캐싱과 API만

---

# Part 3. 다음 액션

## 현재 repo 상태 (2026-08-12)

```
D:\개인폴더\poe-lens\
├── .claude/          (Codex로 간다면 정리 대상)
├── .git/
├── backend/          Gradle Kotlin DSL — Spring Boot 골격 있음
├── builds/           비어 있음
├── docs/             비어 있음
├── .gitignore
├── CLAUDE.md         7,882 bytes — AGENTS.md 로 이전 필요
└── README.md
```

- **`AGENTS.md` 없음.** Codex는 `CLAUDE.md` 를 읽지 않는다 → 이전 필요
- **`docs/` 비어 있음** → 연속성 구조(1.6) 넣을 자리 준비됨
- **폴더명 미확정** — 기존이 `backend/` 인데 논의는 `api` 였음. `web`/`backend`/`worker` 로 갈지 `frontend`/`backend`/`worker` 로 갈지 결정 필요

## 액션 목록

| # | 항목 | 상태 |
|---|---|---|
| 1 | `bench.lua` 로 init / import+calc 실측 | **미완 — 여기가 병목** |
| 2 | 실측 결과로 워커 수·큐·동기여부 결정 (2.14 비용표) | #1 대기 |
| 3 | 폴더명 확정 (`web`/`backend`/`worker`?) | 대기 |
| 4 | `CLAUDE.md` → `AGENTS.md` 이전 (1.7 예시 기반) | 대기 |
| 5 | `docs/STATE.md` + `docs/plans/` 생성 | 대기 |
| 6 | PoB 태그 고정 및 리그↔태그 매핑표 작성 | 대기 |
| 7 | PoB 포함 Docker 이미지 | 대기 |
| 8 | stdio 브릿지 (pob-mcp 방식 참조, stock 체크아웃) | 대기 |
| 9 | 핵심 노드 delta 분석 (Python worker) | #1,#8 대기 |
| 10 | Spring Boot 캐싱 레이어 (코드 해시 키) | 대기 |

## 병목 — 이것부터 풀어야 나머지가 진행된다

```
[B1] bench 실측 ──┬──> 동기 API vs 작업 큐 결정
                  ├──> 하이브리드(2.5~2.8) 채택 여부
                  └──> 핵심 노드 분석 UX (즉시 응답 vs 진행률)
                          ↑
[B2] VM RAM 확인 ─────────┴──> 워커 개수 / 풀 vs 큐 직렬화

[B3] 폴더명 확정 ──> AGENTS.md 작성 ──> 하네스 세팅 전체
```

| ID | 병목 | 막고 있는 것 | 해소 방법 |
|---|---|---|---|
| **B1** | `import+calc` 실측값 없음 | 액션 #2, #9, 아키텍처 전반 | `bench.lua` 실행 (2.10) — 5분 |
| **B2** | VM RAM 사양 미확인 | 워커 수, 동시성 설계 | `free -h` 확인 — 1분 |
| **B3** | 폴더명 미확정 | `AGENTS.md` 작성, Docker 구성 | 결정만 하면 됨 (D1) |

B1, B2 는 **각각 5분 이내에 끝나는데 그 뒤 작업 전부를 막고 있다.** 우선순위 최상.

## 결정 대기 항목

### D1. 폴더명

| 안 | 형태 | 비고 |
|---|---|---|
| A | `web` / `backend` / `worker` | 기존 `backend/` 유지. rename 없음 |
| B | `frontend` / `backend` / `worker` | 가장 흔한 관례. 대칭적 |
| C | `web` / `api` / `worker` | 짧음. `backend/` rename 필요 |

**권장: A.** 이미 Gradle 프로젝트가 `backend/` 에 있어 rename 비용만 생긴다.

### D2. PoB 배포 형태

| 안 | 장점 | 단점 |
|---|---|---|
| Docker 이미지에 **번들** | 사용자가 `docker compose up` 만 하면 됨 | 이미지 크기 증가, 라이선스 표기 필수 |
| 빌드 시 **런타임 clone** | 이미지 가벼움 | 첫 실행이 느리고 네트워크 필요 |

**권장: 번들.** 셀프호스팅 진입장벽이 이 프로젝트의 핵심 리스크(2.11). MIT 라이선스 + 저작자 표기만 지키면 된다.

### D3. 분석 API 동기 vs 비동기

B1 결과에 종속.

| `import+calc` | 형태 |
|---|---|
| ~50ms (40회 = 2초) | 동기 REST. 단순 |
| ~300ms (40회 = 12초) | 작업 큐 + 폴링/SSE 진행률 |

**중간이면 동기 + 타임아웃 늘리기로 시작하고, 느려지면 큐로 옮긴다.** 처음부터 큐를 만들면 과설계.

### D4. 캐시 저장소

| 안 | 비고 |
|---|---|
| 인메모리 (Caffeine) | 가장 단순. **재시작 시 소멸** |
| Redis | 컨테이너 하나 추가 |
| DB 테이블 | 이미 DB를 쓴다면 자연스러움 |

분석 1건이 수 초 걸린다면 재시작마다 날리는 건 아깝다. **DB에 두는 쪽을 권장** — 컨테이너를 늘리지 않으면서 영속된다.

> ⚠️ **캐시 키에 반드시 PoB 버전을 포함할 것.** PoB를 올리면 같은 빌드 코드라도 계산 결과가 달라진다. 키가 코드 해시뿐이면 옛 결과가 계속 나온다.
> `key = hash(pobCode) + ":" + pobVersion`

### D5. 핵심 노드 판정 기준

- 어떤 스탯의 delta로 순위를 매기나? **DPS만? EHP도? 둘 다 보여주고 사용자가 고르게?**
- 상위 몇 개를 "핵심" 으로 보여주나? (N 고정 vs 델타 임계값)
- 클러스터 주얼 노드도 대상에 넣나?
- 마스터리 이펙트(`masteryEffects`)도 delta 대상인가?

**권장: DPS·EHP 둘 다 계산해 두 목록으로 제시.** 빌드마다 중요한 축이 달라 하나로 줄이면 오해를 부른다. 클러스터·마스터리는 2차로 미룬다.

### D6. `builds/` 폴더 용도

현재 비어 있음. 빌드 코드를 파일로 쌓을 것인지, DB에 넣을 것인지 미정. 공유 기능(다른 사람 빌드 열람)이 있을 거면 DB가 맞다.

### D7. 사용자·인증 개념

개인용으로 시작하지만 GitHub 공유 대상자가 각자 셀프호스팅한다. **각 인스턴스가 단일 사용자인가, 멀티유저인가.** 단일 사용자면 인증을 아예 안 넣어도 되고, 이게 스키마 전반에 영향을 준다.

**권장: 단일 사용자 전제로 시작.** 셀프호스팅 도구는 대개 그렇고, 나중에 붙이는 게 처음부터 넣는 것보다 싸다.

### D8. 입력 경로 범위

pobb.in URL / raw 코드 / pastebin URL 중 어디까지 지원하나. pastebin은 차단·레이트리밋 이슈가 있다.

**권장: pobb.in + raw 코드로 시작.**

### D9. `.claude/` 처리

Codex 기준으로 간다면 `.claude/` 는 용도가 사라진다. 삭제할지, Claude Code도 병행할지 결정 필요. 병행한다면 `CLAUDE.md` 는 `@AGENTS.md` 한 줄 포인터로 두면 중복이 없다.

## 리스크 — 나중에 물릴 수 있는 것

| 리스크 | 영향 | 대응 |
|---|---|---|
| **캐시 무효화 누락** | PoB 업글 후 옛 결과가 계속 나옴. **조용히 틀린다** | 캐시 키에 PoB 버전 포함 (D4) |
| **PR #9505 미머지** | 브릿지를 직접 유지보수해야 함 | stock 체크아웃 + 패치 없음 방식 고수. 머지되면 갈아타기 |
| **PoB 내부 구조 변경** | 브릿지가 깨짐 | 태그 고정. 올릴 때마다 회귀 테스트 |
| **PoB 프로세스 행/크래시** | 요청이 영영 안 끝남 | 요청 타임아웃 10초 + 워커 재시작 |
| **장시간 상주 메모리 누수** | VM 메모리 고갈 | N회 처리 후 워커 재활용 |
| **동시 사용자 → 워커 경합** | 응답 지연 | 큐 직렬화 또는 풀. B2 이후 결정 |
| **pobb.in 과다 호출** | 차단 가능. 민폐 | 캐싱 필수. 재시도 백오프 |
| **PoB 태그↔리그 매핑 부재** | 어느 태그가 어느 리그인지 매번 찾게 됨 | 릴리스 노트 보고 표 작성, `docs/` 에 보관 |
| **라이선스 표기 누락** | 공개 배포 시 문제 | PoB MIT 저작자 표기, README 명시 |
| **노드 delta 연결성 오판** | 잘못된 "핵심 노드" 제시 | 해제 노드 수 검증 (2.14) |

## 관측 안 된 가정 (검증 필요)

노트 전반에서 **추정으로 쓴 것들.** 사실로 굳히기 전에 확인할 것.

| 가정 | 근거 수준 | 확인 방법 |
|---|---|---|
| 워밍업 후 계산이 수십~수백ms | **추정** — GUI가 실시간 재계산으로 동작한다는 간접 근거뿐 | B1 벤치 |
| PoB 프로세스당 수백MB | **추정** | 실행 후 `ps` 확인 |
| 노터블 제거 시 연결이 잘 안 끊김 | **추정** — 노터블이 경로 끝단이라는 일반론 | 실제 빌드로 시험 |
| PoB가 항상 현재 리그 데이터로 계산 | **강한 추정** — `src/Data/` 에 버전 구분이 없다는 구조적 근거 | 옛 빌드 임포트해 비교 |
| `build:ImportFromCode` API 이름 | **미확인** | `HeadlessWrapper.lua` 직접 확인 |

## 확정된 결정

| 결정 | 근거 |
|---|---|
| repo 1개 (모노레포) | 배포물 하나. `docker compose up` 으로 끝나야 함 (2.2) |
| PoB 엔진 사용 (직접 파싱 아님) | 트리 데이터 관리 소멸 + DPS 계산은 재현 불가 (2.9) |
| 외부 게임 데이터 보관 안 함 | PoB 체크아웃이 곧 데이터 (2.13) |
| PoB 체크아웃 1벌 (버전별 아님) | TreeData는 전 버전 포함, Data는 버전 구분 없음 (2.13) |
| 트리 렌더링 안 함 | 핵심 노드만 선별 제시 (2.14) |
| 중첩 `AGENTS.md` 만들지 않음 | 혼자 루트에서 작업 → CWD 함정 (1.3) |

---

# 참고 링크

**Codex 하네스**
- [Custom instructions with AGENTS.md](https://learn.chatgpt.com/docs/agent-configuration/agents-md)
- [Build skills](https://learn.chatgpt.com/docs/build-skills)
- [Custom Prompts (deprecated)](https://learn.chatgpt.com/docs/custom-prompts)
- [Config basics](https://developers.openai.com/codex/config-basic)
- [openai/codex#17239 — path-aware dynamic rule loading](https://github.com/openai/codex/issues/17239)
- [openai/codex#13288 — 상위 디렉터리 로딩 이슈](https://github.com/openai/codex/issues/13288)

**연속성 참고 사례**
- [daystar7777/agent-work-mem](https://github.com/daystar7777/agent-work-mem) — AIMemory HOT/WARM/COLD
- [hoangnb24/repository-harness](https://github.com/hoangnb24/repository-harness) — `docs/plans/{active,completed}`
- [akitaonrails/ai-memory](https://github.com/akitaonrails/ai-memory)

**PoB**
- [PathOfBuildingCommunity/PathOfBuilding](https://github.com/PathOfBuildingCommunity/PathOfBuilding)
- [HeadlessWrapper.lua](https://github.com/Openarl/PathOfBuilding/blob/master/HeadlessWrapper.lua)
- [PR #9505 — headless JSON-RPC (미머지)](https://github.com/PathOfBuildingCommunity/PathOfBuilding/pull/9505)
- [ianderse/pob-mcp](https://github.com/ianderse/pob-mcp)
- [atty303/pob-web](https://github.com/atty303/pob-web) / [pob.cool](https://pob.cool/)
- [SkillsTab.lua](https://github.com/PathOfBuildingCommunity/PathOfBuilding/blob/dev/src/Classes/SkillsTab.lua) · [PassiveSpec.lua](https://github.com/PathOfBuildingCommunity/PathOfBuilding/blob/dev/src/Classes/PassiveSpec.lua) · [ItemsTab.lua](https://github.com/PathOfBuildingCommunity/PathOfBuilding/blob/dev/src/Classes/ItemsTab.lua) · [Item.lua](https://github.com/PathOfBuildingCommunity/PathOfBuilding/blob/dev/src/Classes/Item.lua)
- [pob-parser (JS/Rust)](https://lib.rs/crates/pob-parser)
- [pobapi (Python, 2021 정지)](https://pobapi.readthedocs.io/index.html)
- [Node Power 이슈 #4982](https://github.com/PathOfBuildingCommunity/PathOfBuilding/issues/4982) · [#4522](https://github.com/PathOfBuildingCommunity/PathOfBuilding/issues/4522)

**게임 데이터 소스 (현재 미사용, 기록용)**
- [poe-tool-dev/passive-skill-tree-json](https://github.com/poe-tool-dev/passive-skill-tree-json) — 버전별 트리 JSON
- [grindinggear/skilltree-export](https://github.com/grindinggear/skilltree-export) — GGG 공식
- [PathOfBuilding — src/Data](https://github.com/PathOfBuildingCommunity/PathOfBuilding/tree/dev/src/Data)
- [repoe-fork/repoe](https://github.com/repoe-fork/repoe) · [호스팅 인덱스](https://repoe-fork.github.io/)
- [SnosMe/poe-dat-viewer](https://github.com/SnosMe/poe-dat-viewer) · [jchantrell/exiledb](https://github.com/jchantrell/exiledb)
- [erosson/poedat (중단)](https://github.com/erosson/poedat) · [pypoe-json (아카이브)](https://github.com/erosson/pypoe-json)
- [Data Exports — PoE Developer Docs](https://www.pathofexile.com/developer/docs/data)
