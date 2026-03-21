#!/usr/bin/env python3
"""Validator for the features skill.
Re-generate with: /compile-validators features
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "dev-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "features"
FEATURES_DIR = "_concept/2_experience/2_features"
BRIEF = "_concept/1_discovery/1_overview/brief.md"
STORIES = "_concept/2_experience/1_journeys/stories.json"

REQUIRED_FM = ("priority", "story_refs", "roles", "last_updated")


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("organize features in numbered group folders", lambda: (
        v.folders_match_pattern(FEATURES_DIR, r"^\d{2}_")
    ))

    v.must("include required frontmatter on all features", lambda: (
        v.all_files_have_frontmatter(f"{FEATURES_DIR}/**/*.md", *REQUIRED_FM)
    ))

    # MUST include story_refs on every feature
    def check_story_refs():
        files = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        if not files:
            return False, "No feature files found"
        for f in files:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm is None:
                return False, f"{rel}: no frontmatter"
            refs = fm.get("story_refs")
            if not refs or (isinstance(refs, list) and len(refs) == 0):
                return False, f"{rel}: empty story_refs — feature must trace to at least one story"
        return True, ""

    v.must("include story_refs on every feature", check_story_refs)

    # MUST leave screens[] and data_entities[] empty
    def check_empty_downstream():
        files = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        for f in files:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if not fm:
                continue
            screens = fm.get("screens", [])
            entities = fm.get("data_entities", [])
            if screens and isinstance(screens, list) and len(screens) > 0:
                return False, f"{rel}: screens[] is pre-populated — leave empty for screens skill"
            if entities and isinstance(entities, list) and len(entities) > 0:
                return False, f"{rel}: data_entities[] is pre-populated — leave empty for datamodel skill"
        return True, ""

    v.must("leave screens[] and data_entities[] empty", check_empty_downstream)

    # ── NEVER rules ──

    v.skip("write screen specs, data models, brand, or tech stack files",
           rule_type="NEVER", reason="boundary — requires git diff to detect new files")

    v.never("populate screens[] or data_entities[]", check_empty_downstream)

    # ── CHECKLIST ──

    v.checklist("brief.md was read and exists", lambda: v.file_exists(BRIEF))

    v.checklist("stories.json was read and exists", lambda: v.file_exists(STORIES))

    v.checklist("Every feature traces to at least one story", check_story_refs)

    v.checklist("Every feature has required frontmatter", lambda: (
        v.all_files_have_frontmatter(f"{FEATURES_DIR}/**/*.md", *REQUIRED_FM)
    ))

    v.checklist("screens[] and data_entities[] are empty", check_empty_downstream)

    v.checklist("Group folders use sequential NN_ numbering", lambda: (
        v.folders_match_pattern(FEATURES_DIR, r"^\d{2}_")
    ))

    v.skip("Summary table shown and approved by user",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
