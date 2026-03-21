#!/usr/bin/env python3
"""Validator for the techstack skill.
Re-generate with: /compile-validators techstack
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "dev-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "techstack"
STACK = "_concept/3_blueprint/1_techstack/stack.md"

REQUIRED_FM = (
    "platform", "frontend", "ui_library", "backend", "orm",
    "database", "auth", "hosting", "package_manager", "css",
    "tech_stack_skill", "last_updated",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("produce stack.md", lambda: v.file_exists(STACK))

    v.must("include tech_stack_skill field in stack.md frontmatter", lambda: (
        v.frontmatter_has_fields(STACK, "tech_stack_skill")
    ))

    def check_tech_stack_skill_non_empty():
        fm = v.parse_frontmatter(STACK)
        if fm is None:
            return False, f"Cannot read frontmatter from {STACK}"
        val = fm.get("tech_stack_skill", "")
        if not val or str(val).strip() == "":
            return False, "tech_stack_skill is empty — must be a profile-id or 'custom'"
        return True, ""

    v.must("tech_stack_skill is set to a profile-id or 'custom'",
           check_tech_stack_skill_non_empty)

    # ── NEVER rules ──

    v.skip("use stack-specific types or output in stack.md (no SQL, no Prisma schema)",
           rule_type="NEVER", reason="semantic — content scope")

    v.skip("skip user review",
           rule_type="NEVER", reason="process — user interaction")

    # ── CHECKLIST ──

    v.checklist("stack.md exists", lambda: v.file_exists(STACK))

    v.checklist("Frontmatter has all required fields", lambda: (
        v.frontmatter_has_fields(STACK, *REQUIRED_FM)
    ))

    v.checklist("tech_stack_skill field is set", check_tech_stack_skill_non_empty)

    # Check Additional Integrations section
    def check_integrations_section():
        text = v.read_text(STACK)
        if text is None:
            return False, f"Cannot read {STACK}"
        if "Additional Integrations" not in text:
            return False, "No 'Additional Integrations' section found in stack.md"
        return True, ""

    v.checklist("Additional Integrations section present", check_integrations_section)

    # Check Trade-offs Considered section
    def check_tradeoffs_section():
        text = v.read_text(STACK)
        if text is None:
            return False, f"Cannot read {STACK}"
        if "Trade-offs Considered" not in text:
            return False, "No 'Trade-offs Considered' section found in stack.md"
        return True, ""

    v.checklist("Trade-offs Considered section present", check_tradeoffs_section)

    v.skip("User has explicitly approved the stack",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
