---
title: Domains
description: What a domain is, its folder structure, DOMAIN.md, and how domains relate to each other.
---

A **domain** is a top-level folder in `ai-resources/` that groups related skills, agents, flows, prompts, and contracts by concern. Domains are the organizational unit — `arm` discovers assets by scanning within domain directories, and the runner resolves skills by walking across them.

## Folder Structure

Every domain follows the same layout (not all folders are present in every domain):

```
<domain-name>/
├── DOMAIN.md       ← Domain manifest — name, description, building blocks
├── package.yaml    ← Dependency declarations for arm
├── skills/         ← Invocable skills (each in its own subdirectory)
├── agents/         ← GitAgent definitions
├── flows/          ← Flow JSON definitions
├── prompts/        ← Reusable prompt fragments
├── contracts/      ← Domain-specific shared specs
└── docs/           ← Domain documentation
```

## DOMAIN.md

Every domain has a `DOMAIN.md` that describes its purpose and building blocks. This is both human documentation and a machine-readable manifest consumed by `arm` and the `domain-builder` meta-skill.

```yaml
---
name: dev-conceptualization
description: "Structured project concept pipeline..."
type: domain
building_blocks:
  contracts: "..."
  skills: "..."
  agents: "..."
stage: alpha
---

# Dev Conceptualization

...
```

| Field | Description |
|---|---|
| `name` | Domain identifier — matches the directory name |
| `description` | One-line description shown in `arm` catalog |
| `type` | Always `domain` |
| `building_blocks` | What each subfolder contains |
| `stage` | Development maturity: `alpha`, `beta`, or `stable` |

## Domain Types

| Type | Pattern | Examples |
|---|---|---|
| **Pipeline domains** | Numbered skill groups following a workflow order | `dev-conceptualization`, `dev-implementation`, `dev-quality` |
| **Expert domains** | One skill per technology, no pipeline order | `dev-implementation-experts-js`, `-python`, `-typst` |
| **Knowledge domains** | Research and content production | `knowledge-research`, `knowledge-writing` |
| **Integration domains** | One skill per external service | `use` |
| **Meta domains** | Manage the ecosystem itself | `ai-resource-management` |
| **Shared domains** | Reference material only, never invoked | `dev-shared` |

## How Domains Relate

Domains are not isolated — they read each other's output and delegate to each other:

```
dev-conceptualization  →  writes _concept/
                               ↓
dev-implementation     →  reads _concept/, writes code
                               ↓
dev-quality            →  reads code + _concept/, writes test reports
```

Expert domains (`dev-implementation-experts-*`) are called by `dev-implementation` skills via the advisor routing pattern — never invoked directly by a flow.

`dev-shared` sits underneath all domains: its contracts define the vocabulary every other domain must follow.

## Browsing Domains

```bash
# List all domains and their skills
skaile skill list

# Skills in a specific domain
skaile skill list dev-conceptualization

# Interactive explorer
skaile explore ai-resources
```

Browse per-domain skill tables in the [AI Resource Catalog](/catalog/).
