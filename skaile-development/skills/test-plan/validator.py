#!/usr/bin/env python3
"""Auto-generated validator for test-plan.
Re-generate with: skaile-development/compile-validators target=test-plan
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

SKILL = "test-plan"


def validate(cwd: str, target: str | None = None) -> dict:
    v = Validator(cwd, SKILL)

    # This validator is parameterised per target package. When a target is provided,
    # we verify <target>/TEST_PLAN.md exists and has the expected frontmatter.
    target = target or "."

    # ── MUST rules ──
    v.must(
        f"TEST_PLAN.md exists at {target}/TEST_PLAN.md",
        lambda: v.file_exists(f"{target}/TEST_PLAN.md"),
    )
    v.must(
        "TEST_PLAN.md has required frontmatter fields",
        lambda: v.all_files_have_frontmatter(
            f"{target}/TEST_PLAN.md",
            "last_updated", "package", "category", "layers",
        ) if hasattr(v, "all_files_have_frontmatter") else (True, ""),
    )
    v.skip("read target CLAUDE.md before listing any unit", rule_type="MUST",
           reason="runtime — agent input discovery")
    v.skip("exclude units that already have test coverage", rule_type="MUST",
           reason="semantic — coverage mapping")
    v.skip("classify every unit into exactly one layer", rule_type="MUST",
           reason="semantic — layer classification judgment")
    v.skip("provide fixture strategy for every integration and e2e scenario", rule_type="MUST",
           reason="semantic — content completeness")
    v.skip("respect the layers input", rule_type="MUST",
           reason="runtime — input filtering")

    # ── NEVER rules ──
    v.skip("invent public API that isn't in CLAUDE.md or exported from src/", rule_type="NEVER",
           reason="semantic — content fabrication detection")
    v.skip("list units in the wrong layer", rule_type="NEVER",
           reason="semantic — layer classification judgment")

    # ── CHECKLIST ──
    v.skip("CLAUDE.md read before unit discovery", rule_type="CHECKLIST",
           reason="runtime — agent behavior")
    v.skip("Package category classified", rule_type="CHECKLIST",
           reason="semantic — classification")
    v.skip("Existing coverage scanned and excluded", rule_type="CHECKLIST",
           reason="runtime — discovery step")
    v.skip("Every unit classified into exactly one layer", rule_type="CHECKLIST",
           reason="semantic — classification")
    v.skip("Every integration/e2e scenario has a fixture strategy", rule_type="CHECKLIST",
           reason="semantic — content completeness")
    v.skip("Prior TEST_PLAN.md backed up if it existed", rule_type="CHECKLIST",
           reason="runtime — file operation")
    v.skip("Layers input respected (skipped layers have no section)", rule_type="CHECKLIST",
           reason="runtime — output filter")

    return v.result()


if __name__ == "__main__":
    main(validate)
