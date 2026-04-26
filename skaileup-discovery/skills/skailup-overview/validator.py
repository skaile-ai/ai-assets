#!/usr/bin/env python3
"""Validator for the overview skill.
Re-generate with: /compile-validators overview
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "overview"
BRIEF = "_concept/discovery/brief.md"
GOALS = "_concept/discovery/goals.md"
COMPARABLE = "_concept/discovery/comparable.md"
GROUNDING = "_concept/_grounding/overview/user_input.json"

REQUIRED_FRONTMATTER = (
    "elevator_pitch", "audience", "problem",
    "hero_flow", "comparable_products", "last_updated",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("include all required frontmatter fields in brief.md", lambda: (
        v.frontmatter_has_fields(BRIEF, *REQUIRED_FRONTMATTER)
    ))

    v.must("save complexity assessment to _grounding/overview/user_input.json", lambda: (
        v.file_exists(GROUNDING) and v.json_has_field(GROUNDING, "complexity")
    ))

    v.skip("wait for explicit human approval before handing off",
           reason="process — cannot verify conversation flow")

    # ── NEVER rules ──

    v.skip("write features, data models, screens, brand, or tech stack",
           rule_type="NEVER", reason="boundary — requires git diff to detect new files")

    v.never("save complexity_tier to brief.md frontmatter", lambda: (
        not v.frontmatter_has_fields(BRIEF, "complexity_tier")
    ))

    v.never("save status to brief.md frontmatter", lambda: (
        not v.frontmatter_has_fields(BRIEF, "status")
    ))

    # ── CHECKLIST ──

    v.checklist("brief.md exists with all frontmatter fields", lambda: (
        v.frontmatter_has_fields(BRIEF, *REQUIRED_FRONTMATTER)
    ))

    v.checklist("goals.md exists", lambda: v.file_exists(GOALS))

    v.checklist("comparable.md exists", lambda: v.file_exists(COMPARABLE))

    v.checklist("complexity saved to grounding", lambda: (
        v.file_exists(GROUNDING) and v.json_has_field(GROUNDING, "complexity")
    ))

    v.skip("user has explicitly approved the brief",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
