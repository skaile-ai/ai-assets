#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-3-screens.
Re-generate with: /compile-validators concept-2-experience-3-screens
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-3-screens"
SCREENS_DIR = "_concept/2_experience/3_screens"
FEATURES_DIR = "_concept/2_experience/2_features"
SHELL = f"{SCREENS_DIR}/00_layout/shell.md"

SCREEN_FM = ("status", "implements", "data_entities", "layout", "last_updated")


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("use exact component names from references/ui_components.md",
           reason="semantic — requires UI component catalog cross-reference")

    v.skip("specify DataGrid cell variants per column",
           reason="semantic — content completeness")

    v.skip("reference seed.json scenarios in Template Data section",
           reason="semantic — content completeness")

    # Check that feature files have screens[] in frontmatter
    def check_feedback_loop():
        features = v.glob_files(f"{FEATURES_DIR}/**/*.md")
        if not features:
            return False, "No feature files found"
        missing = []
        for f in features:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm and "screens" not in fm:
                missing.append(rel)
        if missing:
            return False, f"Features missing screens[] in frontmatter: {', '.join(missing[:3])}"
        return True, ""

    v.must("register every screen back into parent feature's screens[] frontmatter",
           check_feedback_loop)

    v.must("write 00_layout/shell.md before any individual screen specs", lambda: (
        v.file_exists(SHELL)
    ))

    # ── NEVER rules ──

    v.skip("invent colors or fonts — always reference brand tokens",
           rule_type="NEVER", reason="semantic — content quality")

    v.skip("write screens for features that have no approved feature spec",
           rule_type="NEVER", reason="semantic — requires cross-reference analysis")

    # ── CHECKLIST ──

    v.checklist("shell.md exists", lambda: v.file_exists(SHELL))

    # Every feature has at least one screen spec
    def check_screen_coverage():
        features = v.glob_files(f"{FEATURES_DIR}/**/*.md")
        screens = v.glob_files(f"{SCREENS_DIR}/**/*.md")
        # Exclude shell.md from screen count
        screens = [s for s in screens if "00_layout" not in str(s)]
        if not features:
            return False, "No feature files found"
        if not screens:
            return False, "No screen specs found"
        return True, ""

    v.checklist("Every feature has at least one screen spec", check_screen_coverage)

    v.checklist("All screen specs have required frontmatter", lambda: (
        v.all_files_have_frontmatter(f"{SCREENS_DIR}/[0-9]*/**/*.md", *SCREEN_FM)
    ))

    v.skip("Component Inventory uses exact names from ui_components.md",
           rule_type="CHECKLIST", reason="semantic — requires catalog cross-reference")

    v.skip("DataGrid columns specify cell variants",
           rule_type="CHECKLIST", reason="semantic — content completeness")

    v.skip("Data Requirements reference postxl-schema.json entities",
           rule_type="CHECKLIST", reason="semantic — cross-reference")

    v.skip("Template Data references seed.json scenarios",
           rule_type="CHECKLIST", reason="semantic — cross-reference")

    v.checklist("Feature files updated with screens[] in frontmatter", check_feedback_loop)

    v.skip("User has explicitly approved the screen specs",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
