#!/usr/bin/env python3
"""Generate the item asset map for one pobb.in build."""

from __future__ import annotations

import argparse
import base64
import json
import re
import urllib.parse
import zlib
from pathlib import Path
from xml.etree import ElementTree


EQUIPMENT_SLOTS = (
    "Weapon 1", "Weapon 2", "Helmet", "Body Armour", "Gloves", "Boots",
    "Amulet", "Ring 1", "Ring 2", "Belt", "Flask 1", "Flask 2", "Flask 3",
    "Flask 4", "Flask 5",
)


def decode_build(value: str) -> ElementTree.Element:
    value = value.strip()
    padding = "=" * (-len(value) % 4)
    xml = zlib.decompress(base64.urlsafe_b64decode(value + padding))
    return ElementTree.fromstring(xml)


def parse_item(text: str) -> dict[str, str]:
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    rarity = lines[0].removeprefix("Rarity: ")
    if rarity in {"UNIQUE", "RELIC"}:
        name, base_name = lines[1], lines[2]
    elif rarity in {"RARE", "MAGIC"}:
        name = lines[1]
        base_name = lines[2] if rarity == "RARE" else re.sub(r"^(?:.+?'s\s+)+", "", name)
        if rarity == "MAGIC":
            known_base = next((part for part in ("Diamond Flask", "Ruby Flask", "Silver Flask", "Topaz Flask", "Quicksilver Flask") if part in name), None)
            base_name = known_base or base_name
    else:
        name = base_name = lines[1]
    return {"name": name, "baseName": base_name, "rarity": rarity}


def page_asset_urls(html: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for url in re.findall(r"https://assets\.pobb\.in/1/[^\"')]+?\.webp", html):
        name = urllib.parse.unquote(url.rsplit("/", 1)[1][:-5])
        if "/" not in name:
            result[name] = url
    return result


def asset_name(item: dict[str, str]) -> str:
    name = item["name"] if item["rarity"] in {"UNIQUE", "RELIC"} else item["baseName"]
    return name.removeprefix("Foulborn ")


def asset_key(item: dict[str, str]) -> str:
    kind = "UNIQUE_ITEM" if item["rarity"] in {"UNIQUE", "RELIC"} else "BASE_ITEM"
    return f"{kind}|{item['name'] if kind == 'UNIQUE_ITEM' else item['baseName']}"


def generate(build_id: str, build_text: str, html: str, base_items: dict) -> dict:
    root = decode_build(build_text)
    items_element = root.find("Items")
    tree_element = root.find("Tree")
    if items_element is None or tree_element is None:
        raise ValueError("PoB build has no Items or Tree section")

    item_text_by_id = {item.get("id"): item.text or "" for item in items_element.findall("Item")}
    active_item_set_id = items_element.get("activeItemSet")
    active_item_set = next((entry for entry in items_element.findall("ItemSet") if entry.get("id") == active_item_set_id), items_element)
    active_spec_id = tree_element.get("activeSpec")
    active_spec = next((entry for entry in tree_element.findall("Spec") if entry.get("id") == active_spec_id), tree_element.find("Spec"))
    urls = page_asset_urls(html)
    base_ids_by_name = {
        item["name"]: metadata_id
        for metadata_id, item in base_items.items()
        if isinstance(item, dict) and item.get("release_state") == "released" and item.get("name")
    }

    instances: list[dict] = []
    slot_items = {slot.get("name"): slot.get("itemId") for slot in active_item_set.findall("Slot")}
    for slot in EQUIPMENT_SLOTS:
        item_id = slot_items.get(slot)
        if item_id and item_id != "0" and item_id in item_text_by_id:
            instances.append({"location": "equipment", "slot": slot, **parse_item(item_text_by_id[item_id])})

    if active_spec is not None:
        for socket in active_spec.findall(".//Socket"):
            item_id = socket.get("itemId")
            if item_id in item_text_by_id:
                instances.append({"location": "jewel", "socket": socket.get("nodeId"), **parse_item(item_text_by_id[item_id])})

    assets: dict[str, dict] = {}
    for item in instances:
        key = asset_key(item)
        item["assetKey"] = key
        if key in assets:
            continue
        lookup_name = asset_name(item)
        assets[key] = {
            "type": key.split("|", 1)[0],
            "metadataId": base_ids_by_name.get(item["baseName"]),
            "name": lookup_name,
            "iconUrl": urls.get(lookup_name),
            "source": f"https://pobb.in/{build_id}",
        }

    return {
        "buildId": build_id,
        "sourceUrl": f"https://pobb.in/{build_id}",
        "itemCount": len(instances),
        "assetCount": len(assets),
        "items": instances,
        "assets": assets,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--build-id", required=True)
    parser.add_argument("--build", type=Path, required=True)
    parser.add_argument("--page", type=Path, required=True)
    parser.add_argument("--repoe", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    result = generate(
        args.build_id,
        args.build.read_text(encoding="utf-8"),
        args.page.read_text(encoding="utf-8"),
        json.loads(args.repoe.read_text(encoding="utf-8")),
    )
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    missing = sum(asset["iconUrl"] is None for asset in result["assets"].values())
    print(f"Generated {result['itemCount']} items / {result['assetCount']} assets ({missing} missing): {args.output}")


if __name__ == "__main__":
    main()
