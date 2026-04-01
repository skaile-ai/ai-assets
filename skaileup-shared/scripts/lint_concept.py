#!/usr/bin/env python3
"""
Concept Forge structure linter.

Validates _concept/ folder against shared contracts:
- Frontmatter compliance
- Cross-reference integrity
- Golden principles (entity rules, naming, numbering)
- postxl-schema.json schema validation
- seed.json scenario completeness

Error messages include remediation instructions so agents can self-fix.

Usage:
    python3 scripts/lint_concept.py [path_to_concept_folder]
    python3 scripts/lint_concept.py                          # defaults to ./_concept
"""

import json
import os
import re
import sys
from pathlib import Path
from dataclasses import dataclass, field
from typing import Optional


# ---------------------------------------------------------------------------
# Result types
# ---------------------------------------------------------------------------

@dataclass
class LintResult:
    severity: str          # CRITICAL, HIGH, MEDIUM, LOW
    category: str          # frontmatter, cross_ref, golden, structure, model, seed
    file: str
    message: str
    remediation: str

    def __str__(self):
        icon = {"CRITICAL": "🔴", "HIGH": "🟠", "MEDIUM": "🟡", "LOW": "🔵"}[self.severity]
        return f"{icon} [{self.severity}] {self.category}: {self.message}\n   File: {self.file}\n   Fix: {self.remediation}"


results: list[LintResult] = []


def emit(severity, category, file, message, remediation):
    results.append(LintResult(severity, category, str(file), message, remediation))


# ---------------------------------------------------------------------------
# YAML frontmatter parser (minimal, no dependencies)
# ---------------------------------------------------------------------------

def parse_frontmatter(filepath: Path) -> Optional[dict]:
    """Extract YAML frontmatter from a markdown file. Returns None if no frontmatter."""
    try:
        text = filepath.read_text(encoding="utf-8")
    except Exception:
        return None

    if not text.startswith("---"):
        return None

    end = text.find("---", 3)
    if end == -1:
        return None

    yaml_block = text[3:end].strip()
    fm = {}
    current_key = None
    current_list = None

    for line in yaml_block.split("\n"):
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue

        # List item
        if stripped.startswith("- ") and current_key:
            if current_list is None:
                current_list = []
                fm[current_key] = current_list
            val = stripped[2:].strip().strip("'\"")
            if isinstance(fm.get(current_key), list):
                fm[current_key].append(val)
            continue

        # Key: value
        if ":" in stripped:
            current_list = None
            key, _, val = stripped.partition(":")
            key = key.strip()
            val = val.strip().strip("'\"")
            current_key = key

            if val == "" or val == "|":
                fm[key] = val if val != "|" else ""
                continue
            if val == "[]":
                fm[key] = []
                continue
            if val.startswith("[") and val.endswith("]"):
                items = [v.strip().strip("'\"") for v in val[1:-1].split(",") if v.strip()]
                fm[key] = items
                continue
            if val.lower() in ("true", "false"):
                fm[key] = val.lower() == "true"
                continue
            fm[key] = val

    return fm


def get_body(filepath: Path) -> str:
    """Get markdown body after frontmatter."""
    try:
        text = filepath.read_text(encoding="utf-8")
    except Exception:
        return ""
    if not text.startswith("---"):
        return text
    end = text.find("---", 3)
    if end == -1:
        return text
    return text[end + 3:]


# ---------------------------------------------------------------------------
# Checks
# ---------------------------------------------------------------------------

def check_structure(concept: Path):
    """Check pipeline folders exist."""
    required = {
        "1_discovery/1_overview": "Run `concept-1-discovery-1-overview` to create the project brief.",
        "2_experience/1_journeys": "Run `concept-2-experience-1-journeys` to map user journeys.",
        "2_experience/2_features": "Run `concept-2-experience-2-features` to define features.",
        "3_blueprint/1_techstack": "Run `concept-3-blueprint-1-techstack` or create _concept/3_blueprint/1_techstack/stack.md manually.",
        "3_blueprint/3_datamodel": "Run `concept-3-blueprint-3-datamodel` to design the data model.",
        "2_experience/3_screens": "Run `concept-2-experience-3-screens` to specify screens.",
    }
    optional = {"1_discovery/2_research", "1_discovery/3_brand", "3_blueprint/2_architecture"}

    for folder, fix in required.items():
        if not (concept / folder).is_dir():
            emit("HIGH", "structure", folder, f"Required pipeline folder missing: {folder}/", fix)

    # Check 1_discovery/3_brand specifically for tokens.json
    brand = concept / "1_discovery/3_brand"
    if brand.is_dir():
        if not (brand / "tokens.json").exists():
            emit("MEDIUM", "structure", "1_discovery/3_brand/tokens.json",
                 "Brand folder exists but tokens.json is missing.",
                 "Create _concept/1_discovery/3_brand/tokens.json with color palette, fonts, and mode.")


def check_frontmatter(concept: Path):
    """Check all .md files have valid frontmatter."""
    for md in concept.rglob("*.md"):
        if ".snapshots" in str(md):
            continue
        rel = md.relative_to(concept)

        fm = parse_frontmatter(md)
        if fm is None:
            emit("HIGH", "frontmatter", str(rel),
                 "Missing YAML frontmatter.",
                 f"Add frontmatter to {rel}. See shared/contracts/frontmatter.md for required fields.")
            continue

        # Universal: status
        if "status" not in fm:
            emit("MEDIUM", "frontmatter", str(rel),
                 "Missing 'status' field in frontmatter.",
                 f"Add 'status: draft' to {rel} frontmatter.")

        valid_statuses = {"draft", "approved", "implemented", "tested", "mockup_ready"}
        if "status" in fm and fm["status"] not in valid_statuses:
            emit("MEDIUM", "frontmatter", str(rel),
                 f"Invalid status value: '{fm['status']}'.",
                 f"Use one of: {', '.join(sorted(valid_statuses))}")

        # Universal: last_updated
        if "last_updated" not in fm:
            emit("LOW", "frontmatter", str(rel),
                 "Missing 'last_updated' field.",
                 f"Add 'last_updated: YYYY-MM-DD' to {rel}. Or run `concept-review --garden`.")

        if "last_updated" in fm and fm["last_updated"]:
            if not re.match(r"^\d{4}-\d{2}-\d{2}$", str(fm["last_updated"])):
                emit("LOW", "frontmatter", str(rel),
                     f"Invalid last_updated format: '{fm['last_updated']}'.",
                     "Use ISO format: YYYY-MM-DD")

        # Feature-specific
        if str(rel).startswith("2_experience/2_features/"):
            for req_field in ["priority", "roles"]:
                if req_field not in fm:
                    emit("MEDIUM", "frontmatter", str(rel),
                         f"Feature file missing '{req_field}' field.",
                         f"Add '{req_field}' to {rel}. See shared/contracts/frontmatter.md.")

            if "priority" in fm and fm["priority"] not in ("must-have", "nice-to-have"):
                emit("MEDIUM", "frontmatter", str(rel),
                     f"Invalid priority: '{fm['priority']}'.",
                     "Use 'must-have' or 'nice-to-have'.")

            if "screens" not in fm:
                emit("LOW", "frontmatter", str(rel),
                     "Feature missing 'screens' field (should be [] initially).",
                     f"Add 'screens: []' to {rel}.")

            if "data_entities" not in fm:
                emit("LOW", "frontmatter", str(rel),
                     "Feature missing 'data_entities' field (should be [] initially).",
                     f"Add 'data_entities: []' to {rel}.")

        # Screen-specific
        if str(rel).startswith("2_experience/3_screens/") and not str(rel).startswith("2_experience/3_screens/00_layout"):
            if "implements" not in fm:
                emit("HIGH", "frontmatter", str(rel),
                     "Screen missing 'implements' field.",
                     f"Add 'implements:' with feature paths to {rel}.")


def check_golden_principles(concept: Path):
    """Check mechanical rules from golden_principles.md."""

    # Feature group numbering
    features_dir = concept / "2_experience/2_features"
    if features_dir.is_dir():
        groups = sorted([d.name for d in features_dir.iterdir() if d.is_dir()])
        for i, g in enumerate(groups):
            if not re.match(r"^\d{2}_", g):
                emit("MEDIUM", "golden", f"2_experience/2_features/{g}",
                     f"Feature group folder not numbered: '{g}'.",
                     f"Rename to '{str(i+1).zfill(2)}_{g}' to follow convention.")

        # Sequential check
        nums = [int(g[:2]) for g in groups if re.match(r"^\d{2}_", g)]
        for i in range(len(nums) - 1):
            if nums[i + 1] != nums[i] + 1:
                emit("MEDIUM", "golden", "2_experience/2_features/",
                     f"Feature group numbering gap: {nums[i]:02d} → {nums[i+1]:02d}.",
                     f"Renumber groups to be sequential (01, 02, 03...).")
                break

    # Screen groups must mirror feature groups
    screens_dir = concept / "2_experience/3_screens"
    if features_dir.is_dir() and screens_dir.is_dir():
        feat_groups = {d.name for d in features_dir.iterdir() if d.is_dir()}
        screen_groups = {d.name for d in screens_dir.iterdir() if d.is_dir() and d.name != "00_layout"}

        for fg in feat_groups:
            if fg not in screen_groups and screens_dir.exists():
                emit("LOW", "golden", f"2_experience/3_screens/{fg}",
                     f"Feature group '{fg}' has no matching screen group.",
                     f"Run `concept-2-experience-3-screens` to create screens for {fg}.")

    # Feature files must have requirement checkboxes
    if features_dir.is_dir():
        for md in features_dir.rglob("*.md"):
            body = get_body(md)
            rel = md.relative_to(concept)
            if "- [ ]" not in body and "- [x]" not in body:
                emit("MEDIUM", "golden", str(rel),
                     "Feature has no requirement checkboxes.",
                     f"Add '- [ ] Requirement' items under a ## Requirements heading in {rel}.")

    # Screen files must have component inventory and states
    if screens_dir.is_dir():
        for md in screens_dir.rglob("*.md"):
            if "00_layout" in str(md):
                continue
            body = get_body(md)
            rel = md.relative_to(concept)
            if "## Component Inventory" not in body and "## Components" not in body:
                emit("MEDIUM", "golden", str(rel),
                     "Screen missing component inventory section.",
                     f"Add '## Component Inventory' with numbered components to {rel}.")
            if "## States" not in body:
                emit("MEDIUM", "golden", str(rel),
                     "Screen missing states section.",
                     f"Add '## States' (default, loading, error, success) to {rel}.")

    # Naming: no spaces, lowercase
    for f in concept.rglob("*"):
        if ".snapshots" in str(f):
            continue
        if " " in f.name:
            rel = f.relative_to(concept)
            emit("MEDIUM", "golden", str(rel),
                 f"Filename contains spaces: '{f.name}'.",
                 f"Rename to '{f.name.replace(' ', '_').lower()}'.")


def check_cross_references(concept: Path):
    """Check two-way links between features and screens."""

    # Feature → Screen links
    features_dir = concept / "2_experience/2_features"
    screens_dir = concept / "2_experience/3_screens"

    if not features_dir.is_dir():
        return

    for md in features_dir.rglob("*.md"):
        fm = parse_frontmatter(md)
        if not fm:
            continue
        rel = md.relative_to(concept)

        screens = fm.get("screens", [])
        if isinstance(screens, list):
            for entry in screens:
                path = entry if isinstance(entry, str) else (entry.get("path", "") if isinstance(entry, dict) else "")
                if path and not (concept / path).exists():
                    emit("HIGH", "cross_ref", str(rel),
                         f"References screen '{path}' which does not exist.",
                         f"Remove the broken entry from screens[] in {rel}, or create the missing screen file.")

    # Screen → Feature links
    if not screens_dir.is_dir():
        return

    for md in screens_dir.rglob("*.md"):
        if "00_layout" in str(md):
            continue
        fm = parse_frontmatter(md)
        if not fm:
            continue
        rel = md.relative_to(concept)

        implements = fm.get("implements", [])
        if isinstance(implements, list):
            for feat_path in implements:
                if feat_path and not (concept / feat_path).exists():
                    emit("HIGH", "cross_ref", str(rel),
                         f"Implements feature '{feat_path}' which does not exist.",
                         f"Remove '{feat_path}' from implements[] in {rel}, or create the missing feature file.")

    # data_entities → postxl-schema.json model names
    schema_path = concept / "3_blueprint/3_datamodel" / "postxl-schema.json"
    if schema_path.exists():
        try:
            schema_data = json.loads(schema_path.read_text(encoding="utf-8"))
            model_names = {m.get("name") for m in schema_data.get("models", []) if m.get("name")}
        except (json.JSONDecodeError, AttributeError):
            model_names = set()

        for md in features_dir.rglob("*.md"):
            fm = parse_frontmatter(md)
            if not fm:
                continue
            rel = md.relative_to(concept)
            entities = fm.get("data_entities", [])
            if isinstance(entities, list):
                for entity_name in entities:
                    if entity_name and entity_name not in model_names:
                        emit("MEDIUM", "cross_ref", str(rel),
                             f"References data entity '{entity_name}' which is not a PascalCase model "
                             f"in postxl-schema.json.",
                             f"Add a '{entity_name}' model to postxl-schema.json, or fix the "
                             f"data_entities list in {rel}.")


def _load_postxl_schema(concept: Path):
    """Load and return parsed postxl-schema.json, or None on error."""
    schema_path = concept / "3_blueprint/3_datamodel" / "postxl-schema.json"
    if not schema_path.exists():
        return None

    try:
        data = json.loads(schema_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        emit("CRITICAL", "model", "3_blueprint/3_datamodel/postxl-schema.json",
             f"Invalid JSON: {e}",
             "Fix the JSON syntax error in postxl-schema.json.")
        return None

    return data


def _is_pascal_case(name: str) -> bool:
    """Check if a name is PascalCase (starts uppercase, no underscores)."""
    return bool(name) and name[0].isupper() and "_" not in name


def _is_camel_case(name: str) -> bool:
    """Check if a name is camelCase (starts lowercase, no underscores)."""
    return bool(name) and name[0].islower() and "_" not in name


def check_model(concept: Path):
    """Validate postxl-schema.json structure."""
    data = _load_postxl_schema(concept)
    if data is None:
        return

    # Required top-level fields
    for req in ["name", "slug", "models"]:
        if req not in data:
            emit("HIGH", "model", "3_blueprint/3_datamodel/postxl-schema.json",
                 f"Missing required top-level field: '{req}'.",
                 f"Add \"{req}\" to postxl-schema.json.")

    models = data.get("models", [])
    if not models:
        emit("HIGH", "model", "3_blueprint/3_datamodel/postxl-schema.json",
             "No models defined.",
             "Run `concept-3-blueprint-3-datamodel` to create models from features.")
        return

    # Collect all model names for relation validation
    model_names = set()
    for model in models:
        name = model.get("name", "")
        if name:
            model_names.add(name)

    # Standard models that relation fields may reference
    standard_models = {"User", "File", "Role", "Permission"}

    for model in models:
        mname = model.get("name", "<unknown>")

        # Model name must be PascalCase
        if not _is_pascal_case(mname):
            emit("MEDIUM", "model", "3_blueprint/3_datamodel/postxl-schema.json",
                 f"Model name '{mname}' is not PascalCase.",
                 f"Rename to PascalCase (e.g., start with uppercase, no underscores).")

        # Must have standardFields including id, createdAt, updatedAt
        std_fields = model.get("standardFields", [])
        required_std = {"id", "createdAt", "updatedAt"}
        missing_std = required_std - set(std_fields)
        if missing_std:
            emit("HIGH", "model", "3_blueprint/3_datamodel/postxl-schema.json",
                 f"Model '{mname}' missing standardFields: {', '.join(sorted(missing_std))}.",
                 f"Add {sorted(missing_std)} to standardFields in model '{mname}'. "
                 f"Required: [\"id\", \"createdAt\", \"updatedAt\"].")

        # Validate each field
        for fld in model.get("fields", []):
            fname = fld.get("name", "<unknown>")

            # Field name must be camelCase
            if not _is_camel_case(fname):
                emit("MEDIUM", "model", "3_blueprint/3_datamodel/postxl-schema.json",
                     f"Field '{fname}' in model '{mname}' is not camelCase.",
                     f"Rename to camelCase (start with lowercase, no underscores).")

            ftype = fld.get("type", "")

            # Relation fields must end with Id and reference a valid model
            if isinstance(ftype, str) and fname.endswith("Id"):
                # Derive referenced model name from field name (e.g., projectId -> Project)
                ref_name = fname[:-2]  # strip "Id"
                ref_model = ref_name[0].upper() + ref_name[1:] if ref_name else ""
                if ref_model and ref_model not in model_names and ref_model not in standard_models:
                    emit("MEDIUM", "model", "3_blueprint/3_datamodel/postxl-schema.json",
                         f"Relation field '{fname}' in model '{mname}' references "
                         f"'{ref_model}' which is not a defined model or standard model.",
                         f"Add a '{ref_model}' model to postxl-schema.json, or fix the field name.")

            # Inline enum: type is an object, values should be PascalCase strings
            if isinstance(ftype, dict):
                enum_values = ftype.get("values", [])
                for val in enum_values:
                    if isinstance(val, str) and not _is_pascal_case(val):
                        emit("LOW", "model", "3_blueprint/3_datamodel/postxl-schema.json",
                             f"Inline enum value '{val}' in field '{fname}' of model '{mname}' "
                             f"is not PascalCase.",
                             f"Rename to PascalCase (e.g., '{val[0].upper() + val[1:]}' if simple).")


def check_seed(concept: Path):
    """Validate seed.json scenarios."""
    seed_path = concept / "3_blueprint/3_datamodel" / "seed.json"
    if not seed_path.exists():
        return

    try:
        data = json.loads(seed_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        emit("HIGH", "seed", "3_blueprint/3_datamodel/seed.json",
             f"Invalid JSON: {e}",
             "Fix the JSON syntax error in seed.json.")
        return

    scenarios = data.get("scenarios", {})
    required = ["empty", "single_user", "populated", "edge_cases"]
    for name in required:
        if name not in scenarios:
            emit("MEDIUM", "seed", "3_blueprint/3_datamodel/seed.json",
                 f"Missing required scenario: '{name}'.",
                 f"Add a '{name}' scenario to seed.json. See shared/contracts/seed_data.md.")

    # Check populated has enough data
    pop = scenarios.get("populated", {}).get("data", {})
    for model, entries in pop.items():
        if isinstance(entries, list) and len(entries) < 2:
            emit("LOW", "seed", "3_blueprint/3_datamodel/seed.json",
                 f"Scenario 'populated' has only {len(entries)} {model} entries (need 2+).",
                 f"Add more realistic {model} entries to the 'populated' scenario.")

    # Validate model keys are PascalCase (matching postxl-schema.json)
    for scenario_name, scenario in scenarios.items():
        scenario_data = scenario.get("data", {})
        for model_key in scenario_data:
            if model_key and not _is_pascal_case(model_key):
                emit("LOW", "seed", "3_blueprint/3_datamodel/seed.json",
                     f"Model key '{model_key}' in scenario '{scenario_name}' is not PascalCase.",
                     f"Rename to PascalCase to match postxl-schema.json model names.")


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    concept_path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("_concept")

    if not concept_path.is_dir():
        print(f"Error: '{concept_path}' is not a directory.")
        sys.exit(1)

    print(f"Linting {concept_path.resolve()}\n")

    check_structure(concept_path)
    check_frontmatter(concept_path)
    check_golden_principles(concept_path)
    check_cross_references(concept_path)
    check_model(concept_path)
    check_seed(concept_path)

    if not results:
        print("✅ All checks passed. No issues found.")
        sys.exit(0)

    # Sort by severity
    severity_order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3}
    results.sort(key=lambda r: severity_order[r.severity])

    counts = {}
    for r in results:
        counts[r.severity] = counts.get(r.severity, 0) + 1

    for r in results:
        print(r)
        print()

    print("─" * 60)
    print(f"Total: {len(results)} issues", end="")
    for sev in ["CRITICAL", "HIGH", "MEDIUM", "LOW"]:
        if sev in counts:
            print(f" | {counts[sev]} {sev.lower()}", end="")
    print()

    # Exit code: non-zero if any CRITICAL or HIGH
    blocking = counts.get("CRITICAL", 0) + counts.get("HIGH", 0)
    if blocking:
        print(f"\n❌ {blocking} blocking issue(s). Fix before proceeding.")
        sys.exit(1)
    else:
        print(f"\n⚠ {len(results)} non-blocking issue(s). Consider fixing.")
        sys.exit(0)


if __name__ == "__main__":
    main()
