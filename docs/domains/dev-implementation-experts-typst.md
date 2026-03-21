---
title: dev-implementation-experts-typst
description: Typst document composition expertise and the cross-domain expert advisor router.
---

Covers Typst document composition expertise and the cross-domain expert advisor. The Typst skill provides deep knowledge of Typst's markup, scripting, and layout system. The advisor skill acts as a general-purpose router — it reads project context and delegates to the most relevant expert skill across all expert domains.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Expert skill conventions and advisor routing protocol (shared with other expert domains) |
| `docs/` | Typst version notes, template patterns, data pipeline integration |
| `skills/` | `prog-expert-typst`, `prog-expert-advisor` |

## Skills

| Skill | When to use |
|---|---|
| `prog-expert-typst` | Typst document authoring — markup syntax, scripting functions, layout system, template design, PDF compilation |
| `prog-expert-advisor` | General entry point when you need expert help but aren't sure which specialist to call — reads the tech stack and routes to the right expert |

## The Advisor Pattern

`prog-expert-advisor` is the single entry point for expert routing across all three expert domains (js, python, typst). Implementation skills that need stack-specific guidance call the advisor rather than hard-coding which expert to use. The advisor:

1. Reads `_concept/05_techstack/stack.md` (or equivalent)
2. Determines which technologies are in use
3. Delegates to the relevant `prog-expert-*` skills
