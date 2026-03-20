# GUI Integration: concept-forge-skills ↔ concept-forge

This document defines the contract between the skills package (AI agent side)
and the Nuxt 4 GUI (concept-forge). Both consume `_concept/` artifacts and
`pipeline.json`, but from different angles.

## Current State (What Works Today)

### The frontend already:
1. **Reads `pipeline.json`** from the skills dir to get step definitions
2. **Scans `_concept/` folders** to compute step status (not_started / in_progress / complete)
3. **Renders pipeline steps** as sidebar section headers with status icons
4. **Dispatches skills** via `POST /api/agent/prompt` → `Run skill <skill_name>`
5. **Streams agent output** via SSE at `/api/agent/stream`
6. **Lists skills** by reading SKILL.md frontmatter (name + description only)
7. **Shows mockups** at `/mockups` by listing HTML files in `05_mockups/`

### What the frontend does NOT do yet:
1. **No dynamic forms** — `user_inputs.dialog` fields in SKILL.md are ignored
2. **No route selection** — the `routes` config in pipeline.json is not consumed
3. **No phase grouping** — sidebar shows flat step list, not grouped by phase
4. **No `user_inputs.files` awareness** — doesn't check if prerequisite files exist
5. **No PLANS.md reading** — pipeline state is computed from filesystem, not from orchestrator's PLANS.md
6. **No complexity/route filtering** — all steps shown regardless of preset
7. **Hardcoded start questions** — `startQuestionsMap` in `usePipelineState.ts` is static, not from pipeline.json
8. **No sub_phase awareness** — steps aren't grouped into conceptualization sub-phases
9. **No optional step indicators** — optional steps look the same as required ones
10. **No parallel_group visualization** — parallel steps not visually grouped

---

## Data Contract: pipeline.json → Frontend

The frontend reads `pipeline.json` from `node_modules/concept-forge-skills/cf__shared/pipeline.json` (or `CONCEPT_FORGE_SKILLS_DIR` env override).
Here's what the frontend currently uses vs. what's available:

### Currently Used

| Field | Used By | How |
|-------|---------|-----|
| `steps[].id` | sidebar, status | Step identity |
| `steps[].name` | sidebar | Display label |
| `steps[].skill` | runSkill() | `Run skill ${step.skill}` |
| `steps[].folder` | status computation | `countFiles(_concept/${folder})` |
| `steps[].depends_on` | canRun | All deps must be "complete" |
| `steps[].description` | index page | Shown when step is focused |
| `feedback_loops` | (not used) | — |

### Available but NOT Used

| Field | Purpose | Frontend Value |
|-------|---------|---------------|
| `phases` | Group steps by phase | Sidebar grouping (conceptualization, implementation, etc.) |
| `steps[].phase` | Which phase a step belongs to | Phase-grouped sidebar sections |
| `steps[].sub_phase` | Sub-grouping within phase | Nested sections (functionality, brand, ui-planning) |
| `steps[].optional` | Whether step can be skipped | Dimmed/dashed style, "optional" badge |
| `steps[].parallel_group` | Steps that run concurrently | Visual grouping with "parallel" indicator |
| `steps[].subagent` | Runs in isolated context | Status indicator ("running in background") |
| `steps[].hard_gates` | Prerequisites that must exist | Pre-run validation, disabled button with tooltip |
| `steps[].user_inputs` | Dialog fields + file deps | **Dynamic form generation** |
| `config.complexity_presets` | Step filtering by complexity | Filter visible steps based on user's chosen preset |
| `config.routes` | Project type → step filtering | Route selector during onboarding |
| `config.research_mode` | Parallel research capability | Research toggle/indicator |

### user_inputs Schema (for form generation)

```typescript
interface UserInputs {
  dialog: Array<{
    id: string           // field key
    label: string        // human-readable label
    type: 'text' | 'textarea' | 'select' | 'multiselect' | 'boolean' | 'number'
    required?: boolean   // default false
    options?: string[]   // for select/multiselect
    default?: string     // pre-filled value
    hint?: string        // help text below field
  }>
  files: string[]        // _concept/ paths that must exist (e.g. "01_project/brief.md")
}
```

### Skills with Dialog Fields (forms to build)

| Skill | Fields | When Shown |
|-------|--------|-----------|
| `cf_orchestrator` | complexity, research_depth, conceptualization_depth, route, profile | Onboarding (no _concept/ exists) |
| `cf_concept_overview` | app_name, elevator_pitch, target_audience, problem_statement, hero_flow, comparable_products, success_criteria | Step 1 |
| `cf_research` | research_scope | When research is triggered |
| `cf_concept_functionality_features` | feature_priorities | Step 2 |
| `cf_concept_brand_visual` | reference_urls, mood, light_dark, font_preferences | Brand step |
| `cf_concept_brand_behavioral` | tone, formality_level | Brand behavioral step |
| `cf_concept_techstack` | framework_experience, platform, data_heavy, managed_vs_selfhosted | Tech stack step |
| `cf_implement_feature` | feature_id | Per-feature implementation |
| `cf_test_plan` | test_scope | Test planning |

---

## Suggested Frontend Changes

### Priority 1: Dynamic Form Generation from user_inputs

**The biggest gap.** Instead of hardcoded `startQuestionsMap`, the frontend should:

1. **Load `user_inputs` from pipeline.json** for each step
2. **Render a form** when a step is focused and has unsatisfied inputs
3. **Pre-fill from existing _concept/ files** (check `user_inputs.files`)
4. **Send filled form values as structured prompt** to the agent

#### Implementation sketch:

```typescript
// New API endpoint: GET /api/pipeline/inputs/:stepId
// Returns: { dialog: InputField[], files: FileStatus[], satisfied: boolean }

// In usePipelineState.ts:
interface StepInputs {
  dialog: Array<{
    id: string
    label: string
    type: string
    required: boolean
    options?: string[]
    default?: string
    hint?: string
    currentValue?: string  // pre-filled from _concept/ if file exists
  }>
  files: Array<{
    path: string
    exists: boolean
  }>
  allSatisfied: boolean
}
```

```vue
<!-- In concepts/index.vue, replace startQuestions chips with: -->
<StepInputForm
  v-if="focusedStep && stepInputs && !stepInputs.allSatisfied"
  :inputs="stepInputs"
  @submit="runStepWithInputs"
/>
```

When submitting, format as structured prompt:
```
Run skill cf_concept_overview with inputs:
- app_name: "TaskFlow"
- elevator_pitch: "A simple task manager for small teams"
- target_audience: "Small business owners"
...
```

### Priority 2: Route Selection (Onboarding Flow)

When no `_concept/` exists, show an onboarding wizard instead of the empty pipeline:

1. **Route picker**: Card-based selection (cli_app, prototype, mvp, product)
   - Each card shows: name, description, which phases are included
   - Visual: icons per route type
2. **Complexity selector**: Based on route default, user can override
3. **Research depth selector**: Based on route default
4. **App name + description**: Always required

This replaces the current "Select a concept from the sidebar" fallback.

```vue
<!-- New component: OnboardingWizard.vue -->
<template>
  <div class="max-w-2xl mx-auto py-12">
    <h1>Start a New Concept</h1>

    <!-- Step 1: Route -->
    <RouteSelector v-model="route" :routes="routes" />

    <!-- Step 2: Settings (pre-filled from route defaults) -->
    <SettingsForm v-model="settings" :defaults="routeDefaults" />

    <!-- Step 3: Project basics -->
    <StepInputForm :inputs="overviewInputs" v-model="basics" />

    <UButton @click="startPipeline">Start</UButton>
  </div>
</template>
```

The "Start" button sends the combined inputs to the orchestrator skill.

### Priority 3: Phase-Grouped Sidebar

Group sidebar steps by `phase` and `sub_phase`:

```
▼ Conceptualization                    ← phase header
  ▼ Overall Concept                    ← sub_phase header
    ✓ Project Overview                 ← step
  ▼ Functionality
    ✓ Features
    ○ Behavioral Specs (optional)      ← dimmed, dashed border
  ▼ Data Model
    ◉ Data Model                       ← canRun, highlighted
  ▼ Brand & Identity
    ✓ Brand Visual
    ○ Brand Behavioral (optional)
  ▼ UI Planning
    🔒 Screens                         ← blocked (deps not met)
    ○ Components (optional)

▼ Tech Stack
  ✓ Tech Stack

▶ Implementation                       ← collapsed, all locked
▶ Testing
▶ Quality
```

Implementation: Read `phases` from pipeline.json, group steps by `step.phase`,
then by `step.sub_phase`. Show phase headers as collapsible sections.

### Priority 4: Step Status from PLANS.md

Currently status is computed purely from filesystem (folder has files → complete).
This misses:
- Approval state (files exist but not yet approved)
- Blocked state (dependencies not just "files exist" but "files approved")
- In-progress state (agent is currently running)
- Skipped state (complexity preset excludes this step)

**Hybrid approach:**
1. Keep filesystem scanning as the base
2. **Also read `_concept/PLANS.md`** if it exists
3. PLANS.md has explicit status per step: `- [x] cf_concept_overview — approved 2026-03-13`
4. Prefer PLANS.md status when available, fall back to filesystem scan

```typescript
// server/utils/pipeline.ts additions:
interface PlansEntry {
  stepId: string
  done: boolean
  note: string  // "approved 2026-03-13", "skipped", "blocked by features"
}

function parsePlans(plansPath: string): PlansEntry[] {
  // Parse markdown checkboxes: - [x] step_name — note
  // ...
}
```

### Priority 5: Hard Gate Visualization

Show why a step can't run. Currently it's just a lock icon.

```vue
<!-- When step is locked, show gate requirements on hover/click -->
<UTooltip v-if="!step.canRun">
  <template #text>
    <div v-for="gate in step.hardGates" :key="gate">
      <UIcon :name="gateIcon(gate)" /> {{ gate }}
    </div>
  </template>
  <UIcon name="i-heroicons-lock-closed" />
</UTooltip>
```

Requires pipeline.json to expose `hard_gates` to the frontend (already present
in the data, just not surfaced in the API response).

### Priority 6: Mockup Viewer Upgrade

The `/mockups` page currently lists HTML files. With the new linked prototype
output structure (`05_mockups/index.html` + `screens/*.html`), upgrade to:

1. **Iframe preview** — embed `index.html` in an iframe for live navigation
2. **Device frame selector** — desktop (1280px), tablet (768px), mobile (375px)
3. **Scenario switcher** — toggle between populated/empty/edge_cases
4. **Side-by-side** — screen spec (from `07_screens/`) next to mockup

### Priority 7: Research Mode Toggle

Show research status and allow toggling:
- **Indicator** in header: "Research: moderate" with depth badge
- **Toggle per step**: "Research this step" button that dispatches research skill
- **Research indicator**: Show when `_grounding/` has relevant data for current step

---

## API Changes Needed

### New Endpoints

```
GET  /api/pipeline/definition     → full pipeline.json (phases, routes, config)
GET  /api/pipeline/inputs/:stepId → user_inputs + satisfaction check for a step
GET  /api/pipeline/plans          → parsed PLANS.md (if exists)
POST /api/pipeline/start          → create initial PLANS.md with route + settings
```

### Modified Endpoints

```
GET  /api/pipeline/status
  Add to response:
  + steps[].phase           → string
  + steps[].sub_phase       → string | null
  + steps[].optional        → boolean
  + steps[].parallel_group  → string | null
  + steps[].hard_gates      → string[]
  + steps[].has_user_inputs → boolean
  + steps[].inputs_satisfied → boolean
  + route                   → string | null (from PLANS.md)
  + complexity              → string | null
  + research_depth          → string | null

GET  /api/skills
  Add to response:
  + skills[].user_inputs    → { dialog: Field[], files: string[] } | null
  + skills[].keywords       → string[]
```

---

## pipeline.json v2 Compatibility

The frontend's `PipelineDefinition` interface needs updating to match v2:

```typescript
// Current (v1 shape the frontend expects):
interface PipelineDefinition {
  version: string
  steps: PipelineStep[]
  post_pipeline: PipelinePostStep[]  // ← v2 removed this
  feedback_loops: FeedbackLoop[]
}

// Needed (v2 shape):
interface PipelineDefinitionV2 {
  version: '2.0'
  phases: Record<string, {
    name: string
    description: string
    order: number
    sub_phases?: Record<string, { name: string; order: number }>
  }>
  steps: PipelineStepV2[]
  feedback_loops: FeedbackLoop[]
  config: {
    orchestrator_skill: string
    research_mode: ResearchConfig
    complexity_presets: Record<string, ComplexityPreset>
    routes: Record<string, RouteConfig>
  }
}

interface PipelineStepV2 {
  id: string
  name: string
  skill: string
  phase: string
  sub_phase: string | null
  folder: string | null
  depends_on: string[]
  optional_reads: string[]
  optional: boolean
  parallel_group: string | null
  subagent: boolean
  hard_gates: string[]
  user_inputs: UserInputs | null
  description: string
}
```

**Breaking changes in v2:**
- `post_pipeline` array is gone → quality steps are now regular steps with `phase: "quality"`
- `steps[].required` → replaced by `steps[].optional` (inverted boolean)
- `steps[].outputs` → removed (status computed differently)
- `steps[].parallel_with` → replaced by `steps[].parallel_group`
- New: `phases`, `config`, `steps[].phase`, `steps[].sub_phase`, `steps[].user_inputs`, etc.

The frontend `server/utils/pipeline.ts` needs a compatibility layer or full v2 migration.

---

## File Summary: What to Change Where

### concept-forge (frontend) — files to modify:

| File | Change |
|------|--------|
| `server/utils/pipeline.ts` | Update `PipelineDefinition` to v2 shape, expose phases/config/user_inputs in status |
| `server/api/pipeline/status.get.ts` | Return enriched status with phase, sub_phase, optional, hard_gates, inputs_satisfied |
| `server/api/pipeline/definition.get.ts` | **New**: return full pipeline.json for client-side route/phase rendering |
| `server/api/pipeline/inputs/[stepId].get.ts` | **New**: return user_inputs + file satisfaction for a step |
| `server/api/pipeline/start.post.ts` | **New**: create PLANS.md from onboarding form |
| `server/utils/skill-docs.ts` | Parse `user_inputs` from SKILL.md frontmatter (currently only reads name/description) |
| `app/composables/usePipelineState.ts` | Add phase grouping, route/complexity state, replace hardcoded startQuestionsMap |
| `app/components/AppSidebar.vue` | Phase-grouped sections, optional step styling, hard gate tooltips |
| `app/components/StepInputForm.vue` | **New**: dynamic form from `user_inputs.dialog` |
| `app/components/OnboardingWizard.vue` | **New**: route + settings selection for new concepts |
| `app/components/MockupViewer.vue` | **New**: iframe-based linked prototype viewer |
| `app/pages/concepts/index.vue` | Integrate StepInputForm, OnboardingWizard, phase-aware layout |
| `app/pages/mockups.vue` | Upgrade to iframe viewer with device frames |

### concept-forge-skills — no changes needed

The skills side is already emitting all the data the frontend needs via
`pipeline.json` and `user_inputs` in SKILL.md frontmatter. The contract is
defined; the frontend just needs to consume it.

One minor skill-side cleanup:
- `cf_concept_brand_behavioral/SKILL.md` and `cf_implement_feature/SKILL.md` use
  `prompt` instead of `label` in their dialog fields — normalize to `label`.
