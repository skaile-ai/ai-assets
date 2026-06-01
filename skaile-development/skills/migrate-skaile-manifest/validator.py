#!/usr/bin/env python3
"""Auto-generated validator for migrate-skaile-manifest.
Re-generate with: skaile-development/compile-validators target=migrate-skaile-manifest

All rules are semantic/runtime (interactive disambiguation, agent judgment). When
this skill starts producing structured JSON artifacts, replace the skip() calls
with structural checks.
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

SKILL = "migrate-skaile-manifest"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)
    v.skip("ask the user to pick a publisher when bare deps are ambiguous across multiple GitHub orgs", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("ask the user for a canonical publisher when the source URL is not on github.com", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("ask the user to confirm before editing SKILL.md name: when it disagrees with the directory name", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("reject non-canonical floating pins (#main, #latest, #HEAD) at rewrite time", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("rebrand @skaile namespace dep refs to @skaile-ai", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
