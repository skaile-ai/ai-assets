---
name: "compile-validators"
description: "Compiles MUST/NEVER/CHECKLIST rules from SKILL.md files into fast, deterministic Python validators. Run after editing or creating a skill to generate its validator.py. Replaces slow LLM-based validation with sub-second checks."
metadata:
  version: "1.0.0"
  tags:
    - "validation"
    - "rules"
    - "compilation"
    - "deterministic"
    - "fast"
    - "linting"
  source: "MERGED"
---

# Compile Validators — Validator Generator

## Overview

Reads SKILL.md rule blocks (MUST, NEVER, CHECKLIST) and generates fast Python
validator scripts that check output artifacts deterministically, without calling
an LLM. Replaces slow LLM-based validation with sub-second structural checks.

## When to Use

- After creating or editing a SKILL.md with MUST/NEVER/CHECKLIST rules
- To regenerate a stale `validator.py` after skill rules change
- To bootstrap validators for all skills at once (`compile-validators all`)

## When NOT to Use

- The skill has no MUST/NEVER/CHECKLIST rules — nothing to compile
- The validator already exists and rules haven't changed

---

ROLE  Validator Compiler — reads SKILL.md rules and generates fast Python validator scripts
      that check output artifacts deterministically, without calling an LLM.

READS
  <skill-dir>/SKILL.md                 — the skill definition containing rules to compile

WRITES
  <skill-dir>/validator.py             — generated deterministic validator

REFERENCES
  dev-shared/scripts/validator_lib.py        — shared validation library (read for full API)
  dev-shared/contracts/skill_grammar.md      — MUST/NEVER/CHECKLIST DSL keywords

STEP 1: Determine scope
  IF user names a specific skill (e.g. "compile-validators journeys")
    - Find the SKILL.md in the appropriate ai-resources/<domain>/skills/ directory
    - Compile only that skill
  ELSE IF user says "all" or gives no argument
    - Find all SKILL.md files with MUST/NEVER/CHECKLIST rules under ai-resources/
    - Compile each one

STEP 2: For each skill, read the SKILL.md
  - Extract all MUST rules (lines starting with "MUST")
  - Extract all NEVER rules (lines starting with "NEVER")
  - Extract all CHECKLIST items ("- [ ] ...")
  - Extract WRITES paths — these are the output directories to validate
  - Extract OUTPUT templates — these define the expected file structure and fields
  - Note any JSON Schema references (e.g. "validate against stories_schema.json")

STEP 3: Classify each rule
  Determine whether each rule is **structural** (deterministically checkable)
  or **semantic** (requires human/LLM judgment):

  STRUCTURAL — generate Python check code:
    - File/directory existence: "X exists", "write X before Y"
    - JSON key presence: "include field X in Y.json"
    - JSON key values: "set status: draft", "exactly one hero"
    - JSON Schema validation: "validate against schema.json"
    - Frontmatter field presence: "include frontmatter: X, Y, Z"
    - Frontmatter field values: "set priority on all features"
    - Folder naming: "numbered group folders", "NN_ prefix"
    - Cross-references: "every model maps to a feature", "trace to story"
    - Counting: "exactly one", "at least one per", "every X has Y"
    - Key casing: "no camelCase", "PascalCase models"
    - Boundary: "never write outside X/"

  SEMANTIC — call v.skip() with reason:
    - Quality: "generic", "cookie-cutter", "rich", "memorable"
    - Relevance: "focus on custom business logic"
    - Process: "discuss with user first", "ask before writing"
    - Runtime: "builds without errors", "passes lint"
    - Subjective: "justified typography choices", "not just hex values"

STEP 4: Generate validator.py

  **Determining sys.path depth:**
  Skill validators live at different nesting depths within ai-resources/:
  - Flat: `ai-resources/<domain>/skills/<skill>/validator.py` → 3 parents = `ai-resources/`
  - Grouped: `ai-resources/<domain>/skills/<group>/<skill>/validator.py` → 4 parents = `ai-resources/`

  Count the directory depth of the skill relative to `ai-resources/` and use the
  corresponding number of parents: 3 for flat (`skills/<skill>/`), 4 for grouped
  (`skills/<group>/<skill>/`).

  Read ai-resources/dev-shared/scripts/validator_lib.py to understand the full API.
  Generate a Python script following this exact template:

  OUTPUT <skill-dir>/validator.py
    #!/usr/bin/env python3
    """Auto-generated validator for <skill-name>.
    Re-generate with: /compile-validators <skill-name>
    """
    import sys
    from pathlib import Path
    sys.path.insert(0, str(Path(__file__).resolve().parents[N] / "dev-shared" / "scripts"))
    from validator_lib import Validator, main

    SKILL = "<skill-name>"

    def validate(cwd: str) -> dict:
        v = Validator(cwd, SKILL)

        # ── MUST rules ──
        <generated checks>

        # ── NEVER rules ──
        <generated checks>

        # ── CHECKLIST ──
        <generated checks>

        # ── Semantic (skipped) ──
        <v.skip() calls>

        return v.result()

    if __name__ == "__main__":
        main(validate)

STEP 5: Write checks using the validator_lib API

  Common patterns (read validator_lib.py for the full API):

  # File existence
  v.must("shell.md exists", lambda: v.file_exists("_concept/2_experience/3_screens/00_layout/shell.md"))

  # Directory exists and has content
  v.must("features written", lambda: v.dir_not_empty("_concept/2_experience/2_features", "**/*.md"))

  # JSON top-level keys
  v.must("tokens.json has all sections",
         lambda: v.json_field_exists("_concept/1_discovery/2_brand/tokens.json",
                                     "colors", "fonts", "radius", "mode", "shadows", "atmosphere", "tailwind"))

  # JSON nested structure — use inline lambda with read_json
  v.must("exactly one hero story map", lambda: (
      v.json_count(
          (v.read_json("_concept/2_experience/1_journeys/stories.json") or {}).get("story_maps", []),
          lambda m: m.get("stage") == "hero",
          expected=1, op="eq"
      )
  ))

  # Every item in a JSON array has a field
  v.must("every story has acceptance criteria", lambda: (
      v.json_array_all_have_nonempty(
          [s for m in (v.read_json("_concept/2_experience/1_journeys/stories.json") or {}).get("story_maps", [])
             for s in m.get("stories", [])],
          "acceptance_criteria",
          context="in stories.json"
      )
  ))

  # Frontmatter on all files matching a glob
  v.must("all features have required frontmatter",
         lambda: v.all_files_have_frontmatter(
             "_concept/2_experience/2_features/**/*.md",
             "priority", "story_refs", "roles", "last_updated"))

  # Folder naming pattern
  v.must("numbered group folders",
         lambda: v.folders_match_pattern(
             "_concept/2_experience/2_features",
             r"^\d{2}_"))

  # JSON Schema validation
  v.checklist("stories.json validates against schema", lambda: (
      v.json_schema_validate(
          "_concept/2_experience/1_journeys/stories.json",
          "ai-resources/dev-shared/contracts/stories_schema.json")
  ))

  # Cross-reference: every key maps to existing files
  v.checklist("every model maps to a feature",
              lambda: v.every_key_maps_to_existing_file(
                  "_concept/3_blueprint/3_datamodel/feature_map.json"))

  # Semantic rules — skip
  v.skip("produce generic brand output", rule_type="NEVER", reason="semantic — quality judgment")
  v.skip("focus on custom business logic", rule_type="MUST", reason="semantic — content relevance")

STEP 6: Test the generated validator
  RUN  python3 <skill-dir>/validator.py --cwd <project-dir> --json
  - Verify no import errors
  - Verify the JSON output has the expected structure
  - Fix any issues

STEP 7: Report
  - Show a summary for each compiled skill:
    | Skill | Structural | Semantic (skipped) | Total |
    |-------|-----------|-------------------|-------|
    | <skill> | N checks | N skipped | N rules |
  - If any rules were surprisingly hard to classify, mention them

MUST  read validator_lib.py before generating any validator (for the full API)
MUST  handle missing files gracefully — check existence before reading content
MUST  mark all semantic/subjective rules with v.skip() and a clear reason
MUST  use the correct sys.path depth (3 for flat skills, 4 for grouped skills)
MUST  test each generated validator runs without errors
NEVER  use external dependencies beyond Python stdlib + validator_lib
NEVER  generate validators that call an LLM, subprocess, or network
NEVER  hardcode absolute paths — all paths are relative to cwd
NEVER  delete or overwrite an existing validator.py without reading it first

CHECKLIST
  - [ ] validator_lib.py was read for the full API
  - [ ] Every MUST/NEVER rule is either a structural check or explicitly skipped
  - [ ] Every CHECKLIST item is either a structural check or explicitly skipped
  - [ ] Correct sys.path depth used (count parents to ai-resources/)
  - [ ] Generated validator runs without import or runtime errors
  - [ ] Semantic rules have clear skip reasons
  - [ ] No external dependencies used
