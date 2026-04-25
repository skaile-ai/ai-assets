---
name: skailup
description: Top-level conversational guide for the skaileup concept → implementation pipeline
---

## Purpose

Entry point for users starting or resuming a skaileup pipeline session. Discovers installed
orchestrators and flows at runtime; guides users through each step with or without the flow
engine present. Does not contain pipeline logic — delegates to domain-specific orchestrators.

## Agents

| Agent | Path | What it does | When to use |
|---|---|---|---|
| skailup | agents/skailup/ | Conversational guide and router | Always — start here |

## Notes

- Depends on: `skaileup-conceptualization`, `skaileup-implementation` (for peer agents)
- No contracts of its own; reads `skaileup-shared` contracts indirectly via orchestrators
- The `skaile` router agent (`ai-asset-management/agents/skaile/`) is complementary, not replaced
- New implementation orchestrators (e.g. `skailup-implementation-supabase`) are auto-discovered
  by naming convention — no changes to this domain required
