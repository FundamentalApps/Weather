#!/usr/bin/env python3
"""Check translation completeness and Android format placeholders without a device."""
from collections import Counter
from pathlib import Path
import re
import xml.etree.ElementTree as ET

RES = Path(__file__).resolve().parents[1] / "app/src/main/res"
PLACEHOLDER = re.compile(r"%(\d+)\$([sdf])")


def read(folder):
    elements = list(ET.parse(RES / folder / "strings.xml").getroot())
    keys = [(element.tag, element.attrib["name"]) for element in elements]
    assert len(keys) == len(set(keys)), f"Duplicate resource in {folder}"
    return dict(zip(keys, elements))


def placeholders(text):
    return Counter(PLACEHOLDER.findall(text or ""))


def main():
    default = read("values")
    for language in ("en", "de", "fr", "pl", "ru", "zh"):
        folder = f"values-{language}"
        localized = read(folder)
        assert default.keys() == localized.keys(), f"Resource mismatch in {folder}"
        for (kind, name), resource in localized.items():
            original = default[(kind, name)]
            if kind == "string":
                assert placeholders(original.text) == placeholders(resource.text), (folder, name)
            elif kind == "plurals":
                quantities = {item.attrib["quantity"] for item in resource}
                required = {"other"}
                if language != "zh":
                    required.add("one")
                if language == "fr":
                    required.add("many")
                if language in ("pl", "ru"):
                    required.update(("few", "many"))
                assert required <= quantities, (folder, name, quantities)
                expected = placeholders(original.find("item[@quantity='other']").text)
                for item in resource:
                    assert expected == placeholders(item.text), (folder, name, item.attrib)
        print(f"{language}: {len(localized)} resources, placeholders and plurals OK")


if __name__ == "__main__":
    main()
