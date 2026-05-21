#!/usr/bin/env python3
"""Auto-generated validator for test-e2e.
Re-generate with: skaile-development/compile-validators target=test-e2e
"""
import sys
from pathlib import Path

_VLIB = None
for _base in (Path(__file__).resolve(), *Path(__file__).resolve().parents):
    for _rel in (
        ("ai-assets-skaileup", "contracts", "scripts"),
        ("ai-assets-skaileup", "skaileup-contracts", "scripts"),
        ("skaileup-shared", "scripts"),
    ):
        _cand = _base.joinpath(*_rel)
        if (_cand / "validator_lib.py").exists():
            _VLIB = str(_cand)
            break
    if _VLIB:
        break
if _VLIB:
    sys.path.insert(0, _VLIB)

from validator_lib import Validator, main

SKILL = "test-e2e"


def validate(cwd: str, target: str | None = None, kind: str = "auto") -> dict:
    v = Validator(cwd, SKILL)
    target = target or "."

    tgt = Path(cwd) / target

    # ── MUST rules ──
    # Web kind: playwright.config.ts + test/e2e folder; CLI kind: tests/cli-e2e folder.
    if kind == "cli" or (kind == "auto" and (tgt / "tests" / "cli-e2e").exists()):
        v.must(
            f"CLI e2e setup present at {target}/tests/cli-e2e/setup.ts",
            lambda: v.file_exists(f"{target}/tests/cli-e2e/setup.ts"),
        )
        v.must(
            f"CLI e2e tests folder not empty",
            lambda: v.dir_not_empty(f"{target}/tests/cli-e2e", "**/*.test.ts"),
        )
    else:
        v.must(
            f"playwright.config.ts exists at {target}",
            lambda: v.file_exists(f"{target}/playwright.config.ts"),
        )
        v.must(
            f"e2e test folder not empty",
            lambda: v.dir_not_empty(f"{target}/test/e2e", "**/*.spec.ts")
                    if (tgt / "test" / "e2e").exists()
                    else v.dir_not_empty(f"{target}/tests/e2e", "**/*.spec.ts"),
        )

    v.skip("read CLAUDE.md before scaffolding anything", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("reuse existing Playwright config as reference template", rule_type="MUST",
           reason="runtime — pattern matching")
    v.skip("scaffold isolated per-test sandbox", rule_type="MUST",
           reason="semantic — scaffolding pattern")
    v.skip("start dev server / build CLI before tests run", rule_type="MUST",
           reason="runtime — orchestration")
    v.skip("screenshot every failing journey step automatically", rule_type="MUST",
           reason="runtime — playwright config inspection")
    v.skip("verify generated tests execute before reporting", rule_type="MUST",
           reason="runtime — subprocess")
    v.skip("pick a dedicated port per forge app", rule_type="MUST",
           reason="semantic — port allocation table")

    # ── NEVER rules ──
    v.skip("run against a production URL", rule_type="NEVER",
           reason="semantic — URL inspection")
    v.skip("hardcode absolute paths", rule_type="NEVER",
           reason="semantic — content style")
    v.skip("leave dev servers running", rule_type="NEVER",
           reason="runtime — subprocess cleanup")

    # ── CHECKLIST ──
    v.skip("Kind correctly determined", rule_type="CHECKLIST",
           reason="runtime — detection")
    v.skip("Port unique per forge app", rule_type="CHECKLIST",
           reason="semantic — port allocation table")
    v.skip("globalSetup + globalTeardown in place (web)", rule_type="CHECKLIST",
           reason="conditional — only for web kind")
    v.skip("Per-test sandbox isolates DB / workspace", rule_type="CHECKLIST",
           reason="semantic — content pattern")
    v.skip("Dev server or built bin ready before tests run", rule_type="CHECKLIST",
           reason="runtime — orchestration")
    v.skip("Every journey has error + responsive check", rule_type="CHECKLIST",
           reason="semantic — scenario coverage")
    v.skip("Screenshots configured on failure", rule_type="CHECKLIST",
           reason="runtime — config inspection")
    v.skip("Generated tests pass or source bugs reported", rule_type="CHECKLIST",
           reason="runtime — subprocess")

    return v.result()


if __name__ == "__main__":
    main(validate)
