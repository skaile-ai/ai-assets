---
name: skaileup-project-concept
description: "Meta-concept skills for multi-product ecosystems. Produces an umbrella _concept/ that maps subsystems, integration architecture, and cross-product journeys — complementing per-subsystem single-app concepts."
type: domain
building_blocks:
  contracts: "Meta-concept contract defining the _concept/ structure for multi-repo/multi-product projects."
  docs: "None yet."
  skills: "Overview, subsystem map, integration architecture, and review skills for umbrella concepts."
  agents: "None."
  prompts: "None."
  tools: "None."
stage: alpha
---

# Project Concept — Multi-Product Umbrella

This domain provides skills for creating and maintaining **meta-concepts** — umbrella `_concept/`
directories for projects composed of multiple independent subsystems (repos, apps, libraries).

## When to Use

Use this domain when the project is not a single application but an ecosystem:

- Multiple independent codebases or git repos
- Multiple tech stacks under one brand
- Multiple deployable products serving different audiences
- Per-subsystem `_concept/` directories that already exist

For single-app projects, use `skaileup-conceptualization` instead.

## Relationship to skaileup-conceptualization

This domain **complements**, not replaces, the standard conceptualization pipeline:

| Layer | Domain | What it produces |
|---|---|---|
| Umbrella | `skaileup-project-concept` (this) | Ecosystem brief, subsystem map, integration architecture |
| Per-subsystem | `skaileup-conceptualization` | Features, screens, data model, tech stack per app |

The meta-concept is a routing table that links to per-subsystem concepts. It never duplicates
feature specs, screen specs, or data models that belong to individual subsystems.

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | Meta-concept contract — `_concept/` structure for multi-product projects |
| `skills/` | Skills for generating and reviewing umbrella concepts |

## Skills

| Skill | Purpose | When to use |
|---|---|---|
| `overview` | Generate `1_discovery/` — brief, goals, comparable | Starting a new meta-concept |
| `subsystem-map` | Generate `2_subsystems/` — index + per-subsystem files | After overview, or when subsystems change |
| `integration-architecture` | Generate `3_integration/` — architecture, deployment, shared contracts | After subsystem map is complete |
| `review` | Audit the meta-concept for completeness and consistency | Before major milestones or releases |

## Conventions

- The meta-concept lives at the repo root `_concept/` (or wherever the shell repo is)
- Subsystem concepts live in their own directories (`<subsystem>/_concept/`)
- The meta-concept references subsystem concepts via `concept_ref` paths — never copies content
- PLANS.md tracks ecosystem-level milestones, not per-subsystem feature progress
- Subsystem maturity levels (`concept` → `prototype` → `alpha` → `beta` → `production`) are the primary progress indicator
