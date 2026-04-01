#!/usr/bin/env python3
"""Validator for the review skill.
Re-generate with: /compile-validators review
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "review"
QUALITY = "_concept/quality.json"

REQUIRED_QUALITY_FIELDS = ("timestamp", "score", "breakdown", "issues")
BREAKDOWN_FIELDS = (
    "structure", "frontmatter", "golden_principles",
    "cross_references", "coverage", "entropy",
)
ISSUES_FIELDS = ("critical", "high", "medium", "low")


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("read all skaileup-shared/contracts/ before any checks",
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
        issues = data.get("issues", {})
        if isinstance(issues, dict):
            missing_iss = [k for k in ISSUES_FIELDS if k not in issues]
            if missing_iss:
                return False, f"quality.json issues missing: {', '.join(missing_iss)}"
        return True, ""

    v.must("quality.json has complete structure (all 6 breakdown + 4 issue fields)",
           check_quality_structure)

    v.skip("emit started and completed events with run_id",
           reason="process — observability")

    # ── NEVER rules ──

    v.skip("auto-fix unsafe issues in gardening mode",
           rule_type="NEVER", reason="process — gardening behavior")

    v.skip("delete files — only remove broken references from frontmatter arrays",
           rule_type="NEVER", reason="process — destructive action")

    v.skip("modify model.json or model.dbml directly",
           rule_type="NEVER", reason="process — data model safety")

    # ── CHECKLIST ──

    v.checklist("quality.json written with all 6 breakdown fields", check_quality_structure)

    v.skip("All skaileup-shared/contracts/ read before checks",
           rule_type="CHECKLIST", reason="process — cannot verify")

    v.skip("Every issue classified by severity (CRITICAL/HIGH/MEDIUM/LOW)",
           rule_type="CHECKLIST", reason="semantic — content classification")

    v.skip("Audit: offered to fix; Gardening: reported every change made",
           rule_type="CHECKLIST", reason="process — user interaction")

    v.skip("Score < 70 flagged to user as blocking new pipeline steps",
           rule_type="CHECKLIST", reason="process — cannot verify user communication")

    v.skip("No files deleted — only broken references removed from arrays",
           rule_type="CHECKLIST", reason="process — gardening behavior")

    v.skip("model.json/model.dbml not modified",
           rule_type="CHECKLIST", reason="process — cannot verify")

    return v.result()


if __name__ == "__main__":
    main(validate)
