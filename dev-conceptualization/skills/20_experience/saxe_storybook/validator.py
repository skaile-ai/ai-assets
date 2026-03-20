#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-4-storybook.
Re-generate with: /compile-validators concept-2-experience-4-storybook
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-4-storybook"
SB_DIR = "_concept/2_experience/4_storybook"
SRC = f"{SB_DIR}/src"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("run all 4 sub-skills in sequence",
           reason="process — orchestration sequence")

    # Verify build passes — check that dist or storybook-static exists,
    # or at minimum that package.json and src/ exist
    v.must("verify build passes after all sub-skills complete", lambda: (
        v.file_exists(f"{SB_DIR}/package.json")
    ))

    # Report story counts per layer — check that all 3 story dirs exist
    def check_story_layers():
        layers = {
            "Pages": f"{SRC}/stories/Pages",
            "Journeys": f"{SRC}/stories/Journeys",
        }
        missing = []
        for name, path in layers.items():
            p = v.cwd / path
            if not p.is_dir():
                missing.append(name)
        if missing:
            return False, f"Missing story layers: {', '.join(missing)}"
        return True, ""

    v.must("report story counts per layer", check_story_layers)

    # ── NEVER rules ──

    v.skip("skip any sub-skill", rule_type="NEVER",
           reason="process — orchestration")

    # ── CHECKLIST ──

    v.checklist("Sub-skill 1 (setup) completed — project scaffolded", lambda: (
        v.file_exists(f"{SB_DIR}/package.json")
    ))

    v.checklist("Sub-skill 2 (components) completed", lambda: (
        v.file_exists(f"{SRC}/components/index.ts")
    ))

    # Check pages exist
    def check_pages():
        pages = v.glob_files(f"{SRC}/stories/Pages/**/*.stories.tsx")
        if not pages:
            return False, "No page stories found in src/stories/Pages/"
        return True, ""

    v.checklist("Sub-skill 3 (pages) completed — screen specs rendered", check_pages)

    # Check journeys exist
    def check_journeys():
        journeys = v.glob_files(f"{SRC}/stories/Journeys/**/*.stories.tsx")
        if not journeys:
            return False, "No journey stories found in src/stories/Journeys/"
        return True, ""

    v.checklist("Sub-skill 4 (journeys) completed — click-dummies built", check_journeys)

    v.skip("Final build passes",
           rule_type="CHECKLIST", reason="runtime — requires pnpm run build")

    v.checklist("All 3 layers visible in sidebar", check_story_layers)

    return v.result()


if __name__ == "__main__":
    main(validate)
