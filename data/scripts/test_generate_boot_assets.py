import json
import tempfile
import unittest
from pathlib import Path

from generate_boot_assets import generate, pob_pattern_to_regex


class GenerateBootAssetsTest(unittest.TestCase):
    def test_converts_pob_digit_pattern(self):
        pattern = pob_pattern_to_regex("Metadata/Items/Armours/Boots/BootsStr%d+")
        self.assertIsNotNone(pattern.fullmatch("Metadata/Items/Armours/Boots/BootsStr12"))
        self.assertIsNone(pattern.fullmatch("Metadata/Items/Armours/Boots/BootsDex12"))

    def test_explicit_pob_name_wins_over_repoe_name(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            boots = root / "boots.txt"
            repoe = root / "base_items.json"
            boots.write_text(
                "#base Metadata/Items/Armours/Boots/BootsAtlas2 "
                "Two-Toned Boots (Armour/Evasion)\n",
                encoding="utf-8",
            )
            repoe.write_text(
                json.dumps(
                    {
                        "Metadata/Items/Armours/Boots/BootsAtlas2": {
                            "item_class": "Boots",
                            "release_state": "released",
                            "name": "Two-Toned Boots",
                            "visual_identity": {
                                "dds_file": "Art/2DItems/Armours/Boots/TwoTonedBoots2B.dds"
                            },
                        }
                    }
                ),
                encoding="utf-8",
            )

            result = generate(boots, repoe, 10)

        asset = result["items"]["BASE_ITEM|Two-Toned Boots (Armour/Evasion)"]
        self.assertEqual("Two-Toned Boots", asset["name"])
        self.assertEqual(
            "https://web.poecdn.com/image/Art/2DItems/Armours/Boots/TwoTonedBoots2B.png",
            asset["iconUrl"],
        )


if __name__ == "__main__":
    unittest.main()
