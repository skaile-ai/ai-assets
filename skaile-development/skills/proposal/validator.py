#!/usr/bin/env python3
"""Auto-generated validator for proposal.
Re-generate with: skaile-development/compile-validators target=proposal

This validator is a compact version — all rules are classified as semantic/runtime
and skipped. When this skill starts producing structured JSON artifacts, replace the
skip() calls with structural checks.
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "proposal"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("use the frontmatter format from references/spec-template.md", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("set author field to the person creating the spec", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("leave reviewer field empty until someone explicitly reviews", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("set status to 'draft' when creating, 'review' when requesting review", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("explore the existing codebase before designing — read CLAUDE.md files, key source files", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("propose 2-3 alternatives with trade-offs before settling on a design", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("include concrete TypeScript types/interfaces for all new APIs", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("include a dependency graph showing how new packages relate to existing ones", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("write implementation code — specs are design documents", rule_type="NEVER", reason="see SKILL.md — semantic/runtime")
    v.skip("skip the alternatives section — even if one approach is obvious, document why", rule_type="NEVER", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
