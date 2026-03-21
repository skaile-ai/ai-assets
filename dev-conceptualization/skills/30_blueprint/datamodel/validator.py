#!/usr/bin/env python3
"""Validator for the datamodel skill.
Re-generate with: /compile-validators datamodel
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "dev-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "datamodel"
DBML = "_concept/3_blueprint/3_datamodel/model.dbml"
MODEL = "_concept/3_blueprint/3_datamodel/model.json"
SEED = "_concept/3_blueprint/3_datamodel/seed.json"
FEATURE_MAP = "_concept/3_blueprint/3_datamodel/feature_map.json"
FEATURES_DIR = "_concept/2_experience/2_features"


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.must("produce model.dbml", lambda: v.file_exists(DBML))
    v.must("produce model.json", lambda: v.file_exists(MODEL))
    v.must("produce seed.json", lambda: v.file_exists(SEED))
    v.must("produce feature_map.json", lambda: v.file_exists(FEATURE_MAP))

    # Every entity in model.json traces to a feature in feature_map.json
    def check_feature_map():
        model = v.read_json(MODEL)
        fmap = v.read_json(FEATURE_MAP)
        if model is None:
            return False, f"Cannot read {MODEL}"
        if fmap is None:
            return False, f"Cannot read {FEATURE_MAP}"
        entities = {e.get("id") for e in model.get("entities", []) if e.get("id")}
        # feature_map.json uses PascalCase display_name keys
        # Build a lookup: snake_case entity id → PascalCase display_name
        entity_display = {e.get("id"): e.get("display_name", "") for e in model.get("entities", [])}
        mapped_display = set(fmap.keys())
        unmapped = []
        for eid in entities:
            display = entity_display.get(eid, "")
            if display not in mapped_display and eid not in mapped_display:
                unmapped.append(display or eid)
        if unmapped:
            return False, f"Entities not in feature_map.json: {', '.join(unmapped)}"
        return True, ""

    v.must("trace every entity back to at least one feature in feature_map.json",
           check_feature_map)

    # Check all four seed scenarios present
    def check_seed_scenarios():
        data = v.read_json(SEED)
        if data is None:
            return False, f"Cannot read {SEED}"
        # Support both flat and versioned format
        scenarios = data.get("scenarios", data) if isinstance(data, dict) else {}
        required = ("empty", "single_user", "populated", "edge_cases")
        missing = [s for s in required if s not in scenarios]
        if missing:
            return False, f"seed.json missing scenarios: {', '.join(missing)}"
        return True, ""

    v.must("include all four seed scenarios", check_seed_scenarios)

    # Check field names are snake_case in model.json
    def check_field_naming():
        import re
        model = v.read_json(MODEL)
        if model is None:
            return True, ""
        for entity in model.get("entities", []):
            for field in entity.get("fields", []):
                name = field.get("name", "")
                if name in ("id",) or not name:
                    continue
                # snake_case: lowercase letters, digits, underscores
                if not re.match(r"^[a-z][a-z0-9_]*$", name):
                    return False, (f"Entity '{entity.get('id')}' field '{name}' "
                                   f"is not snake_case (golden_principles)")
        return True, ""

    v.must("use snake_case field names (golden_principles)", check_field_naming)

    # ── NEVER rules ──

    v.skip("use SQL types (VARCHAR, INT, BOOLEAN)",
           rule_type="NEVER", reason="semantic — type validation requires semantic_types catalog")

    # ── CHECKLIST ──

    v.checklist("model.dbml exists", lambda: v.file_exists(DBML))
    v.checklist("model.json exists", lambda: v.file_exists(MODEL))
    v.checklist("seed.json has all four scenarios", check_seed_scenarios)
    v.checklist("feature_map.json — every entity traces to a feature", check_feature_map)
    v.checklist("Field names are snake_case", check_field_naming)

    # Feedback loop: feature data_entities[] populated
    def check_feedback_loop():
        features = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        if not features:
            return False, "No feature files found"
        missing = []
        for f in features:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm and "data_entities" not in fm:
                missing.append(rel)
        if missing:
            return False, f"Features missing data_entities[] in frontmatter: {', '.join(missing[:3])}"
        return True, ""

    v.checklist("Feature files updated with data_entities[] (feedback loop)", check_feedback_loop)

    v.skip("All enum values are PascalCase",
           rule_type="CHECKLIST", reason="semantic — enum value extraction from DBML")

    v.skip("User has explicitly approved the data model",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
