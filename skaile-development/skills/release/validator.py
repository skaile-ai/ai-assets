#!/usr/bin/env python3
"""Auto-generated validator for release.
Re-generate with: skaile-development/compile-validators target=release

This validator is a compact version — all rules are classified as semantic/runtime
and skipped. When this skill starts producing structured JSON artifacts, replace the
skip() calls with structural checks.
"""
import sys
from pathlib import Path

_VLIB = None
for _base in (Path(__file__).resolve(), *Path(__file__).resolve().parents):
    for _rel in (
        ("ai-assets-skaileup", "contracts", "scripts"),
        ("ai-assets-skaileup", "skaileup-contracts", "scripts"),
        ("skaileup-shared", "scripts"),
    ):
        _cand = _base.joinpath(*_rel)
        if (_cand / "validator_lib.py").exists():
            _VLIB = str(_cand)
            break
    if _VLIB:
        break
if _VLIB:
    sys.path.insert(0, _VLIB)

from validator_lib import Validator, main

SKILL = "release"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("read conventional-commits titles and descriptions to auto-generate changelog entries", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("preserve existing CHANGELOG.md content — only prepend new version section", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("ask for user confirmation before writing version bump", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("use Keep Changelog format for CHANGELOG.md", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("determine version source: bundle.yaml for ai-assets domains, package.json for code packages", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("Current version determined from correct source (bundle.yaml vs package.json)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Commits analyzed by conventional-commits title type", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Version bump confirmed by user before writing", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("CHANGELOG.md entries grouped by type", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Existing changelog content preserved", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Version file updated", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Working tree clean before tagging", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tag created as annotated (not lightweight)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
