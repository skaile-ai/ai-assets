#!/usr/bin/env python3
"""Lint a `.skaile-source.yaml` author-shipped inventory.

Checks (lightweight, structural; the heavy verification lives in
`skaile source verify-manifest`):

  - Every `assets[]` entry has `version:` declared
  - No wildcards in `files[]` (the inventory is meant to be explicit)
  - On `--require-sha`, every entry must declare `sha256` (used by Catalog
    publish flow which rejects un-pinned manifests)

Exit codes:
  0  All checks pass
  1  Structural issues found
  2  Usage error (file missing, YAML invalid)

Usage:
  python validator.py <path-to-.skaile-source.yaml> [--require-sha]
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

try:
    import yaml
except ImportError:
    print("ERROR: PyYAML is required (`pip install pyyaml`)", file=sys.stderr)
    sys.exit(2)


SEMVER_RE = r"^\d+\.\d+\.\d+(?:-[a-zA-Z0-9.-]+)?(?:\+[a-zA-Z0-9.-]+)?$"
SHA256_RE = r"^[a-f0-9]{64}$"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("path", type=Path, help="Path to .skaile-source.yaml")
    parser.add_argument(
        "--require-sha",
        action="store_true",
        help="Fail when any entry omits sha256 (pre-publish gate)",
    )
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

    assets = data.get("assets")
    if not isinstance(assets, list):
        print(
            "WARN: no `assets:` block (glob-mode source config — nothing to lint)",
            file=sys.stderr,
        )
        return 0

    import re

    failures: list[str] = []
    for i, entry in enumerate(assets):
        if not isinstance(entry, dict):
            failures.append(f"entry {i}: not a mapping")
            continue

        # version
        version = entry.get("version")
        if not isinstance(version, str) or not re.match(SEMVER_RE, version):
            failures.append(
                f"entry {i} ({entry.get('path', '?')}): "
                f"missing or non-semver version (got {version!r})"
            )

        # files[]: no wildcards
        files = entry.get("files") or []
        for f in files:
            if not isinstance(f, str):
                failures.append(f"entry {i}: non-string file entry {f!r}")
                continue
            if any(c in f for c in ["*", "?", "["]):
                failures.append(
                    f"entry {i} ({entry.get('path', '?')}): "
                    f"wildcard in files entry {f!r} (inventory is explicit)"
                )

        # sha256 (only when --require-sha)
        if args.require_sha:
            sha = entry.get("sha256")
            if not isinstance(sha, str) or not re.match(SHA256_RE, sha):
                failures.append(
                    f"entry {i} ({entry.get('path', '?')}): "
                    f"missing or malformed sha256 (got {sha!r})"
                )

    if failures:
        print("FAIL: structural issues:", file=sys.stderr)
        for f in failures:
            print(f"  - {f}", file=sys.stderr)
        return 1

    print(f"OK: {len(assets)} entries validated")
    return 0


if __name__ == "__main__":
    sys.exit(main())
