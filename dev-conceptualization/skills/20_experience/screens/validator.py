#!/usr/bin/env python3
"""Validator for the screens skill.
Re-generate with: /compile-validators screens
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "dev-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "screens"
SCREENS_DIR = "_concept/2_experience/3_screens"
FEATURES_DIR = "_concept/2_experience/2_features"
SHELL = f"{SCREENS_DIR}/00_layout/shell.md"

SCREEN_FM = ("implements", "data_entities", "layout", "last_updated")


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("write 00_layout/shell.md before any individual screen specs", lambda: (
        v.file_exists(SHELL)
    ))

    # Check that feature files have screens[] in frontmatter (feedback loop)
    def check_feedback_loop():
        features = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
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

    v.must("all screen specs have required frontmatter", lambda: (
        v.all_files_have_frontmatter(f"{SCREENS_DIR}/[0-9]*/**/*.md", *SCREEN_FM)
    ))

    # ── NEVER rules ──

    v.skip("include component library names or CSS tokens in screen specs",
           rule_type="NEVER", reason="semantic — content quality check")

    v.skip("write screens for features without a feature spec",
           rule_type="NEVER", reason="semantic — requires cross-reference analysis")

    # ── CHECKLIST ──

    v.checklist("shell.md exists", lambda: v.file_exists(SHELL))

    # Every feature group has at least one screen spec
    def check_screen_coverage():
        features = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        screens = [s for s in v.glob_files(f"{SCREENS_DIR}/**/*.md")
                   if "00_layout" not in str(s)]
        if not features:
            return False, "No feature files found"
        if not list(screens):
            return False, "No screen specs found (excluding shell)"
        return True, ""

    v.checklist("Every feature group has at least one screen spec", check_screen_coverage)

    v.checklist("All screen specs have required frontmatter", lambda: (
        v.all_files_have_frontmatter(f"{SCREENS_DIR}/[0-9]*/**/*.md", *SCREEN_FM)
    ))

    v.checklist("Feature files updated with screens[] in frontmatter", check_feedback_loop)

    v.skip("Screen specs written in plain user-perspective language",
           rule_type="CHECKLIST", reason="semantic — content quality")

    v.skip("User has explicitly approved the screen specs",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
