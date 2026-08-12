import json
from pathlib import Path
import subprocess
import tempfile
import unittest


SPIKE = Path(__file__).resolve().parents[1]


class BenchmarkReportTest(unittest.TestCase):
    def test_renders_required_measurements_and_go_decision(self):
        report = {
            "pob": {"tag": "v2.67.2", "commit": "b32759ab0f31a1c8499a0d420cb0f0633d4fe478"},
            "host": {"os": "Darwin", "architecture": "arm64", "python": "3.9", "luajit": "2.1"},
            "startup": {"wallMs": {"median": 100, "p95": 120}, "readyRssKb": [1000]},
            "inspect": {"wallMs": {"median": 10, "p95": 20}, "rssKb": [1000, 1001]},
            "analyze": {"wallMs": {"median": 30, "p95": 40}, "rssKb": [1002, 1003]},
            "stateIsolation": {"passed": True},
            "checkoutClean": True,
            "testsPassed": True,
            "processStable": True,
        }
        with tempfile.TemporaryDirectory() as directory:
            source = Path(directory) / "benchmark.json"
            target = Path(directory) / "report.md"
            source.write_text(json.dumps(report), encoding="utf-8")
            subprocess.run(
                ["python3", str(SPIKE / "scripts" / "render_report.py"), str(source), str(target)],
                check=True,
            )
            rendered = target.read_text(encoding="utf-8")

        for expected in (
            "v2.67.2",
            "b32759ab0f31a1c8499a0d420cb0f0633d4fe478",
            "Darwin",
            "arm64",
            "3.9",
            "2.1",
            "Startup wall-time median/p95",
            "Inspect wall-time median/p95",
            "Analyze wall-time median/p95",
            "Ready RSS",
            "Post-request RSS",
            "A → B → A",
            "Unmodified PoB checkout",
            "GO",
        ):
            self.assertIn(expected, rendered)


if __name__ == "__main__":
    unittest.main()
