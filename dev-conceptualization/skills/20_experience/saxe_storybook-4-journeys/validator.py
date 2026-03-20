#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-4-storybook-4-journeys.
Re-generate with: /compile-validators concept-2-experience-4-storybook-4-journeys
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-4-storybook-4-journeys"
SB_DIR = "_concept/2_experience/4_storybook"
SRC = f"{SB_DIR}/src"
JOURNEYS = f"{SRC}/stories/Journeys"
STORIES_JSON = "_concept/2_experience/1_journeys/stories.json"
APPSHELL = f"{SRC}/components/AppShell.tsx"


def _count_story_maps_by_stage(v, stage: str) -> int:
    data = v.read_json(STORIES_JSON)
    if not data:
        return 0
    return sum(1 for m in data.get("story_maps", []) if m.get("stage") == stage)


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    # Every non-backlog story map gets a journey story
    def check_journey_coverage():
        data = v.read_json(STORIES_JSON)
        if not data:
            return False, "Cannot read stories.json"
        non_backlog = [m for m in data.get("story_maps", [])
                       if m.get("stage") in ("hero", "vital", "hygiene")]
        journey_files = v.glob_files(f"{JOURNEYS}/**/*.stories.tsx")
        if len(journey_files) < len(non_backlog):
            return False, (f"Expected {len(non_backlog)} journey stories "
                          f"(hero+vital+hygiene), found {len(journey_files)}")
        return True, ""

    v.must("produce a journey story for EVERY non-backlog story map",
           check_journey_coverage)

    v.must("use ONLY existing page components and AppShell", lambda: (
        v.file_exists(APPSHELL)
    ))

    v.skip("navigate through real UI elements — no Next/Previous buttons",
           reason="semantic — requires code inspection")

    v.skip("include click-hint highlighting (pulsing .click-hint class)",
           reason="semantic — requires code inspection")

    v.skip("show persona and step indicator as subtle banner",
           reason="semantic — requires code inspection")

    v.skip("render full AppShell with active navigation at all times",
           reason="semantic — requires code inspection")

    # ── NEVER rules ──

    v.skip("create journey-specific components, layouts, or wrappers",
           rule_type="NEVER", reason="semantic — requires import analysis")

    v.skip("add Next Step / Previous Step navigation buttons",
           rule_type="NEVER", reason="semantic — requires code inspection")

    # Check that vital and hygiene flows are not skipped
    def check_no_skipped_stages():
        for stage in ("vital", "hygiene"):
            expected = _count_story_maps_by_stage(v, stage)
            if expected == 0:
                continue
            stage_dir = v.cwd / JOURNEYS / stage.capitalize()
            if not stage_dir.is_dir():
                return False, f"No Journeys/{stage.capitalize()}/ directory for {expected} {stage} flows"
            stories = list(stage_dir.glob("*.stories.tsx"))
            if len(stories) < expected:
                return False, f"Journeys/{stage.capitalize()}/: {len(stories)} stories, expected {expected}"
        return True, ""

    v.never("skip vital or hygiene flows", check_no_skipped_stages)

    # ── CHECKLIST ──

    v.checklist("Hero flow has a journey story in Journeys/Hero/", lambda: (
        v.dir_not_empty(f"{JOURNEYS}/Hero", "*.stories.tsx")
    ))

    # Vital flows
    def check_vital():
        expected = _count_story_maps_by_stage(v, "vital")
        if expected == 0:
            return True, ""
        return v.dir_not_empty(f"{JOURNEYS}/Vital", "*.stories.tsx")

    v.checklist("ALL vital flows have journey stories", check_vital)

    # Hygiene flows
    def check_hygiene():
        expected = _count_story_maps_by_stage(v, "hygiene")
        if expected == 0:
            return True, ""
        return v.dir_not_empty(f"{JOURNEYS}/Hygiene", "*.stories.tsx")

    v.checklist("ALL hygiene flows have journey stories", check_hygiene)

    v.checklist("Journey story count matches hero + vital + hygiene",
                check_journey_coverage)

    v.skip("Journeys use ONLY existing pages and AppShell",
           rule_type="CHECKLIST", reason="semantic — requires import analysis")

    v.skip("Navigation via real UI elements, no prev/next buttons",
           rule_type="CHECKLIST", reason="semantic — code inspection")

    v.skip("Click-hint highlighting works",
           rule_type="CHECKLIST", reason="visual — requires rendering")

    v.skip("Persona + step indicator shown",
           rule_type="CHECKLIST", reason="semantic — code inspection")

    v.skip("Build passes", rule_type="CHECKLIST", reason="runtime — requires pnpm")

    return v.result()


if __name__ == "__main__":
    main(validate)
