#!/usr/bin/env python3
"""Auto-generated validator for concept-add-feature.
Re-generate with: /compile-validators concept-add-feature
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-add-feature"
FEATURES_DIR = "_concept/2_experience/2_features"
SCHEMA = "_concept/3_blueprint/3_datamodel/postxl-schema.json"
FEATURE_MAP = "_concept/3_blueprint/3_datamodel/feature_map.json"
SEED = "_concept/3_blueprint/3_datamodel/seed.json"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("read the full existing concept before making any changes",
           reason="process — cannot verify read behavior")

    v.skip("present impact assessment before starting cascade",
           reason="process — user interaction")

    v.skip("get user approval after feature spec AND after cascade changes",
           reason="process — user interaction")

    v.skip("follow feedback loop protocol — cross-references bidirectional",
           reason="semantic — cross-reference integrity")

    # If schema exists, check it's valid JSON
    def check_schema_valid():
        if not (v.cwd / SCHEMA).exists():
            return True, ""  # Schema may not have been modified
        data = v.read_json(SCHEMA)
        if data is None:
            return False, f"Invalid JSON in {SCHEMA}"
        return True, ""

    v.must("validate postxl-schema.json with pxl validate", check_schema_valid)

    # Check labelField if schema exists
    def check_label_fields():
        data = v.read_json(SCHEMA)
        if data is None:
            return True, ""  # Schema may not exist
        for model_name, model in data.get("models", {}).items():
            label = model.get("labelField")
            if not label:
                continue
            fields = model.get("fields", {})
            if label not in fields:
                return False, f"{model_name}.labelField '{label}' not in fields"
            if fields[label].get("type") != "String":
                return False, f"{model_name}.labelField '{label}' not String type"
        return True, ""

    v.must("ensure labelField resolves to a String-type field", check_label_fields)

    v.skip("run regression check after implementation",
           reason="runtime — requires test execution")

    # Check seed data casing if modified
    def check_seed_format():
        data = v.read_json(SEED)
        if data is None:
            return True, ""  # May not exist or not modified
        for scenario_name, scenario in data.items():
            if not isinstance(scenario, dict):
                continue
            for key in scenario.keys():
                if key[0].isupper():
                    return False, f"seed.json '{scenario_name}': PascalCase key '{key}'"
        return True, ""

    v.must("use backend-compatible format for seed data", check_seed_format)

    # ── NEVER rules ──

    v.skip("make cascade changes without user approval",
           rule_type="NEVER", reason="process — user interaction")

    v.skip("create pipeline steps that don't already exist",
           rule_type="NEVER", reason="process — scope control")

    v.skip("invent colors or fonts — consume from tokens.json",
           rule_type="NEVER", reason="semantic — content origin")

    v.skip("skip regression check during implementation",
           rule_type="NEVER", reason="process — testing")

    v.skip("modify existing features' functionality without explicit user request",
           rule_type="NEVER", reason="process — scope control")

    v.skip("break existing cross-references",
           rule_type="NEVER", reason="semantic — integrity")

    v.skip("implement before tests exist (TDD is non-negotiable)",
           rule_type="NEVER", reason="process — TDD workflow")

    # ── CHECKLIST ──

    v.skip("Existing concept fully read and understood",
           rule_type="CHECKLIST", reason="process — cannot verify")

    v.skip("Impact assessment presented and approved",
           rule_type="CHECKLIST", reason="process — user interaction")

    # Check that at least one feature file exists with valid frontmatter
    def check_feature_exists():
        files = v.glob_files(f"{FEATURES_DIR}/**/*.md")
        if not files:
            return False, "No feature files found"
        return True, ""

    v.checklist("Feature spec written and approved", check_feature_exists)

    v.skip("All affected downstream artifacts updated (cascade)",
           rule_type="CHECKLIST", reason="semantic — cross-reference")

    v.skip("Cross-references bidirectional and valid",
           rule_type="CHECKLIST", reason="semantic — integrity")

    v.checklist("postxl-schema.json validates (if modified)", check_schema_valid)

    v.skip("Lint passes",
           rule_type="CHECKLIST", reason="runtime — requires lint execution")

    v.skip("Snapshots updated (if .snapshots/ exists)",
           rule_type="CHECKLIST", reason="process — snapshot management")

    v.skip("Implementation complete with passing E2E tests (if applicable)",
           rule_type="CHECKLIST", reason="runtime — requires test execution")

    v.skip("Regression check passed (if applicable)",
           rule_type="CHECKLIST", reason="runtime — requires test execution")

    return v.result()


if __name__ == "__main__":
    main(validate)
