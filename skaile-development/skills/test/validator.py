#!/usr/bin/env python3
"""Auto-generated validator for test.
Re-generate with: skaile-development/compile-validators target=test

This validator is a compact version — all rules are classified as semantic/runtime
and skipped. When this skill starts producing structured JSON artifacts, replace the
skip() calls with structural checks.
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "test"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    v.skip("read existing test files before constructing new ones — match conventions exactly", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("run tests before reporting results — never report without running", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("triage failures: distinguish regressions from new failures vs. infrastructure issues", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("distinguish test failures (bugs) from test construction errors (bad test code)", rule_type="MUST", reason="see SKILL.md — semantic/runtime")
    v.skip("Existing test patterns read before constructing new tests", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tests match package naming and placement conventions", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Tests cover observable behavior, not implementation details", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("All generated tests run without infrastructure errors", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Failures triaged (regression vs. new vs. infra vs. bad test)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    v.skip("Source bugs reported (tests not modified to hide them)", rule_type="CHECKLIST", reason="see SKILL.md — semantic/runtime")
    return v.result()


if __name__ == "__main__":
    main(validate)
