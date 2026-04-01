---
name: skaileup-architecture
description: "System architecture and design skills for AI-integrated applications, including agent integration patterns and skill system design."
type: domain
building_blocks:
  contracts: "Architecture review criteria, system design constraints, and integration patterns shared by architecture skills."
  docs: "Architecture decision records (ADRs), reference architectures, and integration pattern documentation."
  skills: "Invocable skills for AI agent integration architecture and skill system design."
  agents: "agents/architecture/ (GitAgent spec v0.1.0)"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Dev Architecture

This domain contains skills for system architecture and design, with a focus on AI-integrated applications. It covers how to structure systems that incorporate LLM agents, how to design skill systems, and how to reason about integration boundaries.

Architecture skills in this domain produce design documents, ADRs, and integration blueprints — not code. They feed into `skaileup-implementation` which translates designs into running systems.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared architecture constraints, review criteria, and integration pattern vocabulary |
| `docs/` | Reference architectures, ADRs, and canonical design patterns used across projects |
| `skills/` | Invocable skills — architecture analysis, AI agent integration design, skill system design |

## Skills

| Skill | Purpose |
|-------|---------|
| `dev-architect-ai-agent-integration` | Design architecture for systems that integrate LLM agents |
| `skaileup-implementation-expert-skill-system` | Design and audit skill system structure and conventions |

## Conventions

- Architecture skills produce documents, not code
- Designs reference semantic types and stack-independent patterns where possible
- Output feeds forward to `skaileup-implementation` and `skaileup-conceptualization/30_blueprint/`
