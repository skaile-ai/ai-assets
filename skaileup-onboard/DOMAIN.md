---
name: skaileup-onboard
description: "Project initialization, structured onboarding, seed ingestion, and pipeline orchestration — the entry point for all skaileup concept workflows."
type: domain
building_blocks:
  contracts: "n/a — to be populated after skill migration."
  docs: "n/a — to be populated after skill migration."
  flows: "n/a — to be populated after skill migration."
  agents: "n/a — to be populated after skill migration."
  skills: "Orchestrator, onboarding initialization, and seed ingestion skills."
  tools: "n/a"
stage: alpha
---

# skaileup-onboard

Project initialization, structured onboarding, seed ingestion, and pipeline orchestration — the entry point for all skaileup concept workflows. Every concept pipeline begins here: the orchestrator dispatches skill sequences, the onboard skill collects project context from the user, and the ingest-seeds skill loads raw seed material into structured grounding artifacts.

Skills will be moved into this domain during the architecture reorganization (Phase 5.2+).

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | Invocable skills (see below) |
| `flows/` | Flow definitions for orchestrated pipelines (moved from skaileup-conceptualization later) |
| `agents/` | Agent definitions for supervised orchestration (moved from skaileup-conceptualization later) |
| `contracts/` | Domain-local contracts (moved from skaileup-conceptualization later) |
| `docs/` | Domain-level documentation (moved from skaileup-conceptualization later) |

## Skills (target)

| Skill | Purpose |
|-------|---------|
| `skailup-orchestrator/` | Dispatches skill sequences for a full concept pipeline run |
| `skailup-onboard/` | Collects project context, goals, and constraints from the user |
| `skailup-ingest-seeds/` | Loads raw seed material and produces structured grounding artifacts |

## Conventions

- This domain is always the first to run in any concept pipeline; downstream domains depend on artifacts it produces.
- The orchestrator skill is the only entry point for automated multi-skill flows; do not invoke downstream skills directly for full pipeline runs.
- Seed ingestion writes to `_grounding/seeds/`; all subsequent skills read from there rather than accepting raw input directly.
