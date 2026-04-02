#!/usr/bin/env python3
"""Validator for eval-code skill output."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main


def validate(cwd: str) -> dict:
    v = Validator(cwd, "eval-code")

    # File existence
    v.must(
        "eval-code.json exists",
        lambda: v.file_exists("_implementation/eval-code.json"),
    )

    # Required top-level fields
    v.must(
        "has required fields",
        lambda: v.json_field_exists(
            "_implementation/eval-code.json",
            "schema_version",
            "scope",
            "build",
            "tests",
            "logic",
            "security",
            "ui_ux",
            "blocking_issues",
            "verdict",
        ),
    )

    # scope enum
    def check_scope():
        data = v.read_json("_implementation/eval-code.json")
        if data is None:
            return False, "file not readable"
        scope = data.get("scope")
        allowed = {"scaffold", "feature", "full"}
        if scope not in allowed:
            return False, f"scope={scope!r} not in {sorted(allowed)}"
        return True, ""

    v.must("scope is valid enum value", check_scope)

    # verdict enum
    def check_verdict():
        data = v.read_json("_implementation/eval-code.json")
        if data is None:
            return False, "file not readable"
        verdict = data.get("verdict")
        allowed = {"pass", "warn", "fail"}
        if verdict not in allowed:
            return False, f"verdict={verdict!r} not in {sorted(allowed)}"
        return True, ""

    v.must("verdict is valid enum value", check_verdict)

    # build object fields
    def check_build_fields():
        data = v.read_json("_implementation/eval-code.json")
        if data is None:
            return False, "file not readable"
        build = data.get("build")
        if not isinstance(build, dict):
            return False, "build is not an object"
        for field in ("lint", "types"):
            if field not in build:
                return False, f"build missing field '{field}'"
        return True, ""

    v.must("build object has required fields", check_build_fields)

    # build values enum
    def make_build_value_check(key):
        def check():
            data = v.read_json("_implementation/eval-code.json")
            if data is None:
                return False, "file not readable"
            build = data.get("build", {})
            if not isinstance(build, dict):
                return False, "build is not an object"
            val = build.get(key)
            allowed = {"pass", "fail"}
            if val not in allowed:
                return False, f"build.{key}={val!r} not in {sorted(allowed)}"
            return True, ""
        return check

    for key in ("lint", "types"):
        v.must(f"build.{key} is valid enum value", make_build_value_check(key))

    # logic object has field: score
    def check_logic_score():
        data = v.read_json("_implementation/eval-code.json")
        if data is None:
            return False, "file not readable"
        logic = data.get("logic")
        if not isinstance(logic, dict):
            return False, "logic is not an object"
        if "score" not in logic:
            return False, "logic missing field 'score'"
        return True, ""

    v.must("logic object has score field", check_logic_score)

    # security object has field: score
    def check_security_score():
        data = v.read_json("_implementation/eval-code.json")
        if data is None:
            return False, "file not readable"
        security = data.get("security")
        if not isinstance(security, dict):
            return False, "security is not an object"
        if "score" not in security:
            return False, "security missing field 'score'"
        return True, ""

    v.must("security object has score field", check_security_score)

    # Skip blocking_issues contradiction check as subjective
    v.skip(
        "blocking_issues contradiction check",
        rule_type="MUST",
        reason="semantic — whether blocking_issues justify a non-pass verdict requires human review",
    )

    return v.result()


if __name__ == "__main__":
    main(validate)
