---
name: "skailup-standards-sync"
description: "Use when pushing proven project standards back to profiles, or syncing profile standards into a project. Triggered by 'sync standards', 'update profile conventions', or 'push standards to profile'."
metadata:
  tags:
    - "standards"
    - "sync"
    - "profile"
    - "conventions"
    - "push"
    - "pull"
  stage: "alpha"
  requires:
    - "standards-contract"
  user_inputs:
    dialog:
      - id: "direction"
        label: "Sync Direction"
        type: "select"
        options:
          - "project_to_profile"
          - "profile_to_project"
        required: true
        hint: "Push project standards to profile, or pull profile standards into project"
      - id: "profile_name"
        label: "Profile Name"
        type: "text"
        required: false
        default: "default"
        hint: "Which profile to sync with"
    files: []
---

# Sync Standards

## Overview
Bidirectional sync between project-level standards (`_concept/_standards/`) and
profile-level standards. Pushes proven patterns from a project back to reusable
profiles, or pulls profile standards into a new project.

## When to Use
- After implementation, to capture proven project conventions in a profile
- When starting a new project, to apply standards from a previous project's profile
- User says "sync standards", "push conventions to profile", "apply profile standards"

## When NOT to Use
- No standards have been discovered yet (run cf_discover_standards first)
- No profiles exist (create one in profiles.json first)

## Prerequisites

<HARD-GATE>
No hard gates — this is an optional quality-phase step.
</HARD-GATE>

## Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget
**Must read:** `_concept/_standards/index.yml`, `cf__shared/profiles.json`
**Optional:** Profile-specific standards files
**Never load:** Source code, other _concept/ artifacts

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** None
**If gates fail:** N/A
**On completion:** Present sync summary, then orchestrator suggests next steps.

## Workflow

### project_to_profile direction:
1. Read `_concept/_standards/index.yml`
2. Read target profile from `cf__shared/profiles.json`
3. Compare project standards with profile standards
4. On conflict: show diff, ask user to choose:
   - Keep project version
   - Keep profile version
   - Merge (manual)
5. Write updated profile standards

### profile_to_project direction:
1. Read target profile standards
2. Read `_concept/_standards/` (if exists)
3. Compare and merge, preferring project-specific overrides
4. Write merged standards to `_concept/_standards/`
5. Update index.yml

## Outputs
- Updated profile standards (project_to_profile direction)
- Updated `_concept/_standards/` (profile_to_project direction)

## Completion Summary
Present to user:
- Standards synced (count)
- Conflicts resolved
- Direction of sync

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "I'll overwrite profile standards silently" | Always show diff on conflict. User chooses. |
| "All project standards should go to profile" | Only push standards that proved useful in implementation. |

## Integration
- **Called by:** orchestrator (quality phase, optional) or standalone
- **Pairs with:** cf_discover_standards (source), profiles.json (target)
- **Feedback loops:** None
