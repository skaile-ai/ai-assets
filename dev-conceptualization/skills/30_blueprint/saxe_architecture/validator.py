#!/usr/bin/env python3
"""Auto-generated validator for concept-3-blueprint-2-architecture.
Re-generate with: /compile-validators concept-3-blueprint-2-architecture
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-3-blueprint-2-architecture"
ARCH = "_concept/3_blueprint/2_architecture/architecture.md"

REQUIRED_FM = ("status", "apps", "custom_modules", "protocols",
               "external_integrations", "last_updated")

# The six required sections
REQUIRED_SECTIONS = (
    "overview", "module", "data flow", "protocol", "integration", "infrastructure",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("start from PostXL defaults and extend only where features demand",
           reason="semantic — architecture approach")

    # Check all six sections present
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

    v.skip("document PostXL defaults as baseline in every section",
           reason="semantic — content completeness")

    v.must("include all required frontmatter fields", lambda: (
        v.frontmatter_has_fields(ARCH, *REQUIRED_FM)
    ))

    # ── NEVER rules ──

    v.skip("reinvent standard PostXL modules",
           rule_type="NEVER", reason="semantic — architecture approach")

    v.skip("skip external integration error handling or credential management docs",
           rule_type="NEVER", reason="semantic — content completeness")

    # ── CHECKLIST ──

    v.checklist("architecture.md exists with all frontmatter fields", lambda: (
        v.frontmatter_has_fields(ARCH, *REQUIRED_FM)
    ))

    v.checklist("All six sections present", check_sections)

    v.skip("PostXL defaults documented as baseline in every section",
           rule_type="CHECKLIST", reason="semantic — content completeness")

    v.skip("Custom modules have purpose and dependency listed",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.skip("Non-standard protocols have endpoints, message types, lifecycle, error handling",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.skip("External integrations have API/SDK, data exchanged, error handling, credentials",
           rule_type="CHECKLIST", reason="semantic — content depth")

    v.skip("User has explicitly approved the architecture",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
