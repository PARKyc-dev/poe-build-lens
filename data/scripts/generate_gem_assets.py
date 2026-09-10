#!/usr/bin/env python3
"""Generate PoB gem metadata ID to PoE CDN image mappings from RePoE."""

from __future__ import annotations

import argparse
import json
from pathlib import Path


GEM_CLASSES = {"Active Skill Gem", "Support Skill Gem"}


def generate(base_items: dict[str, dict]) -> dict[str, str]:
    assets: dict[str, str] = {}
    for metadata_id, item in base_items.items():
        dds_file = (item.get("visual_identity") or {}).get("dds_file")
        if item.get("item_class") in GEM_CLASSES and dds_file and dds_file.lower().endswith(".dds"):
            assets[metadata_id] = f"https://web.poecdn.com/image/{dds_file[:-4]}.png"
    return dict(sorted(assets.items()))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repoe", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("web/src/assets-data/gems.json"))
    args = parser.parse_args()

    base_items = json.loads(args.repoe.read_text(encoding="utf-8"))
    assets = generate(base_items)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(assets, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(assets)} gem assets: {args.output}")


if __name__ == "__main__":
    main()
