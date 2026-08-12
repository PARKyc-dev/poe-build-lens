from pathlib import Path
import subprocess
import unittest
import math

from runner_client import PobRunner, PobRunnerError


SPIKE = Path(__file__).resolve().parents[1]
FIXTURE = SPIKE / ".cache" / "fixtures" / "multiple.xml"


class AnalyzeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        subprocess.run(
            ["python3", str(SPIKE / "tests" / "fixture_builder.py")], check=True
        )

    def test_applies_all_three_selections_and_returns_summary(self):
        selection = {"activeSpec": 1, "activeSkillSet": 1, "activeItemSet": 1}
        with PobRunner(SPIKE) as runner:
            result = runner.request("analyze", FIXTURE, selection)
        self.assertEqual(result["selection"], selection)
        self.assertEqual(
            set(result["summary"]),
            {
                "totalDps",
                "combinedDps",
                "life",
                "energyShield",
                "mana",
                "armour",
                "evasion",
                "totalEhp",
            },
        )
        self.assertTrue(
            all(
                value is None or isinstance(value, (int, float))
                for value in result["summary"].values()
            )
        )

    def test_rejects_a_partial_selection(self):
        with PobRunner(SPIKE) as runner:
            with self.assertRaisesRegex(PobRunnerError, "INVALID_SELECTION"):
                runner.request("analyze", FIXTURE, {"activeSpec": 1})

    def test_rejects_an_unknown_selection_id(self):
        selection = {"activeSpec": 99, "activeSkillSet": 1, "activeItemSet": 1}
        with PobRunner(SPIKE) as runner:
            with self.assertRaisesRegex(PobRunnerError, "UNKNOWN_SPEC"):
                runner.request("analyze", FIXTURE, selection)

    def test_repeated_selection_matches_a_fresh_process(self):
        selection = {"activeSpec": 1, "activeSkillSet": 1, "activeItemSet": 1}
        with PobRunner(SPIKE) as runner:
            first = runner.request("analyze", FIXTURE, selection)
            second = runner.request("analyze", FIXTURE, selection)
        with PobRunner(SPIKE) as runner:
            fresh = runner.request("analyze", FIXTURE, selection)

        self.assertEqual(first["selection"], selection)
        self.assertEqual(second["selection"], selection)
        self.assertEqual(fresh["selection"], selection)
        for key, value in fresh["summary"].items():
            self.assertEqual(first["summary"][key] is None, value is None, key)
            self.assertEqual(second["summary"][key] is None, value is None, key)
            if value is not None:
                self.assertTrue(
                    math.isclose(first["summary"][key], value, rel_tol=1e-9, abs_tol=1e-9),
                    key,
                )
                self.assertTrue(
                    math.isclose(second["summary"][key], value, rel_tol=1e-9, abs_tol=1e-9),
                    key,
                )


if __name__ == "__main__":
    unittest.main()
