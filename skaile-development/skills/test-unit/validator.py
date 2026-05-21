#!/usr/bin/env python3
"""Auto-generated validator for test-unit.
Re-generate with: skaile-development/compile-validators target=test-unit
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

SKILL = "test-unit"


def validate(cwd: str, target: str | None = None) -> dict:
    v = Validator(cwd, SKILL)
    target = target or "."

    # ── MUST rules ──
    v.must(
        f"test config present at {target} (vitest or jest)",
        lambda: (
            (Path(cwd) / target / "vitest.config.ts").exists()
            or (Path(cwd) / target / "jest.config.ts").exists()
            or (Path(cwd) / target / "jest.config.js").exists()
            or (Path(cwd) / target / "jest.config.cjs").exists(),
            "no vitest.config.ts or jest.config.* found"
        ),
    )
    v.must(
        f"tests directory not empty at {target}",
        lambda: v.dir_not_empty(f"{target}/tests", "**/*.test.ts")
                if (Path(cwd) / target / "tests").exists()
                else v.dir_not_empty(f"{target}/test", "**/*.test.ts"),
    )
    v.skip("read CLAUDE.md before generating any test", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("detect and respect the framework already in use", rule_type="MUST",
           reason="runtime — detection logic")
    v.skip("read 2–3 existing test files to match conventions", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("prefer TEST_PLAN.md as input when it exists", rule_type="MUST",
           reason="runtime — input preference")
    v.skip("verify generated tests execute before completing", rule_type="MUST",
           reason="runtime — subprocess invocation")
    v.skip("place test files per package convention", rule_type="MUST",
           reason="semantic — convention matching")

    # ── NEVER rules ──
    v.skip("modify existing test files", rule_type="NEVER",
           reason="runtime — behavior check")
    v.skip("test implementation details", rule_type="NEVER",
           reason="semantic — content judgment")
    v.skip("use mocks for pure logic", rule_type="NEVER",
           reason="semantic — content judgment")
    v.skip("hide a real bug by weakening an assertion", rule_type="NEVER",
           reason="semantic — content judgment")

    # ── CHECKLIST ──
    v.skip("CLAUDE.md read before generation", rule_type="CHECKLIST",
           reason="runtime — agent behavior")
    v.skip("Framework detected and respected", rule_type="CHECKLIST",
           reason="runtime — detection")
    v.skip("Existing test patterns matched", rule_type="CHECKLIST",
           reason="semantic — convention matching")
    v.skip("Infrastructure scaffolded if missing", rule_type="CHECKLIST",
           reason="runtime — conditional scaffolding")
    v.skip("One test file per testable module", rule_type="CHECKLIST",
           reason="semantic — generation output structure")
    v.skip("Generated tests executed and passing", rule_type="CHECKLIST",
           reason="runtime — subprocess")
    v.skip("Deferred units flagged with reason", rule_type="CHECKLIST",
           reason="semantic — report completeness")

    return v.result()


if __name__ == "__main__":
    main(validate)
