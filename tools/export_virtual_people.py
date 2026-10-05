#!/usr/bin/env python3
"""Extract reproducible virtual-person cohort tables from the JUnit XML."""
import csv
import pathlib
import sys
import xml.etree.ElementTree as ET


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: export_virtual_people.py TEST-...xml docs/data/prefix")
    root = ET.parse(sys.argv[1]).getroot()
    if root.get("failures") != "0" or root.get("errors") != "0":
        raise SystemExit("test failed; refusing to export")
    output = (root.findtext("system-out") or "").splitlines()
    if not output or not output[0].startswith("VIRTUAL PEOPLE:"):
        raise SystemExit("virtual people output missing")
    prefix = pathlib.Path(sys.argv[2])
    prefix.parent.mkdir(parents=True, exist_ok=True)
    sections = [("topology-group", 1, 14), ("players-mode", 14, 30), ("games", 30, 41)]
    for name, start, end in sections:
        lines = output[start:end]
        if len(lines) != end - start:
            raise SystemExit(f"incomplete {name} table")
        rows = list(csv.reader(lines))
        columns = len(rows[0])
        if any(len(row) != columns for row in rows):
            raise SystemExit(f"bad {name} table")
        with (prefix.parent / f"{prefix.name}-{name}.csv").open("w", newline="") as file:
            csv.writer(file, lineterminator="\n").writerows(rows)


if __name__ == "__main__":
    main()
