---
name: behaviors
description: "Use when features are approved and user wants to formalize behavioral rules, state machines, or entity lifecycle. Also when user says 'behavioral specs', 'state machine', 'formalize rules', 'allium specs'."
keywords: [behavior, specification, allium, rules, states, transitions, domain, lifecycle, state-machine]
user_inputs:
  dialog: []
  files: []
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
---

# Behaviors — Behavioral Specification

## Overview

The **behaviors** skill is the Behavioral Specification agent. It reads approved
features and produces formal `.allium` specifications that capture entity states,
transition rules, and boundary surfaces. It fills the gap between informal feature
requirements and the data model — the rules and state machines that feature
checklists leave implicit.

**Phase:** conceptualization / behavioral-specs
**Pipeline ID:** `cf_concept_functionality_behaviors` (see `cf__shared/pipeline.json`)
**Writes to:** `_concept/03b_behavior/`

This step is **optional**. The pipeline works without it. But when present,
downstream skills (`cf_concept_datamodel`, `cf_concept_ui_screens`) read these specs to produce more
precise schemas and screen definitions.

## When to Use

- Features are approved and the user wants to formalize behavioral rules
- The user says "behavioral specs", "state machine", "formalize rules", "allium specs"
- Complex domain logic needs to be captured before data modeling
- Entity lifecycles have multiple states and transition rules
- The orchestrator dispatches this after features are approved

## When NOT to Use

- Features have not been written yet — run **features** first
- The app is simple enough that feature checklists capture all behavior
- The user wants to skip straight to data modeling — this step is optional

## Prerequisites

### HARD-GATE

`_concept/03_features/` must have at least one feature file. If not:

> "No approved features found. Run the **features** skill first."

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid paths
- `cf__shared/frontmatter.md` — feature frontmatter fields
- `cf__shared/feedback_loop.md` — cross-reference protocol
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

Also read the Allium language subset reference bundled with this skill:
- `references/allium-subset.md` — the constructs you may use

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/03_features/` must have at least one feature file
**If gates fail:** Run `cf_concept_functionality_features` first.
**On completion:** Present summary, then orchestrator suggests next steps.

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/01_project/brief.md` | Yes |
| **Must read** | `_concept/03_features/` (all) | Yes |
| **Optional** | `_concept/_research/general/behavioral_patterns.md` | No |
| **Never load** | `_concept/06_datamodel/`, `_concept/07_screens/` | — |

## Workflow

### Step 1: Read Context

Read `_concept/03_features/**/*.md`. If no feature files exist, stop:

> "No features found. Run the **features** skill first."

Also read `_concept/01_project/brief.md` for domain context.

If `_concept/_research/general/behavioral_patterns.md` exists, read it for domain-specific
behavioral patterns that should inform the specs.

### Step 2: Identify Behavioral Patterns

For each feature group, extract:

| # | Question |
|---|----------|
| 1 | What entities are implied? What states can they be in? |
| 2 | What causes state transitions? (user actions, time, other entities) |
| 3 | What preconditions must hold before a transition? |
| 4 | What are the postconditions after a transition? |
| 5 | Who can see what? Who can do what? (surfaces) |
| 6 | Are there any configurable values (timeouts, limits, defaults)? |
| 7 | Are there ambiguities the features don't resolve? |

### Step 3: Write Allium Specs

```bash
mkdir -p _concept/03b_behavior
```

**Output: one `.allium` file per feature group.**

File naming mirrors feature groups: `_concept/03b_behavior/<group_name>.allium`

For feature group `01_user_auth/`, write `_concept/03b_behavior/user_auth.allium`.
For feature group `02_tasks/`, write `_concept/03b_behavior/tasks.allium`.

Drop the numeric prefix from the filename — it is already ordered by the feature
group it corresponds to.

Every file starts with the version marker and a comment linking to the feature group:

```
-- allium: 2
-- Behavioral spec for feature group: 01_user_auth
-- Source: _concept/03_features/01_user_auth/
```

#### Entity pattern

Extract entities from features. Map feature "Error States" and status descriptions
to entity state enums:

```
entity User {
    email: Email
    password_hash: String
    status: active | locked | suspended
    failed_login_count: Integer
    last_login_at: Timestamp
}
```

#### Rule pattern

Convert feature requirements and error states into formal rules:

```
rule UserLogin {
    when: LoginAttempt(user, credentials)
    requires: user.status = active
    requires: credentials.valid
    ensures: user.last_login_at = now
    ensures: user.failed_login_count = 0
}

rule LoginFailure {
    when: LoginAttempt(user, credentials)
    requires: user.status = active
    requires: not credentials.valid
    ensures: user.failed_login_count = user.failed_login_count + 1
}

rule AccountLockout {
    when: user: User.failed_login_count >= config.max_login_attempts
    requires: user.status = active
    ensures: user.status = locked
}
```

#### Surface pattern

Map feature roles to surfaces — what each actor can see and do:

```
surface LoginPage {
    facing visitor: User

    exposes:
        -- nothing (unauthenticated)

    provides:
        LoginAttempt(visitor, credentials)
            when visitor.status != locked
}
```

#### Config pattern

Extract magic numbers from feature requirements:

```
config {
    max_login_attempts: Integer = 5
    lockout_duration: Duration = 30.minutes
    session_timeout: Duration = 24.hours
}
```

#### Open questions

Flag ambiguities discovered while formalizing:

```
open question "Should locked accounts auto-unlock after lockout_duration, or require admin intervention?"
```

### Step 4: Present Summary

Show what was formalized:

```
| Group | File | Entities | Rules | Surfaces | Open Questions |
|-------|------|----------|-------|----------|----------------|
| 01_user_auth | user_auth.allium | 2 | 5 | 2 | 1 |
| 02_tasks | tasks.allium | 3 | 4 | 3 | 0 |
```

### Step 5: Emit Events

```
[cf_concept_functionality_behaviors] started
  run_id: <uuid>
  reads: 01_project/brief.md, 03_features/

[cf_concept_functionality_behaviors] checkpoint phase=specs_written
  files: user_auth.allium, tasks.allium
  entities: 5, rules: 9, surfaces: 5, open_questions: 1
```

## Outputs

| File | Description |
|------|-------------|
| `_concept/03b_behavior/<group>.allium` | Allium behavioral spec per feature group |

## Completion Summary

Present to user: files produced, key decisions made, suggested next steps (which skills are now unblocked).

```
[cf_concept_functionality_behaviors] completed
  run_id: <uuid>
  files: N
  entities: N
  rules: N
  surfaces: N
  open_questions: N
```

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Writing database schemas in allium | The agent conflates behavioral specs with data modeling | Allium describes observable behavior, not storage. No database types, no API paths. |
| Skipping open questions | The agent resolves ambiguities by guessing | Flag ambiguities explicitly. The user decides, not the agent. |
| Including implementation details | The agent adds UI elements or API paths to surfaces | Surfaces describe what actors can see and do, not how the UI looks. |
| Using unsupported Allium constructs | The agent uses contracts, sum types, or module system | Stick to the subset: entity, rule, surface, config, open question. |
| Not linking to feature groups | The agent omits the source comment in allium files | Every file must comment which feature group it formalizes. |

## Integration

- **Called by:** orchestrator or standalone (optional step after features, before `cf_concept_datamodel`)
- **Reads from:** `_concept/01_project/brief.md`, `_concept/03_features/`
- **Feeds into:** `cf_concept_datamodel` (entity states and relationships), `cf_concept_ui_screens` (surfaces for access and actions)
- **Feedback loops:** None inbound. Outbound: downstream skills read `.allium` files if present.

## Validation Rules

- Every `.allium` file starts with `-- allium: 2`
- Every `.allium` file has a comment linking to its source feature group
- Entity status fields use lowercase pipe-separated enum literals
- Rules have at least one `requires:` and one `ensures:` clause
- Surfaces have a `facing` clause and at least one `exposes` or `provides` block
- Config values have types and defaults
- No implementation details (no database types, no API paths, no UI elements)
