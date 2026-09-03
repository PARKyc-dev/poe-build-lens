# 2026-08-25: Generic skill operation analysis

## Outcome

- Browser PoB facts now supply grounded operation inputs from skills, equipped items and jewels, allocated passive/ascendancy nodes, and player-applied buffs.
- The API derives only supported operation flows: resource consume/gain cycles; on-kill, on-hit, and on-damaged chains; and maintain, reserve, cooldown, convert, and enhance states.
- AI operation prose must cite supplied flow grounds; it must not invent item effects, charge recovery, triggers, or causal links.
- The extractor excludes life, resistance, movement, movement-speed, armour, and `Condition:`, `Multiplier:`, or PvP modifiers.
- `operationFacts` is an optional addition to the analysis request. The public `AnalysisResult` response and `web/src/api/analysis.ts` response type remain unchanged.

## Verification

- `cd api && ./gradlew test --no-daemon`: succeeded.
- `cd api && ./gradlew bootJar --no-daemon`: succeeded.
- `cd web && npm run build`: succeeded.
- `cd web && npm test -- --run`: produced no completed test result after two minutes and was interrupted (exit 130); investigate the stalled Vitest run separately.
