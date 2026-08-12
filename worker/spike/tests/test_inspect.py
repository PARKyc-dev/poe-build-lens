from pathlib import Path
import subprocess
import unittest

from runner_client import PobRunner


SPIKE = Path(__file__).resolve().parents[1]
FIXTURES = SPIKE / ".cache" / "fixtures"


class InspectTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        subprocess.run(
            ["python3", str(SPIKE / "tests" / "fixture_builder.py")], check=True
        )

    def test_inspects_single_configuration(self):
        with PobRunner(SPIKE) as runner:
            result = runner.request("inspect", FIXTURES / "single.xml")
        self.assertEqual(len(result["specs"]), 1)
        self.assertEqual(len(result["skillSets"]), 1)
        self.assertEqual(len(result["itemSets"]), 1)
        self.assertEqual(result["activeSpec"], 1)
        self.assertEqual(result["activeSkillSet"], 1)
        self.assertEqual(result["activeItemSet"], 1)

    def test_inspects_all_multiple_configurations_and_active_values(self):
        with PobRunner(SPIKE) as runner:
            result = runner.request("inspect", FIXTURES / "multiple.xml")
        self.assertEqual([entry["id"] for entry in result["specs"]], [1, 2, 3])
        self.assertEqual([entry["id"] for entry in result["skillSets"]], [1, 2])
        self.assertEqual([entry["id"] for entry in result["itemSets"]], [1, 2])
        self.assertEqual(result["activeSpec"], 3)
        self.assertEqual(result["activeSkillSet"], 2)
        self.assertEqual(result["activeItemSet"], 2)


if __name__ == "__main__":
    unittest.main()
