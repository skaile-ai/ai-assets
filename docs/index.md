---
title: ai-assets
description: The master library of AI agent skills, flows, and agents for the Skaile ecosystem.
_sources:
  - path: ai-assets/README.md
_based_on_commit: 5fd26c1
_last_synced: "2026-04-25"
sidebar:
  order: 1
---

`ai-assets` is the skill library that powers all Skaile agent workflows. It contains `SKILL.md` prompt manifests organized into focused domain directories, `flow.json` state machine definitions, GitAgent definitions, and shared contracts.

## Two Ways to Use

### Via [skaile](/use/cli/) CLI (recommended for running flows)

```bash
skaile flow list                                    # see what's available
skaile run cli-concept --project-dir ./my-project  # run a flow
skaile skill list skaileup-conceptualization             # browse skills
```

### Via skaile asset management (recommended for installing/deploying skills)

```bash
skaile repo add <path-to-ai-assets> ai-assets
skaile explore ai-assets
skaile add cf_concept_overview
```

## Building Blocks

**[Skills](./skills)** — The atomic unit. A `SKILL.md` file containing YAML frontmatter (metadata, inputs, outputs) plus a Markdown prompt body the agent follows when activated. Skills are discovered by directory name and executed by the runner as flow nodes.

**[Flows](./flows)** — The execution plan. A `flow.json` directed graph of skill nodes connected by typed edges. The flow engine computes which nodes are ready to run; the runner executes them in order or in parallel.

**[Agents](./agents)** — The identity layer. A GitAgent definition directory (`agent.yaml` + `SOUL.md` + `RULES.md` + `knowledge/`) that is assembled into a system prompt before any flow starts. Agents give the LLM its persona, constraints, and domain knowledge.

**[Prompts](./prompts)** — Reusable text fragments. Lighter-weight than skills — no procedure, no I/O declarations. Injected into skill prompts or agent imprints.

**[Domains](./domains)** — The organizational layer. Folders grouping related skills, agents, flows, and contracts by concern. Browse per-domain details in the [AI Resource Catalog](/catalog/).

**Contracts** — The shared vocabulary. Specification files in `skaileup-shared/contracts/` that all domains reference. They define file structures, naming rules, cross-reference protocols, and mechanical constraints.

## In This Section

- [Skills](./skills) — SKILL.md format, frontmatter reference, body sections, resolution, optional files
- [Flows](./flows) — flow.json structure, nodes, edges, globals, execution model, available flows
- [Agents](./agents) — GitAgent format, agent.yaml fields, imprint assembly
- [Prompts](./prompts) — prompt format, prompts vs skills, usage
- [Domains](./domains) — domain structure, types, relationships
- [Developer Guide](./developer-guide/) — repository structure, skill resolution, contract inheritance, adding a domain
