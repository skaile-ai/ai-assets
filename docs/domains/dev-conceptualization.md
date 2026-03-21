---
title: dev-conceptualization
description: Structured project concept pipeline — from initial discovery through experience design and technical blueprint, producing a complete _concept/ artifact folder.
---

Implements the full project conceptualization pipeline — from a raw project idea through structured discovery, experience design, technical blueprinting, and review — producing a versioned `_concept/` artifact folder that downstream implementation domains consume.

Skills are organized into numbered groups reflecting pipeline order. CF and Saxe variants coexist under `cf/` and `saxe/` subdirectories until formally merged.

**Stage:** alpha

## Building Blocks

| Folder | Purpose |
|---|---|
| `contracts/` | CF and Saxe rules for artifact structure, frontmatter, naming, cross-references, and semantic types |
| `docs/` | Pipeline architecture, observability specs, workflow guides |
| `flows/` | `cli-concept.json`, `concept-only.json`, `prototype.json`, `reverse-engineer.json` |
| `skills/` | Numbered skill groups (see below) |
| `prompts/` | Reusable prompt fragments for elicitation and briefing |
| `agents/conceptualization/` | Concept pipeline GitAgent (GitAgent spec v0.1.0) |

## Skill Groups

### Phase 1 — Discovery (`skills/10_discovery/`)

| Skill | What it produces |
|---|---|
| `overview` | Project brief, goals, and comparable analysis → `_concept/01_project/` |
| `research` | Domain research, competitors, patterns → `_grounding/` |
| `brand-visual` | Visual identity tokens → `_concept/04_brand/` |
| `brand-behavioral` | Communication tone and copy guidelines → `_concept/04_brand/` |

### Phase 2 — Experience (`skills/20_experience/`)

| Skill | What it produces |
|---|---|
| `journeys` | User journey maps → `_concept/02_journeys/` |
| `features` | Feature specifications → `_concept/03_features/` |
| `behaviors` | Behavioral specs → `_concept/03b_behavior/` |
| `screens` | UI screen specs → `_concept/07_screens/` |
| `components` | Component inventory → `_concept/07_screens/components/` |
| `mock` | Interactive mockups (Alpine/Vue/Preact) |
| `storybook` | Storybook story generation |

### Phase 3 — Blueprint (`skills/30_blueprint/`)

| Skill | What it produces |
|---|---|
| `techstack` | Tech stack decision → `_concept/05_techstack/` |
| `architecture` | System architecture → `_concept/05b_architecture/` |
| `datamodel` | Data model (DBML + JSON) → `_concept/06_datamodel/` |

### Standalone Skills

| Skill | When to use |
|---|---|
| `add-feature` | Add a single feature to an existing concept |
| `reverse-engineer` | Generate a `_concept/` folder from an existing codebase |
| `review` | Concept structure audit and quality review |

## Flows

| Flow | Description |
|---|---|
| `cli-concept` | Concept phase for CLI tools (no UI/brand/screens) |
| `concept-only` | Full concept phase without implementation |
| `prototype` | Concept + rapid implementation for quick prototypes |
| `reverse-engineer` | Start from an existing codebase |
