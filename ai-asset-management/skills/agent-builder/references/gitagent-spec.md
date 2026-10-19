# GitAgent Specification Reference

Summary of the [GitAgent Specification](https://www.gitagent.sh/) for quick reference during agent building.

## Core Principle

"Clone a repo, get an agent." A repository is the agent. Git provides version control, branching for A/B testing, diff-based auditing, and tag-based releases.

## Minimal Agent (2 files)

```
agent-name/
├── agent.yaml    # Manifest
└── SOUL.md       # Identity
```

## Standard Agent (full structure)

```
agent-name/
├── agent.yaml              # Required: manifest
├── SOUL.md                 # Required: identity
├── RULES.md                # Hard constraints
├── DUTIES.md               # Segregation of duties
├── AGENTS.md               # Framework-agnostic instructions
├── skills/                 # Capability modules
├── tools/                  # MCP-compatible schemas
├── workflows/              # SkillsFlow multi-step procedures
├── knowledge/              # Reference documents
│   └── index.yaml          # Listing order
├── memory/runtime/         # Live state (gitignored)
├── hooks/                  # Lifecycle handlers
├── config/                 # Environment overrides
├── compliance/             # Regulatory artifacts
├── agents/                 # Sub-agent definitions
└── examples/               # Few-shot calibration
```

## agent.yaml Required Fields

| Field | Description |
|---|---|
| `spec_version` | Agent spec version: `"0.2"` for skaile assets (upstream GitAgent itself is "0.1.0") |
| `name` | kebab-case identifier |
| `version` | semver |
| `description` | One-line description |

## agent.yaml Optional Fields

| Field | Description |
|---|---|
| `author` | Creator identifier |
| `license` | License name |
| `tags` | Searchable keywords |
| `extends` | Path to parent agent.yaml for inheritance |
| `model` | LLM preferences: preferred, fallback, constraints |
| `composes` | Typed items the agent is built from: `{kind, ref, id?, as?, binding?}` (skills, sub-agents, personas, rulesets, connectors, ...) |
| `lifecycle` | `standing` (default) or `role` (short-lived, callable; needs `io`) |
| `io` | JSON Schemas `{input, output}` for a role |
| `memory` | `blocks[{name, scope, max_chars, source?}]`, stored by the host and rendered as `<MEMORY>` |
| `triggers` | Host-fired `schedule` / `webhook` / `channel` / `event` triggers, each with an `id` |
| `delegation` | Mode: explicit, router, auto (descriptive only) |
| `runtime` | max_turns, timeout |
| `compliance` | Regulatory config (risk_tier, supervision, recordkeeping, SOD) |
| `metadata` | Arbitrary extensions |

## Composition

```yaml
# Inherit from a parent agent.yaml (upstream GitAgent also accepts a git URL;
# in ai-assets the path must be relative and stay inside the repo)
extends: ../parent-agent/agent.yaml

# Compose skills and sub-agents (0.2; replaces the 0.1 `skills:` and `requires:` lists)
composes:
  - { kind: skill, ref: skill:@skaile-ai/deep-research }
  - { kind: agent, ref: "agent:@skaile-ai/fact-checker", as: fact-checker, binding: delegate }
```

`skills:`, `requires:`, `persona:`, `rules:` and `abilities:` are deprecated 0.1 inputs.

## Export Targets

system-prompt, claude-code, openai, crewai, lyzr, github, git, opencode, openclaw, nanobot

## CLI (upstream GitAgent 0.1)

This is the upstream tool and it only knows spec 0.1. For a Skaile 0.2 manifest run `skaile validate` instead.

```bash
gitagent init --template standard
gitagent validate --compliance
gitagent export --format system-prompt
gitagent run <source> --adapter claude-code
gitagent audit
```

## Spec Version

Upstream GitAgent: v0.1.0 — https://github.com/open-gitagent/gitagent. Skaile assets use `spec_version: "0.2"`, which adds `composes`, `lifecycle`, `io`, `memory` and `triggers` (see `docs/agents.md` in ai-assets).
