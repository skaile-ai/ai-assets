---
name: prog-expert-git-agent
description: Use when you need to create, configure, or manage GitAgent-compatible agent definitions (agent.yaml, SOUL.md, RULES.md, SKILL.md). Expert-level programming and pattern management for the GitAgent open standard (spec v0.1.0).
---

# Prog Expert Git Agent

## Goal
Expert-level management of GitAgent agent definitions. Handles agent.yaml manifests, SOUL.md identity files, RULES.md constraints, SKILL.md capabilities, multi-agent hierarchies, knowledge injection, and cross-framework export. Maintains a repository of reusable patterns, recipes, and reference implementations for the GitAgent ecosystem.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze the current repository structure and existing agent definitions.
2. **Knowledge Retrieval**:
    - Check `recipes/` for reusable agent patterns and configuration templates.
    - Check `references/` for the GitAgent spec and Agent Skills spec.
    - If needed, research new patterns via `scripts/research_knowledge.py`.
3. **Implementation**: Create or modify agent definitions following the spec strictly.
4. **Validation**: Run `npx gitagent validate` to verify compliance.
5. **Learning**:
    - Extract patterns from successful agent configurations via `scripts/learn_from_success.py`.
    - Create or refine recipes in `recipes/` using `scripts/manage_recipes.py`.

## GitAgent Spec v0.1.0 — Quick Reference

### Core Files (2 required + optional)

| File | Required | Purpose |
|------|----------|---------|
| `agent.yaml` | YES | Manifest: name, version, model, skills, tools, agents, compliance |
| `SOUL.md` | YES | Identity: personality, values, communication style, expertise |
| `RULES.md` | No | Hard constraints: must-always, must-never, output constraints |
| `DUTIES.md` | No | Segregation of duties: roles, conflicts, handoffs |
| `AGENTS.md` | No | Framework-agnostic fallback instructions |

### agent.yaml Schema

**Required fields:**
```yaml
spec_version: "0.1.0"           # specification version
name: my-agent                   # kebab-case, matches ^[a-z][a-z0-9-]*$
version: 1.0.0                   # semantic version X.Y.Z
description: One-line summary    # what this agent does
```

**Model configuration:**
```yaml
model:
  preferred: claude-opus-4-6     # primary model
  fallback:                      # fallback chain
    - claude-sonnet-4-6
    - claude-haiku-4-5-20251001
  constraints:
    temperature: 0.2             # 0.0-2.0
    max_tokens: 4096
    top_p: 0.9
    top_k: 50
    stop_sequences: []
    presence_penalty: 0.0
    frequency_penalty: 0.0
```

**Skills & tools:**
```yaml
skills:                          # array of skill directory names
  - code-review
  - data-analysis
tools:                           # array of tool names (without extension)
  - search-regulations
  - generate-report
```

**Sub-agents:**
```yaml
agents:
  fact-checker:                  # sub-agent name
    description: Verifies factual claims
    delegation:
      mode: auto                 # auto | explicit | router
      triggers:
        - factual_claim_detected
```

**Dependencies (external agents):**
```yaml
dependencies:
  - name: fact-checker
    source: ./agents/fact-checker       # local path or git URL
    version: 1.0.0
    mount: agents/fact-checker
    vendor_management:                  # optional compliance metadata
      due_diligence_date: "2026-01-15"
      risk_assessment: low
```

**Inheritance:**
```yaml
extends: ../parent-agent/agent.yaml    # inherit from parent agent
```

**Delegation:**
```yaml
delegation:
  mode: router                   # auto | explicit | router
  router: orchestrator-skill     # router skill name (when mode=router)
```

**Runtime:**
```yaml
runtime:
  max_turns: 50                  # max conversation turns
  temperature: 0.1               # override model temperature
  timeout: 300                   # seconds
```

**Metadata & tags:**
```yaml
author: org-name
license: MIT                     # SPDX identifier
tags: [code-review, developer-tools]
metadata:                        # arbitrary key-value (string/number/boolean only)
  department: engineering
  cost_center: CC-ENG-001
```

**Compliance (optional, for regulated environments):**
```yaml
compliance:
  risk_tier: standard            # low | standard | high | critical
  frameworks: [finra, sec]
  supervision:
    human_in_the_loop: conditional  # always | conditional | advisory | none
    kill_switch: true
  recordkeeping:
    audit_logging: true
    retention_period: 6y
  segregation_of_duties:
    roles:
      - id: maker
        permissions: [create, submit]
      - id: checker
        permissions: [review, approve, reject]
    conflicts: [[maker, checker]]
    enforcement: strict          # strict | advisory
```

### SOUL.md Structure

Minimal valid SOUL.md is a single paragraph. Recommended sections:
- **Core Identity**: Agent's primary purpose
- **Communication Style**: Tone, formality, verbosity
- **Values & Principles**: Decision-making priorities
- **Domain Expertise**: Specialized knowledge areas
- **Collaboration Style**: How it interacts

### RULES.md Structure

Recommended sections:
- **Must Always**: Required behaviors
- **Must Never**: Prohibited behaviors
- **Output Constraints**: Format/content restrictions
- **Interaction Boundaries**: Scope limits
- **Safety & Ethics**: Ethical guardrails

### SKILL.md Format (Agent Skills Standard — agentskills.io)

**Frontmatter (YAML):**
```yaml
---
name: skill-name                 # required, 1-64 chars, kebab-case
                                 # MUST match parent directory name
description: >-                  # required, 1-1024 chars
  What it does and when to use it.
  Include keywords for agent discovery.
license: MIT                     # optional
compatibility: >-                # optional, max 500 chars
  Requires Python 3.14+ and uv
metadata:                        # optional, arbitrary string key-value
  author: org-name
  version: "1.0"
allowed-tools: Bash(git:*) Read  # optional, experimental
---
```

**Body:** Free-form markdown. Recommended sections:
- Step-by-step instructions
- Examples of inputs and outputs
- Common edge cases

**Progressive Disclosure (3-tier loading):**
1. **Metadata** (~100 tokens): `name` + `description` — always in context
2. **Instructions** (<5000 tokens): Full SKILL.md body — loaded on activation
3. **Resources** (as needed): `scripts/`, `references/`, `assets/` — loaded on demand

**Naming rules:**
- 1-64 characters
- Lowercase alphanumeric + hyphens only
- No leading/trailing hyphens, no consecutive hyphens
- Must match parent directory name exactly

### Directory Structure Convention

```
my-agent/
├── agent.yaml                 # [REQUIRED] manifest
├── SOUL.md                    # [REQUIRED] identity
├── RULES.md                   # hard constraints
├── DUTIES.md                  # segregation of duties
├── AGENTS.md                  # framework-agnostic fallback
├── skills/
│   └── <skill-name>/
│       ├── SKILL.md           # skill definition
│       ├── scripts/           # executable helpers
│       ├── references/        # domain docs
│       ├── assets/            # templates, schemas
│       └── examples/          # input/output examples
├── tools/
│   ├── <name>.yaml            # MCP-compatible tool schema
│   └── <name>.py|.sh|.js      # tool implementation
├── knowledge/
│   ├── index.yaml             # retrieval hints
│   └── *.md|csv|pdf           # reference documents
├── memory/
│   ├── MEMORY.md              # current state (200 line max)
│   └── archive/               # historical snapshots
├── workflows/
│   └── *.yaml|*.md            # multi-step procedures
├── hooks/
│   ├── hooks.yaml
│   └── scripts/               # lifecycle event handlers
├── examples/
│   ├── good-outputs.md
│   └── bad-outputs.md
├── agents/                    # sub-agents (recursive structure)
│   ├── <name>/agent.yaml      # full sub-agent (directory)
│   └── <name>.md              # lightweight sub-agent (file)
├── config/
│   ├── default.yaml
│   └── <env>.yaml             # environment overrides
├── compliance/                # regulatory artifacts
└── .gitagent/                 # runtime state (gitignored)
    ├── deps/
    ├── state.json
    └── cache/
```

### CLI Commands

```bash
npm install -g gitagent         # install globally
gitagent init --template standard  # scaffold new agent
gitagent validate               # validate agent configuration
gitagent run                    # execute agent
gitagent export                 # export to other frameworks
```

### Multi-Agent Patterns

**Pattern 1: Domain directories as agents**
Each domain dir contains its own `agent.yaml` + `SOUL.md` + skills:
```
workspace/
├── agent.yaml                  # root orchestrator
├── domain-a/
│   ├── agent.yaml              # extends: ../agent.yaml
│   └── skills/...
├── domain-b/
│   ├── agent.yaml
│   └── skills/...
```

**Pattern 2: Monorepo with shared knowledge**
```
workspace/
├── agent.yaml
├── knowledge/                  # shared across all agents
│   ├── index.yaml
│   └── contracts/*.md
├── agents/
│   ├── agent-a/agent.yaml
│   └── agent-b/agent.yaml
└── skills/                     # shared skills
```

**Pattern 3: External dependencies**
```yaml
dependencies:
  - name: shared-skills
    source: https://github.com/org/shared-skills.git
    version: ^1.0.0
    mount: agents/shared
```

### Naming Conventions
- YAML keys: `snake_case`
- Agent/skill/tool names: `kebab-case` (lowercase with hyphens)

## Instructions
- ALWAYS validate with `gitagent validate` after creating or modifying agent definitions.
- Before starting any task, search `recipes/` for relevant agent configuration patterns.
- If a task involves a recurring pattern not yet in `recipes/`, create a new recipe after successful implementation.
- When creating multi-agent systems, ensure `extends:` paths resolve correctly relative to sub-agent locations.
- Skills listed in `agent.yaml` `skills:` array must have matching SKILL.md files discoverable in the repo.
- The `knowledge/` directory is the canonical mechanism for shared context injection across skills.

## Self-Learning & Research
- Gathers knowledge using web research and the `use-context7-api` skill.
- Learn from successful agent configurations when used.
- Keep track of GitAgent spec updates and CLI changes.
- Refine recipes to cover more multi-agent patterns.

## Auto-Improvement
- Every time this skill is used, analyze the usage chat to find out if further improvement of the skill is advised.
- Ask the user if those changes should be made.
- If approved, store the improvement ideas in the `resources/improvement_ideas.md` file.

## References
- [Patterns](references/patterns.md) — Reusable agent configuration patterns.
- [GitAgent Spec](references/gitagent-spec.md) — Full specification reference.
- [Agent Skills Spec](references/agent-skills-spec.md) — SKILL.md format specification.
- [Versions](references/versions.json) — Tracked versions.
- [Recipes](recipes/README.md) — Agent configuration recipes.

## Example Code
When learning or implementing, use these resources. ALWAYS load them via `view_file` to maintain Progressive Disclosure:

- **Atomic Examples** (Minimal agent definitions):
  - [Minimal agent](atomic-examples/minimal-agent.md): Bare-bones 2-file setup
  - [Standard agent](atomic-examples/standard-agent.md): Agent with skills and tools
  - [Multi-agent](atomic-examples/multi-agent.md): Parent + sub-agent hierarchy
- **Recipes** (Reusable patterns):
  - [Domain sub-agent](recipes/recipe-domain-sub-agent.md): Adding a domain agent to a workspace
  - [Shared knowledge](recipes/recipe-shared-knowledge.md): Cross-agent context injection
- **Reference Implementations**:
  - [Concept-forge integration](reference-implementations/concept-forge-gitagent.md): Real-world multi-agent pipeline

## Constraints
- Do not perform unauthorized or destructive actions.
- Do not overwrite existing agent definitions without explicit user confirmation.
- Always follow GitAgent spec v0.1.0 — do not invent non-standard fields.
- The `name` field in agent.yaml and SKILL.md must be kebab-case.
- SKILL.md `name` MUST match its parent directory name.

## Script Integration
- **Research**: `uv run scripts/research_knowledge.py "<query>"`
- **Version Tracking**: `uv run scripts/track_versions.py`
- **Recipe Management**: `uv run scripts/manage_recipes.py <action> [args]`
- **Example Management**: `uv run scripts/manage_examples.py <action> [args]`
- **Pattern Learning**: `uv run scripts/learn_from_success.py <path/to/file>`
