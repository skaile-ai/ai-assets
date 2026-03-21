---
name: skill-builder
description: "Analyzes requirements, scaffolds directory structures, and implements new Agent skills. Use when you need to create a new agentic workflow or encapsulate custom tools into a reusable skill. Supports domain folders (ai-resources), local project skill folders (.claude/skills, .agents/skills), and global skill folders (~/.claude/skills)."
keywords: [skill, scaffold, build, create-skill, new-skill, agent-skill, workflow]
metadata:
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
| `ai-resources` | Domain folder monorepo | `<ai-resources>/<domain>/skills/<skill-name>/` |
| `local-claude` | Project-local Claude skills | `<project>/.claude/skills/<skill-name>/` |
| `local-agents` | Project-local agents folder | `<project>/.agents/skills/<skill-name>/` |
| `global-claude` | User-global Claude skills | `~/.claude/skills/<skill-name>/` |
| `custom` | Any path provided by user | `<path>/<skill-name>/` |

**When target is `ai-resources`:** also ask which domain:
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

## Related Skills

| Skill | When to invoke |
|-------|---------------|
| `uv-cli-implementer` | When building `scripts/*.py` CLI tools for agent invocation |
| `domain-builder` | When the user wants to create a new domain in an `ai-resources` folder |
