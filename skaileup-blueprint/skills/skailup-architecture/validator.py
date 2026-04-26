#!/usr/bin/env python3
"""Validator for the architecture skill.
Re-generate with: /compile-validators architecture
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "architecture"
ARCH = "_concept/blueprint/architecture.md"

REQUIRED_FM = ("apps", "custom_modules", "protocols", "external_integrations", "last_updated")

# The six required sections (case-insensitive substring match)
REQUIRED_SECTIONS = (
    "overview", "backend", "data flow", "protocol", "integration", "infrastructure",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    def check_sections():
        text = v.read_text(ARCH)
        if text is None:
            return False, f"Cannot read {ARCH}"
        text_lower = text.lower()
        missing = [s for s in REQUIRED_SECTIONS if s not in text_lower]
        if missing:
            return False, f"Missing sections in architecture.md: {', '.join(missing)}"
        return True, ""

    v.must("include all six sections in architecture.md", check_sections)

    v.must("include all required frontmatter fields", lambda: (
        v.frontmatter_has_fields(ARCH, *REQUIRED_FM)
    ))

    # ── NEVER rules ──

    v.skip("skip external integration error handling or credential management docs",
           rule_type="NEVER", reason="semantic — content depth check")

    v.skip("invent stack defaults without reading stack.md",
           rule_type="NEVER", reason="semantic — architecture approach")

    # ── CHECKLIST ──

    v.checklist("architecture.md exists with all frontmatter fields", lambda: (
        v.frontmatter_has_fields(ARCH, *REQUIRED_FM)
    ))

    v.checklist("All six sections present", check_sections)

    v.skip("Every section shows stack default baseline before extensions",
           rule_type="CHECKLIST", reason="semantic — content completeness")

    v.skip("Custom modules have purpose and dependencies listed",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.skip("Non-standard protocols document endpoints, message types, lifecycle, error handling",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.skip("External integrations document API/SDK, data exchanged, error handling, credentials",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.skip("User has explicitly approved the architecture",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
