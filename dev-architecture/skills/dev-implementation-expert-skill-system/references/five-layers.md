# The Five Layers of a Skill System

A production skill system has five distinct architectural layers. Each layer has clear responsibilities and interfaces.

## Layer 1: Pipeline Definition

**What it is:** A machine-readable dependency graph that defines execution order, gates, and configuration.

**Key file:** `pipeline.json`

```json
{
  "version": "2.0",
  "phases": {
    "conceptualization": { "name": "Conceptualization", "order": 1,
      "sub_phases": { "overview": { "name": "Overview", "order": 1 } }
    },
    "implementation": { "name": "Implementation", "order": 3 }
  },
  "steps": [
    {
      "id": "step_overview",
      "name": "Project Overview",
      "skill": "skill_overview",
      "phase": "conceptualization",
      "sub_phase": "overview",
      "folder": "01_project/",
      "depends_on": [],
      "hard_gates": [],
      "optional": false,
      "parallel_group": null,
      "subagent": false,
      "user_inputs": { "dialog": [], "files": [] },
      "description": "Creates the project brief from user input"
    }
  ],
  "feedback_loops": [
    { "from": "skill_screens", "to": "03_features/", "field": "screens", "description": "Screen specs register back to features" }
  ],
  "config": {
    "orchestrator_skill": "skill_orchestrator",
    "standalone_mode": { "enabled": true },
    "routes": {
      "mvp": { "name": "MVP", "include_phases": ["conceptualization", "implementation"], "skip_steps": [], "default_complexity": "moderate" }
    },
    "complexity_presets": {
      "simple": { "skip_steps": ["optional_step"], "research_depth": "light" },
      "complex": { "skip_steps": [], "research_depth": "deep" }
    },
    "profiles": { "file": "profiles.json", "project_override": "_concept/profile.json" }
  }
}
```

**Design rules:**
- Steps form a DAG (directed acyclic graph) via `depends_on`
- `hard_gates` are `{ type: "file_exists", path: "..." }` checks — the ONLY gate mechanism
- `parallel_group` enables concurrent execution of independent steps
- `user_inputs.dialog` defines what information the skill needs from the user
- Routes define which phases/steps exist; complexity presets filter within

## Layer 2: Shared Contracts

**What it is:** Rules and schemas that ALL skills must follow. Prevents drift and enables cross-skill interoperability.

**Key files:**
- `frontmatter.md` — YAML field definitions for artifact files
- `concept_structure.md` — canonical folder paths and naming rules
- `feedback_loop.md` — cross-reference protocol (bidirectional links)
- `golden_principles.md` — mechanical rules (entities need id+timestamps, sequential numbering, lowercase_snake_case)
- `iron_laws.md` — non-negotiable constraints
- `agent_patterns.md` — reusable workflow patterns
- `seed_data.md` — test data conventions

**Design rules:**
- Contracts are the source of truth, not individual skill instructions
- Every skill imports relevant contracts before operating
- New contracts require updating all affected skills

## Layer 3: Individual Skills

**What it is:** Single-step workflow definitions that read artifacts, transform them, and produce new artifacts.

**Key file:** Each skill has a `SKILL.md` with standardized sections:

1. **Frontmatter** — name, description, keywords, user_inputs
2. **Overview** — what it produces (1-2 sentences)
3. **When to Use / When NOT to Use** — triggering conditions
4. **Prerequisites** — hard_gates from pipeline.json
5. **Context Budget** — must read / optional / never load (prevents token waste)
6. **Standalone Mode** — gate checks, missing skill naming, completion behavior
7. **Workflow** — numbered steps
8. **Outputs** — files produced
9. **Completion Summary** — user-facing template
10. **Common Mistakes** — rationalization defense table
11. **Integration** — called by, pairs with, feedback loops

**Design rules:**
- Skills read from lower-numbered artifact folders, write to their own
- Skills can run standalone (check own gates) or orchestrated
- Skills collect their own user inputs when running standalone
- Skills emit observability events at major transitions

## Layer 4: Orchestrator

**What it is:** Pipeline controller that dispatches skills, tracks progress, handles user communication, and suggests next steps.

**Responsibilities:**
- Read pipeline.json and compute execution order
- Track progress in a durable plan file (PLANS.md)
- Dispatch skills as subagents with fresh contexts
- Run quality checks between phases (advisory, not blocking)
- After standalone skill completion, suggest which skills are now unblocked
- Handle onboarding (route, profile, complexity selection)

**Design rules:**
- Orchestrator handles user communication directly (no separate facilitator)
- Subagent dispatch uses ONLY skill definition + contracts + input artifacts
- Progress is file-based (PLANS.md), not conversation-based
- Review is advisory: auto-approve when quality score >= threshold

## Layer 5: UI Integration

**What it is:** Browser-based interface for viewing artifacts, tracking pipeline status, and triggering skills.

**Two-way communication pattern:**

```
Browser UI                                  Skill System
    │                                           │
    │  GET /api/pipeline/status  ◄──────────── │ (reads artifact folders, computes step status)
    │  POST /api/pipeline/start  ──────────►   │ (writes config.json + PLANS.md)
    │  POST /api/pipeline/approve/:id ────►    │ (updates approved_steps in config.json)
    │  POST /api/agent/prompt ─────────────►   │ (triggers skill execution)
    │                                           │
    │  sidebar: step status icons              │ (computed from file existence + counts)
    │  sidebar: impl_status badges             │ (aggregated from file frontmatter)
    │  onboarding: profile + route + settings  │ (drives config.json creation)
    │  editor: artifact viewing/editing        │ (reads/writes _concept/ files)
```

**Key patterns:**
- Pipeline status is computed server-side by scanning artifact folders (no status field needed)
- `impl_status` (pending/implemented/tested) is aggregated from file frontmatter for display
- Onboarding wizard creates config.json (machine state) and PLANS.md (agent-readable plan)
- Step approval is pipeline-level (approve & continue), separate from file-level status
- Hidden directories (_grounding) are filtered from the sidebar file tree
- Profile selection during onboarding syncs complexity + research_depth defaults
