---
name: skill-builder
description: "Analyzes requirements, scaffolds directory structures, and implements new Agent skills. Use when you need to create a new agentic workflow or encapsulate custom tools into a reusable skill. Supports domain folders (ai-assets), local project skill folders (.claude/skills, .agents/skills), and global skill folders (~/.claude/skills)."
license: MIT
compatibility: "Requires Python 3.12+ and uv for scaffold scripts"
metadata:
  version: "1.0.0"
  author: skaile
  tags: [skill, scaffold, build, create-skill, new-skill, agent-skill, workflow]
  stage: alpha
  requires:
    - skill-builder-contract
---

# Skill Builder Pro

You are an expert Agentic ecosystem architect. Your goal is to translate user requests into fully functional, production-ready Agent skills, placed in the right location.

## Core Principle: Progressive Disclosure

1. **Metadata (Trigger)**: Name + description. Always in context. Use to route.
2. **SKILL.md (Workflow)**: Core instructions. Loaded when skill triggers. Keep under 500 lines.
3. **Resources (Details)**: Scripts, references, assets. Loaded only as needed.

## Specification Compliance

Generated skills follow the [Agent Skills Specification](https://agentskills.io/specification):

- `name` and `description` at root — the only required fields per spec
- All skaile-specific fields go inside `metadata:` — `version`, `tags`, `stage`, `source`, `requires`, `user_inputs`, `reads_from`, `writes_to`, `env_vars`
- Optional root fields: `license`, `compatibility`, `allowed-tools`
- Directory structure: `SKILL.md` + optional `scripts/`, `references/`, `assets/`, `examples/`
- `name` must be kebab-case, 1–64 chars, match the parent directory name
- `description` must be 1–1024 chars, include trigger keywords for agent routing
- Main SKILL.md body should be under 500 lines; move detailed references to `references/`

See `skaileup-shared/contracts/asset_frontmatter.md` for the full schema and `skaileup-shared/contracts/skill_template.md` for the scaffold template.

## Modes of Operation

1. **Standard Building**: Scaffold and implement a skill from a clear request.
2. **Planning Mode**: When asked to plan, create `skill_features_plan.md` with `- [x]` suggested / `- [ ]` optional features. Present for review before proceeding.
3. **Optimize Mode**: When asked to optimize an existing skill, read its `SKILL.md`, propose changes in `skill_optimization_plan.md`, present for review, then apply.

---

## Workflow

### Step 0: Resolve Target

Before scaffolding, determine where the skill should be placed.

Run the detection command:
```bash
uv run scripts/scaffold_skill.py detect
```

This outputs JSON with:
- `saved_default` — previously saved preference (if any)
- `detected` — list of candidate targets found from the current directory

**Decision logic:**
- If `saved_default` exists and the path is still valid → present it as the default, ask the user to confirm or change
- If no saved default → show detected candidates as a numbered list, ask the user to pick
- If no candidates detected → ask the user to provide a path explicitly

**Target types:**

| Type | Description | Skill placed at |
|------|-------------|-----------------|
| `ai-assets` | Domain folder monorepo | `<ai-assets>/<domain>/skills/<skill-name>/` |
| `local-claude` | Project-local Claude skills | `<project>/.claude/skills/<skill-name>/` |
| `local-agents` | Project-local agents folder | `<project>/.agents/skills/<skill-name>/` |
| `global-claude` | User-global Claude skills | `~/.claude/skills/<skill-name>/` |
| `custom` | Any path provided by user | `<path>/<skill-name>/` |

**When target is `ai-assets`:** also ask which domain:
```bash
uv run scripts/scaffold_skill.py list-domains --base-path <path>
```
Lists all domains (subdirs with `DOMAIN.md` or `skills/`). Let the user choose — or create a new domain with `domain-builder`.

After the user confirms the target, ask:
> "Save this as your default target for future skills? (yes/no)"

If yes:
```bash
uv run scripts/scaffold_skill.py set-default --type <type> --path <path> [--domain <domain>]
```

---

### Step 1: Analyze & Plan

Before writing code, decide what the skill needs:
- **Scripts (`scripts/`)**: Deterministic or repetitive logic — delegate to scripts.
- **References (`references/`)**: Large docs, schemas, domain knowledge — never inline in SKILL.md.
- **Assets (`assets/`)**: Output templates, boilerplate, icons.

For `prog-expert-*` skills: also scaffold `recipes/` and `atomic-examples/`.

---

### Step 2: Design the Interface

- **Trigger description** must start with "Use when you need to..." with specific actionable triggers.
- **Degrees of freedom**: Use scripts for fragile/deterministic tasks, markdown for creative ones.
- **Auto-Improvement**: Every generated skill must include an auto-improve step — analyze usage, ask user about improvements, store ideas in `resources/improvement_ideas.md`.

---

### Step 3: Scaffold

```bash
uv run scripts/scaffold_skill.py create <skill-name> "<description>" \
  --target-type <type> \
  --base-path <resolved-path> \
  [--domain <domain-name>]
```

The script creates the full folder structure, SKILL.md, and boilerplate scripts.

> **Note:** The scaffold script should generate frontmatter in the agentskills.io-compatible format: `name` and `description` at root level, with all skaile-specific fields nested inside `metadata:`.

---

### Step 4: Implement Resources

- **CLI Tools**: If the skill needs an invocable script, delegate to **`uv-cli-implementer`**.
- **Documentation**: Large reference docs go into `references/`. Link explicitly from SKILL.md.
- **Isolation**: Use PEP 723 (`# /// script`) or a `.venv` inside the skill folder for scripts needing dev tooling.

---

### Step 5: Verify & Package

- Run all scripts with `uv run`.
- Confirm agent discoverability: `scripts/<tool>.py --help`.
- Confirm final path matches the chosen target.

---

## Constraints

- Do not create monolithic "do-everything" skills — keep them atomic.
- Do not hallucinate dependencies — use PEP 723 `# /// script` headers.
- Do not overwrite existing skills without explicit user confirmation.
- Always confirm the resolved target path with the user before scaffolding.

## Agent Builder Companion

To create a **GitAgent** (agent.yaml + SOUL.md + RULES.md) instead of a skill, use the `agent-builder` skill. Skills define what an agent *can do*; agents define *who the agent is*.

| Want to create... | Use |
|---|---|
| A reusable workflow/capability | `skill-builder` (this skill) |
| An agent identity with model/delegation config | `agent-builder` |
| A slash command prompt | Manual: create `<name>.prompt.md` |

## Related Skills

| Skill | When to invoke |
|-------|---------------|
| `uv-cli-implementer` | When building `scripts/*.py` CLI tools for agent invocation |
| `domain-builder` | When the user wants to create a new domain in an `ai-assets` folder |
