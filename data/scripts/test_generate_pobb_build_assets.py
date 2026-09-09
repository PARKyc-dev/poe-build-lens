import base64
import json
import unittest
import zlib

from generate_pobb_build_assets import asset_key, asset_name, generate, page_asset_urls, parse_item


class GeneratePobbBuildAssetsTest(unittest.TestCase):
    def test_uses_base_for_rare_item(self):
        item = parse_item("Rarity: RARE\nWoe Chant\nKinetic Wand\nItem Level: 85")
        self.assertEqual("BASE_ITEM|Kinetic Wand", asset_key(item))

    def test_uses_unique_art_name_for_foulborn_item(self):
        item = parse_item("Rarity: UNIQUE\nFoulborn The Iron Fortress\nCrusader Plate\nArmour: 100")
        self.assertEqual("The Iron Fortress", asset_name(item))
        self.assertEqual("UNIQUE_ITEM|Foulborn The Iron Fortress", asset_key(item))

    def test_extracts_named_item_assets_only(self):
        html = 'https://assets.pobb.in/1/Dawnbreaker.webp https://assets.pobb.in/1/art%2Fpassive.webp'
        self.assertEqual({"Dawnbreaker": "https://assets.pobb.in/1/Dawnbreaker.webp"}, page_asset_urls(html))

    def test_extracts_sockets_nested_in_active_spec(self):
        xml = '''<PathOfBuilding><Items activeItemSet="1"><Item id="1">Rarity: UNIQUE
Stormshroud
Viridian Jewel</Item><ItemSet id="1" /></Items><Tree activeSpec="1"><Spec id="1"><Sockets><Socket itemId="1" nodeId="42" /></Sockets></Spec></Tree></PathOfBuilding>'''
        build = base64.urlsafe_b64encode(zlib.compress(xml.encode())).decode().rstrip("=")
        result = generate(
            "example",
            build,
            "https://assets.pobb.in/1/Stormshroud.webp",
            json.loads("{}"),
        )
        self.assertEqual("42", result["items"][0]["socket"])
        self.assertEqual(1, result["itemCount"])


if __name__ == "__main__":
    unittest.main()
