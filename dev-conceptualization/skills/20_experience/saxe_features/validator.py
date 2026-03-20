#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-2-features.
Re-generate with: /compile-validators concept-2-experience-2-features
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-2-features"
FEATURES_DIR = "_concept/2_experience/2_features"
BRIEF = "_concept/1_discovery/1_overview/brief.md"
STORIES = "_concept/2_experience/1_journeys/stories.json"

REQUIRED_FM = ("status", "priority", "roles", "last_updated")


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("organize features in numbered group folders", lambda: (
        v.folders_match_pattern(FEATURES_DIR, r"^\d{2}_")
    ))

    v.must("include required frontmatter on all features", lambda: (
        v.all_files_have_frontmatter(f"{FEATURES_DIR}/**/*.md", *REQUIRED_FM)
    ))

    v.skip("focus on custom business logic — PostXL handles standard CRUD",
           reason="semantic — content relevance")

    # Check status: draft on all features
    def check_draft_status():
        files = v.glob_files(f"{FEATURES_DIR}/**/*.md")
        if not files:
            return False, "No feature files found"
        for f in files:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm and fm.get("status") != "draft":
                return False, f"{rel}: status is '{fm.get('status')}', expected 'draft'"
        return True, ""

    v.must("set status: draft on all new features", check_draft_status)

    # ── NEVER rules ──

    v.skip("write screen specs, data models, brand, or tech stack files",
           rule_type="NEVER", reason="boundary — requires git diff")

    v.skip("specify basic CRUD that PostXL provides out of the box",
           rule_type="NEVER", reason="semantic — content scope")

    # ── CHECKLIST ──

    v.checklist("brief.md was read and exists", lambda: v.file_exists(BRIEF))

    v.checklist("stories.json was read and exists", lambda: v.file_exists(STORIES))

    # Every feature traces to at least one story
    def check_story_refs():
        files = v.glob_files(f"{FEATURES_DIR}/**/*.md")
        if not files:
            return False, "No feature files found"
        for f in files:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm is None:
                return False, f"{rel}: no frontmatter"
            refs = fm.get("story_refs")
            if not refs or (isinstance(refs, list) and len(refs) == 0):
                return False, f"{rel}: empty story_refs"
        return True, ""

    v.checklist("Every feature traces to at least one story", check_story_refs)

    v.checklist("Every feature has valid frontmatter", lambda: (
        v.all_files_have_frontmatter(f"{FEATURES_DIR}/**/*.md", *REQUIRED_FM)
    ))

    v.skip("Features focus on custom logic, not PostXL-provided CRUD",
           rule_type="CHECKLIST", reason="semantic — content scope")

    v.checklist("Group folders use sequential NN_ numbering", lambda: (
        v.folders_match_pattern(FEATURES_DIR, r"^\d{2}_")
    ))

    v.skip("Summary table shown and approved by user",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
