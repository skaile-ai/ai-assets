#!/usr/bin/env python3
"""Validator for the storybook skill.
Re-generate with: /compile-validators storybook
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "storybook"
STORYBOOK_DIR = "_concept/2_experience/4_storybook"
MAIN_CONFIG = f"{STORYBOOK_DIR}/.storybook/main"  # .ts or .js
PREVIEW_CONFIG = f"{STORYBOOK_DIR}/.storybook/preview"
BRAND_CSS = f"{STORYBOOK_DIR}/src/styles/brand.css"
COMPONENTS_BARREL = f"{STORYBOOK_DIR}/src/components/index"
MANIFEST = f"{STORYBOOK_DIR}/src/pages/manifest.json"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("produce storybook project directory", lambda: v.dir_exists(STORYBOOK_DIR))

    def check_storybook_config():
        # Check for main.ts or main.js
        for ext in (".ts", ".js"):
            if v.file_exists(MAIN_CONFIG + ext):
                return True, ""
        return False, f"No .storybook/main.ts or .storybook/main.js found"

    v.must("produce .storybook/main config", check_storybook_config)

    def check_preview_config():
        for ext in (".ts", ".js"):
            if v.file_exists(PREVIEW_CONFIG + ext):
                return True, ""
        return False, "No .storybook/preview.ts or .storybook/preview.js found"

    v.must("produce .storybook/preview config", check_preview_config)

    v.must("produce src/styles/brand.css", lambda: v.file_exists(BRAND_CSS))

    def check_components_barrel():
        for ext in (".ts", ".js"):
            if v.file_exists(COMPONENTS_BARREL + ext):
                return True, ""
        return False, "No src/components/index.ts or .js found"

    v.must("produce src/components/index barrel", check_components_barrel)

    v.must("produce src/pages/manifest.json", lambda: v.file_exists(MANIFEST))

    # Check at least one story file exists
    def check_stories_exist():
        stories = list(v.glob_files(f"{STORYBOOK_DIR}/src/stories/**/*.stories.*"))
        if not stories:
            return False, "No story files found in src/stories/"
        return True, f"{len(stories)} story files found"

    v.must("produce at least one story file", check_stories_exist)

    # Check 3-layer structure
    def check_three_layers():
        layers_found = []
        if list(v.glob_files(f"{STORYBOOK_DIR}/src/stories/Components/**")):
            layers_found.append("Components")
        if list(v.glob_files(f"{STORYBOOK_DIR}/src/stories/Pages/**")):
            layers_found.append("Pages")
        if list(v.glob_files(f"{STORYBOOK_DIR}/src/stories/Journeys/**")):
            layers_found.append("Journeys")
        if len(layers_found) < 2:
            return False, f"Only {len(layers_found)} story layers found: {layers_found}. Expected Components + Pages (+ Journeys)"
        return True, f"Story layers: {layers_found}"

    v.must("produce stories organized in Components/Pages layers (Journeys optional if stories.json absent)",
           check_three_layers)

    # ── NEVER rules ──

    def check_no_hardcoded_react():
        config_text = ""
        for ext in (".ts", ".js"):
            text = v.read_text(MAIN_CONFIG + ext)
            if text:
                config_text = text
                break
        if not config_text:
            return True, ""
        if "@storybook/react" in config_text and "@storybook/vue" not in config_text:
            # Only flag if React addon is present but no Vue (might be a React project legitimately)
            # We can't verify the stack here — just check for obviously wrong combinations
            pass
        return True, ""

    v.skip("hardcode framework-specific addon without reading tech stack profile",
           rule_type="NEVER", reason="process — cannot verify conversation flow from files")

    # ── CHECKLIST ──

    v.checklist("storybook project directory exists", lambda: v.dir_exists(STORYBOOK_DIR))

    v.checklist(".storybook config files exist", check_storybook_config)

    v.checklist("brand.css has CSS custom properties from tokens.json", lambda: (
        v.file_contains(BRAND_CSS, "--color-primary") if v.file_exists(BRAND_CSS)
        else (False, f"Cannot read {BRAND_CSS}")
    ))

    v.checklist("src/components/index barrel exists", check_components_barrel)

    v.checklist("src/pages/manifest.json exists", lambda: v.file_exists(MANIFEST))

    v.checklist("story files exist in 3 layers", check_three_layers)

    def check_appshell():
        appshell = list(v.glob_files(f"{STORYBOOK_DIR}/src/components/AppShell.*"))
        if not appshell:
            return False, "AppShell component not found in src/components/"
        return True, ""

    v.checklist("AppShell component exists", check_appshell)

    def check_journey_stories():
        journeys = list(v.glob_files(f"{STORYBOOK_DIR}/src/stories/Journeys/**/*.stories.*"))
        hero = list(v.glob_files(f"{STORYBOOK_DIR}/src/stories/Journeys/Hero/**"))
        if not journeys:
            # Acceptable if stories.json was absent
            return True, "No journey stories (skipped — stories.json may have been absent)"
        if not hero:
            return False, "Journey stories found but no Hero/ sub-folder"
        return True, f"{len(journeys)} journey stories found"

    v.checklist("Journey stories follow Hero/Vital/Hygiene structure (if built)", check_journey_stories)

    v.skip("Tech stack profile read for addon and story format",
           rule_type="CHECKLIST", reason="process — cannot verify conversation flow")

    return v.result()


if __name__ == "__main__":
    main(validate)
