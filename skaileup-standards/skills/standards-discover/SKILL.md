---
name: "standards-discover"
description: "Use when analyzing an existing codebase to extract conventions, patterns, and standards. Triggered when user says 'discover standards', 'analyze codebase conventions', or when starting implementation against an existing project."
metadata:
  tags:
    - "standards"
    - "conventions"
    - "patterns"
    - "codebase"
    - "analyze"
    - "discover"
  stage: "alpha"
  requires:
    - "standards-contract"
  user_inputs:
    dialog:
      - id: "target_path"
        label: "Target Codebase Path"
        type: "text"
        required: true
        hint: "Path to the existing codebase to analyze"
      - id: "domains"
        label: "Domains to Analyze"
        type: "multiselect"
        options:
          - "api"
          - "database"
          - "ui"
          - "naming"
          - "testing"
          - "architecture"
        default:
          - "api"
          - "database"
          - "ui"
          - "naming"
          - "testing"
          - "architecture"
        required: false
        hint: "Which convention domains to discover"
    files: []
---

# Discover Standards

## Overview
Analyzes a target codebase to extract conventions, patterns, and standards.
Writes discovered standards to `_concept/_standards/` where all skills can
reference them during concept and implementation work.

## When to Use
- User says: "discover standards", "analyze conventions", "what patterns does this codebase use?"
- Starting implementation against an existing project
- Wanting to ensure new features follow existing codebase patterns

## When NOT to Use
- No existing codebase to analyze
- User wants to define standards from scratch (just write them manually)

## Prerequisites

**REQUIRED BACKGROUND:** Read `cf__shared/concept_structure.md` (for _standards/ structure).

<HARD-GATE>
No hard gates — this is a mode-based skill (like research). It can run alongside
any pipeline step.
</HARD-GATE>

## Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget
**Must read:** Target codebase (as specified by user)
**Optional:** `_concept/05_techstack/stack.md` _(fallback: skip_if_absent)_ — helps focus analysis
**Never load:** Other _concept/ artifacts (standards discovery is independent)

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** None (mode-based, no hard gates)
**If gates fail:** N/A
**On completion:** Present discovered standards summary, then orchestrator suggests next steps.

## Workflow

1. **Self-collect inputs** — ask for target codebase path and domains to analyze
2. **Scan codebase** — analyze source files for patterns:
   - **api/**: Route naming, request/response patterns, error handling, middleware
   - **database/**: Schema patterns, migration conventions, query patterns
   - **ui/**: Component structure, state management, styling approach
   - **naming/**: Variable naming, file naming, folder structure conventions
   - **testing/**: Test organization, assertion patterns, fixture conventions
   - **architecture/**: Module boundaries, dependency patterns, service structure
3. **Write standards** — one `.md` file per discovered convention in `_concept/_standards/{domain}/`
4. **Generate index** — create `_concept/_standards/index.yml`

## Standard File Format

Each standard file is concise markdown:

```markdown
---
domain: api
keywords: [routing, rest, endpoints]
applies_to: [implement-feature, architecture]
last_updated: YYYY-MM-DD
---

# Route Naming Convention

- Routes use kebab-case: `/api/user-profiles`
- Nested resources: `/api/projects/:id/tasks`
- Actions use verbs: `/api/auth/login`

## Examples
```typescript
// Good
router.get('/api/user-profiles/:id')
// Bad
router.get('/api/getUserProfile/:id')
```
```

## Index File Format

```yaml
# _concept/_standards/index.yml
standards:
  - path: api/route_naming.md
    domain: api
    keywords: [routing, rest, endpoints]
    applies_to: [implement-feature, architecture]
  - path: database/migration_pattern.md
    domain: database
    keywords: [migration, schema, sql]
    applies_to: [migrate, datamodel]
```

## Outputs
- `_concept/_standards/{domain}/{convention}.md` — one file per discovered convention
- `_concept/_standards/index.yml` — index for fast matching

## Completion Summary
Present to user:
- Number of standards discovered per domain
- Key conventions found
- Suggested next steps

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "I'll document every tiny pattern" | Focus on conventions that affect downstream skills. One concept per file. |
| "I'll infer standards from a few files" | Scan broadly. Patterns need multiple instances to be conventions. |

## Integration
- **Called by:** orchestrator (parallel mode) or standalone
- **Pairs with:** cf_standards_inject (consumer), any implementation skill
- **Feedback loops:** None (writes to _standards/ only)

## Research Mode
Not applicable — this skill IS a discovery mode.
