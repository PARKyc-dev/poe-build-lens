import unittest

from generate_gem_assets import generate


class GenerateGemAssetsTest(unittest.TestCase):
    def test_keeps_only_gems_with_dds_images(self):
        assets = generate({
            "active": {"item_class": "Active Skill Gem", "visual_identity": {"dds_file": "Art/Gem.dds"}},
            "support": {"item_class": "Support Skill Gem", "visual_identity": {"dds_file": "Art/Support.dds"}},
            "armour": {"item_class": "Body Armour", "visual_identity": {"dds_file": "Art/Armour.dds"}},
            "missing": {"item_class": "Active Skill Gem", "visual_identity": None},
        })

        self.assertEqual(assets, {
            "active": "https://web.poecdn.com/image/Art/Gem.png",
            "support": "https://web.poecdn.com/image/Art/Support.png",
        })


if __name__ == "__main__":
    unittest.main()
