#!/usr/bin/env python3
"""Auto-generated validator for quality.
Re-generate with: skaile-development/compile-validators target=quality
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "quality"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──
    v.must(
        "quality JSON artifact written",
        lambda: v.dir_not_empty("_devlog/reports", "quality-*.json"),
    )
    v.must(
        "quality markdown artifact written",
        lambda: v.dir_not_empty("_devlog/reports", "quality-*.md"),
    )
    v.skip("run steps in order: test → audit → doc → ready", rule_type="MUST",
           reason="runtime — orchestration")
    v.skip("early-exit on first hard failure", rule_type="MUST",
           reason="runtime — control flow")
    v.skip("pass artifacts forward", rule_type="MUST",
           reason="runtime — data flow")
    v.skip("respect skip list", rule_type="MUST",
           reason="runtime — input handling")
    v.skip("aggregate all four JSON artifacts", rule_type="MUST",
           reason="semantic — content merge")

    # ── NEVER rules ──
    v.skip("modify source files", rule_type="NEVER",
           reason="runtime — action check")
    v.skip("skip a step implicitly", rule_type="NEVER",
           reason="runtime — skip handling")

    # ── CHECKLIST ──
    v.skip("Steps ran in the defined order", rule_type="CHECKLIST",
           reason="runtime — orchestration")
    v.skip("Early-exit on first hard failure", rule_type="CHECKLIST",
           reason="runtime — control flow")
    v.skip("Skip list validated and explicit", rule_type="CHECKLIST",
           reason="runtime — input handling")
    v.skip("Sub-skill artifacts discovered and read", rule_type="CHECKLIST",
           reason="runtime — data flow")
    v.checklist(
        "Aggregated JSON and markdown written",
        lambda: v.dir_not_empty("_devlog/reports", "quality-*.json"),
    )
    v.skip("Final verdict matches the sub-skill outcomes", rule_type="CHECKLIST",
           reason="semantic — verdict derivation")

    return v.result()


if __name__ == "__main__":
    main(validate)
