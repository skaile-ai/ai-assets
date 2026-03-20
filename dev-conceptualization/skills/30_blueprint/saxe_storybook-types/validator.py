#!/usr/bin/env python3
"""Auto-generated validator for concept-3-blueprint-4-storybook-types.
Re-generate with: /compile-validators concept-3-blueprint-4-storybook-types
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "concept-3-blueprint-4-storybook-types"

SB = "_concept/2_experience/4_storybook"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("generate types with pxl types", rule_type="MUST",
           reason="semantic — runtime command execution")

    v.must("preserve UI-only types in src/@types/ui.ts",
           lambda: v.file_exists(f"{SB}/src/@types/ui.ts"))

    v.must("keep barrel export at src/@types/index.ts",
           lambda: v.file_exists(f"{SB}/src/@types/index.ts"))

    v.must("barrel re-exports generated types", lambda: _barrel_reexports(v, "generated"))

    v.must("barrel re-exports UI-only types", lambda: _barrel_reexports(v, "ui"))

    v.skip("verify compilation passes with pnpm tsc --noEmit", rule_type="MUST",
           reason="semantic — runtime build verification")

    v.skip("verify Storybook builds with pnpm run build", rule_type="MUST",
           reason="semantic — runtime build verification")

    v.skip("fix all type errors in components, pages, and stories", rule_type="MUST",
           reason="semantic — runtime compilation check")

    # ── NEVER rules ──

    v.skip("delete UI-only types that components still reference", rule_type="NEVER",
           reason="semantic — requires cross-reference analysis of imports vs exports")

    v.skip("modify postxl-schema.json", rule_type="NEVER",
           reason="semantic — would need before/after comparison")

    v.skip("change tsconfig.json unless absolutely required", rule_type="NEVER",
           reason="semantic — requires judgment on necessity")

    # ── CHECKLIST ──

    v.skip("pxl types ran successfully against postxl-schema.json", rule_type="CHECKLIST",
           reason="semantic — runtime command execution")

    v.checklist("generated types are in src/@types/ (not hand-written)",
                lambda: v.dir_not_empty(f"{SB}/src/@types", "*.ts"))

    v.checklist("UI-only types preserved in src/@types/ui.ts",
                lambda: v.file_exists(f"{SB}/src/@types/ui.ts"))

    v.checklist("src/@types/index.ts barrel re-exports both",
                lambda: _barrel_has_both_reexports(v))

    v.skip("all existing component/page/story imports still resolve", rule_type="CHECKLIST",
           reason="semantic — runtime import resolution")

    v.skip("pnpm tsc --noEmit passes with zero errors", rule_type="CHECKLIST",
           reason="semantic — runtime build verification")

    v.skip("pnpm run build succeeds", rule_type="CHECKLIST",
           reason="semantic — runtime build verification")

    v.skip("no mocked types remain for schema-backed entities", rule_type="CHECKLIST",
           reason="semantic — requires schema knowledge to classify types")

    return v.result()


def _barrel_reexports(v: Validator, module: str) -> tuple[bool, str]:
    """Check that index.ts re-exports from the given module."""
    text = v.read_text(f"{SB}/src/@types/index.ts")
    if text is None:
        return False, "src/@types/index.ts not found"
    if f'from "./{module}"' in text or f"from './{module}'" in text:
        return True, ""
    return False, f'index.ts does not re-export from "./{module}"'


def _barrel_has_both_reexports(v: Validator) -> tuple[bool, str]:
    """Check barrel re-exports both generated and UI-only types."""
    text = v.read_text(f"{SB}/src/@types/index.ts")
    if text is None:
        return False, "src/@types/index.ts not found"
    has_generated = ('from "./generated"' in text or "from './generated'" in text)
    has_ui = ('from "./ui"' in text or "from './ui'" in text)
    if has_generated and has_ui:
        return True, ""
    missing = []
    if not has_generated:
        missing.append("generated")
    if not has_ui:
        missing.append("ui")
    return False, f"index.ts missing re-exports for: {', '.join(missing)}"


if __name__ == "__main__":
    main(validate)
