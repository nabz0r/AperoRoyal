#!/usr/bin/env python3
"""Export the deterministic organism-inspired JUnit report without changing its values."""

import argparse
import csv
from pathlib import Path
import xml.etree.ElementTree as ET


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--xml", type=Path, default=Path(
        "app/build/test-results/testDebugUnitTest/"
        "TEST-com.aperoroyale.OrganismStressSimulationTest.xml"))
    parser.add_argument("--prefix", type=Path,
                        default=Path("docs/data/organism-10k-20261005"))
    args = parser.parse_args()
    root = ET.parse(args.xml).getroot()
    if int(root.attrib.get("failures", "0")) or int(root.attrib.get("errors", "0")):
        raise SystemExit("JUnit report contains a failure")
    output = root.findtext("system-out") or ""
    rows = list(csv.reader(output.splitlines()))
    meta = next((row for row in rows if row and row[0] == "ORG_META"), None)
    if not meta or meta[1:3] != ["parties", "10000"]:
        raise SystemExit("Expected a completed 10,000-party run")
    for marker, suffix, count in (("ORG_COHORT", "cohorts.csv", 33),
                                  ("ORG_GAME", "games.csv", 10)):
        data = [row[1:] for row in rows if row and row[0] == marker]
        if len(data) != count + 1 or any(len(row) != len(data[0]) for row in data):
            raise SystemExit(f"Incomplete or malformed {marker} section")
        target = args.prefix.parent / f"{args.prefix.name}-{suffix}"
        target.parent.mkdir(parents=True, exist_ok=True)
        with target.open("w", newline="", encoding="utf-8") as file:
            csv.writer(file, lineterminator="\n").writerows(data)
        print(target)


if __name__ == "__main__":
    main()
