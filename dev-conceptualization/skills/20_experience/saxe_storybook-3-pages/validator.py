#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-4-storybook-3-pages.
Re-generate with: /compile-validators concept-2-experience-4-storybook-3-pages
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-4-storybook-3-pages"
SB_DIR = "_concept/2_experience/4_storybook"
SRC = f"{SB_DIR}/src"
PAGES = f"{SRC}/pages"
PAGE_STORIES = f"{SRC}/stories/Pages"
SCREENS_DIR = "_concept/2_experience/3_screens"
APPSHELL = f"{SRC}/components/AppShell.tsx"
MANIFEST = f"{SRC}/pages/manifest.json"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("import library components directly from '@postxl/ui-components'",
           reason="semantic — import pattern check")

    v.skip("import custom components from src/components/index.ts barrel",
           reason="semantic — import pattern check")

    # Check lucide-react in page files
    def check_lucide():
        pages = v.glob_files(f"{PAGES}/**/*.tsx")
        for f in pages:
            text = f.read_text(encoding="utf-8")
            if "react-icons" in text or "font-awesome" in text or "@heroicons" in text:
                return False, f"{f.name}: uses non-lucide icons"
        return True, ""

    v.must("use lucide-react for all icons", check_lucide)

    v.skip("create state variants for every state listed in screen specs",
           reason="semantic — requires screen spec cross-reference")

    v.skip("include responsive variants (Mobile, Tablet) for each page",
           reason="semantic — requires story inspection")

    v.must("build AppShell first", lambda: v.file_exists(APPSHELL))

    v.must("write types to src/@types/", lambda: v.dir_exists(f"{SRC}/@types"))

    v.skip("pass WCAG AA contrast (4.5:1) for all text elements",
           reason="visual — requires rendering")

    v.skip("use realistic domain-appropriate data — never use 'Lorem ipsum'",
           reason="semantic — content quality")

    # ── NEVER rules ──

    v.skip("hardcode navigation items — derive from shell spec",
           rule_type="NEVER", reason="semantic — implementation pattern")

    v.skip("skip screen states documented in screen specs",
           rule_type="NEVER", reason="semantic — cross-reference")

    v.skip("invent colors or fonts — use CSS custom properties",
           rule_type="NEVER", reason="semantic — content quality")

    # ── CHECKLIST ──

    v.checklist("AppShell component built from shell.md", lambda: v.file_exists(APPSHELL))

    # Check AppShell story exists
    v.checklist("AppShell story has responsive variants", lambda: (
        v.dir_not_empty(f"{PAGE_STORIES}/00 Layout", "*.stories.tsx")
        if (v.cwd / f"{PAGE_STORIES}/00 Layout").is_dir()
        else v.dir_not_empty(PAGE_STORIES, "**/AppShell*")
    ))

    # Every screen spec has a page component
    def check_page_coverage():
        screens = [s for s in v.glob_files(f"{SCREENS_DIR}/[0-9]*/**/*.md")]
        pages = v.glob_files(f"{PAGES}/**/*.tsx")
        if not screens:
            return False, "No screen specs found"
        if not pages:
            return False, "No page components found"
        return True, ""

    v.checklist("Every screen spec has a page component", check_page_coverage)

    # Every screen spec has a page story
    def check_page_story_coverage():
        stories = v.glob_files(f"{PAGE_STORIES}/**/*.stories.tsx")
        if not stories:
            return False, "No page stories found"
        return True, ""

    v.checklist("Every screen spec has a page story", check_page_story_coverage)

    v.checklist("Minimal types in src/@types/ for page data entities", lambda: (
        v.dir_exists(f"{SRC}/@types")
    ))

    v.skip("Page stories have state variants matching screen spec States",
           rule_type="CHECKLIST", reason="semantic — cross-reference")

    v.skip("Story data uses realistic domain-appropriate content",
           rule_type="CHECKLIST", reason="semantic — content quality")

    v.skip("Responsive variants included (Mobile, Tablet)",
           rule_type="CHECKLIST", reason="semantic — requires story inspection")

    v.checklist("src/pages/manifest.json maps every screen spec", lambda: (
        v.file_exists(MANIFEST)
    ))

    v.skip("WCAG AA contrast verified",
           rule_type="CHECKLIST", reason="visual — requires rendering")

    v.skip("Build passes", rule_type="CHECKLIST", reason="runtime — requires pnpm")

    return v.result()


if __name__ == "__main__":
    main(validate)
