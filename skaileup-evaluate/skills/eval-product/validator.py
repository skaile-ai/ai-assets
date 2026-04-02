#!/usr/bin/env python3
"""Validator for eval-product skill output."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main


def validate(cwd: str) -> dict:
    v = Validator(cwd, "eval-product")

    # File existence
    v.must(
        "eval-product.json exists",
        lambda: v.file_exists("_implementation/eval-product.json"),
    )

    # Required top-level fields
    v.must(
        "has required fields",
        lambda: v.json_field_exists(
            "_implementation/eval-product.json",
            "schema_version",
            "goals",
            "design",
            "performance",
            "accessibility_score",
            "mobile_score",
            "improvement_priorities",
            "verdict",
        ),
    )

    # Score ranges
    def check_accessibility_score():
        data = v.read_json("_implementation/eval-product.json")
        if data is None:
            return False, "file not readable"
        s = data.get("accessibility_score", -1)
        if not (0 <= s <= 100):
            return False, f"accessibility_score={s} out of range 0-100"
        return True, ""

    def check_mobile_score():
        data = v.read_json("_implementation/eval-product.json")
        if data is None:
            return False, "file not readable"
        s = data.get("mobile_score", -1)
        if not (0 <= s <= 100):
            return False, f"mobile_score={s} out of range 0-100"
        return True, ""

    v.must("accessibility_score in 0-100", check_accessibility_score)
    v.must("mobile_score in 0-100", check_mobile_score)

    # Verdict enum
    def check_verdict():
        data = v.read_json("_implementation/eval-product.json")
        if data is None:
            return False, "file not readable"
        verdict = data.get("verdict")
        allowed = {"approved", "needs_iteration", "fail"}
        if verdict not in allowed:
            return False, f"verdict={verdict!r} not in {sorted(allowed)}"
        return True, ""

    v.must("verdict is valid enum value", check_verdict)

    # design object fields
    def check_design_fields():
        data = v.read_json("_implementation/eval-product.json")
        if data is None:
            return False, "file not readable"
        design = data.get("design")
        if not isinstance(design, dict):
            return False, "design is not an object"
        for field in ("quality", "originality", "craft", "functionality"):
            if field not in design:
                return False, f"design missing field '{field}'"
        return True, ""

    v.must("design object has required fields", check_design_fields)

    # design dimension ranges (0-10)
    def make_design_dim_check(dim):
        def check():
            data = v.read_json("_implementation/eval-product.json")
            if data is None:
                return False, "file not readable"
            design = data.get("design", {})
            if not isinstance(design, dict):
                return False, "design is not an object"
            s = design.get(dim, -1)
            if not (0 <= s <= 10):
                return False, f"design.{dim}={s} out of range 0-10"
            return True, ""
        return check

    for dim in ("quality", "originality", "craft", "functionality"):
        v.must(f"design.{dim} in 0-10", make_design_dim_check(dim))

    # goals structure
    def check_goals_fields():
        data = v.read_json("_implementation/eval-product.json")
        if data is None:
            return False, "file not readable"
        goals = data.get("goals", [])
        for i, goal in enumerate(goals):
            if not isinstance(goal, dict):
                return False, f"goals[{i}] is not an object"
            if "goal" not in goal:
                return False, f"goals[{i}] missing field 'goal'"
            if "achieved" not in goal:
                return False, f"goals[{i}] missing field 'achieved'"
        return True, ""

    def check_goals_achieved_enum():
        data = v.read_json("_implementation/eval-product.json")
        if data is None:
            return False, "file not readable"
        goals = data.get("goals", [])
        allowed = {"achieved", "partial", "not_achieved"}
        for i, goal in enumerate(goals):
            if not isinstance(goal, dict):
                continue
            a = goal.get("achieved")
            if a not in allowed:
                return False, f"goals[{i}].achieved={a!r} not in {sorted(allowed)}"
        return True, ""

    v.must("each goal has required fields", check_goals_fields)
    v.must("each goal achieved is valid enum value", check_goals_achieved_enum)

    # Skip design average contradiction check as subjective
    v.skip(
        "design average contradiction check",
        rule_type="MUST",
        reason="semantic — whether design average justifies verdict requires human review",
    )

    return v.result()


if __name__ == "__main__":
    main(validate)
