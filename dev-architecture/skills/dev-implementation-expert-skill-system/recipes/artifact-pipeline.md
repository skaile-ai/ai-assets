# Recipe: Artifact Pipeline

How to design a complete artifact-based pipeline from scratch.

## Step 1: Define the artifact folder structure

```
_artifacts/
├── 01_requirements/    brief.md, goals.md
├── 02_design/          architecture.md, components.md
├── 03_implementation/  plan.md, feature specs
├── 04_testing/         test_plan.md
└── config.json         machine state
```

**Rules:**
- Numbered folders = execution order
- Skills read from lower numbers, write to their own
- Special folders (unnumbered) are readable by all: `_research/`, `_standards/`

## Step 2: Define the dependency graph (pipeline.json)

For each step, define:
- `depends_on` — which steps must complete first
- `hard_gates` — file existence checks (the ONLY gate)
- `parallel_group` — steps that can run concurrently
- `outputs` — expected files (for in_progress vs complete detection)

## Step 3: Define shared contracts

Minimum viable contracts:
1. **Folder structure** — canonical paths and naming rules
2. **Frontmatter schema** — required fields per file type
3. **Cross-reference protocol** — which fields link between folders
4. **Iron laws** — non-negotiable constraints

## Step 4: Write individual skill SKILL.md files

Use the canonical template (see `references/skill-template.md`). Each skill:
- Reads from depends_on folders
- Writes to its own folder
- Checks hard_gates before starting
- Emits observability events
- Includes standalone mode section

## Step 5: Build the orchestrator

The orchestrator:
- Reads pipeline.json
- Computes execution order from DAG
- Dispatches skills (optionally as subagents)
- Tracks progress in PLANS.md
- Suggests next steps after each skill completes

## Step 6: Build the UI integration (optional)

If you have a companion browser UI:
- Compute step status by scanning artifact folders
- Store machine state in config.json (not in artifacts)
- Provide onboarding wizard for route/profile/settings
- Show pipeline progress in sidebar with step status icons
- Aggregate impl_status from frontmatter for implementation tracking

## Anti-patterns

| Anti-pattern | Why it fails | What to do instead |
|-------------|-------------|-------------------|
| Status lifecycle (draft→approved) | Requires manual state management | File existence = gate |
| Separate facilitator skill | Adds indirection without value | Orchestrator handles communication |
| Conversation-dependent state | Lost between invocations | Artifacts on disk are the source of truth |
| Loading all artifacts | Token waste | Context Budget per skill |
| Monolithic skills | Too many concerns | One skill, one folder, one concern |
