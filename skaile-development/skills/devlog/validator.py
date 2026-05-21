#!/usr/bin/env python3
"""Auto-generated validator for devlog.
Re-generate with: skaile-development/compile-validators target=devlog

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

SKILL = "devlog"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("create each entry as a separate file in _devlog/entries/", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("prepend a link to DEVLOG.md index (newest entries first)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("use plain language — no jargon, no implementation detail dumps", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("include: what changed, why, affected packages, implications", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("write reports for architectural/conceptual changes", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("DEVLOG.md exists (created if needed)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("New entry prepended at top (newest first)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Entry has: what changed, why, affected, implications", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Plain language — no internal jargon", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Detailed report generated when report_needed=yes", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Report linked from DEVLOG.md entry", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Changes committed with 'docs(devlog): ...' message", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
