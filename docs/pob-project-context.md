# PoB Build Mechanism Analyzer 프로젝트 정리

## 1. 프로젝트 최종 목표

사용자가 **Path of Building(PoB) URL**을 입력하면 PoB 데이터를 읽어 빌드의 핵심 메커니즘을 분석하고, 이를 LLM에 정제된 형태로 전달하여 사람이 이해하기 쉬운 설명을 생성한다.

예시:

> 이 빌드는 Cyclone으로 치명타를 발생시키고, Cast on Critical Strike(CoC)를 통해 연결된 주문을 반복적으로 발동시키는 빌드입니다.  
> 따라서 치명타 확률, 공격 속도, 트리거 재사용 대기시간 관련 수치가 중요합니다.

중요한 방향은 **PoB 전체 Raw Data를 LLM에 직접 넣지 않는 것**이다.

```text
PoB URL
  ↓
PoB Engine
  ↓
Build Analyzer
  ↓
정제된 BuildAnalysis
  ↓
Spring Boot
  ↓
LLM
  ↓
빌드 메커니즘 설명
```

LLM의 역할은 빌드를 직접 판별하는 것이 아니라, Analyzer가 결정한 구조와 근거를 바탕으로 자연어 설명을 생성하는 것이다.

---

## 2. 프론트엔드에서 PoB Engine 사용

PoB의 계산 엔진은 Lua 기반이다.

브라우저에서는 PoB의 GUI 전체를 재현하지 않고, **Headless PoB Engine을 Lua/WASM 형태로 실행**하는 방향을 고려한다.

브라우저에서 PoB Engine을 사용하는 이유:

- Skill Set 전환
- Item Set 전환
- Passive Tree Set 전환
- Main Skill 선택
- DPS 계산
- eHP 계산
- Crit Chance
- Attack/Cast Rate
- Trigger Rate
- Max Hit 등 PoB 계산값 조회

사용자가 Skill Set / Item Set / Tree Set을 변경할 때마다 서버 Worker에 요청하지 않고 브라우저 내부에서 즉시 다시 계산할 수 있다.

### 기본 구조

```text
Browser
│
├── React
│   ├── PoB URL 입력
│   ├── Skill Set 선택
│   ├── Item Set 선택
│   ├── Passive Tree 선택
│   ├── Build Stats 표시
│   └── 분석 요청
│
├── PoB Headless Engine (Lua/WASM)
│   ├── Build load
│   ├── Skill/Item/Tree Set 변경
│   ├── Calculation
│   └── PoB Raw Data 추출
│
└── TypeScript Analyzer
    └── BuildAnalysis 생성

          ↓ 분석 요청 시에만

Spring Boot
  ↓
LLM
```

PoB의 그래픽 UI는 사용하지 않는다.

Skill, Item, Passive Tree 등의 시각화는 React에서 처리한다.

---

## 3. React와 PoB Engine의 결합 방식

React가 PoB Lua 내부 구조를 직접 참조하지 않도록 Adapter 계층을 둔다.

```typescript
interface PobEngine {

  loadBuild(code: string): Promise<void>;

  getSkillSets(): SkillSet[];
  selectSkillSet(id: string): void;

  getItemSets(): ItemSet[];
  selectItemSet(id: string): void;

  getTreeSets(): TreeSet[];
  selectTreeSet(id: string): void;

  getMainSkills(): Skill[];

  calculate(): BuildStats;
}
```

구현체:

```typescript
class WasmPobEngine implements PobEngine {
  // Lua/WASM bridge
}
```

구조:

```text
React
  ↓
PobEngine interface
  ↓
WasmPobEngine
  ↓
JS ↔ Lua/WASM Bridge
  ↓
PoB Headless Engine
```

이렇게 하면 PoB 내부 구현이 바뀌어도 React가 직접 영향을 받지 않는다.

---

## 4. PoB Engine과 Analyzer는 분리

PoB Engine과 Build Analyzer의 역할을 명확히 분리한다.

### PoB Engine

PoB가 알고 있는 사실을 정확하게 꺼내는 역할.

예:

- Gem
- Gem Type
- Gem Tags
- Skill Group
- Skill Set
- Item Set
- Tree Set
- Calculated DPS
- eHP
- Crit Chance
- Attack Rate
- Cast Rate
- Trigger Rate

### Analyzer

PoB에서 얻은 사실을 빌드 메커니즘으로 해석하는 역할.

```text
PoB Engine
   ↓
Normalized Build Data
   ↓
Build Analyzer
   ↓
BuildAnalysis
```

Analyzer는 TypeScript로 구현하는 방향을 우선 고려한다.

---

# 5. 빌드 분류 방식

처음에는 다음과 같이 하나의 enum으로 분류하려고 했음.

```text
Attack
Spell
Projectile
Trigger
DoT
Minion
CoC
...
```

하지만 이 값들은 서로 같은 축이 아니다.

예를 들어 CoC Ice Nova는 동시에 다음 특성을 가진다.

```text
Attack
Spell
Trigger
Critical
Cold
AoE
```

따라서 빌드를 하나의 `BuildType`으로 표현하지 않는다.

---

## 6. Analyzer에서 추출할 핵심 축

### 6.1 누가/무엇이 스킬을 실행하는가

Delivery / Execution Mechanism

```text
SELF
TRIGGER
TOTEM
MINION
TRAP
MINE
```

세부 Trigger:

```text
COC
CWC
CWDT
OTHER_TRIGGER
```

예:

```text
CoC Ice Nova

delivery = COC
```

---

### 6.2 실제 Damage Dealer는 무엇인가

```text
ATTACK
SPELL
MINION_SKILL
```

예:

```text
Cyclone
→ Activator

Ice Nova
→ Damage Dealer
```

따라서 CoC Ice Nova는:

```text
activatorType = ATTACK
damageDealerType = SPELL
```

---

### 6.3 어떤 방식으로 피해를 주는가

```text
HIT
DOT
AILMENT
HIT_AND_DOT
```

Ailment:

```text
IGNITE
POISON
BLEED
```

이 부분은 Gem Tag만으로 판단하지 않고 PoB 계산 결과를 적극 활용한다.

예:

```text
Hit DPS      300k
Ignite DPS   12m
```

이라면 Ignite 기반 빌드로 판단하는 것이 자연스럽다.

---

### 6.4 부가적인 특성

예:

```text
PROJECTILE
AOE
CRITICAL
CHANNELLING
MELEE
STRIKE
BOW
DURATION
```

Element:

```text
PHYSICAL
FIRE
COLD
LIGHTNING
CHAOS
```

이 값들은 빌드의 대표 타입이라기보다 Feature로 관리한다.

---

### 6.5 Skill 간 연결 관계

Analyzer의 가장 중요한 목표 중 하나.

예: CoC Ice Nova

```text
PLAYER
  ↓ USES
Cyclone
  ↓ CRITICAL_STRIKE
Cast on Critical Strike
  ↓ TRIGGERS
Ice Nova
  ↓ DEALS_DAMAGE
Enemy
```

LLM에는 이런 관계 데이터가 매우 유용하다.

---

# 7. Active Gem과 Support Gem 태그는 합치지 않음

모든 Gem Tag를 하나로 합쳐서 판단하면 오분류 가능성이 높다.

예:

```text
Cyclone
Cast on Critical Strike
Ice Nova
```

전체 태그를 합치면:

```text
Attack
Spell
Trigger
Critical
AoE
Cold
Melee
Channelling
```

모든 값이 동시에 존재하게 된다.

따라서 다음처럼 별도로 유지한다.

```typescript
interface SkillGroupFeatures {
  activeGemTags: Set<GemTag>;
  supportGemTags: Set<GemTag>;
}
```

Active Gem의 태그는 실제 스킬 특성에 대한 강한 신호다.

Support Gem의 태그는 보조 신호로 사용한다.

특히 CoC, CwC 등은 단순 Tag보다 **Support Gem 자체의 ID**가 훨씬 강한 판단 근거다.

---

# 8. Gem 이름보다 Metadata ID 사용

가능하면 Gem 이름 문자열 대신 PoB / PoE의 Metadata ID를 사용한다.

예:

```text
Metadata/Items/Gems/SupportGemCastOnCrit
```

이유:

- 번역에 영향받지 않음
- 표시 이름 변경에 상대적으로 강함
- Rule 작성이 안정적임

---

# 9. Analyzer 내부 구조

추천 파이프라인:

```text
PoB Raw Data
   ↓
SkillGroupParser
   ↓
NormalizedSkillGroup
   ↓
FeatureExtractor
   ↓
MechanismContext
   ↓
MechanismRule Engine
   ↓
BuildMechanism
   ↓
DamageAnalyzer
   ↓
FeatureExtractor
   ↓
FlowBuilder
   ↓
BuildAnalysis
```

---

## 10. Normalized Skill Model

```typescript
interface SkillGem {
  id: string;
  name: string;
  kind: GemKind;
  tags: Set<GemTag>;
}

type GemKind =
  | "ACTIVE"
  | "SUPPORT";
```

예:

```text
Cyclone
kind = ACTIVE
tags = ATTACK, AOE, MELEE, CHANNELLING

Cast on Critical Strike
kind = SUPPORT
tags = TRIGGER, CRITICAL

Ice Nova
kind = ACTIVE
tags = SPELL, COLD, AOE
```

---

# 11. Skill Role 추출

Active Gem이라고 해서 모두 Damage Dealer는 아니다.

```typescript
type SkillRole =
  | "ACTIVATOR"
  | "DAMAGE_DEALER"
  | "UTILITY"
  | "UNKNOWN";
```

예:

```text
Cyclone
→ ACTIVATOR

Cast on Critical Strike
→ TRIGGER

Ice Nova
→ DAMAGE_DEALER

Increased Critical Damage
→ SUPPORT
```

---

# 12. Trigger Detector / Mechanism Rule

하나의 거대한 if-else보다 Rule을 분리한다.

```typescript
interface MechanismRule {

  matches(context: MechanismContext): boolean;

  analyze(context: MechanismContext): BuildMechanism;
}
```

구현 예:

```text
CocMechanismRule
CwcMechanismRule
TotemMechanismRule
TrapMechanismRule
MineMechanismRule
MinionMechanismRule
DefaultAttackRule
DefaultSpellRule
```

---

## 13. CoC Rule 예시

CoC의 기본 메커니즘:

```text
Attack
  ↓ Critical Strike
Cast on Critical Strike
  ↓
Linked Spell Trigger
```

판별:

```typescript
if (hasSupport(CAST_ON_CRIT)) {
  // CoC
}
```

그 후 Active Gem 중:

```text
ATTACK → Activator
SPELL  → Damage Dealer
```

를 찾는다.

예:

```text
Cyclone
→ ACTIVATOR

Ice Nova
→ DAMAGE_DEALER
```

---

## 14. CwC Rule 예시

```text
Channelling Skill
  ↓
Cast while Channelling
  ↓
Non-Channelling Spell
```

판별:

```text
CwC Support 존재
+
Channelling Active Skill 존재
+
Non-Channelling Spell 존재
```

---

# 15. Damage Analyzer

PoB 계산 결과를 기반으로 판단한다.

예시 모델:

```typescript
interface DamageMetrics {
  hitDps?: number;
  dotDps?: number;
  igniteDps?: number;
  poisonDps?: number;
  bleedDps?: number;
}
```

판단 예:

```text
Poison DPS가 Hit DPS보다 압도적
→ POISON

Ignite DPS가 주력
→ IGNITE

DoT DPS가 주력
→ DOT

그 외
→ HIT
```

정확한 threshold는 추후 테스트하면서 결정한다.

---

# 16. Build Stats

PoB 계산 결과를 그대로 UI에 연결하지 않고 별도의 정규화 모델을 만든다.

```typescript
interface BuildStats {
  offense: OffenseStats;
  defense: DefenseStats;
}
```

```typescript
interface OffenseStats {
  totalDps?: number;
  hitDps?: number;
  dotDps?: number;

  attackRate?: number;
  castRate?: number;

  critChance?: number;
  triggerRate?: number;
}
```

```typescript
interface DefenseStats {
  life?: number;
  energyShield?: number;

  ehp?: number;

  physicalMaxHit?: number;
  fireMaxHit?: number;
  coldMaxHit?: number;
  lightningMaxHit?: number;
  chaosMaxHit?: number;
}
```

---

# 17. Mechanism Flow Model

빌드 메커니즘을 Graph 형태로 표현한다.

```typescript
interface MechanismEdge {
  from: string;
  relation: Relation;
  to: string;
}
```

```typescript
type Relation =
  | "USES"
  | "CAUSES_CRITICAL"
  | "TRIGGERS"
  | "DEALS_DAMAGE"
  | "SUMMONS";
```

CoC 예:

```text
PLAYER -> USES -> Cyclone

Cyclone
-> CAUSES_CRITICAL
-> Cast on Critical Strike

Cast on Critical Strike
-> TRIGGERS
-> Ice Nova

Ice Nova
-> DEALS_DAMAGE
-> ENEMY
```

---

# 18. 최종 BuildAnalysis 모델 예시

```typescript
interface BuildAnalysis {

  delivery: DeliveryMechanism;

  activator?: SkillInfo;

  damageDealers: SkillInfo[];

  damage: DamageProfile;

  stats: BuildStats;

  features: MechanicFeature[];

  mechanismFlow: MechanismEdge[];
}
```

CoC Ice Nova 예:

```json
{
  "delivery": "CAST_ON_CRITICAL_STRIKE",

  "activator": {
    "name": "Cyclone",
    "type": "ATTACK",
    "features": [
      "CHANNELLING",
      "MELEE",
      "AOE"
    ]
  },

  "damageDealers": [
    {
      "name": "Ice Nova",
      "type": "SPELL",
      "features": [
        "COLD",
        "AOE"
      ]
    }
  ],

  "damage": {
    "mode": "HIT",
    "elements": [
      "COLD"
    ]
  },

  "stats": {
    "totalDps": 12800000,
    "critChance": 98.2,
    "triggerRate": 10.1,
    "ehp": 84320
  },

  "features": [
    "TRIGGER",
    "CRITICAL"
  ],

  "mechanismFlow": [
    {
      "from": "PLAYER",
      "relation": "USES",
      "to": "Cyclone"
    },
    {
      "from": "Cyclone",
      "relation": "CAUSES_CRITICAL",
      "to": "Cast on Critical Strike"
    },
    {
      "from": "Cast on Critical Strike",
      "relation": "TRIGGERS",
      "to": "Ice Nova"
    },
    {
      "from": "Ice Nova",
      "relation": "DEALS_DAMAGE",
      "to": "ENEMY"
    }
  ]
}
```

---

# 19. LLM에 전달할 데이터

PoB 전체 XML / Lua 상태를 그대로 전달하지 않는다.

Analyzer 결과만 전달한다.

예:

```json
{
  "mechanism": "COC",
  "activator": "Cyclone",
  "damageDealer": "Ice Nova",

  "damage": {
    "mode": "HIT",
    "element": "COLD"
  },

  "stats": {
    "critChance": 98.2,
    "triggerRate": 10.1
  },

  "importantFactors": [
    "CRITICAL_STRIKE_CHANCE",
    "ATTACK_SPEED",
    "COOLDOWN_RECOVERY"
  ],

  "flow": [
    "Cyclone",
    "Critical Strike",
    "Cast on Critical Strike",
    "Ice Nova"
  ]
}
```

LLM은 이 구조를 바탕으로 자연어 설명만 담당한다.

---

# 20. 중요 능력치 역시 Analyzer에서 일부 결정

주의점까지 전부 LLM이 임의로 추론하게 하지 않는다.

예:

```text
COC
→ Critical Strike Chance
→ Attack Speed
→ Cooldown Recovery

IGNITE
→ Ignite Chance
→ Fire DoT Multiplier
→ Ailment Duration

POISON
→ Poison Chance
→ Chaos DoT Multiplier
→ Attack/Cast Rate
```

Analyzer가 `importantFactors`를 제공하고 LLM은 왜 중요한지 설명한다.

---

# 21. Skill / Item / Tree Set 처리

PoB에는 여러 Skill Set / Item Set / Passive Tree Set이 있을 수 있다.

사용자는 브라우저에서 각각을 선택한다.

예:

```text
Skill Set
- Mapping
- Boss

Item Set
- Budget
- Endgame

Tree
- Lv 90
- Lv 100
```

변경할 때마다 서버 호출하지 않고 브라우저의 PoB Headless Engine에서 다시 계산한다.

```text
선택 변경
  ↓
PoB Engine state 변경
  ↓
Recalculate
  ↓
React 상태 갱신
```

분석 버튼을 눌렀을 때만 BuildAnalysis를 Spring Boot로 전달한다.

---

# 22. React의 역할

React는 다음만 담당한다.

```text
PoB URL 입력

Skill Set 선택
Item Set 선택
Tree Set 선택
Main Skill 선택

Damage / eHP 표시

Gem / Item 표시

Passive Tree 시각화

Analyzer 결과 표시

분석 요청
```

PoB의 원래 UI나 렌더러는 브라우저로 이식하지 않는다.

---

# 23. Spring Boot 역할

```text
BuildAnalysis 요청 수신

사용자 / 분석 기록 관리

LLM 요청

LLM 결과 저장 / 반환

필요 시 캐싱
```

PoB 계산 자체는 기본적으로 브라우저에서 처리하는 방향.

---

# 24. 디렉토리 구조 예시

```text
web/
├── pob/
│   ├── engine/
│   │   ├── PobEngine.ts
│   │   └── WasmPobEngine.ts
│   │
│   ├── model/
│   │   ├── SkillSet.ts
│   │   ├── ItemSet.ts
│   │   ├── TreeSet.ts
│   │   ├── Skill.ts
│   │   └── BuildStats.ts
│   │
│   ├── analyzer/
│   │   ├── BuildAnalyzer.ts
│   │   ├── MechanismRule.ts
│   │   ├── CocMechanismRule.ts
│   │   ├── CwcMechanismRule.ts
│   │   ├── DamageAnalyzer.ts
│   │   └── MechanismFlowBuilder.ts
│   │
│   └── wasm/
│       └── PoB Lua/WASM 관련 코드
│
└── components/
```

전체 프로젝트:

```text
apps/
├── web/
└── api/
```

또는:

```text
web/
api/
```

형태 모두 가능하다.

---

# 25. PoB Headless 버전 업데이트 전략

PoB upstream 버전이 올라갈 때마다 네 웹용 이식본도 업데이트가 필요하다.

단순히 `git pull` 후 바로 배포하는 것은 위험하다.

PoB 내부 구조가 변경될 수 있기 때문이다.

예:

```text
build.skills.skillSets
build.items.itemSets
mainOutput.TotalDPS
```

같은 내부 API가 변경되면 JS ↔ Lua Bridge가 깨질 수 있다.

---

## 26. PoB 원본은 직접 수정하지 않기

추천:

```text
project/
├── web/
├── pob-driver/
│   ├── bridge.lua
│   ├── wasm/
│   └── tests/
│
└── vendor/
    └── PathOfBuilding/
```

PoB upstream은 Git Submodule 등으로 특정 commit/tag를 고정한다.

```text
vendor/PathOfBuilding
→ 특정 commit 고정
```

---

# 27. 업데이트 흐름

```text
PoB 새 버전 확인
  ↓
해당 tag/commit checkout
  ↓
WASM rebuild
  ↓
Regression Test
  ↓
통과
  ↓
프로젝트에서 submodule commit 갱신
  ↓
배포
```

궁극적으로 다음 같은 명령 하나로 처리할 수 있게 만드는 것이 목표.

```bash
./update-pob.sh <version>
```

---

# 28. Regression Test용 Fixture

대표적인 PoB 빌드를 테스트 데이터로 보관한다.

```text
fixtures/
├── coc-ice-nova.txt
├── rf.txt
├── lightning-arrow.txt
├── minion.txt
└── ignite.txt
```

PoB 버전 업데이트 후 확인할 내용:

```text
Build load 성공

Skill Set 개수 정상

Item Set 개수 정상

Tree Set 개수 정상

Main Skill 정상

DPS 계산 가능

eHP 계산 가능

Crit Chance 정상

Analyzer 결과 정상
```

예:

```text
CoC Ice Nova
→ mechanism = COC

RF
→ damage = DOT

Lightning Arrow
→ delivery = SELF
→ damageDealer = ATTACK
→ feature = PROJECTILE
```

---

# 29. PoB 계산 캐시 주의

Headless PoB에서 Build / Skill Set / Item Set / Tree Set을 계속 변경할 경우 이전 계산 캐시가 남지 않도록 주의해야 한다.

권장 흐름:

```text
state 변경
  ↓
PoB의 정상 recalculation path 호출
  ↓
cache invalidation
  ↓
calculate
```

내부 계산 함수를 임의로 직접 호출하는 방식은 피한다.

---

# 30. MVP에서 우선 지원할 메커니즘

처음부터 모든 PoE 빌드를 지원하지 않는다.

초기 후보:

```text
SELF_ATTACK
SELF_CAST

COC
CWC

TOTEM
TRAP
MINE

MINION
```

Damage Mechanism은 별도 축:

```text
HIT
DOT
IGNITE
POISON
BLEED
```

---

# 31. 핵심 설계 원칙

## 31.1 BuildType 하나로 모든 것을 표현하지 않는다

잘못된 예:

```typescript
type BuildType =
  | "ATTACK"
  | "SPELL"
  | "DOT"
  | "COC"
  | "MINION";
```

이 값들은 서로 다른 축이다.

---

## 31.2 PoB Engine과 Analyzer를 분리한다

```text
PoB Engine
= 사실 추출 / 계산

Analyzer
= 메커니즘 해석
```

---

## 31.3 Analyzer와 LLM을 분리한다

```text
Analyzer
= deterministic rule

LLM
= 자연어 설명
```

LLM이 빌드 타입을 임의 판단하는 구조는 지양한다.

---

## 31.4 PoB 원본과 Web Adapter를 분리한다

```text
PoB upstream
  ↓
bridge / adapter
  ↓
PobEngine interface
  ↓
React / Analyzer
```

PoB 업데이트 시 영향 범위를 Bridge 계층으로 제한한다.

---

## 31.5 분류 근거를 남긴다

Analyzer는 결과뿐 아니라 왜 그렇게 판단했는지도 반환할 수 있어야 한다.

예:

```text
COC Support 발견

Attack Active Skill 발견
→ Cyclone

Spell Active Skill 발견
→ Ice Nova

CoC Rule 조건 충족
```

이 근거는:

- 디버깅
- 테스트
- LLM 설명
- 사용자에게 분석 근거 표시

등에 활용할 수 있다.

---

# 32. 현재 권장 전체 구조

```text
PoB URL
  ↓
React
  ↓
PoB Headless Engine (Lua/WASM)
  ↓
Normalized PoB Data
  ↓
TypeScript Build Analyzer
  ↓
BuildAnalysis
  ├── React UI
  │    ├── DPS
  │    ├── eHP
  │    ├── Skill
  │    ├── Item
  │    └── Tree
  │
  └── 분석 요청
        ↓
     Spring Boot
        ↓
       LLM
        ↓
빌드 메커니즘 자연어 설명
```

---

# 33. 현재 가장 중요한 다음 작업

1. PoB Headless Engine을 브라우저에서 최소 기능으로 실행
2. PoB URL Build Load
3. Skill Set / Item Set / Tree Set 목록 조회
4. 선택 변경 후 Recalculate
5. DPS / eHP 등 핵심 Output 조회
6. `PobEngine` TypeScript 인터페이스 작성
7. `NormalizedSkillGroup` 모델 작성
8. CoC Rule 하나를 첫 Analyzer로 구현
9. CoC Fixture 기반 Regression Test 작성
10. BuildAnalysis → Spring Boot → LLM 연결

첫 Analyzer는 **CoC 하나를 완성도 높게 구현한 뒤 Rule 구조가 적절한지 검증**하고 CwC, Self Cast 등의 Rule을 추가하는 방식이 좋다.
