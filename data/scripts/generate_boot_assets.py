#!/usr/bin/env python3
"""Generate a small PoB-to-GGG asset mapping for boot base items."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


REPOE_SOURCE = "https://lvlvllvlvllvlvl.github.io/RePoE/base_items.json"


def parse_directives(path: Path) -> tuple[list[tuple[str, str | None]], list[str]]:
    direct: list[tuple[str, str | None]] = []
    patterns: list[str] = []

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if line.startswith("#baseMatch "):
            patterns.append(line.removeprefix("#baseMatch ").strip())
        elif line.startswith("#base "):
            parts = line.split(maxsplit=2)
            direct.append((parts[1], parts[2] if len(parts) == 3 else None))

    return direct, patterns


def pob_pattern_to_regex(pattern: str) -> re.Pattern[str]:
    escaped = re.escape(pattern).replace(r"%d\+", r"\d+")
    return re.compile(f"^{escaped}$")


def natural_metadata_key(metadata_id: str) -> tuple[str, int]:
    match = re.match(r"^(.*?)(\d+)$", metadata_id)
    return (match.group(1), int(match.group(2))) if match else (metadata_id, -1)


def icon_url(dds_file: str) -> str:
    if not dds_file.lower().endswith(".dds"):
        raise ValueError(f"Unsupported image path: {dds_file}")
    return f"https://web.poecdn.com/image/{dds_file[:-4]}.png"


def generate(boots_path: Path, repoe_path: Path, limit: int) -> dict:
    direct, patterns = parse_directives(boots_path)
    repoe = json.loads(repoe_path.read_text(encoding="utf-8"))
    selected: list[tuple[str, str | None]] = []
    seen: set[str] = set()

    # Keep PoB's explicit aliases in a small sample so ambiguous names such as
    # the three Two-Toned Boots variants are exercised from the first run.
    for metadata_id, pob_name in direct:
        if metadata_id in repoe and metadata_id not in seen:
            selected.append((metadata_id, pob_name))
            seen.add(metadata_id)

    compiled_patterns = [pob_pattern_to_regex(pattern) for pattern in patterns]
    matched_ids = sorted(
        (
            metadata_id
            for metadata_id, item in repoe.items()
            if isinstance(item, dict)
            and item.get("item_class") == "Boots"
            and item.get("release_state") == "released"
            and any(pattern.fullmatch(metadata_id) for pattern in compiled_patterns)
        ),
        key=natural_metadata_key,
    )
    for metadata_id in matched_ids:
        if metadata_id not in seen:
            selected.append((metadata_id, None))
            seen.add(metadata_id)

    assets: dict[str, dict] = {}
    for metadata_id, pob_name_override in selected[:limit]:
        item = repoe[metadata_id]
        visual_identity = item.get("visual_identity") or {}
        dds_file = visual_identity.get("dds_file")
        if not dds_file:
            continue
        pob_name = pob_name_override or item["name"]
        assets[f"BASE_ITEM|{pob_name}"] = {
            "type": "BASE_ITEM",
            "metadataId": metadata_id,
            "pobBaseName": pob_name,
            "name": item["name"],
            "iconUrl": icon_url(dds_file),
        }

    return {
        "source": {
            "pobExport": str(boots_path),
            "repoe": REPOE_SOURCE,
        },
        "count": len(assets),
        "items": assets,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--pob-root", type=Path, required=True)
    parser.add_argument("--repoe", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("data/generated/poe-assets.json"))
    parser.add_argument("--limit", type=int, default=10)
    args = parser.parse_args()

    if args.limit < 1:
        parser.error("--limit must be at least 1")

    boots_path = args.pob_root / "src/Export/Bases/boots.txt"
    result = generate(boots_path, args.repoe, args.limit)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {result['count']} boot assets: {args.output}")


if __name__ == "__main__":
    main()
