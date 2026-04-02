#!/usr/bin/env python3
"""Validator for eval-concept skill output."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent.parent / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main


def validate(cwd: str) -> dict:
    v = Validator(cwd, "eval-concept")

    # File existence
    v.must("eval-concept.json exists", lambda: v.file_exists("_concept/eval-concept.json"))

    # Required top-level fields
    v.must(
        "has required fields",
        lambda: v.json_field_exists(
            "_concept/eval-concept.json",
            "schema_version",
            "evaluated_at",
            "completeness_score",
            "clarity_score",
            "traceability_score",
            "overall_score",
            "verdict",
            "blocking_flags",
            "warning_flags",
            "summary",
        ),
    )

    # Score ranges
    def check_completeness_score():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        s = data.get("completeness_score", -1)
        if not (0 <= s <= 100):
            return False, f"completeness_score={s} out of range 0-100"
        return True, ""

    def check_clarity_score():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        s = data.get("clarity_score", -1)
        if not (0 <= s <= 100):
            return False, f"clarity_score={s} out of range 0-100"
        return True, ""

    def check_traceability_score():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        s = data.get("traceability_score", -1)
        if not (0 <= s <= 100):
            return False, f"traceability_score={s} out of range 0-100"
        return True, ""

    def check_overall_score():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        s = data.get("overall_score", -1)
        if not (0 <= s <= 100):
            return False, f"overall_score={s} out of range 0-100"
        return True, ""

    v.must("completeness_score in 0-100", check_completeness_score)
    v.must("clarity_score in 0-100", check_clarity_score)
    v.must("traceability_score in 0-100", check_traceability_score)
    v.must("overall_score in 0-100", check_overall_score)

    # Verdict enum
    def check_verdict():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        verdict = data.get("verdict")
        allowed = {"pass", "needs_resolution", "fail"}
        if verdict not in allowed:
            return False, f"verdict={verdict!r} not in {sorted(allowed)}"
        return True, ""

    v.must("verdict is valid enum value", check_verdict)

    # blocking_flags structure
    def check_blocking_flags_fields():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        flags = data.get("blocking_flags", [])
        for i, flag in enumerate(flags):
            if not isinstance(flag, dict):
                return False, f"blocking_flags[{i}] is not an object"
            for field in ("type", "location", "description", "resolution"):
                if field not in flag:
                    return False, f"blocking_flags[{i}] missing field '{field}'"
        return True, ""

    def check_blocking_flags_type_enum():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        flags = data.get("blocking_flags", [])
        allowed = {"missing", "ambiguous", "contradiction", "orphan", "untraceable"}
        for i, flag in enumerate(flags):
            if not isinstance(flag, dict):
                continue
            t = flag.get("type")
            if t not in allowed:
                return False, f"blocking_flags[{i}].type={t!r} not in {sorted(allowed)}"
        return True, ""

    v.must("each blocking_flag has required fields", check_blocking_flags_fields)
    v.must("each blocking_flag type is valid enum value", check_blocking_flags_type_enum)

    # Contradiction check: pass verdict with non-empty blocking_flags
    def check_verdict_contradiction():
        data = v.read_json("_concept/eval-concept.json")
        if data is None:
            return False, "file not readable"
        if data.get("verdict") == "pass" and data.get("blocking_flags"):
            return False, "verdict='pass' but blocking_flags is non-empty — contradiction"
        return True, ""

    v.must("verdict=pass only when blocking_flags is empty", check_verdict_contradiction)

    return v.result()


if __name__ == "__main__":
    main(validate)
