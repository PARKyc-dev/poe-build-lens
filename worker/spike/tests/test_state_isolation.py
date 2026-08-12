import math
from pathlib import Path
import subprocess
import unittest

from runner_client import PobRunner


SPIKE = Path(__file__).resolve().parents[1]
FIXTURES = SPIKE / ".cache" / "fixtures"
SINGLE_SELECTION = {"activeSpec": 1, "activeSkillSet": 1, "activeItemSet": 1}
MULTIPLE_SELECTION = {"activeSpec": 1, "activeSkillSet": 1, "activeItemSet": 1}


class StateIsolationTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        subprocess.run(
            ["python3", str(SPIKE / "tests" / "fixture_builder.py")], check=True
        )

    def test_a_to_b_to_a_matches_fresh_processes(self):
        fresh_a = self.analyze_fresh("single.xml", SINGLE_SELECTION)
        fresh_b = self.analyze_fresh("multiple.xml", MULTIPLE_SELECTION)

        with PobRunner(SPIKE) as runner:
            sequence_a1 = runner.request("analyze", FIXTURES / "single.xml", SINGLE_SELECTION)
            sequence_b = runner.request(
                "analyze", FIXTURES / "multiple.xml", MULTIPLE_SELECTION
            )
            sequence_a2 = runner.request("analyze", FIXTURES / "single.xml", SINGLE_SELECTION)

        self.assert_result_matches("single.xml first", sequence_a1, fresh_a)
        self.assert_result_matches("multiple.xml", sequence_b, fresh_b)
        self.assert_result_matches("single.xml second", sequence_a2, fresh_a)

    def analyze_fresh(self, fixture_name, selection):
        with PobRunner(SPIKE) as runner:
            return runner.request("analyze", FIXTURES / fixture_name, selection)

    def assert_result_matches(self, fixture_name, actual, expected):
        self.assertEqual(actual["selection"], expected["selection"], fixture_name)
        for key, expected_value in expected["summary"].items():
            actual_value = actual["summary"][key]
            self.assertEqual(
                actual_value is None,
                expected_value is None,
                f"{fixture_name}: {key} null placement",
            )
            if expected_value is not None:
                self.assertTrue(
                    math.isclose(
                        actual_value,
                        expected_value,
                        rel_tol=1e-9,
                        abs_tol=1e-9,
                    ),
                    f"{fixture_name}: {key} expected {expected_value}, got {actual_value}",
                )


if __name__ == "__main__":
    unittest.main()
