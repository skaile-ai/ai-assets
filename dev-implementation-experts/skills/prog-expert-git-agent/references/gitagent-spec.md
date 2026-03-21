# GitAgent Specification v0.1.0

Source: https://github.com/open-gitagent/gitagent

## Overview

GitAgent is an open standard for defining AI agents natively in git repositories.
Framework-agnostic — works with Claude Code, OpenAI, CrewAI, Lyzr, Google ADK,
LangChain, and more.

The core philosophy: separate what the agent IS from how it RUNS. Definition files
(agent.yaml, SOUL.md, SKILL.md) are stable across frameworks. The export layer
handles conversion to framework-specific formats.

## agent.yaml — Complete Schema

### Required Fields

| Field | Type | Constraints |
|-------|------|-------------|
| `name` | string | kebab-case, matches `^[a-z][a-z0-9-]*$` |
| `version` | string | semantic version `X.Y.Z[-prerelease][+build]` |
| `description` | string | one-line summary |

### Recommended

| Field | Type | Default |
|-------|------|---------|
| `spec_version` | string | `"0.1.0"` |

### Optional — Model Configuration

```yaml
model:
  preferred: claude-opus-4-6          # primary model ID
  fallback:                            # ordered fallback chain
    - claude-sonnet-4-6
    - claude-haiku-4-5-20251001
  constraints:
    temperature: 0.2                   # 0.0–2.0
    max_tokens: 4096
    top_p: 0.9
    top_k: 50
    stop_sequences: []
    presence_penalty: 0.0
    frequency_penalty: 0.0
```

### Optional — Composition

| Field | Type | Purpose |
|-------|------|---------|
| `extends` | string | Parent agent git URL or local path (inheritance) |
| `dependencies` | array | Composed agents with vendor metadata |
| `skills` | array of strings | Enabled skill directory names |
| `tools` | array of strings | Enabled tool names (without extension) |
| `agents` | object | Sub-agent configuration |

### Optional — Execution

```yaml
runtime:
  max_turns: 50                        # max conversation turns
  temperature: 0.1                     # override model temperature
  timeout: 300                         # seconds

delegation:
  mode: auto                           # auto | explicit | router
  router: orchestrator-skill           # when mode=router
```

### Optional — Metadata

```yaml
author: string
license: MIT                           # SPDX identifier
tags: [tag1, tag2]
metadata:                              # arbitrary key-value pairs
  key: value                           # string/number/boolean values only
a2a: {}                                # agent-to-agent protocol metadata
```

### Optional — Compliance

Full compliance configuration for regulated environments:

```yaml
compliance:
  risk_tier: standard                  # low | standard | high | critical
  frameworks: [finra, federal_reserve, sec, cfpb]

  supervision:
    designated_supervisor: null
    review_cadence: quarterly
    human_in_the_loop: conditional     # always | conditional | advisory | none
    escalation_triggers:
      - confidence_below: 0.7
      - action_type: customer_communication
      - error_detected: true
    override_capability: true
    kill_switch: true

  recordkeeping:
    audit_logging: true
    log_format: structured_json
    retention_period: 6y
    log_contents:
      - prompts_and_responses
      - tool_calls
      - decision_pathways
      - model_version
      - timestamps
    immutable: true

  model_risk:
    inventory_id: null
    validation_cadence: annual         # quarterly | annual
    validation_type: full              # full | targeted | change_based
    ongoing_monitoring: true
    outcomes_analysis: true
    drift_detection: true
    parallel_testing: false

  data_governance:
    pii_handling: redact               # redact | encrypt | prohibit | allow
    data_classification: confidential
    consent_required: true
    cross_border: false
    bias_testing: true

  segregation_of_duties:
    roles:
      - id: maker
        description: Creates proposals
        permissions: [create, submit]
      - id: checker
        description: Reviews outputs
        permissions: [review, approve, reject]
    conflicts: [[maker, checker]]
    assignments:
      agent-name: [role-list]
    isolation:
      state: full                      # full | shared | none
      credentials: separate
    handoffs:
      - action: critical_decision
        required_roles: [maker, checker]
        approval_required: true
    enforcement: strict                # strict | advisory
```

## SOUL.md

Defines agent identity. Minimal valid SOUL.md is a single paragraph.

### Recommended Sections

- **Core Identity**: Agent's primary purpose
- **Communication Style**: Tone, formality, verbosity
- **Values & Principles**: Decision-making priorities
- **Domain Expertise**: Specialized knowledge areas
- **Collaboration Style**: Interaction approach

## RULES.md

Hard behavioral constraints.

### Recommended Sections

- **Must Always**: Required behaviors
- **Must Never**: Prohibited behaviors
- **Output Constraints**: Format/content restrictions
- **Interaction Boundaries**: Scope limits
- **Safety & Ethics**: Ethical guardrails
- **Regulatory Constraints**: Compliance-specific rules

## DUTIES.md

Segregation of duties policy for multi-agent compliance.

### Root-Level Sections
- Role definitions
- Conflict matrix
- Agent-to-role assignments
- Isolation policy
- Handoff procedures

### Per-Agent Sections
- Assigned roles
- Permitted actions
- Prohibited combinations
- Escalation procedures

## Directory Structure

```
my-agent/
├── agent.yaml                 # [REQUIRED] manifest
├── SOUL.md                    # [REQUIRED] identity
├── RULES.md                   # constraints
├── DUTIES.md                  # segregation of duties
├── AGENTS.md                  # framework-agnostic fallback
├── README.md                  # human docs
├── skills/<name>/SKILL.md     # capabilities
├── tools/<name>.yaml + .py    # MCP-compatible tools
├── knowledge/index.yaml + *   # reference documents
├── memory/MEMORY.md           # state (200 line max)
├── workflows/*.yaml           # multi-step procedures
├── hooks/hooks.yaml           # lifecycle handlers
├── examples/*.md              # few-shot calibration
├── agents/<name>/agent.yaml   # full sub-agents (recursive)
├── agents/<name>.md           # lightweight sub-agents
├── compliance/                # regulatory artifacts
├── config/*.yaml              # env-specific overrides
└── .gitagent/                 # runtime state (gitignored)
```

## Sub-Agents

### Full Sub-Agent (Directory)
Self-contained: own `agent.yaml`, `SOUL.md`, `RULES.md`, skills.

### Lightweight Sub-Agent (File)
Single `.md` file with frontmatter defining agent properties.

## Inheritance & Dependencies

- `extends: ../parent/agent.yaml` — inherit SOUL, RULES, skill configs
- `dependencies:` — composed agents with version pinning and vendor metadata
- Parent-level `knowledge/`, `skills/`, `tools/` cascade to children

## CLI

```bash
gitagent init --template standard   # scaffold
gitagent validate                   # check compliance
gitagent run                        # execute
gitagent export                     # export to frameworks
```

Install: `npm install -g gitagent`

## Naming Conventions

- YAML keys: `snake_case`
- Agent/skill/tool names: `kebab-case`
