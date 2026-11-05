#!/usr/bin/env python3
"""Builds a Markdown coverage report from JaCoCo CSV reports (target/site/jacoco/jacoco.csv).

Usage: coverage-report.py CURRENT_CSV [BASE_CSV]

Coverage is line-based, aggregated per package. If a report of main is given (BASE_CSV), the report shows the
difference per package and in total.
"""
import csv
import os
import sys
from collections import defaultdict

PACKAGE_PREFIX = "com.dreitier.sunspecexporter"
MARKER = "<!-- coverage-report -->"


def parse(path):
    """Returns {package: (covered lines, total lines)} or None if the file doesn't exist."""
    if not path or not os.path.isfile(path):
        return None
    packages = defaultdict(lambda: [0, 0])
    with open(path, encoding="utf-8", newline="") as f:
        for row in csv.DictReader(f):
            package = row["PACKAGE"][len(PACKAGE_PREFIX):].lstrip(".") or "."
            covered, missed = int(row["LINE_COVERED"]), int(row["LINE_MISSED"])
            packages[package][0] += covered
            packages[package][1] += covered + missed
    return {p: tuple(v) for p, v in packages.items()}


def percent(covered_total):
    covered, total = covered_total
    return 100.0 * covered / total if total else 0.0


def total(packages):
    return (sum(c for c, _ in packages.values()), sum(t for _, t in packages.values()))


def delta(current, base):
    d = current - base
    if abs(d) < 0.05:
        return "±0.0"
    return f"{'▲' if d > 0 else '▼'} {d:+.1f}"


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    current = parse(sys.argv[1])
    if current is None:
        sys.exit(f"coverage report {sys.argv[1]} not found")
    base = parse(sys.argv[2]) if len(sys.argv) > 2 else None

    cur_total = percent(total(current))
    lines = [MARKER, "### Test coverage", ""]
    if base:
        base_total = percent(total(base))
        lines.append(f"**{cur_total:.1f}%** of lines covered (main: {base_total:.1f}%, {delta(cur_total, base_total)})")
        lines += ["", "| Package | main | this PR | Δ |", "|---|---:|---:|---:|"]
        for package in sorted(set(current) | set(base)):
            if package not in current:
                lines.append(f"| `{package}` | {percent(base[package]):.1f}% | removed | |")
            elif package not in base:
                lines.append(f"| `{package}` | new | {percent(current[package]):.1f}% | |")
            else:
                c, b = percent(current[package]), percent(base[package])
                lines.append(f"| `{package}` | {b:.1f}% | {c:.1f}% | {delta(c, b)} |")
    else:
        lines.append(f"**{cur_total:.1f}%** of lines covered (no coverage of main available for comparison)")
        lines += ["", "| Package | Coverage |", "|---|---:|"]
        for package in sorted(current):
            lines.append(f"| `{package}` | {percent(current[package]):.1f}% |")
    print("\n".join(lines))


if __name__ == "__main__":
    main()
