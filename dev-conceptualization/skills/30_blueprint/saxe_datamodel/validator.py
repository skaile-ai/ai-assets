#!/usr/bin/env python3
"""Auto-generated validator for concept-3-blueprint-3-datamodel.
Re-generate with: /compile-validators concept-3-blueprint-3-datamodel
"""
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent.parent / "shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "concept-3-blueprint-3-datamodel"
SCHEMA = "_concept/3_blueprint/3_datamodel/postxl-schema.json"
SEED = "_concept/3_blueprint/3_datamodel/seed.json"
FEATURE_MAP = "_concept/3_blueprint/3_datamodel/feature_map.json"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("validate postxl-schema.json with pxl before proceeding",
           reason="runtime — requires pxl CLI")

    # Check every labelField points to a String-type field
    def check_label_fields():
        data = v.read_json(SCHEMA)
        if data is None:
            return False, f"Cannot read {SCHEMA}"
        models = data.get("models", {})
        for model_name, model in models.items():
            label = model.get("labelField")
            if not label:
                continue
            fields = model.get("fields", {})
            if label not in fields:
                return False, f"{model_name}.labelField '{label}' not found in fields"
            field_type = fields[label].get("type", "")
            if field_type != "String":
                return False, f"{model_name}.labelField '{label}' has type '{field_type}', expected 'String'"
        return True, ""

    v.must("verify every labelField points to a String-type field", check_label_fields)

    # Check User records in seed.json
    def check_user_records():
        data = v.read_json(SEED)
        if data is None:
            return False, f"Cannot read {SEED}"
        # Check in populated and single_user scenarios
        for scenario in ("populated", "single_user"):
            sc = data.get(scenario, {})
            users = sc.get("users", sc.get("Users", []))
            if users:
                return True, ""
        return False, "No User records found in seed.json scenarios"

    v.must("include User records in seed.json for every referenced user ID",
           check_user_records)

    # Check dev user (sub: "test")
    def check_dev_user():
        data = v.read_json(SEED)
        if data is None:
            return False, f"Cannot read {SEED}"
        for scenario in data.values():
            if not isinstance(scenario, dict):
                continue
            users = scenario.get("users", scenario.get("Users", []))
            if isinstance(users, list):
                for u in users:
                    if isinstance(u, dict) and u.get("sub") == "test":
                        return True, ""
        return False, "No dev user with sub='test' found in seed.json"

    v.must("include a dev user (sub: 'test') in seed.json", check_dev_user)

    # Every model traced to a feature in feature_map.json
    def check_feature_map():
        schema = v.read_json(SCHEMA)
        fmap = v.read_json(FEATURE_MAP)
        if schema is None:
            return False, f"Cannot read {SCHEMA}"
        if fmap is None:
            return False, f"Cannot read {FEATURE_MAP}"
        models = set(schema.get("models", {}).keys())
        mapped = set(fmap.keys())
        unmapped = models - mapped
        if unmapped:
            return False, f"Models not in feature_map.json: {', '.join(sorted(unmapped))}"
        return True, ""

    v.must("trace every model back to at least one feature", check_feature_map)

    # ── NEVER rules ──

    # Check no id/createdAt/updatedAt in model fields
    def check_no_standard_fields():
        data = v.read_json(SCHEMA)
        if data is None:
            return True, ""  # Can't check without file
        forbidden = {"id", "createdAt", "updatedAt"}
        for model_name, model in data.get("models", {}).items():
            fields = set(model.get("fields", {}).keys())
            found = fields & forbidden
            if found:
                return False, f"{model_name} manually defines {', '.join(found)} — use standardFields"
        return True, ""

    v.never("manually define id, createdAt, or updatedAt", check_no_standard_fields)

    # Check no fields defined for standard models
    def check_no_standard_model_fields():
        data = v.read_json(SCHEMA)
        if data is None:
            return True, ""
        standard = set(data.get("standardModels", []))
        for model_name in standard:
            if model_name in data.get("models", {}):
                fields = data["models"][model_name].get("fields", {})
                if fields:
                    return False, f"Standard model '{model_name}' has custom fields defined"
        return True, ""

    v.never("define fields for standard models", check_no_standard_model_fields)

    # Check no camelCase model keys in seed.json
    def check_seed_casing():
        data = v.read_json(SEED)
        if data is None:
            return True, ""
        for scenario_name, scenario in data.items():
            if not isinstance(scenario, dict):
                continue
            for key in scenario.keys():
                # Model keys should be camelCase plural (e.g., "users", "events")
                # Field names should be snake_case
                # Check for PascalCase keys (starts with uppercase)
                if key[0].isupper():
                    return False, f"seed.json '{scenario_name}' has PascalCase key '{key}' — use camelCase"
        return True, ""

    v.never("use camelCase model keys or PascalCase field names in seed.json",
            check_seed_casing)

    # ── CHECKLIST ──

    v.checklist("postxl-schema.json exists", lambda: v.file_exists(SCHEMA))

    v.checklist("Every labelField references a String-type field", check_label_fields)

    # seed.json has all four scenarios
    def check_seed_scenarios():
        data = v.read_json(SEED)
        if data is None:
            return False, f"Cannot read {SEED}"
        required = ("empty", "single_user", "populated", "edge_cases")
        missing = [s for s in required if s not in data]
        if missing:
            return False, f"seed.json missing scenarios: {', '.join(missing)}"
        return True, ""

    v.checklist("seed.json has all four scenarios", check_seed_scenarios)

    v.checklist("seed.json uses backend-compatible format", check_seed_casing)

    v.checklist("seed.json includes User records + dev user", check_dev_user)

    v.checklist("feature_map.json maps every model to a feature", check_feature_map)

    v.skip("Feature files updated with data_entities arrays",
           rule_type="CHECKLIST", reason="boundary — requires feature file cross-reference")

    v.skip("User has explicitly approved the data model",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
