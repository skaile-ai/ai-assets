#!/usr/bin/env python3
"""Auto-generated validator for concept-2-experience-4-storybook-1-setup.
Re-generate with: /compile-validators concept-2-experience-4-storybook-1-setup
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-2-experience-4-storybook-1-setup"
SB_DIR = "_concept/2_experience/4_storybook"
SRC = f"{SB_DIR}/src"


def _no_placeholders(v, rel_path: str) -> tuple[bool, str]:
    """Check that no {{PLACEHOLDER}} markers remain."""
    text = v.read_text(rel_path)
    if text is None:
        return False, f"Cannot read {rel_path}"
    matches = re.findall(r"\{\{[A-Z_a-z.]+\}\}", text)
    if matches:
        return False, f"Unreplaced placeholders in {rel_path}: {', '.join(matches[:5])}"
    return True, ""


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("use the pinned package.json from templates", lambda: (
        v.file_exists(f"{SB_DIR}/package.json")
    ))

    v.skip("use templates as the starting point",
           reason="process — cannot verify template origin")

    # Check no placeholders remain in templated files
    def check_all_placeholders():
        templated = [
            f"{SB_DIR}/.storybook/theme.ts",
            f"{SRC}/styles/brand.css",
        ]
        for f in templated:
            ok, detail = _no_placeholders(v, f)
            if not ok:
                return ok, detail
        return True, ""

    v.must("replace ALL {{PLACEHOLDER}} values in templated files",
           check_all_placeholders)

    # ── NEVER rules ──

    v.skip("invent colors, fonts, or spacing — everything comes from tokens.json",
           rule_type="NEVER", reason="semantic — content origin")

    v.skip("modify the static CSS section in brand.css",
           rule_type="NEVER", reason="semantic — requires template diff")

    # ── CHECKLIST ──

    v.checklist("package.json copied and pnpm install succeeded", lambda: (
        v.file_exists(f"{SB_DIR}/package.json")
    ))

    v.checklist("Static templates copied (main.ts, preview.ts, vite.config.ts, tsconfig.json)", lambda: (
        (True, "") if all(
            (v.cwd / f).exists() for f in [
                f"{SB_DIR}/.storybook/main.ts",
                f"{SB_DIR}/.storybook/preview.ts",
                f"{SB_DIR}/vite.config.ts",
                f"{SB_DIR}/tsconfig.json",
            ]
        ) else (False, "Missing static template files")
    ))

    v.checklist("theme.ts has all placeholders replaced", lambda: (
        _no_placeholders(v, f"{SB_DIR}/.storybook/theme.ts")
    ))

    v.checklist("brand.css has all placeholders replaced", lambda: (
        _no_placeholders(v, f"{SRC}/styles/brand.css")
    ))

    v.skip("brand.css .dark block removed if light-only mode",
           rule_type="CHECKLIST", reason="conditional — depends on tokens.json mode")

    # Check no placeholders in ANY output file
    def check_no_placeholders_anywhere():
        files = v.glob_files(f"{SB_DIR}/**/*.ts") + v.glob_files(f"{SB_DIR}/**/*.css")
        for f in files:
            if "node_modules" in str(f):
                continue
            rel = str(f.relative_to(v.cwd))
            ok, detail = _no_placeholders(v, rel)
            if not ok:
                return ok, detail
        return True, ""

    v.checklist("No {{PLACEHOLDER}} markers remain in any output file",
                check_no_placeholders_anywhere)

    v.checklist("src/@types/ directory created", lambda: (
        v.dir_exists(f"{SRC}/@types")
    ))

    v.skip("Storybook builds without errors",
           rule_type="CHECKLIST", reason="runtime — requires pnpm run build")

    return v.result()


if __name__ == "__main__":
    main(validate)
