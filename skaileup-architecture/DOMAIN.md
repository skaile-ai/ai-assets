---
name: skaileup-architecture
description: "System architecture and design skills for AI-integrated applications, including agent integration patterns and skill system design."
type: domain
building_blocks:
  contracts: "Architecture review criteria, system design constraints, and integration patterns shared by architecture skills."
  skills: "Invocable skills for AI agent integration architecture and skill system design."
  agents: "agents/architecture/ (GitAgent spec v0.1.0)"
stage: alpha
---

# Dev Architecture

This domain contains skills for system architecture and design, with a focus on AI-integrated applications. It covers how to structure systems that incorporate LLM agents, how to design skill systems, and how to reason about integration boundaries.

Architecture skills in this domain produce design documents, ADRs, and integration blueprints — not code. They feed into `skaileup-implementation` which translates designs into running systems.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared architecture constraints, review criteria, and integration pattern vocabulary |
| `skills/` | Invocable skills — AI agent integration design, skill system design |
| `agents/` | Agent specs for orchestrated architecture analysis runs |

## Skills

| Skill | Purpose |
|-------|---------|
| `skills/skailup-prog-expert-integration-ai-agents/` | Design architecture for systems that integrate LLM agents |
| `skills/skailup-prog-expert-skill-system/` | Design and audit skill system structure and conventions |

## Conventions

- Architecture skills produce documents, not code
- Designs reference semantic types and stack-independent patterns where possible
- Output feeds forward to `skaileup-implementation`
