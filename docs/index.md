---
title: ai-resources
description: The master library of AI agent skills, flows, and agents for the Skaile ecosystem.
---

`ai-resources` is the skill library that powers all Skaile agent workflows. It contains `SKILL.md` prompt manifests organized into 14 domain directories, `flow.json` state machine definitions, GitAgent definitions, and shared contracts.

## Two Ways to Use

### Via skaile CLI (recommended for running flows)

```bash
skaile flow list                                    # see what's available
skaile run cli-concept --project-dir ./my-project  # run a flow
skaile skill list dev-conceptualization             # browse skills
```

### Via arm (recommended for installing/deploying skills)

```bash
arm resource add <path-to-ai-resources> --name ai-resources
arm explore ai-resources
arm install cf_concept_overview
arm deploy cf_concept_overview
```

## In This Section

- [Concepts](./concepts) — skills, domains, agents, flows, and contracts explained
- [Skill Anatomy](./skill-anatomy) — SKILL.md frontmatter reference, body sections, optional files
- [Flows](./flows) — available flows table, how to run them, reading flow JSON, writing a new flow
- [Domains](./domains) — per-domain reference with key skills and when to use them
- [Developer Guide](./developer-guide/) — repository structure, skill resolution, contract inheritance, adding a domain
