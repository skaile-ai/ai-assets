#!/usr/bin/env python3
"""Validator for eval-code skill output."""
import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import ValidationResult, check_field, check_range, check_enum

def validate(project_dir: str) -> ValidationResult:
    result = ValidationResult()
    output_path = Path(project_dir) / "_implementation" / "eval-code.json"

    if not output_path.exists():
        result.error("eval-code.json not found at _implementation/eval-code.json")
        return result

    with open(output_path) as f:
        data = json.load(f)

    check_field(result, data, "schema_version", str)
    check_enum(result, data, "scope", ["scaffold", "feature", "full"])
    check_field(result, data, "build", dict)
    check_field(result, data, "blocking_issues", list)
    check_enum(result, data, "verdict", ["pass", "warn", "fail"])

    build = data.get("build", {})
    for key in ["lint", "types", "bundle"]:
        check_enum(result, build, key, ["pass", "fail"])

    scope = data.get("scope")
    if scope in ("feature", "full"):
        check_field(result, data, "tests", dict)

    if scope == "full":
        for audit in ["logic", "security", "ui_ux"]:
            if audit in data:
                check_range(result, data[audit], "score", 0, 100)
                check_field(result, data[audit], "findings", list)

    if data.get("verdict") == "pass" and data.get("blocking_issues"):
        result.warning("verdict is 'pass' but blocking_issues is non-empty")

    return result

if __name__ == "__main__":
    project_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    r = validate(project_dir)
    r.report()
    sys.exit(0 if r.passed else 1)
