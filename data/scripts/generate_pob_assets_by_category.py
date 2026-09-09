#!/usr/bin/env python3
"""Generate category + PoB base-name asset maps from PoB Export Bases."""

from __future__ import annotations

import argparse
import json
import re
from collections import defaultdict
from pathlib import Path


REPOE_SOURCE = "https://lvlvllvlvllvlvl.github.io/RePoE/base_items.json"

CATEGORY_BY_EXPORT_FILE = {
    "amulet": "AMULET",
    "axe": "WEAPON",
    "belt": "BELT",
    "body": "BODY_ARMOUR",
    "boots": "BOOT",
    "bow": "WEAPON",
    "claw": "WEAPON",
    "dagger": "WEAPON",
    "fishing": "FISHING_ROD",
    "flask": "FLASK",
    "gloves": "GLOVES",
    "graft": "GRAFT",
    "helmet": "HELMET",
    "jewel": "JEWEL",
    "mace": "WEAPON",
    "quiver": "QUIVER",
    "ring": "RING",
    "shield": "SHIELD",
    "staff": "WEAPON",
    "sword": "WEAPON",
    "tincture": "TINCTURE",
    "wand": "WEAPON",
}

FILE_BY_CATEGORY = {
    "AMULET": "amulets.json",
    "BELT": "belts.json",
    "BODY_ARMOUR": "body-armours.json",
    "BOOT": "boots.json",
    "CLUSTER_JEWEL": "cluster-jewels.json",
    "FISHING_ROD": "fishing-rods.json",
    "FLASK": "flasks.json",
    "GLOVES": "gloves.json",
    "GRAFT": "grafts.json",
    "HELMET": "helmets.json",
    "JEWEL": "jewels.json",
    "QUIVER": "quivers.json",
    "RING": "rings.json",
    "SHIELD": "shields.json",
    "TINCTURE": "tinctures.json",
    "WEAPON": "weapons.json",
}


def parse_directives(path: Path) -> tuple[list[tuple[str, str | None]], list[tuple[bool, str]]]:
    direct: list[tuple[str, str | None]] = []
    patterns: list[tuple[bool, str]] = []
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line.startswith("#baseMatch "):
            value = line.removeprefix("#baseMatch ").strip()
            is_base_type = value.startswith("BaseType ")
            patterns.append((is_base_type, value.removeprefix("BaseType ") if is_base_type else value))
        elif line.startswith("#base "):
            parts = line.split(maxsplit=2)
            direct.append((parts[1], parts[2] if len(parts) == 3 else None))
    return direct, patterns


def lua_pattern_to_regex(pattern: str) -> re.Pattern[str]:
    result: list[str] = []
    index = 0
    while index < len(pattern):
        char = pattern[index]
        if char == "%" and index + 1 < len(pattern):
            next_char = pattern[index + 1]
            result.append(r"\d" if next_char == "d" else re.escape(next_char))
            index += 2
            continue
        if char == "[":
            end = pattern.find("]", index)
            if end != -1:
                result.append(pattern[index:end + 1])
                index = end + 1
                continue
        if char in "+?*()|.^$":
            result.append(char)
        else:
            result.append(re.escape(char))
        index += 1
    return re.compile("".join(result))


def inherited_from(metadata_id: str, ancestor: str, base_items: dict[str, dict]) -> bool:
    current = metadata_id
    seen: set[str] = set()
    while current not in seen:
        seen.add(current)
        parent = base_items.get(current, {}).get("inherits_from")
        if parent == ancestor:
            return True
        if not parent:
            return False
        current = parent
    return False


def icon_url(item: dict) -> str | None:
    dds_file = (item.get("visual_identity") or {}).get("dds_file")
    if not dds_file or not dds_file.lower().endswith(".dds"):
        return None
    return f"https://web.poecdn.com/image/{dds_file[:-4]}.png"


def asset(metadata_id: str, pob_base_name: str, item: dict | None) -> dict:
    item = item or {}
    return {
        "nameEn": item.get("name", pob_base_name),
        "nameKo": None,
        "metadataId": metadata_id,
        "iconUrl": icon_url(item),
        "source": "pob-export + repoe",
    }


def resolve_pob_base_name(metadata_id: str, item: dict | None, explicit_name: str | None) -> str | None:
    if explicit_name:
        return explicit_name
    name = (item or {}).get("name")
    # PoB Export gives the two Energy Blade item types distinct lookup names.
    # This mirrors src/Export/Scripts/bases.lua.
    if name == "Energy Blade":
        if "/OneHandWeapons/" in metadata_id:
            return "Energy Blade One Handed"
        if "/TwoHandWeapons/" in metadata_id:
            return "Energy Blade Two Handed"
    return name


def add_asset(entries: dict[str, dict], pob_base_name: str, new_asset: dict, stats: dict[str, int]) -> None:
    previous = entries.get(pob_base_name)
    if not previous:
        entries[pob_base_name] = new_asset
        return
    if previous["iconUrl"] != new_asset["iconUrl"]:
        raise ValueError(f"Conflicting images for PoB base name {pob_base_name!r}")
    previous.setdefault("metadataIds", [previous["metadataId"]]).append(new_asset["metadataId"])
    stats["mergedMetadata"] += 1


def generate(export_root: Path, base_items: dict[str, dict]) -> tuple[dict[str, dict[str, dict]], dict[str, int]]:
    categories: dict[str, dict[str, dict]] = defaultdict(dict)
    stats = {"direct": 0, "pattern": 0, "missingRepoe": 0, "missingIcon": 0, "mergedMetadata": 0}
    metadata_ids = sorted(base_items)

    for export_name, category in CATEGORY_BY_EXPORT_FILE.items():
        path = export_root / f"{export_name}.txt"
        direct, patterns = parse_directives(path)
        selected: dict[str, str | None] = {metadata_id: name for metadata_id, name in direct}

        for is_base_type, pattern in patterns:
            matcher = lua_pattern_to_regex(pattern) if not is_base_type else None
            for metadata_id in metadata_ids:
                # Export's baseMatch handler deliberately omits Royale-only variants.
                if "Royale" in metadata_id:
                    continue
                matched = inherited_from(metadata_id, pattern, base_items) if is_base_type else bool(matcher and matcher.search(metadata_id))
                if matched:
                    selected.setdefault(metadata_id, None)

        for metadata_id, explicit_pob_name in selected.items():
            item = base_items.get(metadata_id)
            pob_base_name = resolve_pob_base_name(metadata_id, item, explicit_pob_name)
            if not pob_base_name:
                stats["missingRepoe"] += 1
                continue
            resolved_category = "CLUSTER_JEWEL" if category == "JEWEL" and "JewelPassiveTreeExpansion" in metadata_id else category
            add_asset(categories[resolved_category], pob_base_name, asset(metadata_id, pob_base_name, item), stats)
            if explicit_pob_name:
                stats["direct"] += 1
            else:
                stats["pattern"] += 1
            if not icon_url(item or {}):
                stats["missingIcon"] += 1

    return dict(categories), stats


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pob-root", type=Path, required=True)
    parser.add_argument("--repoe", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, default=Path("web/src/assets-data"))
    args = parser.parse_args()

    base_items = json.loads(args.repoe.read_text(encoding="utf-8"))
    categories, stats = generate(args.pob_root / "src/Export/Bases", base_items)
    args.output_dir.mkdir(parents=True, exist_ok=True)
    for category in FILE_BY_CATEGORY:
        entries = categories.get(category, {})
        output_path = args.output_dir / FILE_BY_CATEGORY[category]
        output_path.write_text(json.dumps(dict(sorted(entries.items())), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"categories": {category: len(categories.get(category, {})) for category in sorted(FILE_BY_CATEGORY)}, **stats}, ensure_ascii=False))


if __name__ == "__main__":
    main()
