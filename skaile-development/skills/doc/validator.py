#!/usr/bin/env python3
"""Auto-generated validator for doc.
Re-generate with: skaile-development/compile-validators target=doc

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

SKILL = "doc"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("read `skaileup-shared/contracts/doc_tracking.md` and `references/doc_tiers.md` before starting any operation", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("run the appropriate helper script first and consume its output before writing anything", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("verify all claims against actual source code before rewriting or creating documentation", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("use monorepo-relative paths in all `_sources` frontmatter entries", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("set `_based_on_commit` to the current HEAD SHA and `_last_synced` to today's date on all Starlight pages", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("preserve existing frontmatter fields (title, description, badge, _sources) when updating pages", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("delegate `_devlog` entries to `devlog` — never write devlog content directly", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
