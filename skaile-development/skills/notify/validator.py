#!/usr/bin/env python3
"""Auto-generated validator for notify.
Re-generate with: skaile-development/compile-validators target=notify

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

SKILL = "notify"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("leave a blank line before every table header row", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("use `|:---` alignment markers in table separator rows for reliable rendering", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("use `--stdin` JSON for any message containing newlines, tables, or code blocks", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("Channel resolved (ID, not just name)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Message uses `--stdin` JSON for multi-line content", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tables have blank line before header row", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tables use `|:---` alignment markers", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Post ID saved from response (for edits/threads/reactions)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Template message matches the expected format (if template was used)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
