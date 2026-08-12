import argparse
import json
import math
from pathlib import Path
import platform
import subprocess
import sys
import time


SPIKE = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SPIKE / "tests"))

from runner_client import PobRunner


FIXTURES = SPIKE / ".cache" / "fixtures"
SINGLE = FIXTURES / "single.xml"
MULTIPLE = FIXTURES / "multiple.xml"
SELECTION = {"activeSpec": 1, "activeSkillSet": 1, "activeItemSet": 1}


def lock_values():
    return dict(
        line.split("=", 1)
        for line in (SPIKE / "pob.lock").read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")
    )


def rss_kb(pid):
    return int(
        subprocess.check_output(["ps", "-o", "rss=", "-p", str(pid)], text=True).strip()
    )


def summary(samples):
    ordered = sorted(samples)
    return {
        "median": statistics_median(ordered),
        "p95": ordered[math.ceil(len(ordered) * 0.95) - 1],
        "min": ordered[0],
        "max": ordered[-1],
        "raw": samples,
    }


def statistics_median(values):
    midpoint = len(values) // 2
    if len(values) % 2:
        return values[midpoint]
    return (values[midpoint - 1] + values[midpoint]) / 2


def request_samples(runner, operation):
    wall_samples = []
    cpu_samples = []
    rss_samples = []
    for index in range(1, 21):
        fixture = SINGLE if index % 2 else MULTIPLE
        start = time.perf_counter()
        envelope = runner.request_envelope(operation, fixture, SELECTION)
        wall_samples.append((time.perf_counter() - start) * 1000)
        cpu_samples.append(envelope["timingsCpuMs"][operation])
        if index in (5, 10, 20):
            rss_samples.append(rss_kb(runner.pid))
    return {"wallMs": summary(wall_samples), "cpuMs": summary(cpu_samples), "rssKb": rss_samples}


def same_result(actual, expected):
    if actual["selection"] != expected["selection"]:
        return False
    for key, expected_value in expected["summary"].items():
        actual_value = actual["summary"][key]
        if (actual_value is None) != (expected_value is None):
            return False
        if expected_value is not None and not math.isclose(
            actual_value, expected_value, rel_tol=1e-9, abs_tol=1e-9
        ):
            return False
    return True


def analyze_fresh(fixture):
    with PobRunner(SPIKE) as runner:
        return runner.request("analyze", fixture, SELECTION)


def state_isolation():
    fresh_a = analyze_fresh(SINGLE)
    fresh_b = analyze_fresh(MULTIPLE)
    with PobRunner(SPIKE) as runner:
        a1 = runner.request("analyze", SINGLE, SELECTION)
        b = runner.request("analyze", MULTIPLE, SELECTION)
        a2 = runner.request("analyze", SINGLE, SELECTION)
    return same_result(a1, fresh_a) and same_result(b, fresh_b) and same_result(a2, fresh_a)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument(
        "--tests-passed",
        action="store_true",
        help="Require that the full spike test command passed immediately before measuring.",
    )
    arguments = parser.parse_args()

    subprocess.run([str(SPIKE / "scripts" / "check-runtime.sh")], check=True)
    subprocess.run([str(SPIKE / "scripts" / "fetch-pob.sh")], check=True)
    subprocess.run(["python3", str(SPIKE / "tests" / "fixture_builder.py")], check=True)
    startup_wall = []
    ready_rss = []
    startup_cpu = []
    for _ in range(5):
        start = time.perf_counter()
        with PobRunner(SPIKE) as runner:
            startup_wall.append((time.perf_counter() - start) * 1000)
            ready_rss.append(rss_kb(runner.pid))
            startup_cpu.append(runner.ready["startupCpuMs"])

    with PobRunner(SPIKE) as runner:
        inspect = request_samples(runner, "inspect")
        analyze = request_samples(runner, "analyze")

    checkout_clean = not subprocess.check_output(
        ["git", "-C", str(SPIKE / ".cache" / "PathOfBuilding"), "status", "--porcelain"],
        text=True,
    ).strip()
    isolation_passed = state_isolation()
    result = {
        "pob": {"tag": lock_values()["POB_TAG"], "commit": lock_values()["POB_COMMIT"]},
        "host": {
            "os": platform.system(),
            "architecture": platform.machine(),
            "python": platform.python_version(),
            "luajit": subprocess.check_output(["luajit", "-v"], text=True, stderr=subprocess.STDOUT).strip(),
        },
        "startup": {"wallMs": summary(startup_wall), "cpuMs": summary(startup_cpu), "readyRssKb": ready_rss},
        "inspect": inspect,
        "analyze": analyze,
        "stateIsolation": {"passed": isolation_passed},
        "checkoutClean": checkout_clean,
        "testsPassed": arguments.tests_passed,
        "processStable": True,
    }
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    if not (checkout_clean and isolation_passed):
        raise SystemExit("Spike correctness checks failed")


if __name__ == "__main__":
    main()
