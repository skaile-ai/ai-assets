#!/usr/bin/env python3
"""Validator for eval-feature skill output."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main


def validate(cwd: str) -> dict:
    v = Validator(cwd, "eval-feature")

    # Directory and file existence
    v.must(
        "_implementation/eval-feature/ directory exists",
        lambda: v.dir_exists("_implementation/eval-feature"),
    )
    v.must(
        "at least one .json file in _implementation/eval-feature/",
        lambda: v.dir_not_empty("_implementation/eval-feature", "*.json"),
    )

    # Per-file checks — collected after the directory checks above
    json_files = v.glob_files("_implementation/eval-feature/*.json")

    for json_file in json_files:
        rel = str(json_file.relative_to(Path(cwd)))
        name = json_file.name

        def make_fields_check(path):
            def check():
                return v.json_field_exists(
                    path,
                    "schema_version",
                    "feature_group",
                    "acceptance_criteria",
                    "screen_fidelity_score",
                    "journey_completable",
                    "regression_issues",
                    "deviations",
                    "verdict",
                )
            return check

        v.must(f"{name}: has required fields", make_fields_check(rel))

        def make_fidelity_check(path, fname):
            def check():
                data = v.read_json(path)
                if data is None:
                    return False, "file not readable"
                s = data.get("screen_fidelity_score", -1)
                if not (0 <= s <= 100):
                    return False, f"screen_fidelity_score={s} out of range 0-100"
                return True, ""
            return check

        v.must(f"{name}: screen_fidelity_score in 0-100", make_fidelity_check(rel, name))

        def make_journey_check(path, fname):
            def check():
                data = v.read_json(path)
                if data is None:
                    return False, "file not readable"
                jc = data.get("journey_completable")
                allowed = {"true", "false", "partial"}
                if jc not in allowed:
                    return False, f"journey_completable={jc!r} not in {sorted(allowed)}"
                return True, ""
            return check

        v.must(f"{name}: journey_completable is valid enum value", make_journey_check(rel, name))

        def make_verdict_check(path, fname):
            def check():
                data = v.read_json(path)
                if data is None:
                    return False, "file not readable"
                verdict = data.get("verdict")
                allowed = {"approved", "needs_revision", "escalate"}
                if verdict not in allowed:
                    return False, f"verdict={verdict!r} not in {sorted(allowed)}"
                return True, ""
            return check

        v.must(f"{name}: verdict is valid enum value", make_verdict_check(rel, name))

        def make_criteria_fields_check(path, fname):
            def check():
                data = v.read_json(path)
                if data is None:
                    return False, "file not readable"
                criteria = data.get("acceptance_criteria", [])
                for i, c in enumerate(criteria):
                    if not isinstance(c, dict):
                        return False, f"acceptance_criteria[{i}] is not an object"
                    for field in ("id", "text", "result"):
                        if field not in c:
                            return False, f"acceptance_criteria[{i}] missing field '{field}'"
                return True, ""
            return check

        v.must(f"{name}: each acceptance criterion has required fields", make_criteria_fields_check(rel, name))

        def make_criteria_result_check(path, fname):
            def check():
                data = v.read_json(path)
                if data is None:
                    return False, "file not readable"
                criteria = data.get("acceptance_criteria", [])
                allowed = {"pass", "fail", "partial", "untestable"}
                for i, c in enumerate(criteria):
                    if not isinstance(c, dict):
                        continue
                    r = c.get("result")
                    if r not in allowed:
                        return False, f"acceptance_criteria[{i}].result={r!r} not in {sorted(allowed)}"
                return True, ""
            return check

        v.must(f"{name}: each criterion result is valid enum value", make_criteria_result_check(rel, name))

        # Subjective check: non-approved verdict should have revision_instructions
        v.skip(
            f"{name}: non-approved verdict has non-empty revision_instructions",
            rule_type="MUST",
            reason="semantic — whether revision_instructions are sufficient requires human review",
        )

    return v.result()


if __name__ == "__main__":
    main(validate)
