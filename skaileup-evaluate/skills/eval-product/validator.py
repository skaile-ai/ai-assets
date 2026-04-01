#!/usr/bin/env python3
"""Validator for eval-product skill output."""
import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import ValidationResult, check_field, check_range, check_enum

def validate(project_dir: str) -> ValidationResult:
    result = ValidationResult()
    output_path = Path(project_dir) / "_implementation" / "eval-product.json"

    if not output_path.exists():
        result.error("eval-product.json not found at _implementation/eval-product.json")
        return result

    with open(output_path) as f:
        data = json.load(f)

    check_field(result, data, "schema_version", str)
    check_field(result, data, "goals", list)
    check_field(result, data, "design", dict)
    check_field(result, data, "performance", dict)
    check_range(result, data, "accessibility_score", 0, 100)
    check_range(result, data, "mobile_score", 0, 100)
    check_field(result, data, "improvement_priorities", list)
    check_enum(result, data, "verdict", ["approved", "needs_iteration", "fail"])

    design = data.get("design", {})
    for dim in ["quality", "originality", "craft", "functionality"]:
        check_range(result, design, dim, 0, 10)

    for goal in data.get("goals", []):
        check_field(result, goal, "goal", str)
        check_enum(result, goal, "achieved", ["achieved", "partial", "not_achieved"])

    design_avg = sum(design.get(d, 0) for d in ["quality", "originality", "craft", "functionality"]) / 4
    if data.get("verdict") == "approved" and design_avg < 7:
        result.warning("verdict is 'approved' but design average is below 7.0")

    return result

if __name__ == "__main__":
    project_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    r = validate(project_dir)
    r.report()
    sys.exit(0 if r.passed else 1)
