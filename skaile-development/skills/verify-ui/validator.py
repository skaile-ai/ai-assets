#!/usr/bin/env python3
"""Auto-generated validator for verify-ui.
Re-generate with: skaile-development/compile-validators target=verify-ui

This validator is a compact version — all rules are classified as semantic/runtime
and skipped. When this skill starts producing structured JSON artifacts, replace the
skip() calls with structural checks.
"""
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main

SKILL = "verify-ui"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    pass  # no rules detected
    return v.result()


if __name__ == "__main__":
    main(validate)
