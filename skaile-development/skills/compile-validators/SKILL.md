---
name: "compile-validators"
description: "Compiles MUST/NEVER/CHECKLIST rules from skaile-development SKILL.md
  files into fast, deterministic Python validators. Generates a validator.py alongside
  each SKILL.md. Replaces slow LLM-based validation with sub-second structural checks.
  Run after editing any skill in ai-assets/skaile-development/skills/."
metadata:
  tags:
  - "validation"
  - "rules"
  - "compilation"
  - "deterministic"
  - "linting"
  - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    inputs_optional:
    - id: target
      label: "Skill to compile (or 'all')"
      type: text
      default: "all"
      hint: "e.g. audit, test-plan, test-unit, or 'all' for every skaile-development
        skill"
    reads:
    - path: "ai-assets/skaile-development/skills/<skill>/SKILL.md"
      description: "Skill definition with MUST/NEVER/CHECKLIST rules"
    - path: "ai-assets-skaileup/contracts/scripts/validator_lib.py"
      description: "Shared validation library API"
    produces:
    - path: "ai-assets/skaile-development/skills/<skill>/validator.py"
      description: "Generated deterministic validator"
  user_inputs:
    dialog:
    - id: "target"
      label: "Skill name (or 'all')"
      type: "text"
      required: false
      default: "all"
    files: []
---

# Compile Validators — Skaile-Development Rule Compiler

## Overview

Reads MUST / NEVER / CHECKLIST blocks from `ai-assets/skaile-development/skills/<skill>/SKILL.md` and emits a Python validator that checks skill outputs deterministically (without calling an LLM).

Scoped to `ai-assets/skaile-development/skills/` by default. Produces one `validator.py` per skill.

## When to Use

- After creating or editing any SKILL.md in `ai-assets/skaile-development/skills/`
- To bootstrap validators for every skaile-development skill at once (`target=all`)
- When a skill's WRITES or OUTPUT template changes

## When NOT to Use

- For skills that have no MUST/NEVER/CHECKLIST — nothing to compile
- For skills in other domains (those are compiled by their own domain tooling)

---

ROLE  Validator compiler for skaile-development skills. Reads a SKILL.md, classifies each rule as structural or semantic, generates a Python validator that runs in sub-second time.

READS
  ! ai-assets/skaile-development/skills/<skill>/SKILL.md
  ! ai-assets-skaileup/contracts/scripts/validator_lib.py   — shared validator API (referenced, not copied)

WRITES
  ai-assets/skaile-development/skills/<skill>/validator.py

MUST  read validator_lib.py before generating any validator (for the full API)
MUST  handle missing files gracefully — check existence before reading content
MUST  mark every semantic/subjective rule with v.skip() and a clear reason
MUST  locate validator_lib with the walk-up resolver (never a hardcoded parents[N]) so the validator runs from both the ai-assets source tree and the .claude/skills copy
MUST  test every generated validator runs without errors
NEVER use external dependencies beyond Python stdlib + validator_lib
NEVER generate validators that call an LLM, subprocess, or network
NEVER hardcode absolute paths — all paths are relative to cwd
NEVER overwrite an existing validator.py without reading it first

EMIT [compile-validators] started target=<target>

# ── Phase 1: Scope ───────────────────────────────────────────────

STEP 1: Resolve target
  IF target = all:
    - Glob ai-assets/skaile-development/skills/*/SKILL.md
  ELSE:
    - Use ai-assets/skaile-development/skills/<target>/SKILL.md
    - If missing: stop with "Skill not found"

# ── Phase 2: Extract Rules ───────────────────────────────────────

STEP 2: For each SKILL.md, extract:
  - All MUST rules (lines starting with `MUST  ` at column 1)
  - All NEVER rules (lines starting with `NEVER ` at column 1)
  - All CHECKLIST items (`  - [ ] ...`)
  - WRITES paths (output directories to validate)
  - OUTPUT templates (expected file structure and JSON shape)
  - Any referenced schemas

# ── Phase 3: Classify ────────────────────────────────────────────

STEP 3: For each rule, decide structural vs. semantic

  STRUCTURAL (generate a check):
    - "exists" / "written before" — v.file_exists, v.dir_not_empty
    - "has field X in Y.json" — v.json_field_exists
    - "field value equals Z" — v.json_field_value
    - "validate against schema.json" — v.json_schema_validate
    - "every file has frontmatter X" — v.all_files_have_frontmatter
    - "folder naming pattern" — v.folders_match_pattern
    - "every key maps to existing file" — v.every_key_maps_to_existing_file
    - "exactly one / at least one" — v.json_count

  SEMANTIC (v.skip with reason):
    - Quality / "well-written" / "clean" / "idiomatic"
    - Process ("discuss with user first", "ask before writing")
    - Runtime ("builds without errors" — belongs to `audit` not a validator)
    - Subjective judgments

# ── Phase 4: Emit Python ─────────────────────────────────────────

STEP 4: Write validator.py

  Template:
  ```python
  #!/usr/bin/env python3
  """Auto-generated validator for <skill-name>.
  Re-generate with: skaile-development/compile-validators target=<skill-name>
  """
  import sys
  from pathlib import Path

  # Locate the shared validator_lib by walking up from this file and probing
  # known relative paths. Location-independent: the same file works whether run
  # from the ai-assets source tree or the .claude/skills copy (different depths).
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

  SKILL = "<skill-name>"

  def validate(cwd: str) -> dict:
      v = Validator(cwd, SKILL)

      # ── MUST rules ──
      <generated structural checks>

      # ── NEVER rules ──
      <generated structural checks>

      # ── CHECKLIST ──
      <generated structural checks>

      # ── Semantic (skipped) ──
      <v.skip() calls>

      return v.result()

  if __name__ == "__main__":
      main(validate)
  ```

  **Locating validator_lib:** never hardcode a single `parents[N]` — the shared
  `validator_lib.py` lives in the sibling submodule at
  `ai-assets-skaileup/contracts/scripts/`, and the validator must also run from the
  shallower `.claude/skills/<skill>/` copy. Use the walk-up resolver shown in the
  template above: it ascends from the validator's own directory through every
  ancestor, probing each candidate relative path, so it resolves correctly from any
  copy/depth. `("ai-assets-skaileup", "skaileup-contracts", "scripts")` and
  `("skaileup-shared", "scripts")` are kept only as legacy fallbacks.

# ── Phase 5: Test ────────────────────────────────────────────────

STEP 5: Verify every generated validator runs
  $ python3 ai-assets/skaile-development/skills/<skill>/validator.py --cwd <test-cwd> --json
  - Verify no import errors
  - Verify JSON output has expected keys (must, never, checklist, skipped)
  - If import fails, confirm the walk-up resolver's candidate paths still match where validator_lib.py actually lives
  - Run the same check on the .claude/skills/<skill>/ copy too — the resolver must work from both depths

# ── Phase 6: Report ──────────────────────────────────────────────

STEP 6: Emit summary
  | Skill | Structural | Semantic (skipped) | Total rules |
  |---|---|---|---|
  | audit | 12 | 4 | 16 |
  | test-plan | 8 | 2 | 10 |
  | test-unit | 10 | 3 | 13 |

  If any rule was hard to classify, list it under "Ambiguous" for human review.

EMIT [compile-validators] completed skills=<N> structural=<N> semantic=<N>

CHECKLIST
  - [ ] validator_lib.py read before generating any validator
  - [ ] Every MUST rule is either structural or skipped
  - [ ] Every NEVER rule is either structural or skipped
  - [ ] Every CHECKLIST item is either structural or skipped
  - [ ] validator_lib located via the walk-up resolver (no hardcoded parents[N]); runs from both ai-assets and .claude/skills copies
  - [ ] Each generated validator imports and runs without error
  - [ ] Semantic rules carry a clear skip reason
  - [ ] No external dependencies beyond stdlib + validator_lib

---

## Integration

- **Called by:** Skill authors (after edits), `ready` (checks validator freshness via mtime), `quality`
- **Reads:** skill SKILL.md files, validator_lib.py
- **Writes:** `<skill>/validator.py` alongside each SKILL.md
