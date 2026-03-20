#!/usr/bin/env python3
"""Auto-generated validator for concept (orchestrator).
Re-generate with: /compile-validators concept
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept"
PLANS = "PLANS.md"
LEARNINGS = "LEARNINGS.md"
MANIFEST = "_concept/.snapshots/manifest.json"
BRIEF = "_concept/1_discovery/1_overview/brief.md"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("run prerequisites check from shared/contracts/prerequisites.md",
           reason="process — runtime check")

    v.skip("follow the Checkpoint Protocol after every phase",
           reason="process — orchestration flow")

    # Check snapshots exist after approvals
    def check_snapshots():
        data = v.read_json(MANIFEST)
        if data is None:
            return False, "No _concept/.snapshots/manifest.json found"
        snapshots = data.get("snapshots", [])
        if not snapshots:
            return False, "manifest.json has no snapshots"
        return True, ""

    v.must("create snapshots after every approval", check_snapshots)

    v.must("update PLANS.md progress and decisions at every checkpoint", lambda: (
        v.file_exists(PLANS)
    ))

    v.skip("execute sub-skills completely — never partial runs",
           reason="process — orchestration")

    v.must("log learnings to LEARNINGS.md", lambda: v.file_exists(LEARNINGS))

    # Check validation results in manifest
    def check_validation_in_manifest():
        data = v.read_json(MANIFEST)
        if data is None:
            return False, "Cannot read manifest.json"
        snapshots = data.get("snapshots", [])
        missing_validation = [s["id"] for s in snapshots
                              if "validation" not in s and isinstance(s, dict)]
        if missing_validation:
            return False, f"Snapshots without validation: {', '.join(missing_validation[:3])}"
        return True, ""

    v.must("persist validation results in manifest.json", check_validation_in_manifest)

    v.skip("run skill rule validation after each sub-skill completes",
           reason="process — hook integration")

    # ── NEVER rules ──

    v.skip("skip phases or continue without approval",
           rule_type="NEVER", reason="process — orchestration")

    v.skip("proceed to Phase 3 until Phase 2 is approved",
           rule_type="NEVER", reason="process — orchestration")

    # ── CHECKLIST ──

    # PLANS.md exists with progress checkboxes
    def check_plans_progress():
        text = v.read_text(PLANS)
        if text is None:
            return False, "PLANS.md not found"
        if "- [x]" not in text and "- [ ]" not in text:
            return False, "PLANS.md has no progress checkboxes"
        return True, ""

    v.checklist("PLANS.md exists with all progress checkboxes", check_plans_progress)

    v.checklist("All step snapshots exist in _concept/.snapshots/", check_snapshots)

    v.checklist("manifest.json is up to date with validation results",
                check_validation_in_manifest)

    v.checklist("Every sub-skill has a validation entry", check_validation_in_manifest)

    # Check implementation plan appended
    def check_impl_plan():
        text = v.read_text(PLANS)
        if text is None:
            return False, "PLANS.md not found"
        if "implementation" not in text.lower() and "Implementation" not in text:
            return False, "No implementation plan section in PLANS.md"
        return True, ""

    v.checklist("Implementation plan section appended to PLANS.md", check_impl_plan)

    v.skip("Every decision logged in PLANS.md Decisions section",
           rule_type="CHECKLIST", reason="semantic — content completeness")

    return v.result()


if __name__ == "__main__":
    main(validate)
