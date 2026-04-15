---
name: "architecture-contract"
description: "Shared contract for skaileup-architecture skills. Describes architecture artifact locations, AI agent integration patterns, expert skill naming convention, and system design file structure."
metadata:
  stage: "alpha"
  do_not_invoke: true
---

# Architecture Domain — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `skaileup-architecture` skills read this before operating.

## Scope

Architecture skills produce system design artifacts and expert-level integration guidance. They may serve as `prog-expert-*` dependencies for implementation skills.

## Architecture Artifacts

```
_concept/3_blueprint/2_architecture/
└── architecture.md       ← system design: components, data flow, protocols, boundaries

_concept/3_blueprint/1_techstack/
└── stack.md              ← tech stack choices (drives expert skill selection)
```

`architecture.md` must document:
- System components and boundaries
- Data flow between layers
- External integrations and protocols
- Custom processes or sidecars (e.g., LLM agent sidecars)
- Deployment topology (if relevant)

## Expert Skill Naming Convention

Architecture domain skills that serve as implementation experts follow this naming:

```
prog-expert-<technology>/SKILL.md
```

Examples:
- `skailup-prog-expert-integration-ai-agents/`
- `skailup-prog-expert-skill-system/`

Expert skills are **discovered at runtime** by implementation skills:
1. Read `stack.md` for declared technologies
2. Search skill paths for matching `prog-expert-<tech>` directories
3. Load the expert's SKILL.md into the implementation subagent context

## Expert SKILL.md Conventions

Expert skills must declare:

```yaml
---
name: prog-expert-<tech>
source: MIGRATED | MERGED
description: "Expert-level implementation guidance for <tech>."
metadata:
  type: expert
  technology: <tech>
  discovery_keywords: [<tech>, <alias>, ...]
---
```

The `discovery_keywords` field is used by implementation skills to match stack declarations to the right expert.
