---
name: "skill-builder-contract"
description: "Shared contract for all skill-builder domain skills. Describes skill folder structure, SKILL.md frontmatter conventions, CLI.md format, progressive disclosure loading strategy, and how skill-builder skills are composed. REQUIRED reading for skills that create or scaffold other skills."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Skill Builder Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `skill-builder` domain skills read this before operating.

## Scope

The `skill-builder` domain contains skills that **create other skills** — scaffolding, implementing, and refining SKILL.md prompts and their supporting artifacts. This contract defines the output format, folder conventions, and loading strategy those skills must follow.

## Domain Skills

| Skill | Purpose |
|-------|---------|
| `skill-builder` | Scaffold and implement full Agent skills from requirements |
| `uv-cli-implementer` | Build single-file Python CLI tools (uv + Typer) for agent consumption |

## Skill Folder Structure

Every skill output must follow this layout:

```
<skill-name>/
├── SKILL.md                 ← Required. YAML frontmatter + agent prompt body
├── CLI.md                   ← Optional. How to invoke from the CLI / slash command
├── resources/
│   └── README.md            ← Reference material, links, background knowledge
├── examples/
│   └── <example>.md         ← Worked examples of the skill in action
└── scripts/
    └── <utility>.py         ← Supporting scripts (uv-runnable, zero-dependency)
```

## SKILL.md Frontmatter

All skills produced by this domain must use this frontmatter:

```yaml
---
name: <skill-name>
description: "<Third-person trigger description. Used for routing — be specific about when to use this skill.>"
keywords: [<keyword>, ...]
source: MIGRATED | MERGED | CF | SAXE | TEST
stage: alpha | beta | production
user_inputs:
  - key: VARIABLE_NAME
    prompt: "Question shown to user"
    required: true | false
reads_from: [<path>, ...]
writes_to: [<path>, ...]
---
```

Required fields: `name`, `description`, `stage`.

## CLI.md Format

```markdown
# <Skill Name> — CLI Usage

## Slash Command
/<skill-name>

## Arguments
- `--arg`: description

## Examples
/<skill-name> --arg value
```

## Progressive Disclosure Loading

Skills must be designed for three-level loading to maximize context efficiency:

| Level | What is loaded | When |
|-------|---------------|------|
| **Metadata** | `name` + `description` frontmatter only | Always — used for routing |
| **SKILL.md** | Full prompt body | When skill triggers |
| **Resources** | `resources/`, `examples/`, `scripts/` | Only when needed mid-execution |

**Keep SKILL.md under 500 lines.** Move reference material to `resources/`.

## uv Script Conventions (uv-cli-implementer output)

CLI tools produced by `uv-cli-implementer` must:

- Be a **single Python file** with inline PEP 723 dependency metadata
- Run with `uv run <script>.py [args]` — zero external setup
- Accept structured input (flags/args), return JSON on `stdout`
- Report errors on `stderr` with non-zero exit code
- Include `--help` for agent self-discovery

```python
# /// script
# requires-python = ">=3.11"
# dependencies = ["typer", "httpx"]
# ///
import typer, json, sys
app = typer.Typer()

@app.command()
def main(query: str = typer.Argument(..., help="What to query")):
    # ... do work ...
    print(json.dumps({"result": ...}))

if __name__ == "__main__":
    app()
```
