#!/usr/bin/env python3
"""Auto-generated validator for review.
Re-generate with: skaile-development/compile-validators target=review

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

SKILL = "review"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("read the full diff before making any findings", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("load REVIEW.md from each affected package root to apply repo-specific rules", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("categorize findings by severity (Important, Nit, Pre-existing)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("cite specific file:line for each finding", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("keep the review concise - max 5 Nits, summarize if more", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("make changes to files - this is a read-only review", rule_type="NEVER", reason="see SKILL.md — semantic/runtime")
    v.skip("report issues that linters/formatters/type-checkers already catch", rule_type="NEVER", reason="see SKILL.md — semantic/runtime")
    v.skip("report issues in files outside the diff", rule_type="NEVER", reason="see SKILL.md — semantic/runtime")
    v.skip("flag test-only code for violating production rules", rule_type="NEVER", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
