---
name: "skailup-prog-expert-skill-system"
description: "Use when you need to design, build, or refine LLM agent skill systems with pipeline orchestration, artifact-based gates, cross-references, profiles, and UI integration. Expert-level skill architecture and pattern management."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "architecture-contract"
---

# Prog Expert Skill System

## Goal
Expert-level design and implementation of **LLM agent skill systems** — multi-step pipelines where skills produce artifacts, enforce gates, maintain cross-references, and integrate with browser UIs. Based on battle-tested patterns from the Concept Forge ecosystem.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze the user's skill system requirements and existing codebase.
2. **Knowledge Retrieval**:
    - Check `recipes/` for proven skill system patterns.
    - Read `references/` for architecture principles and contract templates.
    - If needed, research new patterns via web search.
3. **Implementation**: Design and build the skill system. Leverage `atomic-examples/` and `recipes/` for guidance.
4. **Learning**:
    - Extract patterns from successful implementations via `scripts/learn_from_success.py`.
    - Create or refine recipes in `recipes/` using `scripts/manage_recipes.py`.

## Instructions

### Design Principles (from Concept Forge learnings)

Before designing any skill system, internalize these proven principles:

1. **Artifacts over conversation** — files on disk are the source of truth, not chat history. Skills read artifacts, produce artifacts, and never rely on conversation context surviving between invocations.

2. **File existence is the only gate** — no status lifecycle bureaucracy. If the prerequisite file exists, the gate passes. Simple, deterministic, debuggable.

3. **Numbered folders = strict read direction** — skills read from lower-numbered folders and write to their own. This prevents circular dependencies and makes data flow obvious.

4. **Cross-references are bidirectional and mandatory** — when skill A creates a link to artifact B, artifact B must also link back to A. Both updated atomically. Broken cross-refs compound exponentially.

5. **Routes define scope, complexity filters within** — two orthogonal dimensions prevent combinatorial explosion. Route says "which phases exist." Complexity says "how deep within those phases."

6. **Skills run standalone or orchestrated** — every skill checks its own gates and can run independently. The orchestrator is a convenience, not a requirement.

7. **Fresh subagent contexts** — when dispatching skills as subagents, provide ONLY the skill definition + required contracts + input artifacts. Never forward full conversation history.

8. **Observable transitions** — structured events at every major boundary enable debugging, progress tracking, and UI integration.

9. **Entropy compounds exponentially** — fix broken links immediately. Audit frequently. Auto-gardening for safe fixes.

10. **Profiles enable reuse** — configuration presets with inheritance avoid per-project boilerplate.

### When designing a skill system

- ALWAYS start by defining the **artifact folder structure** (what files/folders each skill reads and writes)
- ALWAYS define the **dependency graph** before implementing individual skills
- ALWAYS define **hard gates** (file existence checks) that block premature execution
- Before starting any task, search the `recipes/` directory for relevant patterns
- If a task involves a pattern not yet in `recipes/`, create a new recipe after successful implementation

### The five layers of a skill system

Read `references/five-layers.md` for the full architecture guide. In brief:

| Layer | Purpose | Example |
|-------|---------|---------|
| **Pipeline definition** | Machine-readable dependency graph | `pipeline.json` with steps, phases, hard_gates |
| **Shared contracts** | Rules all skills must follow | frontmatter schemas, naming rules, feedback loop protocol |
| **Individual skills** | Single-step workflow definitions | `SKILL.md` files with prerequisites, workflow, outputs |
| **Orchestrator** | Pipeline controller + user communication | Dispatches skills, tracks progress, suggests next steps |
| **UI integration** | Browser-based editing and status display | API endpoints, composables, sidebar components |

## Self-Learning & Research
- Gathers knowledge using web research and the `use-context7-api` skill.
- Learn from successful skill system implementations.
- Refine recipes to be more granular based on implementation experience.

## Auto-Improvement
- Every time this skill is used, analyze the usage chat to find out if further improvement of the skill is advised.
- Ask the user if those changes should be made.
- If approved, store the improvement ideas in the `resources/improvement_ideas.md` file.

## References
- [Five Layers](references/five-layers.md) — The five architectural layers of a skill system.
- [Pipeline Contract](references/pipeline-contract.md) — How to define pipeline.json.
- [Skill Template](references/skill-template.md) — Canonical SKILL.md structure.
- [Feedback Loops](references/feedback-loops.md) — Cross-reference integrity protocol.
- [UI Integration](references/ui-integration.md) — Two-way skill-to-UI communication patterns.
- [Patterns](references/patterns.md) — Reusable code patterns and best practices.
- [Versions](references/versions.json) — Tracked library versions.
- [Recipes](recipes/README.md) — Coding recipes for recurring tasks.

## Example Code
When learning or implementing, use these code examples. ALWAYS load them via `view_file` to maintain Progressive Disclosure:

- **Atomic Examples** (Small code chunks from docs):
  - [Pipeline Step Definition](atomic-examples/pipeline-step.md): single step in pipeline.json
  - [Hard Gate Check](atomic-examples/hard-gate-check.md): file existence gate pattern
  - [Observability Event](atomic-examples/observability-event.md): structured event emission
  - [Frontmatter Schema](atomic-examples/frontmatter-schema.md): TypeBox schema for artifact frontmatter
- **Recipes** (Larger patterns):
  - [Artifact Pipeline](recipes/artifact-pipeline.md): full pipeline from brief to implementation
  - [Cross-Reference Repair](recipes/cross-reference-repair.md): bidirectional link maintenance
  - [Profile System](recipes/profile-system.md): configuration presets with inheritance
  - [Standalone Skill](recipes/standalone-skill.md): skill that runs independently with gate checks
- **Reference Implementations** (From Concept Forge):
  - [Pipeline Status Computation](reference-implementations/pipeline-status.md): server-side step status from artifacts
  - [Sidebar Step Display](reference-implementations/sidebar-step-display.md): UI component for pipeline progress
  - [Onboarding Wizard](reference-implementations/onboarding-wizard.md): profile-aware project setup flow

## Constraints
* Do not perform unauthorized or destructive actions.
* Do not overwrite existing files without explicit user confirmation.
* Always recommend file-existence gates over status-lifecycle gates.
* Never design skills that depend on conversation history surviving between invocations.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Example Management**: `uv run scripts/manage_examples.py <action> [args]`
- **Pattern Learning**: `uv run scripts/learn_from_success.py <path/to/file>`
