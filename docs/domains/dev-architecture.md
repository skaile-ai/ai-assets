---
title: dev-architecture
description: System architecture and design skills for AI-integrated applications, including agent integration patterns and skill system design.
---

Skills for system architecture and design, with a focus on AI-integrated applications. Covers how to structure systems that incorporate LLM agents, how to design skill systems, and how to reason about integration boundaries.

Architecture skills produce design documents, ADRs, and integration blueprints — not code. They feed into `dev-implementation` which translates designs into running systems.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Architecture review criteria, system design constraints, and integration pattern vocabulary |
| `docs/` | Reference architectures, ADRs, and canonical design patterns used across projects |
| `skills/` | `prog-expert-integration-ai-agents`, `prog-expert-skill-system` |
| `agents/architecture/` | Architecture GitAgent (GitAgent spec v0.1.0) |

## Skills

| Skill | When to use |
|---|---|
| `prog-expert-integration-ai-agents` | Design a system that integrates LLM agents — routing, tool use, multi-agent patterns |
| `prog-expert-skill-system` | Design or review a skill system — skill structure, discovery, loading, progressive disclosure |
