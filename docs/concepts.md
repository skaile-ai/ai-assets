---
title: Concepts — Overview
description: The core building blocks of ai-resources — skills, flows, agents, domains, and contracts.
---

`ai-resources` is organized around five interlocking concepts. Understanding how they relate to each other is the key to working effectively with the ecosystem.

## Building Blocks

**[Skills](./concepts/skills)** — The atomic unit. A `SKILL.md` file containing YAML frontmatter (metadata, inputs, outputs) plus a Markdown prompt body the agent follows when activated. Skills are discovered by directory name and executed by the runner as flow nodes.

**[Flows](./concepts/flows)** — The execution plan. A `flow.json` directed graph of skill nodes connected by typed edges. The flow engine computes which nodes are ready to run; the runner executes them in order or in parallel.

**[Agents](./concepts/agents)** — The identity layer. A GitAgent definition directory (`agent.yaml` + `SOUL.md` + `RULES.md` + `knowledge/`) that is assembled into a system prompt before any flow starts. Agents give the LLM its persona, constraints, and domain knowledge.

**Domains** — The organizational layer. Folders grouping related skills, agents, flows, and contracts by concern. See the [Domain Reference](./domains).

**Contracts** — The shared vocabulary. Specification files in `dev-shared/contracts/` that all domains reference. They define file structures, naming rules, cross-reference protocols, and mechanical constraints. See [dev-shared](./domains/dev-shared).
