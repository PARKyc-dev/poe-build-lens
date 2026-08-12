import copy
import json
from pathlib import Path
import xml.etree.ElementTree as ET


SPIKE = Path(__file__).resolve().parents[1]
POB = SPIKE / ".cache" / "PathOfBuilding"
FIXTURES = SPIKE / ".cache" / "fixtures"
SINGLE_SOURCE = Path("spec/TestBuilds/3.13/OccVortex.lua")
MULTIPLE_SOURCE = Path("spec/TestBuilds/3.13/Mirage Archer Toxic Rain.lua")


def extract_xml(source: Path) -> ET.Element:
    text = source.read_text(encoding="utf-8")
    start = text.index("xml = [[") + len("xml = [[")
    end = text.index("]],", start)
    return ET.fromstring(text[start:end])


def normalize_skills(root: ET.Element, title: str) -> ET.Element:
    skills = root.find("Skills")
    if skills is None:
        raise ValueError("PoB export does not contain Skills")

    direct_skills = skills.findall("Skill")
    if not direct_skills:
        raise ValueError("PoB export does not contain direct Skill entries")

    skill_set = ET.Element("SkillSet", {"id": "1", "title": title})
    for skill in direct_skills:
        skills.remove(skill)
        skill_set.append(skill)
    skills.append(skill_set)
    return skill_set


def lock_values() -> dict[str, str]:
    return dict(
        line.split("=", 1)
        for line in (SPIKE / "pob.lock").read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#")
    )


def write_xml(root: ET.Element, target: Path) -> None:
    ET.indent(root, space="  ")
    ET.ElementTree(root).write(target, encoding="utf-8", xml_declaration=True)


def build_fixtures() -> None:
    FIXTURES.mkdir(parents=True, exist_ok=True)

    single = extract_xml(POB / SINGLE_SOURCE)
    normalize_skills(single, "Fixture SkillSet A")
    write_xml(single, FIXTURES / "single.xml")

    multiple = extract_xml(POB / MULTIPLE_SOURCE)
    skill_set = normalize_skills(multiple, "Fixture SkillSet A")
    second_skill_set = copy.deepcopy(skill_set)
    second_skill_set.set("id", "2")
    second_skill_set.set("title", "Fixture SkillSet B")
    skills = multiple.find("Skills")
    if skills is None:
        raise ValueError("PoB export does not contain Skills")
    skills.append(second_skill_set)
    skills.set("activeSkillSet", "2")

    items = multiple.find("Items")
    if items is None:
        raise ValueError("PoB export does not contain Items")
    first_item_set = items.find("ItemSet[@id='1']")
    if first_item_set is None:
        raise ValueError("PoB export does not contain ItemSet 1")
    first_item_set.set("title", "Fixture ItemSet A")
    second_item_set = copy.deepcopy(first_item_set)
    second_item_set.set("id", "2")
    second_item_set.set("title", "Fixture ItemSet B")
    items.append(second_item_set)
    items.set("activeItemSet", "2")
    write_xml(multiple, FIXTURES / "multiple.xml")

    values = lock_values()
    (FIXTURES / "manifest.json").write_text(
        json.dumps(
            {
                "pobCommit": values["POB_COMMIT"],
                "singleSource": str(SINGLE_SOURCE),
                "multipleSource": str(MULTIPLE_SOURCE),
            },
            indent=2,
        )
        + "\n",
        encoding="utf-8",
    )


if __name__ == "__main__":
    build_fixtures()
