---
name: "skailup-research"
description: "Use when grounding decisions in real-world data — competitor analysis, audience research, design inspiration, behavioral patterns, color/font research. Research is a MODE that runs in parallel with other steps, not a sequential pipeline step."
metadata:
  version: "1.0.0"
  tags:
    - "research"
    - "competitors"
    - "market"
    - "audience"
    - "personas"
    - "brand"
    - "inspiration"
    - "templates"
    - "layouts"
    - "design"
    - "patterns"
    - "grounding"
  source: "MIGRATED"
  parameters:
    depth:
      type: enum
      values: [none, light, medium, max]
      default: medium
  artifacts:
    requires: []
    produces:
      - id: research-domain
      - id: research-competitors
      - id: research-audiences
      - id: research-design-inspiration
      - id: research-patterns
    consumes:
      - id: brief
        gate: soft
      - id: onboarding-profile
        gate: soft
  prerequisites:
    inputs_required:
      - id: research_scope
        label: "Research scope"
        type: select
        options:
          - domain
          - competitors
          - audiences
          - design
          - patterns
          - colors
          - behavioral
          - all
        hint: "What area to research (select 'all' for comprehensive research)"
    reads:
      - path: "_concept/discovery/brief.md"
        description: "App name, problem statement, target audience for focused research"
      - path: "_concept/_grounding/onboarding/inputs/overview.json"
        description: "Pre-collected answers to sharpen research scope"
    produces:
      - path: "_concept/_grounding/research/domain.md"
        description: "Domain landscape findings"
      - path: "_concept/_grounding/research/competitors.md"
        description: "Competitor analysis"
      - path: "_concept/_grounding/research/audiences.md"
        description: "Target audience personas"
      - path: "_concept/_grounding/research/design-inspiration.md"
        description: "Visual and layout references"
      - path: "_concept/_grounding/research/behavioral-patterns.md"
        description: "UX behavioral patterns"
  user_inputs:
    dialog:
      - id: "research_scope"
        label: "Research scope"
        type: "select"
        required: true
        options:
          - "domain"
          - "competitors"
          - "audiences"
          - "design"
          - "patterns"
          - "colors"
          - "behavioral"
          - "all"
        hint: "What area to research (select 'all' for comprehensive research)"
    files: []
---

# Research — Domain Research Mode

## Overview

The **research** skill is the Domain Research agent. It investigates the landscape
around the user's app idea — comparable products, competitor features, target audiences,
visual/brand inspiration, behavioral patterns, and color/font trends. Its findings
inform every downstream skill: features learn from competitor gaps, brand draws from
design references, and screens adopt proven layout patterns.

**This is a MODE skill, not a sequential pipeline step.** It can be dispatched
alongside any other step in the pipeline. Research runs in parallel.

**Writes to:** `_grounding/research/` (cross-cutting research), `_grounding/step/{step}/` (step-specific research), `_grounding/findings/` (raw screenshots and excerpts)

## When to Use

- The user wants to ground decisions in real-world data
- Competitor analysis, audience research, or design inspiration is needed
- The user says "research this", "what do competitors do", "find inspiration"
- Any other skill would benefit from grounding data before proceeding
- The orchestrator dispatches this in parallel with any concept step

## When NOT to Use

- The user already has comprehensive research and just wants to proceed
- Research scope is unclear — ask the user to narrow down first

## Prerequisites

None. Research can run at any time. It has no hard dependencies.

## Shared Contracts

Before starting, read:
- `skaileup-shared/contracts/concept_structure.md` — valid `_concept/` paths
- `skaileup-shared/contracts/frontmatter.md` — required YAML fields
- `skaileup-shared/contracts/iron_laws.md` — non-negotiable constraints
- `skaileup-shared/contracts/agent_patterns.md` — communication style, standalone mode

## Context Budget

| Source | Priority |
|--------|----------|
| `_concept/discovery/brief.md` | Required (if exists) |
| `_concept/discovery/goals.md` | Required (if exists) |
| `_concept/discovery/comparable.md` | Required (if exists) |
| Active `_concept/` step files | Skim for context |
| All other `_concept/` files | Never load |
| Source code | Never load |

---

ROLE  Domain Research agent — investigates comparable products, competitor features,
      target audiences, and visual/brand inspiration. Produces structured, evidence-based
      research artifacts with cited sources.

READS
  _concept/discovery/brief.md       — elevator pitch, audience, problem, hero flow
  _concept/discovery/goals.md       — success criteria, constraints
  _concept/discovery/comparable.md  — user-identified reference apps

WRITES
  _grounding/research/domain.md              — industry terms, regulations, trends, workflows
  _grounding/research/competitors.md         — per-product analysis (features, strengths, gaps)
  _grounding/research/audiences.md           — persona profiles with design implications
  _grounding/research/design-inspiration.md  — layout patterns, color, typography, components
  _grounding/research/patterns.md            — UX patterns for this domain
  _grounding/research/colors-fonts.md        — color palette and typography research
  _grounding/research/behavioral-patterns.md — state machines and lifecycle patterns
  _grounding/step/{step}/*.md                — step-specific research when dispatched alongside a skill
  _grounding/findings/index.md               — catalog of screenshots and raw material
  _grounding/findings/*.png                  — screenshots captured via browser tool

REFERENCES
  skaileup-shared/contracts/concept_structure.md       — valid paths
  skaileup-shared/contracts/frontmatter.md             — required YAML fields
  references/competitor_template.md               — per-competitor analysis structure
  references/persona_template.md                  — per-persona profile structure
  references/design_inspiration_template.md       — layout/color/typography/component structure

MUST  cite sources or note evidence for all factual claims
MUST  include "Relevance to Our App" section for every competitor
MUST  include "Design Implications" section for every persona
MUST  always produce design-inspiration.md — even if other sections are thin
MUST  save screenshots to _grounding/findings/ when browser tool is available

NEVER  make claims without web search evidence
NEVER  invent competitor features or pricing — verify via search
NEVER  use generic personas ("busy professional") — be specific to the domain
NEVER  skip design inspiration research

EMIT  [research] started run_id=<uuid> scope=<research_scope>

STEP 1: Read brief and plan research
  - Read all files in _concept/discovery/ (if they exist)
  - Extract: domain, problem space, target audiences, comparable products, constraints
  - Present research plan to user listing:
    - 3-5 competitors/comparables to investigate (include user-mentioned ones from comparable.md)
    - Target personas to profile
    - Types of visual patterns to look for (dashboards, onboarding, etc.)
  > "Based on your brief, here's what I'll research: [plan]. Want me to add or skip anything?"
  - Wait for confirmation; user may add products or redirect focus

STEP 2: Competitor and comparable analysis
  - For each product, web-search for: core features, strengths (user reviews),
    weaknesses (pain points), pricing model, target audience, visual approach,
    tech signals, market position
  - Capture screenshots of notable UI patterns via browser tool when available
  - Write each competitor using the structure in references/competitor_template.md

OUTPUT _grounding/research/competitors.md
  ---
  products_analyzed: <N>
  last_updated: <YYYY-MM-DD>
  ---
  ## [Product Name]
  ... (see references/competitor_template.md for full structure)

EMIT  [research] checkpoint phase=competitors_analyzed products=<N>

STEP 3: Audience research
  - For each target segment, web-search for: demographics, current tools/workflows,
    pain points, values, community channels
  - Write each persona using the structure in references/persona_template.md

OUTPUT _grounding/research/audiences.md
  ---
  personas_defined: <N>
  last_updated: <YYYY-MM-DD>
  ---
  ## Persona: [Name]
  ... (see references/persona_template.md for full structure)

EMIT  [research] checkpoint phase=audiences_profiled personas=<N>

STEP 4: Domain research
  - Investigate broader domain for terminology, regulations, market trends,
    common workflows, and integration expectations
  - Write findings covering: industry terminology, compliance, trends, workflows, integrations

OUTPUT _grounding/research/domain.md
  ---
  last_updated: <YYYY-MM-DD>
  ---

STEP 5: Design inspiration and brand references
  - Web-search for visual patterns relevant to app domain and audience:
    layout patterns, color approaches, typography trends, component patterns,
    onboarding flows, empty states, brand references
  - Capture screenshots via browser tool where possible; save to _grounding/findings/
  - Write using the structure in references/design_inspiration_template.md

OUTPUT _grounding/research/design-inspiration.md
  ---
  references_collected: <N>
  last_updated: <YYYY-MM-DD>
  ---
  ## Layout Patterns
  ... (see references/design_inspiration_template.md for full structure)

EMIT  [research] checkpoint phase=design_inspiration_collected references=<N>

STEP 6: Additional scope coverage (based on research_scope)
  - `patterns`    → write _grounding/research/patterns.md (UX patterns for this domain)
  - `colors`      → write _grounding/research/colors-fonts.md (color palette and typography)
  - `behavioral`  → write _grounding/research/behavioral-patterns.md (state machines and lifecycles)
  - `all`         → write all three files above
  - Step-specific research (when dispatched alongside another skill): write to _grounding/step/{step}/*.md

STEP 7: Save raw findings
  - Save screenshots, excerpts, and links to _grounding/findings/
  - Create _grounding/findings/index.md cataloging each file with source, date, and notes

OUTPUT _grounding/findings/index.md
  ---
  last_updated: <YYYY-MM-DD>
  ---
  | File | Source | Date | Notes |
  |------|--------|------|-------|

STEP 8: Present summary
  > "Research complete. Competitors: N, Personas: N, References: N.
  >  Key opportunities: [list]. Review _grounding/ for full details."

EMIT  [research] checkpoint phase=research_complete

CHECKPOINT research_review
  > "Review _grounding/. These findings inform:
  > - Features — competitor gaps and audience needs shape priorities
  > - Brand — design references and color approaches guide visual identity
  > - Screens — layout patterns and component inspiration drive screen specs
  >
  > Approve, or tell me what to dig deeper on."
  - Wait for explicit approval before suggesting proceeding to next steps

STEP 9: Hand off
  > "Research approved. Next steps:
  > - Run `brand-visual` to define visual identity (reads design_inspiration)
  > - Run `journeys` to map user journeys (reads audience research)
  > - Run `features` to define feature requirements (reads competitor gaps + domain)"

EMIT  [research] completed run_id=<uuid> artifacts=_grounding/research/ scope=<research_scope>

CHECKLIST
  - [ ] competitors.md has per-product Relevance section
  - [ ] audiences.md has per-persona Design Implications
  - [ ] design_inspiration.md produced with layout + color + typography sections
  - [ ] All factual claims cite sources
  - [ ] _grounding/findings/index.md catalogs all raw material
  - [ ] domain.md covers terminology and compliance

---

## Outputs

| File | Description |
|------|-------------|
| `_grounding/research/domain.md` | Domain terminology, trends, regulations |
| `_grounding/research/competitors.md` | Competitor analysis with features, strengths, weaknesses |
| `_grounding/research/audiences.md` | Persona profiles with design implications |
| `_grounding/research/design-inspiration.md` | Layout patterns, color approaches, typography, components |
| `_grounding/research/patterns.md` | UX patterns for this domain (scope: patterns or all) |
| `_grounding/research/colors-fonts.md` | Color palette and typography research (scope: colors or all) |
| `_grounding/research/behavioral-patterns.md` | State machines and lifecycle patterns (scope: behavioral or all) |
| `_grounding/step/{step}/*.md` | Step-specific research when dispatched alongside a skill |
| `_grounding/findings/` | Raw screenshots, excerpts, links |

## Depth Behavior

| Depth | Behavior |
|---|---|
| `none` | Skip this skill entirely |
| `light` | Produce minimal output — key points only, no elaboration |
| `medium` | Standard output — balanced detail and coverage (default) |
| `max` | Comprehensive output — exhaustive analysis, extended examples, edge cases |

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Making claims without web search evidence | Every factual claim must come from web search. Cite sources. |
| Inventing competitor features or pricing | Verify via search. If unknown, say "not publicly available". |
| Generic personas ("busy professional") | Be specific to the domain. Use real job titles, real pain points. |
| Skipping design inspiration | Design inspiration is critical input for brand. Always produce it. |
| Writing to `_concept/discovery/2_research/` or `_concept/_research/` | Research output goes to `_grounding/`. Cross-cutting topics to `_grounding/research/`, step-specific to `_grounding/step/{step}/`. |

## Backward Compatibility

When reading from `_grounding/`, check for both old and new paths:
- `_grounding/general/` → `_grounding/research/` (prefer new)
- `_grounding/{step}/user_input.json` → `_grounding/onboarding/inputs/{step}.json` (prefer new)
- Underscore filenames (`design_inspiration.md`) → hyphenated (`design-inspiration.md`) (prefer new)

When writing, always use new paths. When reading, try new path first, fall back to old.
