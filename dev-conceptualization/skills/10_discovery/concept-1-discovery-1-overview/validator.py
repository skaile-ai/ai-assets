#!/usr/bin/env python3
"""Auto-generated validator for concept-1-discovery-1-overview.
Re-generate with: /compile-validators concept-1-discovery-1-overview
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-1-discovery-1-overview"
BRIEF = "_concept/1_discovery/1_overview/brief.md"
GOALS = "_concept/1_discovery/1_overview/goals.md"
COMPARABLE = "_concept/1_discovery/1_overview/comparable.md"

REQUIRED_FRONTMATTER = (
    "status", "complexity_tier", "elevator_pitch", "audience",
    "problem", "hero_flow", "comparable_products", "last_updated",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("include all required frontmatter fields in brief.md", lambda: (
        v.frontmatter_has_fields(BRIEF, *REQUIRED_FRONTMATTER)
    ))

    v.skip("ask all clarifying questions before writing any files",
           reason="process — cannot verify conversation flow")

    v.skip("wait for explicit human approval before handing off",
           reason="process — user interaction")

    # ── NEVER rules ──

    v.skip("write features, data models, screens, brand, or tech stack",
           rule_type="NEVER", reason="boundary — requires git diff to detect new files")

    v.skip("proceed past the checkpoint without user approval",
           rule_type="NEVER", reason="process — user interaction")

    # ── CHECKLIST ──

    v.checklist("brief.md exists with all frontmatter fields", lambda: (
        v.frontmatter_has_fields(BRIEF, *REQUIRED_FRONTMATTER)
    ))

    v.checklist("goals.md exists", lambda: v.file_exists(GOALS))

    v.checklist("comparable.md exists", lambda: v.file_exists(COMPARABLE))

    v.skip("User has explicitly approved the brief",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
