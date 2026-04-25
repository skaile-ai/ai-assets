#!/usr/bin/env python3
"""Auto-generated validator for faq.
Re-generate with: skaile-development/compile-validators target=faq

This validator is a compact version — all rules are classified as semantic/runtime
and skipped. When this skill starts producing structured JSON artifacts, replace the
skip() calls with structural checks.
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "faq"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("resolve the target FAQ file from context before checking for duplicates", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("check existing faq.md for duplicate or near-duplicate questions before proposing", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("check existing package docs to verify the answer isn't already well-covered", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("present the formatted entry to the user and wait for explicit approval before writing", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("append new entries at the end of faq.md (preserve existing entries)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("use the simple Q&A format (### heading = question, body = answer)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("Target FAQ file resolved from context", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Checked faq.md for duplicate entries", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Checked package docs for existing coverage", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("All three FAQ-worthiness criteria evaluated", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Entry formatted as simple ### Q / answer pair", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("User explicitly approved the entry before writing", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Entry appended (not prepended) to faq.md", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Existing entries not modified", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
