#!/usr/bin/env python3
"""Lint a sidecar `.skaile-source.yaml` after the curator has touched it.

Checks for common smells the curator should have addressed:

  - Every `assets[]` entry has `version:` declared (semver)
  - No wildcards in `files[]` (the inventory is explicit)
  - Every entry has `sha256` pinned (sidecars are always auto-pinned)
  - Every `requires:` entry follows `@publisher/name[@version]` syntax
  - Weak requires ranges (`*`, `>=0`) — warning, not failure

Exit codes:
  0  All checks pass
  1  Structural issues found
  2  Usage error (file missing, YAML invalid)

Usage:
  python validator.py <path-to-sidecar/.skaile-source.yaml>
  python validator.py ~/.skaile/sources/<slug>/.skaile-source.yaml
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    print("ERROR: PyYAML is required (`pip install pyyaml`)", file=sys.stderr)
    sys.exit(2)


SEMVER_RE = r"^\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?(?:\+[a-zA-Z0-9.-]+)?$"
SHA256_RE = r"^[a-f0-9]{64}$"
REQUIRES_RE = r"^@[\w-]+/[\w.-]+(?:@.+)?$"
WEAK_REQUIRES_RE = r"@(\*|>=0)$"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", type=Path, help="Path to sidecar .skaile-source.yaml")
    args = parser.parse_args()

    if not args.path.exists():
        print(f"ERROR: {args.path} does not exist", file=sys.stderr)
        return 2

    try:
        data = yaml.safe_load(args.path.read_text())
    except yaml.YAMLError as exc:
        print(f"ERROR: invalid YAML: {exc}", file=sys.stderr)
        return 2

    if not isinstance(data, dict):
        print("ERROR: root must be a YAML mapping", file=sys.stderr)
        return 2

    if data.get("version") != 2:
        print("ERROR: sidecar manifest must have `version: 2`", file=sys.stderr)
        return 1

    assets = data.get("assets")
    if not isinstance(assets, list) or len(assets) == 0:
        print(
            "ERROR: sidecar has empty `assets:` block. Run "
            "`skaile source sidecar rebuild <slug>` to populate it.",
            file=sys.stderr,
        )
        return 1

    failures: list[str] = []
    warnings: list[str] = []

    for i, entry in enumerate(assets):
        if not isinstance(entry, dict):
            failures.append(f"entry {i}: not a mapping")
            continue
        tag = entry.get("path", f"#{i}")

        # version
        version = entry.get("version")
        if not isinstance(version, str) or not re.match(SEMVER_RE, version):
            failures.append(f"{tag}: missing or non-semver version (got {version!r})")

        # files[]: no wildcards
        files = entry.get("files") or []
        if not isinstance(files, list) or len(files) == 0:
            failures.append(f"{tag}: empty `files[]` array")
        for f in files:
            if not isinstance(f, str):
                failures.append(f"{tag}: non-string file entry {f!r}")
                continue
            if any(c in f for c in ["*", "?", "["]):
                failures.append(f"{tag}: wildcard in files entry {f!r} (inventory is explicit)")

        # sha256: required on sidecars (always auto-pinned)
        sha = entry.get("sha256")
        if not isinstance(sha, str) or not re.match(SHA256_RE, sha):
            failures.append(f"{tag}: missing or malformed sha256 (got {sha!r})")

        # requires: syntax + weak-range warning
        requires = entry.get("requires") or []
        if not isinstance(requires, list):
            failures.append(f"{tag}: `requires:` must be a list")
            continue
        for r in requires:
            if not isinstance(r, str):
                failures.append(f"{tag}: non-string requires entry {r!r}")
                continue
            if not re.match(REQUIRES_RE, r):
                failures.append(f"{tag}: invalid requires syntax {r!r}")
                continue
            if re.search(WEAK_REQUIRES_RE, r):
                warnings.append(f"{tag}: weak requires range {r!r}")

    if failures:
        print("FAIL: structural issues:", file=sys.stderr)
        for f in failures:
            print(f"  - {f}", file=sys.stderr)
        if warnings:
            print("WARN:", file=sys.stderr)
            for w in warnings:
                print(f"  - {w}", file=sys.stderr)
        return 1

    if warnings:
        print(f"OK with warnings: {len(assets)} entries validated, {len(warnings)} warnings")
        for w in warnings:
            print(f"  - {w}", file=sys.stderr)
        return 0

    print(f"OK: {len(assets)} entries validated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
