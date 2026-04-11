---
title: "<Title — 3-8 words, plain language>"
date: YYYY-MM-DD
status: draft | review | approved | implementing | complete | abandoned
author: "<who created this spec>"
reviewer: "<who reviewed and approved — empty until reviewed>"
scope: "<affected packages, comma-separated>"
type: proposal | design | rfc | adr
---

# <Title>

## Problem

<What is broken or missing. 2-5 sentences. No solution details — just the pain.>

## Context

<What exists today that is relevant. Link to CLAUDE.md sections, existing code, prior decisions.
If this replaces or extends a previous spec, link it. Keep to facts, not opinions.>

## Design

### Package Overview

<If creating new packages or modifying existing ones, show the dependency table:>

| Package | Purpose | Depends on |
|---------|---------|------------|
| `package-a` | ... | `types` |

### <Section per major component>

<For each component: types/interfaces, data flow, key design decisions.
Use code blocks for type definitions. Use ASCII diagrams for data flow.>

### Integration

<How components connect to each other and to existing code. What the platform/CLI/Forge
consumer API looks like.>

## Alternatives Considered

| Approach | Pros | Cons | Why rejected |
|----------|------|------|-------------|
| ... | ... | ... | ... |

## Non-Goals

<What this spec explicitly does NOT cover. Prevents scope creep during review.>

## Open Questions

<Numbered list of unresolved decisions. Each should have a recommendation and a
"resolve before" note (e.g., "before implementation" or "during implementation").>

## Implementation Notes

<Optional. Hints for whoever implements this: ordering, risks, what to prototype first.>
