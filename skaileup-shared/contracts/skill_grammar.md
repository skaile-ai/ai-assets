# Skill DSL Grammar

This document defines the structured language used by all skill files.
Read this once before consuming any DSL-formatted skill.

The agent IS the parser. Keywords are uppercase labels at the start of a
line. Indented lines belong to the preceding keyword. YAML frontmatter
(name, description, keywords) is preserved for skill discovery.

---

## Keywords

### ROLE

One-sentence identity statement. Defines the agent's scope boundary.

```
ROLE  Data Model agent — produces the canonical data model from features and tech stack.
```

---

### READS

Input artifacts. Each line: path (glob ok) + brief purpose.
`?` prefix = optional. Stop if required reads are missing.

```
READS
  _concept/discovery/brief.md          — app name, audience
  _concept/experience/features/**/*.md          — requirements per feature
  _concept/blueprint/techstack.md         — chosen stack (drives output format)
  ? _concept/experience/journeys/stories.json   — user journeys and acceptance criteria
```

---

### WRITES

Output artifacts. Each line: path + brief description.

```
WRITES
  _concept/blueprint/datamodel/model.json       — canonical semantic data model
  _concept/blueprint/datamodel/seed.json        — scenario-based test data
  _concept/blueprint/datamodel/feature_map.json — model-to-feature mapping
```

---

### REFERENCES

Pointers to detailed reference files that explain HOW and WHY.
The DSL says WHAT; references explain the rest.

```
REFERENCES
  skaileup-shared/contracts/semantic_types.md   — field type catalog and translation table
  skaileup-shared/contracts/feedback_loop.md   — cross-reference protocol
  skaileup-shared/contracts/golden_principles.md — mechanical rules enforced across artifacts
```

---

### REQUIRES

File and tool prerequisites. `hard:` = block execution without it. `soft:` = warn and continue.
Path lines accept `_concept/` paths (files or directories) or tool names.

```
REQUIRES
  hard: _concept/discovery/brief.md  — Project brief must exist
  soft: _concept/experience/journeys/stories.json  — Enriches output but not required
  hard: git
  soft: docker (database setup deferred without it)
  state: _concept/experience/features/ contains at least one .md file
```

`hard:` lines map to `metadata.prerequisites.files[].gate: hard` in the frontmatter schema.
`soft:` lines map to `gate: soft`. The orchestrator enforces `hard` gates before dispatching;
`soft` gates emit a warning to the user and continue.

---

### INPUT

User input declaration. Declares where pre-collected inputs are read from and what to ask if
missing. Corresponds to `metadata.prerequisites.inputs_required` and `inputs_optional`.

```
INPUT
  Read from: _concept/_grounding/{skill-id}/input.json
  If missing, ask the user:
  - scope: Feature scope (required) [must-have-only | all-features] default: all-features
  - target_stack: Target technology stack (optional) [nuxt | nextjs | sveltekit]
```

Format for each input line: `- <id>: <label> (<required|optional>) [<option1> | <option2>] default: <value>`
Omit options for free-text inputs. Omit default if none.

---

### STEP <n>: <title>

Numbered workflow step. Body contains imperative instructions.
Substeps use indented `- ` bullets. Shell commands use `$ ` prefix.

```
STEP 1: Read context
  - Read brief.md for app name and slug
  - Read stack.md to determine output format (model.json / schema.prisma / postxl-schema.json)
  - Count feature files and note entity candidates

STEP 2: Draft data model
  - Derive entities from feature data_entities[] fields
  - Apply standard_fields: ["id", "created_at", "updated_at"] to every entity
  - $ cat _concept/experience/features/**/*.md | grep data_entities
```

---

### RUN

Shell command to execute. Inline within a step, or standalone.
Multiple commands = multiple RUN lines. Use `||` for fallbacks.

```
RUN  bun run validate:model
RUN  bun run test:unit
RUN  bun run lint || echo "Fix lint errors above before continuing"
```

---

### OUTPUT <path>

Expected file output with inline template. Shows structure, not full content.

```
OUTPUT _concept/blueprint/datamodel/model.json
  {
    "entities": {
      "<entity_name>": {
        "standard_fields": ["id", "created_at", "updated_at"],
        "fields": { ... }
      }
    }
  }
```

---

### EMIT

Observability event. Format: `[skill-name] event_type key=value ...`
Skill name is the canonical kebab-case name from SKILL.md frontmatter.

```
EMIT  [datamodel] started run_id=<uuid>
EMIT  [datamodel] checkpoint entities=5 stack=prisma
EMIT  [datamodel] completed entities=5 seed_scenarios=4
```

---

### CHECKPOINT [<name>]

Human approval gate. Execution pauses until user approves.
Optional name for tracking in PLANS.md.

```
CHECKPOINT data_model
  Show the entity list with field counts to the user.
  > "Approve to continue, or tell me what to change."
```

---

### IF / ELSE

Conditional branch. Condition is a plain-language predicate.

```
IF _concept/experience/journeys/stories.json exists
  - Read journey acceptance criteria for state transitions
  - Use EARS criteria to derive enum values
ELSE
  - Derive enums from feature requirements only
```

---

### UNTIL <condition>

Repeat the preceding step(s) until condition is met.

```
STEP 6: Implement feature
  - Build from screen spec component inventory
  - $ bun run e2e -- --grep "<feature>"
  UNTIL all E2E tests pass
```

---

### MUST / NEVER

Hard constraints. MUST = required invariant. NEVER = forbidden action.
Enforced post-hoc by `validate_skill_rules.py` (see Enforcement below).

```
MUST  apply standard_fields (id, created_at, updated_at) to every entity
MUST  validate data model before proceeding to screens
NEVER  invent field types not in semantic_types.md
NEVER  modify upstream _concept/ files during implementation
```

**Enforcement:** A Claude Code `PostToolUse` hook runs
`skaileup-shared/scripts/validate_skill_rules.py` after every skill completes.
The script extracts all MUST/NEVER lines, reads the generated output
files, and calls `claude -p` to validate compliance. Exit code 2 blocks
the agent with a violation report until issues are fixed.

**Authoring tips for reliable enforcement:**

1. **Inline critical rules into STEPs.** A MUST in a consolidated block
   at line 460 competes with 400 lines of procedural steps. Repeat the
   rule inside the STEP where the action happens — redundancy is
   acceptable for compliance.
2. **Keep rule count manageable.** Skills with 30+ constraints see lower
   compliance than skills with <10. Use templates to bake in defaults
   so the rule isn't needed.
3. **Make rules verifiable from files.** Rules like "MUST import X" can
   be checked via grep. Rules like "MUST build without errors" require
   runtime checks — pair these with a RUN command in the CHECKLIST.
4. **Place constraints early.** Skills with constraints before STEP 1
   show better compliance than skills with constraints after the last step.

---

### CHECKLIST

Verification items. Agent checks each before completing the skill.

```
CHECKLIST
  - [ ] All output files written to correct paths
  - [ ] Cross-references valid (feature_map.json, data_entities[])
  - [ ] Build succeeds
  - [ ] No lint or type errors introduced
```

---

### PROCEDURE <name>

Named reusable sub-procedure. Referenced from steps via `DO <name>`.

```
PROCEDURE validate_cross_refs
  - Check every path in frontmatter resolves to an existing file
  - Check every data_entities[] entry exists in model.json
  - Report any broken references before continuing

STEP 5: Finalize
  CHECKPOINT model_approved
  DO validate_cross_refs
```

---

### PATTERNS

Inline code patterns. Keep minimal — move extended examples to `references/`.

```
PATTERNS
  # Brand token usage (never hardcode colors)
  color: var(--color-primary)

  # Scoped Playwright locator (avoid sidebar false matches)
  await page.locator('main').getByText(/label/i)
```

---

## Conventions

- **One keyword per line.** Indented lines are continuation.
- **Steps are sequential** unless wrapped in `IF` or marked `(parallel)`.
- **`?` prefix** on READS/REQUIRES lines means optional.
- **`$ ` prefix** in step body means shell command (equivalent to RUN).
- **`> "..."` in CHECKPOINT** is the prompt shown to the user.
- **`DO <name>`** invokes a PROCEDURE defined elsewhere in the skill.
- **`hard:` / `soft:` in REQUIRES** maps to `gate: hard` / `gate: soft` in frontmatter `prerequisites.files`.
- **INPUT** declares pre-collection location and fallback prompts; maps to `prerequisites.inputs_required` / `inputs_optional`.
- Prose explanations belong in `references/` files, not in the DSL body.
- YAML frontmatter is preserved unchanged for skill discovery tooling.
