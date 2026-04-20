#!/usr/bin/env python3
"""Auto-generated validator for test-integration.
Re-generate with: skaile-development/compile-validators target=test-integration
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "test-integration"


def validate(cwd: str, target: str | None = None) -> dict:
    v = Validator(cwd, SKILL)
    target = target or "."

    # ── MUST rules ──
    v.must(
        f"integration setup file exists at {target}/tests/integration/setup.ts",
        lambda: v.file_exists(f"{target}/tests/integration/setup.ts"),
    )
    v.must(
        f"integration tests folder not empty at {target}",
        lambda: v.dir_not_empty(f"{target}/tests/integration", "**/*.test.ts"),
    )
    v.skip("read package CLAUDE.md before generating", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("detect storage backend from package.json + config files", rule_type="MUST",
           reason="runtime — detection logic")
    v.skip("scaffold DB isolation before any test is generated", rule_type="MUST",
           reason="runtime — execution order")
    v.skip("reset state in afterEach", rule_type="MUST",
           reason="semantic — test content pattern")
    v.skip("generate seed/fixture for every scenario that needs data", rule_type="MUST",
           reason="semantic — content completeness")
    v.skip("include both success and auth/validation tests for every endpoint", rule_type="MUST",
           reason="semantic — scenario coverage")
    v.skip("verify tests run and pass before reporting", rule_type="MUST",
           reason="runtime — subprocess")

    # ── NEVER rules ──
    v.skip("use the production database", rule_type="NEVER",
           reason="semantic — DB connection source inspection")
    v.skip("hardcode test data inline", rule_type="NEVER",
           reason="semantic — content style judgment")
    v.skip("mock the DB", rule_type="NEVER",
           reason="semantic — content style judgment")
    v.skip("leave test DB running after suite completion", rule_type="NEVER",
           reason="runtime — subprocess cleanup")

    # ── CHECKLIST ──
    v.skip("Storage backend correctly detected", rule_type="CHECKLIST",
           reason="runtime — detection")
    v.skip("Test DB / workspace isolation scaffolded", rule_type="CHECKLIST",
           reason="semantic — scaffolding pattern")
    v.skip("State reset between tests", rule_type="CHECKLIST",
           reason="semantic — content pattern")
    v.skip("Fixtures modular and reusable", rule_type="CHECKLIST",
           reason="semantic — content quality")
    v.skip("Auth helper present where needed", rule_type="CHECKLIST",
           reason="semantic — conditional content")
    v.skip("Each endpoint: happy + 400 + auth cases at minimum", rule_type="CHECKLIST",
           reason="semantic — scenario coverage")
    v.skip("All tests run", rule_type="CHECKLIST",
           reason="runtime — subprocess")

    return v.result()


if __name__ == "__main__":
    main(validate)
