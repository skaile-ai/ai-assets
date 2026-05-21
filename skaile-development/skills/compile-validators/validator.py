#!/usr/bin/env python3
"""Auto-generated validator for compile-validators.
Re-generate with: skaile-development/compile-validators target=compile-validators
"""
import sys
from pathlib import Path

_VLIB = None
for _base in (Path(__file__).resolve(), *Path(__file__).resolve().parents):
    for _rel in (
        ("ai-assets-skaileup", "contracts", "scripts"),
        ("ai-assets-skaileup", "skaileup-contracts", "scripts"),
        ("skaileup-shared", "scripts"),
    ):
        _cand = _base.joinpath(*_rel)
        if (_cand / "validator_lib.py").exists():
            _VLIB = str(_cand)
            break
    if _VLIB:
        break
if _VLIB:
    sys.path.insert(0, _VLIB)

from validator_lib import Validator, main

SKILL = "compile-validators"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # Structural: every skaile-development skill with rules has a validator.py next to its SKILL.md.
    skills_dir = Path(cwd) / "ai-assets" / "skaile-development" / "skills"

    def every_skill_has_validator():
        if not skills_dir.is_dir():
            return True, "skills directory not present in this cwd — skipped"
        missing = []
        for skill_md in skills_dir.glob("*/SKILL.md"):
            text = skill_md.read_text()
            has_rules = ("MUST " in text or "NEVER " in text or "- [ ]" in text)
            validator_py = skill_md.parent / "validator.py"
            if has_rules and not validator_py.exists():
                missing.append(str(skill_md.parent.relative_to(cwd)))
        if missing:
            return False, f"Missing validator.py in: {', '.join(missing)}"
        return True, ""

    v.must(
        "every skaile-development skill with MUST/NEVER/CHECKLIST has a validator.py",
        every_skill_has_validator,
    )

    v.skip("read validator_lib.py before generating any validator", rule_type="MUST",
           reason="runtime — agent behavior")
    v.skip("handle missing files gracefully", rule_type="MUST",
           reason="semantic — generated code style")
    v.skip("mark every semantic/subjective rule with v.skip()", rule_type="MUST",
           reason="semantic — classification correctness")
    v.skip("use sys.path depth = 3 for flat skills", rule_type="MUST",
           reason="semantic — generated code correctness")
    v.skip("test every generated validator runs without errors", rule_type="MUST",
           reason="runtime — subprocess invocation")

    # ── NEVER rules ──
    v.skip("use external dependencies beyond Python stdlib + validator_lib", rule_type="NEVER",
           reason="semantic — generated code content")
    v.skip("generate validators that call an LLM, subprocess, or network", rule_type="NEVER",
           reason="semantic — generated code content")
    v.skip("hardcode absolute paths", rule_type="NEVER",
           reason="semantic — generated code style")
    v.skip("overwrite an existing validator.py without reading it first", rule_type="NEVER",
           reason="runtime — safety check")

    # ── CHECKLIST ──
    v.skip("validator_lib.py read before generating any validator", rule_type="CHECKLIST",
           reason="runtime — agent behavior")
    v.skip("Every MUST rule is either structural or skipped", rule_type="CHECKLIST",
           reason="semantic — classification")
    v.skip("Every NEVER rule is either structural or skipped", rule_type="CHECKLIST",
           reason="semantic — classification")
    v.skip("Every CHECKLIST item is either structural or skipped", rule_type="CHECKLIST",
           reason="semantic — classification")
    v.skip("Correct sys.path depth (parents[3])", rule_type="CHECKLIST",
           reason="semantic — generated code correctness")
    v.skip("Each generated validator imports and runs without error", rule_type="CHECKLIST",
           reason="runtime — subprocess")
    v.skip("Semantic rules carry a clear skip reason", rule_type="CHECKLIST",
           reason="semantic — content quality")
    v.skip("No external dependencies beyond stdlib + validator_lib", rule_type="CHECKLIST",
           reason="semantic — generated code content")

    return v.result()


if __name__ == "__main__":
    main(validate)
