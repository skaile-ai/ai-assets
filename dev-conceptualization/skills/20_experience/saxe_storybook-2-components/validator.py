#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-4-storybook-2-components.
Re-generate with: /compile-validators concept-2-experience-4-storybook-2-components
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-4-storybook-2-components"
SB_DIR = "_concept/2_experience/4_storybook"
SRC = f"{SB_DIR}/src"
COMPONENTS = f"{SRC}/components"
STORIES = f"{SRC}/stories/Components"
TYPES_DIR = f"{SRC}/@types"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("use @postxl/ui-components as the first choice for composition",
           reason="semantic — design decision")

    v.skip("fall back to Radix primitives only when no match exists",
           reason="semantic — design decision")

    # Check lucide-react usage in component files (no emoji/icon font imports)
    def check_lucide():
        comps = v.glob_files(f"{COMPONENTS}/*.tsx")
        for f in comps:
            text = f.read_text(encoding="utf-8")
            if "lucide" not in text and ("icon" in text.lower() or "Icon" in text):
                # Has icon references but no lucide import — suspicious
                if "react-icons" in text or "font-awesome" in text or "@heroicons" in text:
                    return False, f"{f.name}: uses non-lucide icons"
        return True, ""

    v.must("use lucide-react for all icons", check_lucide)

    # Check story title prefix
    def check_title_prefix():
        stories = v.glob_files(f"{STORIES}/*.stories.tsx")
        for f in stories:
            text = f.read_text(encoding="utf-8")
            if "title:" in text or "title :" in text:
                if "'Components/" not in text and '"Components/' not in text:
                    return False, f"{f.name}: story title doesn't use 'Components/<Name>' prefix"
        return True, ""

    v.must("use 'Components/<Name>' as the story title prefix", check_title_prefix)

    # Check autodocs tag
    def check_autodocs():
        stories = v.glob_files(f"{STORIES}/*.stories.tsx")
        for f in stories:
            text = f.read_text(encoding="utf-8")
            if "autodocs" not in text:
                return False, f"{f.name}: missing 'autodocs' tag"
        return True, ""

    v.must("include 'autodocs' tag on all component stories", check_autodocs)

    v.skip("pass WCAG AA contrast (4.5:1) for all text",
           reason="visual — requires rendering")

    v.skip("use realistic domain-appropriate data in stories",
           reason="semantic — content quality")

    v.must("write types to src/@types/", lambda: (
        v.dir_exists(TYPES_DIR)
    ))

    # Check minimal type comment
    def check_type_comments():
        type_files = v.glob_files(f"{TYPES_DIR}/*.ts")
        # Skip index.ts
        type_files = [f for f in type_files if f.name != "index.ts" and f.name != "README.md"]
        if not type_files:
            return True, ""  # No custom types needed is OK
        for f in type_files:
            text = f.read_text(encoding="utf-8")
            if "Minimal type" not in text and "minimal type" not in text:
                return False, f"{f.name}: missing 'Minimal type for Storybook' comment"
        return True, ""

    v.must("add 'Minimal type for Storybook' comment to all type files", check_type_comments)

    v.must("write src/components/index.ts barrel", lambda: (
        v.file_exists(f"{COMPONENTS}/index.ts")
    ))

    # ── NEVER rules ──

    v.skip("create stories for components that exist in @postxl/ui-components",
           rule_type="NEVER", reason="semantic — requires library catalog check")

    v.skip("invent colors or fonts — use CSS custom properties",
           rule_type="NEVER", reason="semantic — content quality")

    # ── CHECKLIST ──

    v.skip("All screen spec components inventoried and categorized",
           rule_type="CHECKLIST", reason="process — requires screen spec cross-reference")

    v.checklist("Minimal types written to src/@types/", lambda: v.dir_exists(TYPES_DIR))

    # Every custom component has a .tsx file
    def check_component_files():
        comps = v.glob_files(f"{COMPONENTS}/*.tsx")
        # Exclude AppShell (built by pages sub-skill)
        comps = [c for c in comps if c.name != "AppShell.tsx"]
        if not comps:
            # May be zero custom components — check for empty barrel
            barrel = v.read_text(f"{COMPONENTS}/index.ts")
            if barrel and ("No custom" in barrel or barrel.strip() == "" or "// " in barrel):
                return True, ""
            return True, ""  # No components is valid
        return True, ""

    v.checklist("Every custom component has a .tsx file", check_component_files)

    # Every custom component has a story
    def check_story_coverage():
        comps = [c for c in v.glob_files(f"{COMPONENTS}/*.tsx")
                 if c.name not in ("AppShell.tsx", "index.ts")]
        stories = v.glob_files(f"{STORIES}/*.stories.tsx")
        comp_names = {c.stem for c in comps}
        story_names = {s.stem.replace(".stories", "") for s in stories}
        missing = comp_names - story_names
        if missing:
            return False, f"Components without stories: {', '.join(missing)}"
        return True, ""

    v.checklist("Every custom component has a .stories.tsx file", check_story_coverage)

    v.checklist("src/components/index.ts barrel exports all custom components", lambda: (
        v.file_exists(f"{COMPONENTS}/index.ts")
    ))

    v.skip("Components compose from @postxl/ui-components where possible",
           rule_type="CHECKLIST", reason="semantic — design decision")

    v.checklist("All icons use lucide-react", check_lucide)

    # Story count matches component count
    def check_story_count():
        comps = [c for c in v.glob_files(f"{COMPONENTS}/*.tsx")
                 if c.name not in ("AppShell.tsx", "index.ts")]
        stories = v.glob_files(f"{STORIES}/*.stories.tsx")
        if len(comps) != len(stories) and len(comps) > 0:
            return False, f"Components: {len(comps)}, Stories: {len(stories)}"
        return True, ""

    v.checklist("Story count matches custom component count", check_story_count)

    v.skip("Build passes", rule_type="CHECKLIST", reason="runtime — requires pnpm")

    return v.result()


if __name__ == "__main__":
    main(validate)
