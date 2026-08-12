from pathlib import Path
import subprocess
import unittest


SPIKE = Path(__file__).resolve().parents[1]


class BootstrapTest(unittest.TestCase):
    def test_pob_lock_pins_the_approved_release(self):
        values = dict(
            line.split("=", 1)
            for line in (SPIKE / "pob.lock").read_text().splitlines()
            if line and not line.startswith("#")
        )
        self.assertEqual(values["POB_TAG"], "v2.67.2")
        self.assertEqual(
            values["POB_COMMIT"], "b32759ab0f31a1c8499a0d420cb0f0633d4fe478"
        )
        self.assertEqual(
            values["POB_REPOSITORY"],
            "https://github.com/PathOfBuildingCommunity/PathOfBuilding.git",
        )

    def test_runtime_check_is_an_executable_contract(self):
        result = subprocess.run(
            [str(SPIKE / "scripts" / "check-runtime.sh")],
            text=True,
            capture_output=True,
        )
        self.assertIn(result.returncode, (0, 1))
        self.assertNotEqual(result.stdout.strip(), "")


if __name__ == "__main__":
    unittest.main()
