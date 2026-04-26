# Contract Migration Notes

Documents structural changes between the legacy CF/Saxe variants and the merged contracts.
Skills use this to detect and handle projects created with older tooling.

---

## concept_structure.md

### Path renames

| Old path (CF flat) | Old path (Saxe grouped) | New canonical path |
|---|---|---|
| `01_project/` | `discovery/` | `discovery/` |
| `04_brand/` | `discovery/3_brand/` | `discovery/brand/` |
| `03_features/` | `experience/features/` | `experience/features/` |
| `03b_behavior/` | — | `experience/behaviors/` |
| `05_techstack/` | `blueprint/` | `blueprint/` |
| `05b_architecture/` | `blueprint/` | `blueprint/` |
| `06_datamodel/` | `blueprint/datamodel/` | `blueprint/datamodel/` |
| `07_screens/` | `experience/screens/` | `experience/screens/` |
| `08_testing/` | — | removed |
| `_research/` | `discovery/2_research/` | `_grounding/general/` |
| `02_research/` | — | `_grounding/general/` |

### Feature group prefix change

| Old | New |
|---|---|
| `A_01_user_auth/` (CF letter+number) | `01_user_auth/` |
| `01_user_auth/` (Saxe number only) | `01_user_auth/` ✓ no change |

### Datamodel file renames

| Old file | Condition | New file |
|---|---|---|
| `model.dbml` + `model.json` | generic/unknown stack | `model.dbml` + `model.json` |
| `postxl-schema.json` | PostXL/NestJS stack | `postxl-schema.json` |
| — | Prisma stack | `schema.prisma` |
| `seed.json` | any | `seed.json` ✓ no change |
| — | any | `feature_map.json` (new, added) |

### Detection heuristic for agents

```
if _concept/01_project/ exists → legacy CF flat structure
  remap paths per table above when writing new files
  read from both old and new paths during transition

if _concept/discovery/ exists → Saxe or merged structure
  check subfolder numbering:
    2_brand/ (new) vs 3_brand/ (old Saxe) → prefer 2_brand/

if _concept/A_01_*/ exists inside features/ → legacy letter+number groups
  treat as equivalent to 01_*/
```

### Removed

- `08_testing/` / `4_testing/` — test plans are no longer part of the concept artifact
- `PLANS.md` reference — documented in `plans.md` contract only

---

---

## frontmatter.md

### Removed fields

| Field | Was in | Reason |
|---|---|---|
| `status` (universal + per-file) | Saxe | Removed — lifecycle tracking removed entirely |
| `impl_status` | CF | Removed — replaced nothing; lifecycle tracking removed |
| `complexity_tier` | Saxe (brief.md) | Moved — stored as user decision in `_grounding/` |
| `hosting` | CF (stack.md) | Removed |

### Added fields

| Field | File | Source |
|---|---|---|
| `permissions` | feature.md | Saxe |
| `story_refs` | feature.md | Saxe |
| `framework` | stack.md | Saxe |
| `orm` | stack.md | Saxe |
| `custom_modules` | architecture.md | Saxe |

### Path changes

| Old reference | New reference |
|---|---|
| `02_research/` / `discovery/2_research/` | `_grounding/general/` |
| `07_screens/` | `experience/screens/` |
| `03_features/` | `experience/features/` |
| `05_techstack/` | `blueprint/` |
| `05b_architecture/` | `blueprint/` |

---

---

## semantic_types.md

### Comparison

| Aspect | CF | Saxe | Decision |
|---|---|---|---|
| Approach | Semantic abstraction layer (`string`, `text`, `uuid`, etc.) | No abstraction — writes Prisma/PostXL types directly (`String`, `Int`) | **Keep CF semantic layer** — stack translation is an output step, not the model |
| Optionality | Not expressed in the type | `?` suffix on type (`String?`, `DateTime?`) | **`required: false` on the field** — `?` is a translation artifact |
| `decimal` type | Not present | `Decimal` for financial/precise values | **Adopted from Saxe** |
| Enum definition | `enum_id` referencing a top-level enum | Inline object `{"Draft": "...", "Published": "..."}` with PascalCase values | **Inline object adopted from Saxe** — cleaner, self-contained |
| Enum value casing | Not specified | PascalCase enforced (`Draft`, `InProgress`) | **PascalCase rule adopted from Saxe** |
| Relation convention | `m2o`/`o2m`/`m2m` types, separate `relationships[]` array | Field ends in `Id`, type = target model name; inverse auto-generated; m2m = explicit junction | **Saxe convention adopted** — simpler, less duplication |
| Model metadata | None | `labelField`, `keyField`, `standardFields`, `faker` | **Adopted from Saxe**, renamed to snake_case (`label_field`, `key_field`, `standard_fields`) |
| Field properties | None | `isUnique`, `isReadonly`, `hasIndex`, `maxLength`, `placeholder` | **Adopted from Saxe**, renamed to snake_case |
| Translation table | Directus / Prisma / Supabase / Raw SQL | Migration reference from semantic → PostXL | **Merged** — PostXL added as a column (Prisma column updated with `?` and `Decimal`) |
| Field naming | Not specified | camelCase | **snake_case in semantic layer** — camelCase is a Prisma/PostXL output convention |

### Renamed conventions

| Saxe (camelCase) | Merged (snake_case) |
|---|---|
| `labelField` | `label_field` |
| `keyField` | `key_field` |
| `standardFields` | `standard_fields` |
| `isUnique` | `unique` |
| `isReadonly` | `readonly` |
| `hasIndex` | `indexed` |
| `maxLength` | `max_length` |
| `isCreatedAt` / `isUpdatedAt` | handled by `standard_fields` |

### Stack translator responsibilities (not in semantic layer)

- Prisma/PostXL: append `?` for optional fields, emit `standardFields` in PascalCase (`createdAt`), generate inverse relations
- Directus: map to system collections, M2O/O2M/M2M field types
- SQL: generate `CREATE TABLE` DDL with appropriate NULL constraints and FK references

---

---

## feedback_loop.md

### Comparison

| Protocol | CF | Saxe | Decision |
|---|---|---|---|
| Journeys → Features | Not present | `story_refs` back-links on feature frontmatter | **Adopted from Saxe** |
| Features → Screens | Present | Present (same rule) | **Kept, paths updated** |
| Screens → Features (back-link) | Present | Present (same rule) | **Kept, paths updated** |
| Datamodel → Features | `from_features` on model entities | `feature_map.json` as dedicated file | **`feature_map.json` adopted from Saxe** |
| Behaviors protocol | Present (allium files) | Not present | **Kept from CF**, path updated to `experience/behaviors/` |
| `screens[]` entry format | `path:` only | `path:` + `status:` | **`path:` only** — status removed globally |
| `story_refs` validation rule | Not present | Warning when story_refs → missing journey ID | **Adopted from Saxe** |

### Path changes in examples

| Old (CF) | Old (Saxe) | Canonical |
|---|---|---|
| `07_screens/01_user_auth/login.md` | `experience/screens/01_user_auth/login.md` | `experience/screens/01_user_auth/login.md` |
| `03_features/01_user_auth/login.md` | `experience/features/01_user_auth/login.md` | `experience/features/01_user_auth/login.md` |
| `03b_behavior/user_auth.allium` | not present | `experience/behaviors/user_auth.allium` |

---

---

## pipeline.json → removed

### Comparison

| Aspect | CF pipeline.json | Saxe pipeline.json | Decision |
|---|---|---|---|
| Skill catalog (hard_gates, user_inputs, folder) | Centralized in one file | Centralized in one file | **Distributed into flow nodes** — each node carries its own metadata |
| Execution order / dependency graph | Embedded in steps[] | Explicit depends_on arrays | **Already in flows** via edges — not needed in pipeline.json |
| Phases taxonomy | Named objects with sub-phases and orders | Simple arrays of step IDs | **Remains in flows** as group nodes + `skaileup-shared/contracts/flows.md` |
| Feedback loops (machine-readable) | `feedback_loops[]` top-level array | `feedback_loops[]` top-level array | **Moved to flow node `feedback` field** per-skill |
| Global runtime config | `config` section (standalone_mode, grounding_mode, standards_mode) | Not present | **Moved to `skaileup-shared/agent-config.json`** |
| `grounding_mode.step_folders` mapping | Central lookup table | Not present | **Moved to flow node `grounding_folder` field** per-skill |
| Implementation pipeline | Embedded as steps with phase:implementation | Explicit `implementation_pipeline[]` | **Already in mvp.json and other flows** |
| Post-pipeline / incremental | Embedded as steps with phase:quality | `post_pipeline[]` + `incremental[]` | **Already in flows** as separate flow files |

### What replaced pipeline.json

| Was in pipeline.json | Now lives in |
|---|---|
| `steps[].hard_gates` | flow node `data.requires` |
| `steps[].user_inputs` | flow node `data.user_inputs` |
| `steps[].folder` | flow node `data.writes` |
| `steps[].description` | flow node `data.label` + SKILL.md Overview |
| `feedback_loops[]` | flow node `data.feedback` |
| `config.standalone_mode` | `skaileup-shared/agent-config.json` |
| `config.grounding_mode` (global) | `skaileup-shared/agent-config.json` |
| `config.grounding_mode.step_folders` | flow node `data.grounding_folder` |
| `config.standards_mode` | `skaileup-shared/agent-config.json` |
| `config.orchestrator_skill` | `skaileup-shared/agent-config.json` |

### New files created

| File | Purpose |
|---|---|
| `skaileup-shared/agent-config.json` | Global runtime config (standalone_mode, grounding, standards, orchestrator IDs) |
| `skaileup-shared/contracts/flows.md` | Contract explaining the flow system, how to read/write flows |
| `skaileup-conceptualization/flows/flow.schema.json` | Updated JSON schema with writes, requires, user_inputs, feedback, grounding_folder |
| `skaileup-conceptualization/flows/mvp.json` | Reference implementation — fully enriched with all new node fields |
| `skaileup-shared/contracts/skill_template.md` | Updated — removed all pipeline.json references, uses flow node fields |

### Migration note for remaining flow files

`concept-only.json`, `prototype.json`, `cli-app.json`, `product.json`, `reverse-engineer.json`
still use the old node schema (no `writes`, `requires`, `user_inputs`, `feedback`).
Update them incrementally as skills are merged. Use `mvp.json` as the reference.

### concept-forge app impact

The app has `GET /api/skills` and `shared/schemas/pipeline.ts`. These need to be updated
to read from flows instead of pipeline.json — aggregate node metadata from relevant flow files,
or switch to reading `agent-config.json` + individual SKILL.md frontmatter.
Flag this as a breaking change when updating the app.

---

## golden_principles.md

### Comparison

| Rule | CF | Saxe | Decision |
|---|---|---|---|
| Entity names | `lowercase_snake_case` (`user`) | PascalCase (`User`) | **snake_case** — PascalCase is a translator output artifact |
| Field names | `lowercase_snake_case` (`created_at`) | camelCase (`createdAt`) | **snake_case** — already locked in by semantic_types.md |
| Standard fields declaration | `id: uuid, primary:true` + `created_at`/`updated_at` separately | `standardFields: ["id","createdAt","updatedAt"]` | **`standard_fields`** (snake_case) + explicit types for `id` (`uuid`+`primary:true`) |
| Enum values | `lowercase_snake_case` (`in_progress`) | PascalCase (`InProgress`) | **PascalCase** — already locked in by semantic_types.md |
| Enum IDs | `lowercase_snake_case` (`task_status`) | Auto-named `{ModelName}{FieldName}` (PascalCase) | **snake_case** (`task_status`) — naming at semantic layer |
| Relation rules | Implicit (via cross-ref rules only) | `Id` suffix, type = referenced model name | **Explicit Relation Rules section added**, `_id` suffix (snake_case) |
| Feature group prefix | `A_01_` (letter + number) | `01_` (number only) | **`01_`** — already locked in by concept_structure.md |
| Cross-ref data target | `model.json` | `postxl-schema.json` | **`model.json`** — canonical stack-agnostic target; postxl-schema.json is derived |
| Frontmatter required fields | `last_updated` | `status` + `last_updated` | **`last_updated` only** — status removed globally |
| Seed data structure | Informal (2 entries, realistic, edge cases) | 4 named scenarios: `empty`, `single_user`, `populated`, `edge_cases` | **4-scenario structure adopted from Saxe**, naming stays snake_case |
| Seed data key casing | Implicit snake_case | PascalCase models, camelCase fields | **snake_case** throughout — stack output is a separate step |

### Changes from CF golden_principles.md

- Entity rule: replaced ad-hoc `id: uuid, primary:true` + separate `created_at`/`updated_at` bullet with unified `standard_fields` declaration
- Enum rule: changed value casing from `lowercase_snake_case` to PascalCase
- Feature group rule: removed letter prefix requirement (`A_01_` → `01_`)
- Frontmatter rule: removed `status` requirement
- Seed data rule: replaced informal rule with 4-scenario structure from Saxe

### Added

- **Field Rules** section — makes snake_case + semantic type requirements explicit
- **Relation Rules** section — from Saxe; field suffix `_id`, type = referenced entity name (snake_case), inverse auto-generated, m2m needs junction entity
- `story_refs` cross-reference rule — each referenced story ID must exist in `stories.json`
- Translation note on entity naming clarifying output-layer conventions
- `model.json` cross-reference note clarifying it is canonical vs stack-specific derived files

### Dropped from Saxe golden_principles.md

- `status` lifecycle rule in Feature Rules
- `postxl-schema.json` naming rules (naming section) — postxl is translator output
- PascalCase model + camelCase field rules in Naming section — translator output, not semantic layer

---

## skill_grammar.md

Source: ADOPT_SAXE — no CF equivalent.

### Generalizations applied

| Saxe original | Merged version | Reason |
|---|---|---|
| Examples use `postxl-schema.json`, `pxl validate`, `@postxl/cli` | Stack-agnostic examples (`model.json`, `bun run validate:model`) | PostXL is one output target, not the canonical layer |
| EMIT skill IDs: `[implement-1-setup-1-scaffold]` (path-based) | EMIT skill IDs: `[scaffold]` (canonical name) | Skills are referenced by canonical name globally |
| REFERENCES point to `shared/contracts/` (legacy path) | REFERENCES point to `skaileup-shared/contracts/` | Updated to monorepo structure |
| Enforcement script: `shared/scripts/validate_skill_rules.py` | `skaileup-shared/scripts/validate_skill_rules.py` | Updated path |
| PROCEDURE example uses snapshot mechanism | PROCEDURE example uses `validate_cross_refs` | Snapshot mechanism is deprecated |

### Content unchanged

All DSL keywords, semantics, and authoring tips are adopted verbatim from Saxe.
No CF-side changes required (no equivalent existed).

---

## iron_laws.md

Source: ADOPT_CF — no Saxe equivalent.

### Changes from CF iron_laws.md

| Change | Old | New |
|---|---|---|
| Intro reference | "Skills read these from `pipeline.json` hard_gates" | "Skills enforce these via their `requires` field (flow node or SKILL.md frontmatter)" |
| Law 1 path | `01_project/brief.md` | `discovery/brief.md` |
| Law 2 paths | `06_datamodel/`, `03_features/` | `blueprint/datamodel/`, `experience/features/` |
| Law 3 paths | `07_screens/`, `04_brand/tokens.json` | `experience/screens/`, `discovery/brand/tokens.json` |
| Law 4 paths | `07_screens/`, `06_datamodel/model.json` | `experience/screens/`, `blueprint/datamodel/model.json` |
| Law 5 skill ref | `cf_concept_mock/` (directory path) | `mock` (canonical skill name) |
| Law 7 wording | "verify its hard_gates (file existence checks)" | "verify its `requires` paths (file/folder existence)" |
| Rationalization | "Use complexity presets in pipeline.json" | "Use a lighter flow (e.g. `prototype`)" |

### Content unchanged

All 9 laws and their WHY explanations adopted verbatim. No laws added or removed.

---

## acceptance_criteria.md

Source: ADOPT_SAXE — no CF equivalent.

### Generalizations applied

| Saxe original | Merged version | Reason |
|---|---|---|
| `discovery/3_brand/tokens.json` | `discovery/brand/tokens.json` | Brand folder renumbered in concept_structure.md merge |
| `blueprint/datamodel/postxl-schema.json` (backend ACs source) | `blueprint/datamodel/model.json` | model.json is the canonical cross-ref target per golden_principles.md |
| "tRPC endpoints" / "tRPC call, dispatch, or service method" | "generated API endpoints" / "service method call, event dispatch, or endpoint invocation" | Stack-agnostic — tRPC is one possible implementation |
| `UpdateService \| ViewService \| Adapter` (test target example) | `ServiceClass \| Adapter \| Handler` | Removed PostXL-specific naming convention from example |

### Content unchanged

AC file format, all 7 frontend AC rules, derivation steps 1–5, count guidelines,
backend AC format, all 5 backend rules, backend count guidelines, and derivation
examples are adopted verbatim from Saxe.

---

## plans.md

### Comparison

| Aspect | CF | Saxe | Decision |
|---|---|---|---|
| Progress path format | Flat: `01_project`, `03_features` | Phase-grouped: `discovery` | **Phase-grouped** (Saxe/canonical) |
| Brand path | `04_brand/tokens.json` | `discovery/3_brand/tokens.json` | **`discovery/brand/tokens.json`** (canonical renumbering) |
| Data model path | `model.json` | `postxl-schema.json` | **`model.json`** (canonical per golden_principles.md) |
| Skill refs in Progress | `cf_implement_bootstrap`, `cf_test_e2e` | `implement-1-setup-1-scaffold`, `app-e2e` | **Canonical names**: `scaffold`, `foundation`, `e2e`, `audit` |
| Infrastructure section | Not present | Present (modules, providers, services, comms) | **Adopted from Saxe** |
| Deploy item | Present | Present | **Kept** |
| Orchestrator ref | `cf_orchestrator` | `concept` | **`concept-orchestrator`** (canonical name) |
| Verification skill refs | `cf_quality_audit`, `cf_test_e2e` | `app-audit`, `app-e2e` | **`audit`, `e2e`** (canonical names) |

### Rules unchanged

All 7 rules are identical between CF and Saxe — kept verbatim.

---

## skill_testing.md

### Comparison

| Aspect | CF | Saxe | Decision |
|---|---|---|---|
| Example input/expected paths | Flat: `01_project/`, `03_features/` | Phase-grouped: `discovery/` | **Phase-grouped** (canonical) |
| `skill` field in `_validation.json` | `cf_concept_functionality_features` | `concept-2-experience-2-features` (path-based) | **Canonical name**: `features`, `overview`, etc. |
| `status` frontmatter check | Present (`"draft"`) | Present (`"draft"`) | **Removed** — status field dropped globally |
| Data model check target | `06_datamodel/model.json` + `"entities"` | `blueprint/datamodel/postxl-schema.json` + `"models"` | **`blueprint/datamodel/model.json`** + `"entities"` |
| Seed scenarios checked | `empty`, `populated` | `empty`, `populated` | **All 4 scenarios**: `empty`, `single_user`, `populated`, `edge_cases` (per golden_principles.md) |
| "What to Cover" skill names | CF directory names | Saxe path-based IDs | **Canonical skill names**: `overview`, `features`, `datamodel`, etc. |

### Content unchanged

All check types, "How to Run" steps, and the directory structure pattern are
identical between CF and Saxe — kept verbatim.

---

## seed_data.md

### Comparison

| Aspect | CF | Saxe | Decision |
|---|---|---|---|
| File path | `06_datamodel/seed.json` | `blueprint/datamodel/seed.json` | **Canonical path** |
| Entity key format | Singular snake_case: `"user"`, `"task"` | camelCase plural: `"users"`, `"tasks"` | **Singular snake_case** — matches entity names in model.json (golden_principles.md) |
| Relation field | `assigned_to` (no suffix) | `assigned_to_id` | **`assigned_to_id`** — `_id` suffix rule |
| Enum values | `"done"`, `"in_progress"` | `"Done"`, `"InProgress"` | **PascalCase** — locked in by semantic_types.md |
| Dev user requirement | Not present | `"sub": "test"` (PostXL-specific) | **Generalized** — stack-specific note, not a universal field |
| Data quality preamble | Clean | "Backend-compatible format" (PostXL-specific) | **Dropped** — replaced with semantic layer rules |
| camelCase plural keys rule | Not present | Present | **Dropped** — PostXL translator output, not semantic layer |
| Scaffold integration section | Not present | Present (PostXL-specific) | **Adopted, generalized** — stack translator does format conversion |
| Standard scenarios table | Present | Present (identical) | **Kept verbatim** |
| Skill refs | CF directory names | Saxe path-based IDs | **Canonical names**: `mock`, `screens`, `e2e`, `datamodel`, `scaffold` |

### Content unchanged

Standard scenarios table, data quality rules for locale diversity / length variation /
special characters / null optionals / realistic emails, and all scenario descriptions
are kept from both (they were identical).

---

## agent_patterns.md

Source: ADOPT_CF — no Saxe equivalent.

### Changes from CF agent_patterns.md

| Pattern | Change | Detail |
|---|---|---|
| Read-Context-First | `pipeline.json hard_gates` → SKILL.md / flow node `requires` | Path existence checks now come from the skill's own `requires` field |
| Self-Collect Inputs | `pipeline.json user_inputs` → flow node `user_inputs.dialog` | Dialog field IDs from flow node; pre-collected answers still in `_grounding/{grounding_folder}/` |
| Self-Collect Inputs | Complexity setting → `globals.verbosity` | `brief`/`standard`/`detailed` replaces `simple`/`moderate`/`complex` |
| Communication Style | Complexity setting → `globals.verbosity` | Same mapping as above |
| Standalone Mode | `pipeline.json` → SKILL.md frontmatter `requires` | Skills read their own frontmatter for gate checks |
| Next-Step Suggestion | `pipeline.json depends_on` → flow `next_flows` hints | Suggestions come from flow file or skill's own knowledge |
| Research Mode | `pipeline.json step_folders` → flow node `grounding_folder` | Folder name now on the node, not in a central mapping |
| User Input Persistence | `pipeline.json step_folders` / dialog IDs → flow node `grounding_folder` + `user_inputs.dialog[].id` | Same concept, source updated |
| Subagent Dispatch | `pipeline.json "subagent": true` → flow node `"subagent": true` | Same flag, now on the node |
| Expert Discovery | `cf__shared/pipeline.json expert_search_paths` → monorepo structure | `dev-implementation-experts-js/` + `dev-implementation-experts-python/`; advisor skill added |
| Expert Discovery | `prog-expert-*` skill naming → `skaileup-implementation-expert-<tech>` | Matches actual monorepo skill directory names |
| Path refs | `05_techstack/stack.md`, `03_features/` | `blueprint/techstack.md`, `experience/features/` |
| `cf__shared/` refs | `cf__shared/` | `skaileup-shared/` |

### Content unchanged

All pattern logic, the standalone-question rule with examples, the feedback loop
update steps, the subagent context isolation rule, and the research grounding topic
list are kept verbatim.

---

## journeys skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `journeys` | `concept-2-experience-1-journeys` | **`journeys`** — canonical flat name |
| Output path | `_concept/02_journeys/stories.json` | `_concept/experience/journeys/stories.json` | **`_concept/experience/journeys/stories.json`** |
| Format | Prose + DSL hybrid | Pure DSL | **DSL** — consistent with monorepo style |
| Research reads | `_grounding/general/` | `discovery/2_research/` (wrong) | **`_grounding/general/`** — canonical research location |
| Hero checkpoint | CHECKPOINT after STEP 3 | No intermediate checkpoint | **CHECKPOINT kept** — high-value approval gate |
| Final approval | Simple confirmation | Summary table with counts | **Summary table kept** — more informative |
| Downstream field names | `entities`, `screens` (short form) | `candidate_entities`, `candidate_screens` | **`candidate_*` prefix** — more explicit, consistent |
| EMIT prefix | `[journeys]` (inferred) | `[concept-2-experience-1-journeys]` | **`[journeys]`** — canonical name |
| REFERENCES path | `shared/contracts/` | `shared/contracts/` | **`skaileup-shared/contracts/`** |
| Script path | n/a | `../shared/scripts/` | **`skaileup-shared/scripts/`** (4 levels up) |

### Content adopted from each source

**From CF:**
- Hero CHECKPOINT (mid-flow approval gate before mapping remaining journeys)
- Prose header sections (Overview, When to Use, When NOT to Use, Prerequisites, Context Budget, Common Mistakes, Integration)
- Minimum 2 EARS criteria per hero story rule

**From Saxe:**
- DSL skill body format
- Detailed story structure in STEP 3 (all required fields listed)
- STEP 8 OUTPUT block with full stories.json schema example
- Summary table format in STEP 9
- validator.py with full validation suite (hero count, EARS on every story, downstream hints, status:proposed, priority distribution)
- ears_format.md (adopted verbatim — no PostXL content)

**Fixed:**
- journey_stages.md: Saxe-platform-specific "SAXE examples" replaced with generic, stack-agnostic examples

---

## features skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `features` | `concept-2-experience-2-features` | **`features`** |
| Output path | `03_features/<X_NN_group>/` | `experience/features/<NN_group>/` | **`experience/features/<NN_group>/`** |
| Group prefix | `A_01_` (letter+number) | `01_` (number only) | **`01_`** — golden_principles.md rule |
| Hard gates | brief.md only | brief.md + stories.json | **both** — features must derive from journeys |
| `status` field | Not present | `status: draft` | **Dropped** — globally removed from markdown frontmatter |
| `story_refs` field | Not present | Present | **Adopted** — traceability to journeys is required |
| `permissions` field | Not present | Present | **Adopted** — role-action matrix per feature |
| Research reads | `_research/general/` | `discovery/2_research/` | **`_grounding/general/`** — canonical |
| Pre-collected inputs | `_research/features/user_input.json` | Not present | **`_grounding/features/user_input.json`** |
| `feature_priorities` user input | Present | Not present (uses story stages) | **Adopted** — scope calibration; story stages set default |
| PostXL CRUD rule | Not present | NEVER specify PostXL CRUD | **Generalized** — "focus on custom logic over framework defaults" |

### Content adopted from each source

**From CF:**
- `feature_priorities` user input (must-have only / must-have+nice-to-have / comprehensive)
- Research Mode section (competitor + audience research integration)
- Common Mistakes table
- Integration section (feedback loop documentation)

**From Saxe:**
- DSL skill body format
- Hard gate on stories.json
- `story_refs` frontmatter field (traceability to journeys)
- `permissions` frontmatter field + ## Permissions section
- STEP 2b (define roles and permissions)
- Priority derivation from story stages (hero/vital → must-have, backlog → nice-to-have)
- `feature_template.md` reference file (generalized: PostXL section → stack-agnostic scope guidance)
- validator.py (updated paths, removed status check, added empty downstream fields check)

---

## screens skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `screens` | `concept-2-experience-3-screens` | **`screens`** |
| Output path | `07_screens/` | `experience/screens/` | **`experience/screens/`** |
| Hard gates | features + brand + techstack + datamodel | features only (rest optional) | **features required; rest check-if-present** |
| `status` in frontmatter | Not present | `status: draft` | **Dropped** — globally removed |
| Component inventory | Explicitly forbidden | PostXL `@postxl/ui-components` required | **Dropped** — CF's plain-language approach adopted |
| `ui_components.md` | Not present | Present (PostXL catalog) | **Not included** — stack-specific; belongs in skaileup-standards/profiles |
| Screen spec style | User-perspective plain language | Technical component inventory | **CF approach** — plain language, no lib names |
| Journey integration | Not present | Reads stories.json, maps to screen flow | **Adopted** — optional enrichment |
| `model.json` vs `postxl-schema.json` | `model.json` | `postxl-schema.json` | **`model.json`** — canonical |
| Template Data section | Not present | Present (seed.json scenarios) | **Adopted** — conditional on seed.json existing |
| Feedback loop step | Present (add screens[] to features) | Present (same) | **Kept** — both identical intent |

### Content adopted from each source

**From CF:**
- Plain-language writing approach (no component names, no CSS, no hex codes)
- CHECKPOINT screen_list + CHECKPOINT screens_approved flow
- Common Mistakes table
- Research Mode section

**From Saxe:**
- DSL skill body format
- Journey integration (STEP 1 reads stories.json, STEP 2 uses journey flow for navigation)
- Screen list confirmation before writing
- `screen_spec_template.md` structure (sections: Purpose, Route, What User Sees, Information Displayed, Actions, Situations, UI Elements, Template Data)
- Template Data section referencing seed.json scenarios
- Enrichment from Architecture section in template
- validator.py (feedback loop check, shell existence check)

**Changed:**
- screen_spec_template.md "Component Inventory" → "UI Elements" (plain language)
- `postxl-schema.json` → `model.json` throughout
- `@postxl/ui-components` references removed entirely

---

## architecture skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `architecture` | `concept-3-blueprint-2-architecture` | **`architecture`** |
| Output path | `05b_architecture/` | `blueprint/` | **`blueprint/`** |
| `status` in frontmatter | Not present | `status: draft` | **Dropped** — globally removed |
| `custom_services` vs `custom_modules` | `custom_services` | `custom_modules` | **`custom_modules`** — more precise |
| Stack defaults | Inline multi-stack blocks (Directus/Supabase/Nuxt/NestJS) | `references/postxl_defaults.md` (PostXL only) | **Neither** — agent derives from stack.md; details in skaileup-standards/profiles |
| `postxl_defaults.md` | Not present | Present (PostXL-specific) | **Not included** — PostXL-specific |
| Involvement level | Not present | Based on `complexity_tier` frontmatter | **Adopted** — sourced from `_grounding/overview/user_input.json` complexity (complexity_tier removed from frontmatter) |
| Behaviors read | `.allium` files optional | Not present | **Adopted** from CF |
| CHECKPOINT | Not present | Present (business summary + technical details) | **Adopted** from Saxe |
| `output_template.md` | Not present | Present (PostXL-specific) | **Adopted, generalized** — stack-agnostic six-section template |

### Content adopted from each source

**From CF:**
- Stack-agnostic approach (no hardcoded PostXL module names)
- Multi-stack baseline concept (derive defaults from stack.md)
- Behaviors optional read (`.allium` files signal event-driven patterns)
- Common Mistakes section
- Integration section

**From Saxe:**
- DSL skill body format
- STEP 1b involvement level (automatic vs. involved based on complexity)
- Business summary CHECKPOINT with plain-language explanation
- `output_template.md` structure (six sections) — generalized
- validator.py (section presence + frontmatter checks)

---

## datamodel skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `datamodel` | `concept-3-blueprint-3-datamodel` | **`datamodel`** |
| Output path | `06_datamodel/` | `blueprint/datamodel/` | **`blueprint/datamodel/`** |
| Primary format | `model.dbml` + `model.json` (semantic types) | `postxl-schema.json` (Prisma-based) | **`model.dbml` + `model.json`** — CF canonical, stack-independent |
| `postxl-schema.json` | Not present | Primary output | **Not included** — PostXL-specific |
| `feature_map.json` | Not present (uses `from_features[]` inside model.json) | Present as separate file | **Adopted** — explicit cross-reference file |
| `pxl validate` | Not present | Required MUST rule | **Dropped** — PostXL CLI tool |
| Dev user in seed | Not present | `sub: "test"` required | **Generalized** — dev/test user included, PostXL `sub` field dropped |
| Seed entity key format | Singular snake_case (`"task"`) | camelCase plural (`"tasks"`) | **Singular snake_case** — canonical (seed_data.md) |
| `standardModels` | Not present | PostXL standard models | **Not included** — PostXL-specific |
| Journey-derived state machines | Not present | In schema_conventions.md | **Adopted** — EARS criteria → enum values |
| Involvement level | Not present | Based on `complexity_tier` frontmatter | **Adopted** — sourced from `_grounding/overview/user_input.json` |
| CHECKPOINT | Not present | Business summary + tech details | **Adopted** from Saxe |
| `schema_conventions.md` | Not present | PostXL-specific | **Replaced** by `model_conventions.md` (stack-agnostic) |
| Behaviors read | `.allium` files → state machines | Not present | **Adopted** from CF |
| Stack translation | On-request step (Step 7) | Not present | **Kept** from CF |

### Content adopted from each source

**From CF:**
- `model.dbml` + `model.json` canonical dual-format approach
- Stack-independent semantic types
- On-request stack translation step (Prisma, Directus, Supabase, SQL DDL)
- `.allium` optional read for state machines
- Common Mistakes table
- Research Mode section

**From Saxe:**
- `feature_map.json` as a separate cross-reference file
- Involvement level (automatic vs. involved based on complexity)
- Business summary CHECKPOINT
- Journey-derived state machines from EARS criteria
- `schema_conventions.md` structure → generalized to `model_conventions.md`
- validator.py structure (adapted: postxl-specific checks removed, model.json checks added)

---

## techstack skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `techstack` | `concept-3-blueprint-1-techstack` | **`techstack`** |
| Output path | `05_techstack/` | `blueprint/` | **`blueprint/`** |
| Stack selection | Dynamic — scans `skaileup-standards/profiles/*/SKILL.md` | Fixed — hardcoded PostXL/NestJS/Next.js | **CF approach** — dynamic discovery at runtime |
| `tech_stack_skill` field | Present — key downstream contract | Not present (hardcoded) | **Required** — all downstream skills depend on it |
| `status` in frontmatter | Not present | `status: draft` | **Dropped** — globally removed |
| `orm` field | Not present | Present | **Adopted** — ORM/query layer is a real choice |
| `framework` vs `frontend` | `frontend` (framework+version) | `framework` (PostXL only) | **`frontend`** — CF field; `framework` was PostXL-specific |
| Involvement level | Not present | Not present | **Added** — sourced from `_grounding/overview/user_input.json` complexity |
| CHECKPOINT | Not present | Not present (linear flow) | **Added** `stack_approved` — consistent with other blueprint skills |
| `integration_categories.md` | Not present | Present (PostXL framing) | **Adopted, generalized** — removed "PostXL core is fixed" framing |

### Content adopted from each source

**From CF:**
- Dynamic stack discovery (scans `skaileup-standards/profiles/*/SKILL.md` at runtime)
- `tech_stack_skill` field as downstream contract
- Plain-language question approach (STEP 3)
- `feature_priorities` user input (scope calibration)
- Common Mistakes table
- Profile recommendation format

**From Saxe:**
- `orm` field in stack.md frontmatter
- `integration_categories.md` reference (generalized — PostXL framing removed)
- Consultation approach framing (feature-aware questions)

**Added:**
- Involvement level step (STEP 2b) — automatic/involved based on complexity
- CHECKPOINT `stack_approved` — explicit approval gate
- Trade-offs Considered section in output template

**Dropped:**
- Saxe's hardcoded PostXL/NestJS/React stack — PostXL is a profile, not the default
- `status` field from stack.md frontmatter
- `framework` field (renamed to `frontend` per CF convention)
- PostXL-specific framing in integration_categories.md ("PostXL core is fixed")

---

## brand skills (brand-visual + brand-behavioral)

Source: MERGED — CF variants + Saxe combined variant.

### Structure decision

CF's split (brand-visual / brand-behavioral) kept over Saxe's combined approach — granularity
allows running only visual (common) without behavioral (less common). Two separate merged skills.

### brand-visual structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `brand-visual` | `concept-1-discovery-3-brand` | **`brand-visual`** |
| Output path | `04_brand/` | `discovery/3_brand/` | **`discovery/brand/`** (canonical renumbering) |
| `status` in identity.md frontmatter | Not present | `status: draft` | **Dropped** — globally removed |
| tokens.json `tailwind` section | Not present | Present (PostXL/Radix CSS vars) | **Adopted, generalized** — CSS custom properties for any Tailwind/CSS theming |
| CHECKPOINT before writing | No (write directly) | `CHECKPOINT brand_proposal` + `CHECKPOINT brand_final` | **`CHECKPOINT brand_approved`** — consistent with other blueprint skills |
| `brandbook.html` | Present (self-contained HTML) | Not present | **Kept** from CF |
| Research reads | `_research/general/` | `discovery/2_research/` (wrong) | **`_grounding/general/`** — canonical |
| `design_philosophy.md` | Inline in SKILL.md | Separate reference file | **Separate file** (Saxe approach), PostXL section dropped |
| `tokens_schema.md` | Inline in SKILL.md | Separate reference file | **Separate file** (Saxe approach), status removed, paths updated |
| `discovery_questions.md` | Inline in SKILL.md | Separate reference file | **Separate file** (Saxe approach), adopted verbatim |

### brand-behavioral structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `brand-behavioral` | (combined in saxe_brand) | **`brand-behavioral`** — kept from CF |
| Output path | `04_brand/` | (combined) | **`discovery/brand/`** (canonical) |
| Hard gates | brief + features + identity + tokens | n/a | **Kept** from CF: brief, features, identity, tokens |
| Path refs throughout | `01_project/`, `03_features/`, `04_brand/`, `07_screens/` | n/a | **Canonical**: `discovery/`, `experience/features/`, `discovery/brand/`, `experience/screens/` |
| Skill refs | CF directory names | n/a | **Canonical names**: `overview`, `features`, `brand-visual`, `screens`, `datamodel` |

### Content adopted from each source

**brand-visual from CF:**
- `brandbook.html` output (self-contained HTML visual reference)
- Detailed brandbook specification (8 sections, component previews, do's/don'ts)
- Common Mistakes table

**brand-visual from Saxe:**
- `tokens.json` `tailwind` section with CSS custom properties
- CHECKPOINT before writing artifacts (brand_approved)
- Separate reference files (design_philosophy.md, tokens_schema.md, discovery_questions.md)
- Full required fields validation in validator.py
- NEVER check for generic brand output in validator.py

**brand-behavioral from CF:**
- Complete skill (Saxe has no separate behavioral skill)
- All workflow steps, output format, common mistakes

---

## storybook skills

Source: MERGED — CF stub + Saxe 4-sub-skill suite.

### Structure decision

Saxe's 4-sub-skill architecture adopted (CF's single-skill stub was insufficient).
Five skills: `storybook` (orchestrator) + `storybook-setup` + `storybook-components` + `storybook-pages` + `storybook-journeys`.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Architecture | Single skill | Orchestrator + 4 sub-skills | **Saxe's 4-sub-skill approach** |
| Output path | `08_storybook/` | `experience/4_storybook/` | **`experience/4_storybook/`** |
| Component library | Tech stack profile lookup | `@postxl/ui-components` hardcoded | **`component_library` from tech stack profile** |
| Story file format | Derived from tech stack profile | React `.tsx` hardcoded | **`story_extension` from tech stack profile** |
| Package manager | `package_manager` from stack.md | `pnpm` hardcoded | **`package_manager` from stack.md** |
| Icon library | Derived from tech stack | `lucide-react` hardcoded | **`icon_library` from tech stack profile** |
| Type generation | Not present | `pxl types` CLI | **Dropped** — storybook-types is a separate skill |
| Click-dummy journeys | Layer 3 (basic) | Click-dummy with `.click-hint`, persona banner | **Saxe's click-dummy pattern kept** (framework-agnostic) |
| Brand path | `04_brand/` | `discovery/3_brand/` | **`discovery/brand/`** (canonical renumbering) |
| `status` field | Not present | `status: draft` | **Dropped** — globally removed |
| Setup templates | Not present | PostXL-specific templates/ | **Not included** — templates belong in skaileup-standards/profiles |

### PostXL-specific content dropped

- `@postxl/ui-components` barrel read → generalized to read component_library from installed packages
- React `useState`/`onClick` patterns → "framework-appropriate reactive state" language
- `pnpm` hardcoded throughout → `<package_manager>` from stack.md
- `lucide-react` hardcoded → `icon_library` from tech stack profile
- `pxl types` → dropped (storybook-types is a separate adoption TODO)
- `@postxl/ui-components` as composition base → component_library
- Saxe templates/ (PostXL-specific Vite + React config) → not included

### Content adopted from Saxe verbatim

- Click-dummy journey pattern: AppShell wrapper, reactive step tracking, real UI element navigation, `.click-hint` CSS class pulse on wrong click, persona + step indicator banner
- 3-layer structure: Components / Pages / Journeys
- `manifest.json` contract between pages and journeys sub-skills
- Barrel export contract between components and pages sub-skills
- `src/@types/` incremental build pattern (minimal types per sub-skill)
- Build verification after each sub-skill

---

## add-feature skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `add-feature` | `concept-add-feature` | **`add-feature`** |
| Paths throughout | Old flat paths (`01_project/`, `06_datamodel/`, etc.) | Canonical phase-grouped paths | **Canonical paths** (Saxe) |
| Data model output | `model.json` + `model.dbml` | `postxl-schema.json` | **`model.json` + `model.dbml`** — CF canonical |
| `postxl-schema.json` | Not present | Primary cascade target | **Dropped** — not canonical |
| `pxl validate` | Not present | Required MUST rule | **Dropped** — PostXL CLI tool |
| `pnpm run generate` | Not present | Phase 4 if schema modified | **Dropped** — PostXL-specific |
| Implementation Phase 4 | Generic handoff to implement-feature | PostXL TDD workflow (Prisma, tRPC, @postxl/ui-components) | **Generic handoff** from CF |
| `status` in feature frontmatter | `status: draft` | `status: draft` | **Dropped** — globally removed |
| `story_refs` in feature frontmatter | `stories: []` (CF's name) | Not present | **`story_refs`** — matches merged features skill |
| `permissions` in feature frontmatter | Not present | Not present | **Added** — consistent with merged features skill |
| `agent_notes` in feature frontmatter | Not present | Present | **Adopted** from Saxe — useful for cascade context |
| Seed key format | Not specified | camelCase plural (`"users"`, `"tasks"`) | **Singular snake_case** — canonical per golden_principles.md |
| `snapshots.md` reference | Not present | Present | **Dropped** — deprecated |
| `pipeline.json` reference | Not present | Present | **Dropped** — replaced by flow nodes |

### Cascade rules changes

| Rule | CF | Saxe | Merged |
|---|---|---|---|
| Journeys path | `02_journeys/stories.json` | `experience/journeys/stories.json` | **Canonical** |
| Tech stack path | `05_techstack/stack.md` | `blueprint/techstack.md` | **Canonical** |
| Architecture path | `05b_architecture/architecture.md` | `blueprint/architecture.md` | **Canonical** |
| Data model path | `06_datamodel/model.json` | `blueprint/datamodel/postxl-schema.json` | **`blueprint/datamodel/model.json`** |
| Schema validation | Not present | `pxl validate` | **Dropped** |
| Seed key format | Not specified | camelCase plural | **Singular snake_case** |
| `labelField` validation | Not present | Present (PostXL-specific) | **Dropped** — PostXL schema concept |
| Screen spec format | CF component-based | Saxe `@postxl/ui-components` component inventory | **Plain language UI Elements** (merged screens skill) |
| Screen `status` in screens[] | Not present | `{path: ..., status: draft}` | **`path:` only** — status removed globally |

---

## review skill

Source: MERGED — CF variant + Saxe variant.

### Structural decisions

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `review` | `concept-review` | **`review`** |
| `status` field check | Flag absence as issue | Flag absence as issue | **Flag PRESENCE as issue** — globally removed |
| Pipeline step lookup | Uses `pipeline.json` | Uses `pipeline.json` | **Uses `concept_structure.md`** — pipeline.json replaced |
| Data model cross-ref | `model.json` | `postxl-schema.json` | **`model.json` + `feature_map.json`** |
| Feature required fields | `priority`, `roles`, `screens`, `data_entities` | `priority`, `status`, `roles`, `screens`, `data_entities` | **`priority`, `story_refs`, `roles`, `screens`, `data_entities`** — no status; `story_refs` added |
| Event prefix | `[concept-review]` | `[concept-review]` | **`[review]`** — canonical skill name |
| Gardening: missing status | Add `status: draft` | Add `status: draft` | **Dropped** — status globally removed |
| Gardening: status present | Not checked | Not checked | **Remove `status:` field** — new safe fix |
| Orphaned entity label | "Orphaned model" | "Orphaned model (postxl-schema.json)" | **"Orphaned entity (model.json)"** |

### Checks changes

| Check | Before (CF/Saxe) | After (merged) |
|---|---|---|
| `status` field | Required to be present; valid lifecycle value | Flag PRESENCE as `STALE STATUS FIELD` entropy indicator |
| Feature frontmatter | `priority`, `status`, `roles`, `screens`, `data_entities` | `priority`, `story_refs`, `roles`, `screens`, `data_entities` |
| Data model source | `postxl-schema.json` | `model.json` + `feature_map.json` |
| Cascade warnings | Snapshot-based table | Removed (snapshots deprecated) |
| Screen "component inventory" rule | Present | Replaced with "plain language UI Elements" |
| `story_refs` field | Not checked | Required in feature frontmatter |

### Gardening changes

| Rule | Before | After |
|---|---|---|
| Missing `status:` field | Safe fix: add `status: draft` | **Dropped** — status globally removed |
| Present `status:` field | Not checked | **Safe fix: remove the field** — new |
| Orphaned entities source | `postxl-schema.json` | `model.json` |

---

## skaileup-implementation skills

Source: MERGED — CF + Saxe variants.

### implement (00_orchestrator)

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill type | Plan generator (PLANS.md only, no execution) | Full pipeline orchestrator | **Full orchestrator** with CF's planning depth |
| Feature order | Numeric group order | Journey-first (hero→vital→hygiene) | **Journey-first** (preferred); numeric fallback if no stories.json |
| Complexity tiers | Not present | small/standard/complex | **Adopted from Saxe** |
| Stack-specific commands | None | `pxl create-project`, `pnpm run generate` | **Dropped** — dispatches to `scaffold`/`foundation` sub-skills |
| Data model reference | `model.json` | `postxl-schema.json` | **`model.json` + `model.dbml`** |
| LEARNINGS.md | Not present | PostXL-specific categories | **Adopted**, generalized categories |
| UAT phase | Not present | Present | **Adopted** from Saxe |
| Auto-approval | Not present | Present | **Adopted** from Saxe |
| Sub-skills called | None (plan only) | PostXL sub-skill names | **Canonical**: scaffold, foundation, infrastructure, implement-feature, verify |
| `impl_status` on features | Updates to `implemented` | Updates to `implemented` | **Not updated** (status globally removed); update `last_updated` only |

### scaffold (10_setup/scaffold)

| Dimension | CF (cf_scaffold utility) | Saxe (saxe_scaffold) | Merged |
|---|---|---|---|
| Scaffold approach | Generic — reads tech stack profile | PostXL-specific — `pxl create-project --schema postxl-schema.json` | **Profile-based** — reads skaileup-standards/profiles/<tech_stack_skill>/SKILL.md |
| `postxl-schema.json` | Not used | Primary scaffold input | **Dropped** — use `model.json` |
| `@postxl/cli` | Not used | Required | **Dropped** — stack-specific CLI from profile |
| `_implementation/` tracking | Not present (separate utility) | Present | **Adopted** from Saxe |
| User confirmation | Not present | Present (CHECKPOINT scaffold_confirm) | **Adopted** from Saxe |

### foundation (10_setup/foundation)

| Dimension | CF (cf_foundation) | Saxe (saxe_foundation) | Merged |
|---|---|---|---|
| Brand tokens path | `04_brand/tokens.json` (old flat) | `discovery/3_brand/tokens.json` | **`discovery/brand/tokens.json`** (canonical) |
| Stack specifics | Generic (css_vars_mapping from profile) | PostXL-specific (HSL, @postxl/ui-components) | **Generic** from tech-stack profile |
| Seed data setup | Not present | PostXL Prisma seed | **Generic** — stack-appropriate seed from seed.json |
| Storybook config | Not present | Present | **Adopted** — conditional on storybook being installed |
| E2E test update | Not present | PostXL-specific E2E update | **Dropped** — not meaningful without PostXL |
| Shell path | `07_screens/00_layout/shell.md` | `experience/screens/00_layout/shell.md` | **`experience/screens/00_layout/shell.md`** (canonical) |

### implement-feature (20_features)

| Dimension | CF (cf/) | Saxe (saxe/ + saxe_page/) | Merged |
|---|---|---|---|
| Strategy | Single-feature TDD | Journey-first 3-level TDD | **Journey-first** (default); single-feature as standalone mode |
| TDD Guard | Not present | Present (hooks in frontmatter) | **Adopted** from Saxe |
| Page sub-skill | Not present | saxe_page as separate sub-skill | **Adopted** — implement-feature-page sub-skill |
| UI reference | Not present | Storybook page compositions | **Adopted** — `experience/4_storybook/src/pages/` |
| `postxl-schema.json` | `model.json` | `postxl-schema.json` | **`model.json`** |
| `pnpm run generate` | Not present | Required after customAction | **Dropped** |
| `@postxl/ui-components` | Not present | Required | **Dropped** — use `component_library` from stack profile |
| `@custom-start/@custom-end` | Not present | Required in generated files | **Dropped** |
| `impl_status` update | Updates to `implemented` | Updates to `implemented` | **Dropped** (status removed); update `last_updated` only |
| Data paths | Old flat paths | Canonical phase-grouped | **Canonical** |

### verify (30_verify)

| Dimension | CF (cf/) | Saxe (saxe/) | Merged |
|---|---|---|---|
| Scope | Concept-spec-fidelity only (browser walkthrough) | Full-stack: E2E + visual + build + storybook + browser | **Both scopes combined** |
| E2E + build + types | Not present | Present | **Adopted** from Saxe |
| Spec-fidelity matrix | Present (feature × acceptance criteria) | Not present | **Adopted** from CF |
| Storybook check | Not present | Present | **Adopted** from Saxe |
| `impl_status: tested` | Updates feature frontmatter | Not present | **Changed**: update `last_updated` only |
| `full-verification.json` | Not present | Present | **Adopted** from Saxe; added `spec_fidelity` section |
| PostXL-specific | `check-frontend-paths.sh`, Keycloak | `check-frontend-paths.sh` | **Dropped** |
| Data model source | `model.json` | `postxl-schema.json` | **`model.json`** |

### utilities

| Skill | Change |
|---|---|
| `migrate` | Canonical paths: `blueprint/datamodel/`, `blueprint/`, `skaileup-shared/contracts/`. Expert skills from `dev-implementation-experts-*`. |
| `seed` | Same path updates as migrate. |
| `generate` | Saxe adopted unchanged. Marked explicitly PostXL-specific. `postxl-schema.json` sync note with `model.json`. |
| `utilities/scaffold` | Merged into `10_setup/scaffold` — overlap resolved. |

---

## quality skills

Source: MERGED — CF + Saxe variants.

### audit

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `audit` (cf_quality_audit) | `app-audit` | **`audit`** |
| Format | Prose workflow | DSL | **Hybrid** (prose header + DSL body) |
| Sub-agents | 3 parallel (Logic, UI/UX, Security) | 3 parallel (identical scope) | **Adopted verbatim** — both identical |
| Structure check | When `_concept/` exists | When `_concept/` exists | **Kept** — both identical |
| Schema reference | None | `postxl-schema.json` | **Dropped** — structure check uses canonical paths |
| Contract refs | `cf__shared/` | `shared/contracts/` | **`skaileup-shared/contracts/`** |
| Offer fixes | Present | Present | **Kept** |
| Event prefix | `[cf_quality_audit]` | `[app-audit]` | **`[audit]`** — canonical |

### e2e

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `e2e` (cf_test_e2e) | `app-e2e` | **`e2e`** |
| Format | Prose workflow | DSL | **Hybrid** (prose header + DSL body) |
| Paths | Old flat paths (`01_project/`, `03_features/`, `06_datamodel/`) | Canonical phase-grouped | **Canonical phase-grouped** (Saxe) |
| Data model | `06_datamodel/model.json` | `blueprint/datamodel/postxl-schema.json` | **`blueprint/datamodel/model.json`** |
| DB validation | Against `model.json` entity definitions | Against `postxl-schema.json` models | **Against `model.json`** |
| `status: tested` | Updates `impl_status: tested` in feature frontmatter | Updates `status: tested` | **Dropped** — update `last_updated` only (status globally removed) |
| MUST/NEVER rules | Implicit in prose | Explicit DSL block | **Saxe's explicit rules adopted** |
| Event prefix | `[cf_test_e2e]` | `[app-e2e]` | **`[e2e]`** — canonical |
| Contract refs | `cf__shared/` | `shared/contracts/` | **`skaileup-shared/contracts/`** |

### ready

| Dimension | CF | Saxe | Merged |
|---|---|---|---|
| Skill name | `ready` (cf_quality_ready) | `app-ready` | **`ready`** |
| Format | Prose workflow | DSL | **Hybrid** (prose header + DSL body) |
| Paths | Old flat paths (`03_features/`, `07_screens/`, etc.) | Canonical phase-grouped | **Canonical phase-grouped** (Saxe) |
| Data model | `06_datamodel/model.json` | `blueprint/datamodel/postxl-schema.json` | **`blueprint/datamodel/model.json` + `feature_map.json`** |
| Brand tokens path | `04_brand/tokens.json` | `discovery/3_brand/tokens.json` | **`discovery/brand/tokens.json`** (canonical renumbering) |
| Status check | `impl_status: implemented` | `status: implemented` or `status: mockup_ready` | **Dropped** — status globally removed from frontmatter |
| Mockup check | `05_mockups/*.html` | `05_mockups/*.html` | **Generalized** — storybook compositions (`experience/4_storybook/`) OR mockup HTML; soft check |
| Fix skill refs | CF directory names | Path-based IDs | **Canonical names**: `screens`, `datamodel`, `storybook` |
| Event prefix | `[cf_quality_ready]` | `[app-ready]` | **`[ready]`** — canonical |

### compile-validators

| Dimension | Saxe (only source) | Merged |
|---|---|---|
| Skill name | `compile-validators` | **`compile-validators`** — unchanged |
| Script path | `shared/scripts/validator_lib.py` | **`skaileup-shared/scripts/validator_lib.py`** |
| Contract path | `shared/contracts/skill_grammar.md` | **`skaileup-shared/contracts/skill_grammar.md`** |
| Skill paths | `skills/<category>/<skill>/` | **`ai-assets/<domain>/skills/<skill>/`** or `ai-assets/<domain>/skills/<group>/<skill>/` |
| sys.path depth | `parents[3]` (fixed 3-level nesting) | **Variable** — 3 for flat skills, 4 for grouped skills |
| JSON Schema path | `skills/shared/contracts/stories_schema.json` | **`ai-assets/skaileup-shared/contracts/stories_schema.json`** |

---

## (more skills to follow as merges complete)
