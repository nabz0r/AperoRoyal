#!/usr/bin/env python3
"""Export the virtual-clock simulation's machine-readable JUnit tables."""
import argparse
import xml.etree.ElementTree as ET
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("xml", type=Path)
    parser.add_argument("prefix", type=Path)
    args = parser.parse_args()
    suite = ET.parse(args.xml).getroot()
    if int(suite.attrib.get("failures", "0")) or int(suite.attrib.get("errors", "0")):
        raise SystemExit("simulation failed; refusing to export")
    output = suite.find("system-out")
    if output is None or output.text is None:
        raise SystemExit("simulation output missing")
    lines = output.text.splitlines()
    cohort = next(i for i, line in enumerate(lines)
                  if line.startswith("players/topology/mode/scenario,"))
    games = next(i for i, line in enumerate(lines)
                 if line.startswith("game,rounds,"))
    args.prefix.parent.mkdir(parents=True, exist_ok=True)
    for suffix, selected in (("-cohorts.csv", lines[cohort:games]),
                             ("-games.csv", lines[games:])):
        destination = args.prefix.with_name(args.prefix.name + suffix)
        destination.write_text("\n".join(selected) + "\n", encoding="utf-8")
        print(destination)


if __name__ == "__main__":
    main()
