#!/usr/bin/env python3
"""Auto-generated validator for concept-3-blueprint-1-techstack.
Re-generate with: /compile-validators concept-3-blueprint-1-techstack
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-3-blueprint-1-techstack"
STACK = "_concept/3_blueprint/1_techstack/stack.md"

REQUIRED_FM = (
    "status", "platform", "framework", "frontend", "ui_library",
    "backend", "orm", "database", "auth", "package_manager", "last_updated",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("use the fixed PostXL stack", lambda: (
        v.frontmatter_field_equals(STACK, "framework", "PostXL")
    ))

    v.skip("document all additional integrations identified by the user",
           reason="semantic — content completeness")

    v.must("set frontmatter status to draft until approved", lambda: (
        v.frontmatter_field_equals(STACK, "status", "draft")
    ))

    # ── NEVER rules ──

    v.skip("suggest alternative frameworks to PostXL core components",
           rule_type="NEVER", reason="semantic — content scope")

    v.skip("skip the integration consultation step",
           rule_type="NEVER", reason="process — user interaction")

    # ── CHECKLIST ──

    # Check stack.md has the PostXL definition sections
    def check_stack_content():
        text = v.read_text(STACK)
        if text is None:
            return False, f"Cannot read {STACK}"
        required_terms = ["PostXL", "React", "NestJS", "PostgreSQL"]
        missing = [t for t in required_terms if t not in text]
        if missing:
            return False, f"stack.md missing mentions of: {', '.join(missing)}"
        return True, ""

    v.checklist("stack.md contains complete PostXL stack definition", check_stack_content)

    v.checklist("Frontmatter has all required fields", lambda: (
        v.frontmatter_has_fields(STACK, *REQUIRED_FM)
    ))

    # Check Additional Integrations section exists
    def check_integrations_section():
        text = v.read_text(STACK)
        if text is None:
            return False, f"Cannot read {STACK}"
        if "integration" not in text.lower() and "Integration" not in text:
            return False, "No integrations section found in stack.md"
        return True, ""

    v.checklist("Additional integrations section reflects user consultation",
                check_integrations_section)

    v.checklist("status is draft", lambda: (
        v.frontmatter_field_equals(STACK, "status", "draft")
    ))

    return v.result()


if __name__ == "__main__":
    main(validate)
