import json
from pathlib import Path
import sys


def format_measurement(label, values):
    return f"- {label}: {values['median']:.2f} ms / {values['p95']:.2f} ms"


def main(source: Path, target: Path):
    result = json.loads(source.read_text(encoding="utf-8"))
    passed = all(
        (
            result["testsPassed"],
            result["stateIsolation"]["passed"],
            result["checkoutClean"],
            result["processStable"],
        )
    )
    decision = "GO" if passed else "REVIEW"
    reasons = [] if passed else [
        name
        for name, value in (
            ("contract tests", result["testsPassed"]),
            ("A → B → A state isolation", result["stateIsolation"]["passed"]),
            ("unmodified PoB checkout", result["checkoutClean"]),
            ("process stability", result["processStable"]),
        )
        if not value
    ]
    lines = [
        "# PoB Headless Spike Results",
        "",
        f"## Decision: {decision}",
        "",
        "## Runtime",
        "",
        f"- PoB: {result['pob']['tag']} ({result['pob']['commit']})",
        f"- Host OS / architecture: {result['host']['os']} / {result['host']['architecture']}",
        f"- Python: {result['host']['python']}",
        f"- LuaJIT: {result['host']['luajit']}",
        "",
        "## Measurements",
        "",
        format_measurement("Startup wall-time median/p95", result["startup"]["wallMs"]),
        format_measurement("Inspect wall-time median/p95", result["inspect"]["wallMs"]),
        format_measurement("Analyze wall-time median/p95", result["analyze"]["wallMs"]),
        f"- Ready RSS: {result['startup']['readyRssKb']} KiB",
        f"- Post-request RSS: inspect {result['inspect']['rssKb']} KiB; analyze {result['analyze']['rssKb']} KiB",
        "",
        "## Correctness",
        "",
        f"- A → B → A: {'PASS' if result['stateIsolation']['passed'] else 'FAIL'}",
        f"- Unmodified PoB checkout: {'PASS' if result['checkoutClean'] else 'FAIL'}",
        f"- Contract tests: {'PASS' if result['testsPassed'] else 'FAIL'}",
        f"- Process stability: {'PASS' if result['processStable'] else 'FAIL'}",
        "",
        "## Notes",
        "",
        "No latency SLA is applied in this spike. The measured time and RSS data inform the next worker design's timeout and process-count choices.",
    ]
    if reasons:
        lines.extend(["", "Review is required because: " + ", ".join(reasons) + "."])
    target.write_text("\n".join(lines) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main(Path(sys.argv[1]), Path(sys.argv[2]))
