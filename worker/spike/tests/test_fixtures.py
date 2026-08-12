import json
from pathlib import Path
import subprocess
import unittest
import xml.etree.ElementTree as ET


SPIKE = Path(__file__).resolve().parents[1]
FIXTURES = SPIKE / ".cache" / "fixtures"


class FixtureTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        subprocess.run(
            ["python3", str(SPIKE / "tests" / "fixture_builder.py")], check=True
        )

    def test_single_fixture_has_one_of_each_configuration(self):
        root = ET.parse(FIXTURES / "single.xml").getroot()
        self.assertEqual(len(root.find("Tree").findall("Spec")), 1)
        self.assertEqual(len(root.find("Skills").findall("SkillSet")), 1)
        self.assertEqual(len(root.find("Items").findall("ItemSet")), 1)

    def test_multiple_fixture_has_expected_active_values(self):
        root = ET.parse(FIXTURES / "multiple.xml").getroot()
        self.assertEqual(len(root.find("Tree").findall("Spec")), 3)
        self.assertEqual(len(root.find("Skills").findall("SkillSet")), 2)
        self.assertEqual(len(root.find("Items").findall("ItemSet")), 2)
        self.assertEqual(root.find("Tree").get("activeSpec"), "3")
        self.assertEqual(root.find("Skills").get("activeSkillSet"), "2")
        self.assertEqual(root.find("Items").get("activeItemSet"), "2")

    def test_manifest_names_the_official_sources_and_locked_commit(self):
        manifest = json.loads((FIXTURES / "manifest.json").read_text())
        self.assertEqual(
            manifest["pobCommit"], "b32759ab0f31a1c8499a0d420cb0f0633d4fe478"
        )
        self.assertEqual(
            manifest["singleSource"], "spec/TestBuilds/3.13/OccVortex.lua"
        )
        self.assertEqual(
            manifest["multipleSource"],
            "spec/TestBuilds/3.13/Mirage Archer Toxic Rain.lua",
        )


if __name__ == "__main__":
    unittest.main()
