# PoB Headless Spike Results

## Decision: GO

## Runtime

- PoB: v2.67.2 (b32759ab0f31a1c8499a0d420cb0f0633d4fe478)
- Host OS / architecture: Darwin / arm64
- Python: 3.9.6
- LuaJIT: LuaJIT 2.1.1785763465 -- Copyright (C) 2005-2026 Mike Pall. https://luajit.org/

## Measurements

- Startup wall-time median/p95: 1368.08 ms / 1807.39 ms
- Inspect wall-time median/p95: 397.47 ms / 1540.00 ms
- Analyze wall-time median/p95: 482.78 ms / 529.52 ms
- Ready RSS: [268496, 256000, 258480, 268080, 269408] KiB
- Post-request RSS: inspect [673456, 759424, 1168448] KiB; analyze [1168064, 1178000, 1202192] KiB

## Correctness

- A → B → A: PASS
- Unmodified PoB checkout: PASS
- Contract tests: PASS
- Process stability: PASS

## Notes

No latency SLA is applied in this spike. The measured time and RSS data inform the next worker design's timeout and process-count choices.
