#!/usr/bin/env python3
"""Auto-generated validator for concept-review.
Re-generate with: /compile-validators concept-review
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-review"
QUALITY = "_concept/quality.json"

REQUIRED_QUALITY_FIELDS = ("timestamp", "score", "breakdown", "issues")
BREAKDOWN_FIELDS = (
    "structure", "frontmatter", "golden_principles",
    "cross_references", "coverage", "entropy",
)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("read all shared/contracts/ contracts before any checks",
           reason="process — cannot verify read order")

    v.skip("classify every issue by severity (CRITICAL, HIGH, MEDIUM, LOW)",
           reason="semantic — content classification")

    # quality.json must be written
    v.must("write _concept/quality.json after every run", lambda: (
        v.file_exists(QUALITY)
    ))

    # Check quality.json structure
    def check_quality_structure():
        data = v.read_json(QUALITY)
        if data is None:
            return False, f"Cannot read {QUALITY}"
        missing = [k for k in REQUIRED_QUALITY_FIELDS if k not in data]
        if missing:
            return False, f"quality.json missing fields: {', '.join(missing)}"
        breakdown = data.get("breakdown", {})
        if isinstance(breakdown, dict):
            missing_bd = [k for k in BREAKDOWN_FIELDS if k not in breakdown]
            if missing_bd:
                return False, f"quality.json breakdown missing: {', '.join(missing_bd)}"
        return True, ""

    v.must("quality.json has complete structure", check_quality_structure)

    v.skip("emit started and completed events with run_id",
           reason="process — observability")

    # ── NEVER rules ──

    v.skip("auto-fix unsafe issues in gardening mode",
           rule_type="NEVER", reason="process — gardening behavior")

    v.skip("delete files — only remove broken references",
           rule_type="NEVER", reason="process — destructive action")

    v.skip("modify postxl-schema.json model fields",
           rule_type="NEVER", reason="process — data model safety")

    # ── CHECKLIST ──
    # These are usage guidelines, not output checks
    v.skip("Run after each skill completes",
           rule_type="CHECKLIST", reason="process — usage guideline")
    v.skip("Run before app-e2e",
           rule_type="CHECKLIST", reason="process — usage guideline")
    v.skip("Run weekly",
           rule_type="CHECKLIST", reason="process — usage guideline")
    v.skip("Run before merging concept changes",
           rule_type="CHECKLIST", reason="process — usage guideline")
    v.skip("Block new pipeline steps if score < 70",
           rule_type="CHECKLIST", reason="process — usage guideline")

    return v.result()


if __name__ == "__main__":
    main(validate)
