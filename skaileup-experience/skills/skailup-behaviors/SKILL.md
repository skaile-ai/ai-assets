---
name: "skailup-behaviors"
description: "Use when features are approved and user wants to formalize behavioral rules, state machines, or entity lifecycle. Also when user says 'behavioral specs', 'state machine', 'formalize rules', 'allium specs'."
metadata:
  version: "1.0.0"
  tags:
    - "behavior"
    - "specification"
    - "allium"
    - "rules"
    - "states"
    - "transitions"
    - "domain"
    - "lifecycle"
    - "state-machine"
  source: "MIGRATED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires:
      - id: features
        gate: hard
    produces:
      - id: behaviors
        description: "Behavioral specs (.allium format) per feature group"
    consumes:
      - id: brief
        gate: soft
  prerequisites:
    files:
      - path: "_concept/experience/features"
        gate: hard
        description: "Features must exist — behavioral specs formalize feature rules into state machines"
        min_entries: 1
    reads:
      - path: "_concept/_grounding/general/behavioral_patterns.md"
        description: "Domain behavioral patterns from research phase"
    produces:
      - path: "_concept/experience/behaviors"
        description: "Allium behavioral specification files (one per feature group)"
---

# Behaviors — Behavioral Specification

## Overview

The **behaviors** skill is the Behavioral Specification agent. It reads approved
features and produces formal `.allium` specifications that capture entity states,
transition rules, and boundary surfaces. It fills the gap between informal feature
requirements and the data model — the rules and state machines that feature
checklists leave implicit.

**Writes to:** `_concept/experience/behaviors/`

This step is **optional**. The pipeline works without it. But when present,
downstream skills (`datamodel`, `screens`) read these specs to produce more
precise schemas and screen definitions.

## When to Use

- Features are approved and the user wants to formalize behavioral rules
- The user says "behavioral specs", "state machine", "formalize rules", "allium specs"
- Complex domain logic needs to be captured before data modeling
- Entity lifecycles have multiple states and transition rules
- The orchestrator dispatches this after features are approved

## When NOT to Use

- Features have not been written yet — run `features` first
- The app is simple enough that feature checklists capture all behavior
- The user wants to skip straight to data modeling — this step is optional

## Prerequisites

**Hard gate:** `_concept/experience/features/` must have at least one feature file.

If not: "No approved features found. Run the `features` skill first."

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — valid paths
- `skaileup-shared/contracts/frontmatter.md` — feature frontmatter fields
- `skaileup-shared/contracts/feedback_loop.md` — cross-reference protocol
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints
- `skaileup-shared/contracts/agent_patterns.md` — communication style, standalone mode

Also read the Allium language subset reference bundled with this skill:
- `references/allium-subset.md` — the constructs you may use

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/discovery/brief.md` | Required |
| `_concept/experience/features/**/*.md` | Required |
| `_grounding/general/behavioral_patterns.md` | Optional |

**Never load:** `_concept/blueprint/datamodel/`, `_concept/experience/screens/`

## Workflow

### Step 1: Read Context

Read `_concept/experience/features/**/*.md`. If no feature files exist, stop:

> "No features found. Run the `features` skill first."

Also read `_concept/discovery/brief.md` for domain context.

If `_grounding/general/behavioral_patterns.md` exists, read it for domain-specific
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

**Output: one `.allium` file per feature group.**

File naming mirrors feature groups: `_concept/experience/behaviors/<group_name>.allium`

For feature group `01_user_auth/`, write `_concept/experience/behaviors/user_auth.allium`.
For feature group `02_tasks/`, write `_concept/experience/behaviors/tasks.allium`.

Drop the numeric prefix from the filename — it is already ordered by the feature
group it corresponds to.

Every file starts with the version marker and a comment linking to the feature group:

```
-- allium: 2
-- Behavioral spec for feature group: 01_user_auth
-- Source: _concept/experience/features/01_user_auth/
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

## Outputs

| File | Description |
|------|-------------|
| `_concept/experience/behaviors/<group>.allium` | Allium behavioral spec per feature group |

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Core items only — list names and one-line descriptions, skip edge cases |
| `medium` | Standard coverage — full specs for core items, brief for secondary (default) |
| `max` | Exhaustive coverage — every feature/screen/component with full detail, edge cases, error states |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Writing database schemas in allium | Allium describes observable behavior, not storage. No database types, no API paths. |
| Skipping open questions | Flag ambiguities explicitly. The user decides, not the agent. |
| Including implementation details | Surfaces describe what actors can see and do, not how the UI looks. |
| Using unsupported Allium constructs | Stick to the subset: entity, rule, surface, config, open question. |
| Not linking to feature groups | Every file must comment which feature group it formalizes. |

## Validation Rules

- Every `.allium` file starts with `-- allium: 2`
- Every `.allium` file has a comment linking to its source feature group
- Entity status fields use lowercase pipe-separated enum literals
- Rules have at least one `requires:` and one `ensures:` clause
- Surfaces have a `facing` clause and at least one `exposes` or `provides` block
- Config values have types and defaults
- No implementation details (no database types, no API paths, no UI elements)

EMIT  [behaviors] started run_id=<uuid>
EMIT  [behaviors] checkpoint phase=specs_written files=<N> entities=<N> rules=<N> surfaces=<N> open_questions=<N>
EMIT  [behaviors] completed run_id=<uuid> files=<N> entities=<N> rules=<N> surfaces=<N> open_questions=<N>
