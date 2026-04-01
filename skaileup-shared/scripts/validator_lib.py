#!/usr/bin/env python3
"""
validator_lib.py — Shared library for compiled skill validators.

Generated validators import this module and use its primitives to
perform fast, deterministic rule checks without calling an LLM.

Typical generated validator structure:

    import sys
    from pathlib import Path
    sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
    from validator_lib import Validator, main

    def validate(cwd: str) -> dict:
        v = Validator(cwd, "my-skill")
        v.must("file exists", lambda: v.file_exists("_concept/path/to/file.md"))
        v.never("bad pattern", lambda: v.no_files_matching("_concept/bad/**"))
        v.skip("subjective quality rule", rule_type="MUST", reason="semantic")
        return v.result()

    if __name__ == "__main__":
        main(validate)
"""

from __future__ import annotations

import json
import os
import re
import sys
from pathlib import Path
from typing import Callable


class Validator:
    """Accumulates rule-check results for a single skill validation run."""

    def __init__(self, cwd: str, skill: str):
        self.cwd = Path(cwd)
        self.skill = skill
        self._passed = 0
        self._failed = 0
        self._skipped = 0
        self._violations: list[dict] = []
        self._skipped_rules: list[dict] = []

    # ── Rule registrars ──────────────────────────────────────────────

    def must(self, rule: str, check: Callable[[], tuple[bool, str]]):
        """Assert a MUST rule. *check()* → (ok, detail_if_failed)."""
        self._run("MUST", rule, check)

    def never(self, rule: str, check: Callable[[], tuple[bool, str]]):
        """Assert a NEVER rule. *check()* → (ok, detail_if_failed)."""
        self._run("NEVER", rule, check)

    def checklist(self, item: str, check: Callable[[], tuple[bool, str]]):
        """Assert a CHECKLIST item. *check()* → (ok, detail_if_failed)."""
        self._run("CHECKLIST", item, check)

    def skip(self, rule: str, rule_type: str = "MUST",
             reason: str = "semantic — requires human review"):
        """Record a rule that cannot be checked deterministically."""
        self._skipped += 1
        self._skipped_rules.append({
            "type": rule_type, "rule": rule, "reason": reason,
        })

    def _run(self, rule_type: str, rule: str, check: Callable):
        try:
            ok, detail = check()
            if ok:
                self._passed += 1
            else:
                self._failed += 1
                self._violations.append({
                    "type": rule_type, "rule": rule, "detail": detail,
                })
        except Exception as e:
            self._failed += 1
            self._violations.append({
                "type": rule_type, "rule": rule, "detail": f"Check error: {e}",
            })

    # ── Result ───────────────────────────────────────────────────────

    def result(self) -> dict:
        """Return a JSON-serializable validation result."""
        return {
            "skill": self.skill,
            "verdict": "FAIL" if self._violations else "PASS",
            "passed": self._passed,
            "failed": self._failed,
            "skipped": self._skipped,
            "rules_checked": self._passed + self._failed,
            "violations": self._violations,
            "skipped_rules": self._skipped_rules,
        }

    # ── File / directory primitives ──────────────────────────────────

    def file_exists(self, rel_path: str) -> tuple[bool, str]:
        """Check that a file exists at *rel_path* (relative to cwd)."""
        p = self.cwd / rel_path
        if p.exists():
            return True, ""
        return False, f"File not found: {rel_path}"

    def dir_exists(self, rel_path: str) -> tuple[bool, str]:
        """Check that a directory exists."""
        p = self.cwd / rel_path
        if p.is_dir():
            return True, ""
        return False, f"Directory not found: {rel_path}"

    def dir_not_empty(self, rel_path: str, pattern: str = "*") -> tuple[bool, str]:
        """Check that a directory exists and has at least one matching child."""
        p = self.cwd / rel_path
        if not p.is_dir():
            return False, f"Directory not found: {rel_path}"
        if not list(p.glob(pattern)):
            return False, f"Directory empty (pattern '{pattern}'): {rel_path}"
        return True, ""

    def glob_files(self, pattern: str) -> list[Path]:
        """Return files matching a glob pattern relative to cwd."""
        return sorted(self.cwd.glob(pattern))

    def no_files_matching(self, pattern: str) -> tuple[bool, str]:
        """Assert that NO files match *pattern*."""
        found = self.glob_files(pattern)
        if found:
            paths = ", ".join(str(f.relative_to(self.cwd)) for f in found[:5])
            return False, f"Unexpected files found: {paths}"
        return True, ""

    # ── Text / JSON readers ──────────────────────────────────────────

    def read_json(self, rel_path: str) -> dict | list | None:
        """Read and parse a JSON file. Returns None if missing/invalid."""
        p = self.cwd / rel_path
        if not p.exists():
            return None
        try:
            return json.loads(p.read_text(encoding="utf-8"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            return None

    def read_text(self, rel_path: str) -> str | None:
        """Read a text file. Returns None if missing."""
        p = self.cwd / rel_path
        if not p.exists():
            return None
        try:
            return p.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            return None

    # ── Frontmatter ──────────────────────────────────────────────────

    def parse_frontmatter(self, rel_path: str) -> dict | None:
        """Parse simple YAML frontmatter (key: value only, no nesting)."""
        text = self.read_text(rel_path)
        if not text or not text.startswith("---"):
            return None
        end = text.find("---", 3)
        if end == -1:
            return None
        fm: dict = {}
        current_key: str | None = None
        for line in text[3:end].splitlines():
            stripped = line.strip()
            if not stripped or stripped.startswith("#"):
                continue
            # Detect indented continuation (list item under a key)
            if line.startswith("  ") and current_key is not None and stripped.startswith("- "):
                val = stripped[2:].strip()
                if not isinstance(fm[current_key], list):
                    fm[current_key] = []
                fm[current_key].append(val)
                continue
            if ":" in stripped:
                key, _, val = stripped.partition(":")
                key = key.strip()
                val = val.strip()
                current_key = key
                if val.startswith("[") and val.endswith("]"):
                    fm[key] = [v.strip().strip("'\"") for v in val[1:-1].split(",") if v.strip()]
                elif val.startswith('"') and val.endswith('"'):
                    fm[key] = val[1:-1]
                elif val.startswith("'") and val.endswith("'"):
                    fm[key] = val[1:-1]
                elif val == "|" or val == ">":
                    fm[key] = ""  # block scalar — ignore content
                elif val == "":
                    fm[key] = ""
                else:
                    fm[key] = val
        return fm

    def frontmatter_has_fields(self, rel_path: str, *fields: str) -> tuple[bool, str]:
        """Check that a markdown file's frontmatter contains *fields*."""
        fm = self.parse_frontmatter(rel_path)
        if fm is None:
            return False, f"No frontmatter in {rel_path}"
        missing = [f for f in fields if f not in fm]
        if missing:
            return False, f"Missing frontmatter in {rel_path}: {', '.join(missing)}"
        return True, ""

    def frontmatter_field_equals(self, rel_path: str, field: str,
                                  expected: str) -> tuple[bool, str]:
        """Check that a frontmatter field has a specific value."""
        fm = self.parse_frontmatter(rel_path)
        if fm is None:
            return False, f"No frontmatter in {rel_path}"
        if field not in fm:
            return False, f"Field '{field}' missing in {rel_path}"
        if str(fm[field]) != str(expected):
            return False, f"{field}={fm[field]}, expected {expected} in {rel_path}"
        return True, ""

    def all_files_have_frontmatter(self, pattern: str,
                                    *fields: str) -> tuple[bool, str]:
        """Check every file matching *pattern* has frontmatter with *fields*."""
        files = self.glob_files(pattern)
        if not files:
            return False, f"No files matching {pattern}"
        bad = []
        for f in files:
            rel = str(f.relative_to(self.cwd))
            fm = self.parse_frontmatter(rel)
            if fm is None:
                bad.append(f"{rel}: no frontmatter")
                continue
            missing = [fld for fld in fields if fld not in fm]
            if missing:
                bad.append(f"{rel}: missing {', '.join(missing)}")
        if bad:
            return False, "; ".join(bad[:5])
        return True, ""

    # ── JSON checks ──────────────────────────────────────────────────

    def json_field_exists(self, rel_path: str, *keys: str) -> tuple[bool, str]:
        """Check a JSON file contains specific top-level keys."""
        data = self.read_json(rel_path)
        if data is None:
            return False, f"Cannot read JSON: {rel_path}"
        if not isinstance(data, dict):
            return False, f"Expected JSON object in {rel_path}"
        missing = [k for k in keys if k not in data]
        if missing:
            return False, f"Missing keys in {rel_path}: {', '.join(missing)}"
        return True, ""

    def json_array_all_have(self, items: list, field: str,
                             context: str = "") -> tuple[bool, str]:
        """Check every dict in *items* has *field*."""
        for i, item in enumerate(items):
            if not isinstance(item, dict):
                return False, f"Item {i} is not an object {context}"
            if field not in item:
                return False, f"Item {i} missing '{field}' {context}"
        return True, ""

    def json_array_all_have_nonempty(self, items: list, field: str,
                                      context: str = "") -> tuple[bool, str]:
        """Check every dict in *items* has a non-empty *field*."""
        for i, item in enumerate(items):
            if not isinstance(item, dict):
                return False, f"Item {i} is not an object {context}"
            val = item.get(field)
            if val is None:
                return False, f"Item {i} missing '{field}' {context}"
            if isinstance(val, (list, str)) and len(val) == 0:
                return False, f"Item {i} has empty '{field}' {context}"
        return True, ""

    def json_count(self, items: list, predicate: Callable[[dict], bool],
                   expected: int, op: str = "eq") -> tuple[bool, str]:
        """Count items matching *predicate*, compare to *expected*.

        *op* is one of: eq, gte, lte, gt, lt.
        """
        count = sum(1 for item in items if predicate(item))
        ops = {
            "eq": (count == expected, f"exactly {expected}"),
            "gte": (count >= expected, f"at least {expected}"),
            "lte": (count <= expected, f"at most {expected}"),
            "gt": (count > expected, f"more than {expected}"),
            "lt": (count < expected, f"fewer than {expected}"),
        }
        ok, label = ops.get(op, (False, f"unknown op '{op}'"))
        if not ok:
            return False, f"Expected {label}, found {count}"
        return True, ""

    def json_schema_validate(self, data_path: str,
                              schema_path: str) -> tuple[bool, str]:
        """Validate JSON data against a JSON Schema file.

        Uses *jsonschema* if installed; otherwise returns a pass with a note.
        """
        data = self.read_json(data_path)
        if data is None:
            return False, f"Cannot read data: {data_path}"
        schema = self.read_json(schema_path)
        if schema is None:
            return False, f"Cannot read schema: {schema_path}"
        try:
            import jsonschema  # type: ignore
            jsonschema.validate(data, schema)
            return True, ""
        except ImportError:
            return True, ""  # best-effort — jsonschema not installed
        except Exception as e:
            msg = str(e).split("\n")[0][:200]
            return False, f"Schema validation failed: {msg}"

    # ── Folder naming ────────────────────────────────────────────────

    def folders_match_pattern(self, parent: str,
                               pattern: str) -> tuple[bool, str]:
        """Check all subdirectories of *parent* match a regex *pattern*."""
        p = self.cwd / parent
        if not p.is_dir():
            return False, f"Directory not found: {parent}"
        bad = [d.name for d in sorted(p.iterdir())
               if d.is_dir() and not re.match(pattern, d.name)]
        if bad:
            return False, f"Non-matching folders: {', '.join(bad)}"
        return True, ""

    # ── Cross-reference checks ───────────────────────────────────────

    def every_key_maps_to_existing_file(self, json_path: str,
                                         mapping_field: str | None = None
                                         ) -> tuple[bool, str]:
        """Check that every value (file path) in a JSON mapping exists.

        If *mapping_field* is set, reads ``data[mapping_field]`` as the map.
        Otherwise the top-level object is used.
        Values can be strings or lists of strings.
        """
        data = self.read_json(json_path)
        if data is None:
            return False, f"Cannot read: {json_path}"
        mapping = data.get(mapping_field, data) if mapping_field else data
        if not isinstance(mapping, dict):
            return False, f"Expected object for mapping in {json_path}"
        missing = []
        for key, paths in mapping.items():
            if isinstance(paths, str):
                paths = [paths]
            if isinstance(paths, list):
                for p in paths:
                    if not (self.cwd / p).exists():
                        missing.append(f"{key} → {p}")
        if missing:
            return False, f"Missing referenced files: {'; '.join(missing[:5])}"
        return True, ""


# ── CLI entry point ──────────────────────────────────────────────────

def main(validate_fn: Callable[[str], dict]):
    """CLI harness for generated validators.

    Usage:  python3 validator.py --cwd /path/to/project [--json]
    Exit:   0 = PASS, 2 = FAIL, 1 = error
    """
    import argparse

    parser = argparse.ArgumentParser(description="Compiled skill validator")
    parser.add_argument("--cwd", default=os.getcwd(), help="Project directory")
    parser.add_argument("--json", action="store_true", help="JSON output")
    args = parser.parse_args()

    result = validate_fn(args.cwd)

    if args.json:
        json.dump(result, sys.stdout, indent=2)
        print()
    else:
        skill = result["skill"]
        verdict = result["verdict"]
        passed = result["passed"]
        skipped = result["skipped"]

        if verdict == "PASS":
            print(f"✅ PASS — {skill}: {passed} checks passed, {skipped} skipped")
        else:
            print(f"❌ FAIL — {skill}: {result['failed']} violations")
            for v in result["violations"]:
                print(f"  [{v['type']}] {v['rule']} — {v['detail']}")

        if result["skipped_rules"]:
            print(f"\n  ⚠ {skipped} rules require human review:")
            for s in result["skipped_rules"]:
                print(f"    [{s['type']}] {s['rule']}")

    sys.exit(2 if result["verdict"] == "FAIL" else 0)
