#!/usr/bin/env python3
"""Validator for eval-concept skill output."""
import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import ValidationResult, check_field, check_range, check_enum

def validate(project_dir: str) -> ValidationResult:
    result = ValidationResult()
    output_path = Path(project_dir) / "_concept" / "eval-concept.json"

    if not output_path.exists():
        result.error("eval-concept.json not found at _concept/eval-concept.json")
        return result

    with open(output_path) as f:
        data = json.load(f)

    check_field(result, data, "schema_version", str)
    check_field(result, data, "evaluated_at", str)
    check_range(result, data, "completeness_score", 0, 100)
    check_range(result, data, "clarity_score", 0, 100)
    check_range(result, data, "traceability_score", 0, 100)
    check_range(result, data, "overall_score", 0, 100)
    check_enum(result, data, "verdict", ["pass", "needs_resolution", "fail"])
    check_field(result, data, "blocking_flags", list)
    check_field(result, data, "warning_flags", list)
    check_field(result, data, "summary", str)

    for flag in data.get("blocking_flags", []):
        check_enum(result, flag, "type", ["missing", "ambiguous", "contradiction", "orphan", "untraceable"])
        check_field(result, flag, "location", str)
        check_field(result, flag, "description", str)
        check_field(result, flag, "resolution", str)

    if data.get("verdict") == "pass" and data.get("blocking_flags"):
        result.error("verdict is 'pass' but blocking_flags is non-empty — contradiction")

    return result

if __name__ == "__main__":
    project_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    r = validate(project_dir)
    r.report()
    sys.exit(0 if r.passed else 1)
