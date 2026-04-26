#!/usr/bin/env python3
"""Validator for the add-feature skill.
Re-generate with: /compile-validators add-feature
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[4] / "skaileup-shared" / "scripts"))
from validator_lib import Validator, main  # noqa: E402

SKILL = "add-feature"
FEATURES_DIR = "_concept/experience/features"
MODEL_JSON = "_concept/blueprint/datamodel/model.json"
FEATURE_MAP = "_concept/blueprint/datamodel/feature_map.json"
SEED = "_concept/blueprint/datamodel/seed.json"

REQUIRED_FM = ("priority", "story_refs", "roles", "last_updated")


def validate(cwd: str) -> dict:
    v = Validator(cwd, SKILL)

    # ── MUST rules ──

    v.skip("read the full existing concept before making any changes",
           reason="process — cannot verify read behavior")

    v.skip("present impact assessment before starting cascade",
           reason="process — user interaction")

    v.skip("get user approval after feature spec AND after cascade changes",
           reason="process — user interaction")

    # Check at least one feature file exists
    def check_features_exist():
        files = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        if not files:
            return False, f"No feature files found in {FEATURES_DIR}"
        return True, f"{len(files)} feature file(s) found"

    v.must("produce at least one feature spec", check_features_exist)

    # Check feature files have required frontmatter fields (no status field)
    def check_feature_frontmatter():
        files = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        missing_fields = []
        for f in files:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm is None:
                continue
            for field in REQUIRED_FM:
                if field not in fm:
                    missing_fields.append(f"{rel}: missing '{field}'")
        if missing_fields:
            return False, f"Feature files missing required fields: {'; '.join(missing_fields[:3])}"
        return True, ""

    v.must("feature spec has required frontmatter fields (priority, story_refs, roles, last_updated)",
           check_feature_frontmatter)

    # If model.json exists, check it's valid JSON
    def check_model_valid():
        if not (v.cwd / MODEL_JSON).exists():
            return True, ""  # Model may not have been modified
        data = v.read_json(MODEL_JSON)
        if data is None:
            return False, f"Invalid JSON in {MODEL_JSON}"
        return True, ""

    v.must("model.json is valid JSON (if modified)", check_model_valid)

    # Check seed.json uses singular snake_case keys (not PascalCase/camelCase plural)
    def check_seed_format():
        data = v.read_json(SEED)
        if data is None:
            return True, ""  # May not exist or not modified
        scenarios = data.get("scenarios", data) if isinstance(data, dict) else {}
        for scenario_name, scenario in scenarios.items():
            if not isinstance(scenario, dict):
                continue
            for key in scenario.keys():
                # PascalCase or camelCase plural keys are wrong
                if key[0].isupper():
                    return False, (f"seed.json '{scenario_name}': PascalCase key '{key}' — "
                                   f"use singular snake_case (golden_principles.md)")
        return True, ""

    v.must("use singular snake_case entity keys in seed.json (golden_principles.md)",
           check_seed_format)

    v.skip("follow feedback loop protocol — cross-references bidirectional",
           reason="semantic — cross-reference integrity checking requires full scan")

    # ── NEVER rules ──

    # Check no status field in feature frontmatter
    def check_no_status():
        files = list(v.glob_files(f"{FEATURES_DIR}/**/*.md"))
        for f in files:
            rel = str(f.relative_to(v.cwd))
            fm = v.parse_frontmatter(rel)
            if fm and "status" in fm:
                return False, f"{rel} has 'status' field — globally removed"
        return True, ""

    v.never("add status field to feature frontmatter (globally removed)", check_no_status)

    v.skip("cascade to artifacts that don't already exist",
           rule_type="NEVER", reason="process — scope control")

    v.skip("renumber existing feature groups",
           rule_type="NEVER", reason="process — cannot verify from files")

    v.skip("invent colors or fonts — consume from tokens.json",
           rule_type="NEVER", reason="semantic — content origin")

    # ── CHECKLIST ──

    v.checklist("Feature spec exists with required frontmatter", check_feature_frontmatter)

    v.checklist("No status field in feature frontmatter", check_no_status)

    v.checklist("model.json is valid JSON (if modified)", check_model_valid)

    v.checklist("seed.json uses singular snake_case entity keys (if modified)", check_seed_format)

    # Check feature_map.json is valid JSON if it exists
    def check_feature_map_valid():
        if not (v.cwd / FEATURE_MAP).exists():
            return True, ""
        data = v.read_json(FEATURE_MAP)
        if data is None:
            return False, f"Invalid JSON in {FEATURE_MAP}"
        return True, ""

    v.checklist("feature_map.json is valid JSON (if modified)", check_feature_map_valid)

    v.skip("Impact assessment presented and approved",
           rule_type="CHECKLIST", reason="process — user interaction")

    v.skip("Cascade applied in correct order (Journeys → Techstack → Architecture → Datamodel → Screens)",
           rule_type="CHECKLIST", reason="process — execution order")

    v.skip("All cross-references bidirectional and valid",
           rule_type="CHECKLIST", reason="semantic — requires full concept scan")

    v.skip("User has explicitly approved cascade",
           rule_type="CHECKLIST", reason="process — user interaction")

    return v.result()


if __name__ == "__main__":
    main(validate)
