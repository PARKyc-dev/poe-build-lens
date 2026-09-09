import unittest

from generate_pob_assets_by_category import add_asset, generate, lua_pattern_to_regex, resolve_pob_base_name


class GeneratePobAssetsByCategoryTest(unittest.TestCase):
    def test_converts_pob_lua_patterns(self):
        self.assertIsNotNone(lua_pattern_to_regex("Metadata/Items/Armours/Boots/BootsStr%d+").fullmatch("Metadata/Items/Armours/Boots/BootsStr12"))
        self.assertIsNotNone(lua_pattern_to_regex("Metadata/Items/Weapons/OneHandWeapons/Wands/Wand[MEK]?%d+").fullmatch("Metadata/Items/Weapons/OneHandWeapons/Wands/WandM2"))

    def test_prefix_base_match_can_match_a_flask_variant(self):
        self.assertIsNotNone(lua_pattern_to_regex("Metadata/Items/Flasks/FlaskUtility").search("Metadata/Items/Flasks/FlaskUtility14"))

    def test_keeps_explicit_pob_name_for_two_toned_boots(self):
        from pathlib import Path

        base_items = {
            "Metadata/Items/Armours/Boots/BootsAtlas2": {
                "name": "Two-Toned Boots",
                "visual_identity": {"dds_file": "Art/2DItems/Armours/Boots/TwoTonedBoots2B.dds"},
            }
        }
        categories, _ = generate(Path("/Users/parkyc/Desktop/Code/PathOfBuilding/src/Export/Bases"), base_items)
        item = categories["BOOT"]["Two-Toned Boots (Armour/Evasion)"]
        self.assertEqual("Two-Toned Boots", item["nameEn"])
        self.assertEqual("Metadata/Items/Armours/Boots/BootsAtlas2", item["metadataId"])

    def test_matches_pob_energy_blade_lookup_names(self):
        item = {"name": "Energy Blade"}
        self.assertEqual(
            "Energy Blade One Handed",
            resolve_pob_base_name("Metadata/Items/Weapons/OneHandWeapons/OneHandSwords/StormBladeOneHand", item, None),
        )
        self.assertEqual(
            "Energy Blade Two Handed",
            resolve_pob_base_name("Metadata/Items/Weapons/TwoHandWeapons/TwoHandSwords/StormBladeTwoHand", item, None),
        )

    def test_merges_equivalent_metadata_for_one_pob_base_name(self):
        entries = {}
        stats = {"mergedMetadata": 0}
        add_asset(entries, "Shadowed Ring", {"metadataId": "a", "iconUrl": "same"}, stats)
        add_asset(entries, "Shadowed Ring", {"metadataId": "b", "iconUrl": "same"}, stats)
        self.assertEqual(["a", "b"], entries["Shadowed Ring"]["metadataIds"])
        self.assertEqual(1, stats["mergedMetadata"])


if __name__ == "__main__":
    unittest.main()
