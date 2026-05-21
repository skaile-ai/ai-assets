#!/usr/bin/env python3
"""Auto-generated validator for implement.
Re-generate with: skaile-development/compile-validators target=implement

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

SKILL = "implement"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("read the target package(s) CLAUDE.md before writing any code", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("identify the correct prog-expert for the tech stack and note it in the plan", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("run tests after implementation (via test skill)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("run audit scope=diff after tests pass (gate Phase 5 on audit ≠ fail)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("run doc --mode update after any public API or structure change", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("add a devlog entry after every completed implementation", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("create a git branch before implementing (via git skill)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("Package CLAUDE.md(s) read before any implementation", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tech stack identified and prog-expert noted in plan", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Plan approved before implementation starts", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Git branch created (never commit to main)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Spec compliance review run for every task", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Full test suite passing before audit", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("audit scope=diff run and verdict ≠ fail before docs sync", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("doc --mode update run after any public API change", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Devlog entry written", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Branch finished (merge / PR / keep)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
