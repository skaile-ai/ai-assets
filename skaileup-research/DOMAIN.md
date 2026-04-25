---
name: skaileup-research
description: "Agentic web research and knowledge grounding — produces structured research artifacts in _grounding/research/ for competitors, audiences, design patterns, and technology evaluation."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  skills: "Web research, deep research synthesis, and targeted research skills."
  tools: "n/a"
stage: alpha
---

# skaileup-research

Agentic web research and knowledge grounding — produces structured research artifacts in `_grounding/research/` for competitors, audiences, design patterns, and technology evaluation. Research skills run in parallel with or prior to discovery, providing grounded external evidence that supplements the project owner's own inputs.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-research/` | General-purpose agentic web research producing structured grounding artifacts |
| `skailup-research-deep/` | Deep multi-source synthesis for complex research questions (future) |
| `skailup-research-targeted/` | Targeted research against a specific topic or competitor (future) |

## Conventions

- All output is written to `_grounding/research/` and never directly into `_concept/` artifacts.
- Research skills are designed to run in parallel; they do not depend on each other.
- Downstream discovery and blueprint skills read from `_grounding/research/` to ground their outputs in external evidence.
