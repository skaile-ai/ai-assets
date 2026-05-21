#!/usr/bin/env python3
"""Auto-generated validator for ready.
Re-generate with: skaile-development/compile-validators target=ready
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

SKILL = "ready"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──
    v.must(
        "readiness JSON artifact written",
        lambda: v.dir_not_empty("_devlog/reports", "readiness-*.json"),
    )
    v.must(
        "readiness markdown artifact written",
        lambda: v.dir_not_empty("_devlog/reports", "readiness-*.md"),
    )
    v.skip("check every required criterion — never sample", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("run real commands (not just file presence) for tests/build/lint/typecheck", rule_type="MUST",
           reason="runtime — subprocess execution")
    v.skip("classify each package by category before applying checks", rule_type="MUST",
           reason="semantic — classification logic")
    v.skip("exit non-zero on any blocker", rule_type="MUST",
           reason="runtime — exit code behavior")
    v.skip("name the exact fix command for every blocker and warning", rule_type="MUST",
           reason="semantic — report content quality")

    # ── NEVER rules ──
    v.skip("mark a package ready when any required criterion fails", rule_type="NEVER",
           reason="semantic — verdict logic")
    v.skip("run destructive commands during the check", rule_type="NEVER",
           reason="runtime — command check")
    v.skip("modify any files — report only", rule_type="NEVER",
           reason="runtime — git status inspection")

    # ── CHECKLIST ──
    v.skip("Every package classified by category", rule_type="CHECKLIST",
           reason="semantic — classification")
    v.skip("Every applicable required criterion checked (no sampling)", rule_type="CHECKLIST",
           reason="runtime — completeness")
    v.skip("Real subprocesses run for tests/build/lint/typecheck", rule_type="CHECKLIST",
           reason="runtime — execution")
    v.skip("Global criteria evaluated", rule_type="CHECKLIST",
           reason="runtime — execution")
    v.checklist(
        "JSON and markdown artifacts written",
        lambda: v.dir_not_empty("_devlog/reports", "readiness-*.json"),
    )
    v.skip("Fix command named for every blocker and warning", rule_type="CHECKLIST",
           reason="semantic — content quality")
    v.skip("Exit code reflects pass/blocked", rule_type="CHECKLIST",
           reason="runtime — exit code")

    return v.result()


if __name__ == "__main__":
    main(validate)
