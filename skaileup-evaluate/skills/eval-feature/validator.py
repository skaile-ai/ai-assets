#!/usr/bin/env python3
"""Validator for eval-feature skill output."""
import sys
import json
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import ValidationResult, check_field, check_range, check_enum

def validate(project_dir: str, group: str = None) -> ValidationResult:
    result = ValidationResult()
    eval_dir = Path(project_dir) / "_implementation" / "eval-feature"

    if not eval_dir.exists():
        result.error("_implementation/eval-feature/ directory not found")
        return result

    files = list(eval_dir.glob("*.json"))
    if not files:
        result.error("No eval-feature result files found in _implementation/eval-feature/")
        return result

    target = [f for f in files if group and f.stem == group] or files
    for path in target:
        with open(path) as f:
            data = json.load(f)

        check_field(result, data, "schema_version", str)
        check_field(result, data, "feature_group", str)
        check_field(result, data, "acceptance_criteria", list)
        check_range(result, data, "screen_fidelity_score", 0, 100)
        jc = data.get("journey_completable")
        if jc not in ["true", "false", "partial", True, False]:
            result.error(f"journey_completable must be 'true', 'false', or 'partial', got: {jc!r}")
        check_field(result, data, "regression_issues", list)
        check_field(result, data, "deviations", list)
        check_enum(result, data, "verdict", ["approved", "needs_revision", "escalate"])

        for criterion in data.get("acceptance_criteria", []):
            check_field(result, criterion, "id", str)
            check_field(result, criterion, "text", str)
            check_enum(result, criterion, "result", ["pass", "fail", "partial", "untestable"])

        if data.get("verdict") != "approved" and not data.get("revision_instructions"):
            result.warning(f"{path.name}: verdict is not 'approved' but revision_instructions is empty")

    return result

if __name__ == "__main__":
    project_dir = sys.argv[1] if len(sys.argv) > 1 else "."
    group = sys.argv[2] if len(sys.argv) > 2 else None
    r = validate(project_dir, group)
    r.report()
    sys.exit(0 if r.passed else 1)
