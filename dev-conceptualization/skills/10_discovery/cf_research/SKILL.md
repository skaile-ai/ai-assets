---
name: research
description: "Use when grounding decisions in real-world data — competitor analysis, audience research, design inspiration, behavioral patterns, color/font research. Research is a MODE that runs in parallel with other steps, not a sequential pipeline step."
keywords: [research, competitors, market, audience, personas, brand, inspiration, templates, layouts, design, patterns, grounding]
user_inputs:
  dialog:
  - id: research_scope
    label: "Research scope"
    type: select
    required: true
    options: [domain, competitors, audiences, design, patterns, colors, behavioral, all]
    hint: "What area to research (select 'all' for comprehensive research)"
  files: []
metadata:
  stage: alpha
  requires:
  - conceptualization-contract
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

**Writes to:** `_concept/_research/` — cross-cutting research goes to `_research/general/` (domain, competitors, audiences, design_inspiration, patterns, colors_fonts, behavioral_patterns), step-specific research goes to `_research/{step}/`, raw material goes to `_research/findings/`

**Backward compatibility:** If `_grounding/` exists, read from it but write new output to `_research/`.

## When to Use

- The user wants to ground decisions in real-world data
- Competitor analysis, audience research, or design inspiration is needed
- The user says "research this", "what do competitors do", "find inspiration"
- Any other skill would benefit from grounding data before proceeding
- The orchestrator dispatches this in parallel with any concept step

## When NOT to Use

- The user already has comprehensive research and just wants to proceed
- The user wants to write features or screens directly without research
- Research scope is unclear — ask the user to narrow down first

## Prerequisites

### HARD-GATE

None. Research can run at any time. It has no hard dependencies.

### Shared Contracts

Before starting, read:
- `cf__shared/concept_structure.md` — valid `_concept/` paths
- `cf__shared/frontmatter.md` — required YAML fields
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages, no overwrite without approval)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Context Budget

| Action | Path | Required |
|--------|------|----------|
| **Must read** | `_concept/01_project/brief.md` (if exists) | Yes (if available) |
| **Must read** | Whatever `_concept/` step is currently active | Yes |
| **Optional** | Everything else in `_concept/` | No |
| **Never load** | Source code | — |

## Standalone Mode

This skill can be invoked directly without the orchestrator.
**Gate check:** None
**If gates fail:** N/A
**On completion:** Present summary, then suggest next steps (cf_concept_functionality_features, cf_concept_brand_visual, cf_concept_ui_screens — all benefit from grounding data).

## Workflow

### Step 1: Read Brief and Plan Research

Read all files in `_concept/01_project/` (if they exist). Extract:
- The app's domain and problem space
- Target audience(s)
- Comparable products the user already mentioned
- Success criteria and constraints

Present a research plan to the user based on the selected `research_scope`:

> "Based on your brief, here's what I'll research:
>
> **Competitors & Comparables**
> - [List 3-5 products to investigate, including user-mentioned ones]
>
> **Audience Research**
> - [Target personas to profile]
>
> **Design Inspiration**
> - [Types of visual patterns to look for: dashboards, onboarding flows, etc.]
>
> Want me to add or skip anything?"

Wait for confirmation before proceeding.

### Step 2: Competitor and Comparable Analysis

For each comparable product, use web search to investigate:

| Aspect | What to capture |
|--------|----------------|
| **Core features** | What does it do? Feature list with brief descriptions |
| **Strengths** | What do users praise? (check reviews, social media, forums) |
| **Weaknesses** | What do users complain about? Common pain points |
| **Pricing model** | Free, freemium, subscription, one-time |
| **Target audience** | Who uses it? Company size, role, technical level |
| **Visual approach** | Design style, layout patterns, color scheme, typography |
| **Tech signals** | Mobile app? API? Integrations? Open source? |
| **Market position** | Leader, challenger, niche, emerging |

**Write: `_concept/_research/general/competitors.md`**

Body structure per competitor:

```markdown
## [Product Name]

**URL:** [url]
**Tagline:** [their one-liner]
**Target audience:** [who they serve]
**Market position:** leader | challenger | niche | emerging

### Features
- Feature 1 — brief description

### Strengths
- What users praise (with source if available)

### Weaknesses
- What users complain about (with source if available)

### Pricing
[pricing model summary]

### Visual Approach
- Style: [e.g., minimal, dashboard-heavy, playful]
- Notable patterns: [e.g., sidebar nav, card-based layout, dark mode default]

### Relevance to Our App
- **Borrow:** [specific things to learn from]
- **Avoid:** [specific things to do differently]
- **Gap:** [what they don't do that we should]
```

### Step 3: Audience Research

Profile each target audience segment.

**Write: `_concept/_research/general/audiences.md`**

Body structure per persona:

```markdown
## Persona: [Name — e.g., "The Overwhelmed Project Manager"]

**Role:** [job title / context]
**Technical level:** non-technical | semi-technical | technical
**Company size:** solo | startup | SMB | enterprise

### Current Workflow
How they solve the problem today.

### Pain Points
1. [Specific frustration]

### What They Value
- [e.g., "Quick setup — no patience for complex onboarding"]

### Design Implications
- [e.g., "Needs mobile support — often away from desk"]

### Where They Hang Out
- [Relevant communities, forums, social channels]
```

### Step 4: Domain Research

Investigate the broader domain for patterns, terminology, and conventions.

**Write: `_concept/_research/general/domain.md`**

Body covers:
- **Industry terminology** — standard terms users expect
- **Regulatory or compliance considerations** — if any
- **Market trends** — where the space is heading
- **Common workflows** — standard patterns in this domain
- **Integration expectations** — what users expect to connect with

### Step 5: Design Inspiration and Brand References

Research visual patterns, templates, and brand approaches.

**Write: `_concept/_research/general/design_inspiration.md`**

Step-specific design inspiration (e.g., during brand visual step) can also be written
to `_concept/_research/brand_visual/design_inspiration.md`.

Body structure:

```markdown
## Layout Patterns

### Pattern: [e.g., "Sidebar + Main Content"]
- **Used by:** [Product A, Product B]
- **Why it works:** [reason]
- **Adaptation for our app:** [how to use it]

## Color & Brand Approaches

### Approach: [e.g., "Dark Mode with Accent Colors"]
- **Example:** [Product name / URL]
- **Palette:** [describe dominant colors]

## Typography

### Trend: [e.g., "Geometric Sans for Tech Products"]
- **Fonts seen:** [font names]

## Component Patterns
## Onboarding Inspiration
## Empty State Approaches
```

### Step 6: Additional Research Scopes

Based on `research_scope`, also write to:
- `_concept/_research/general/patterns.md` — common UX patterns for this domain
- `_concept/_research/general/colors_fonts.md` — color palette and typography research
- `_concept/_research/general/behavioral_patterns.md` — state machines and lifecycle patterns in the domain

When research is dispatched for a specific step, write step-relevant findings to
`_concept/_research/{step}/` (e.g., patterns discovered during features step go to
`_concept/_research/features/patterns.md`).

### Step 7: Save Raw Findings

Save any raw material to `_concept/_research/findings/`:
- Screenshots (from `browser` skill)
- Saved pages or excerpts
- Links to resources

Create `findings/index.md` to catalog what is there.

### Step 8: Present Summary

> "Research complete. Here's what I found:
>
> **Competitors analyzed:** N products
> - [Key insight 1]
>
> **Audience personas:** N personas defined
> - [Key insight]
>
> **Design inspiration:** N references collected
> - [Key insight]
>
> **Opportunities identified:**
> 1. [Gap competitors miss]
> 2. [Audience need not being met]
>
> Review `_concept/_research/` for full details."

### Step 9: Emit Events

```
[research] started
  run_id: <uuid>
  scope: <research_scope>
  reads: 01_project/brief.md

[research] checkpoint phase=competitors_analyzed
  products: 5

[research] checkpoint phase=audiences_profiled
  personas: 3

[research] checkpoint phase=design_inspiration_collected
  references: 8

[research] completed
  run_id: <uuid>
  artifacts: _research/general/domain.md, _research/general/competitors.md,
    _research/general/audiences.md, _research/general/design_inspiration.md,
    _research/findings/
```

## Outputs

| Folder / File | Description |
|--------|-------------|
| `_concept/_research/general/domain.md` | Domain terminology, trends, regulations |
| `_concept/_research/general/competitors.md` | Competitor analysis with features, strengths, weaknesses |
| `_concept/_research/general/audiences.md` | Persona profiles with design implications |
| `_concept/_research/general/design_inspiration.md` | Layout patterns, color approaches, typography, components |
| `_concept/_research/general/patterns.md` | UX patterns for this domain |
| `_concept/_research/general/colors_fonts.md` | Color palette and typography research |
| `_concept/_research/general/behavioral_patterns.md` | State machines and lifecycle patterns |
| `_concept/_research/{step}/` | Step-specific research (e.g., `brand_visual/design_inspiration.md`) |
| `_concept/_research/{step}/user_input.json` | Saved dialog field values for each step |
| `_concept/_research/findings/` | Raw screenshots, excerpts, links |

## Completion Summary

Present to user: files produced (`_research/general/` and `_research/{step}/` with research artifacts), key decisions made (competitors analyzed, personas profiled, design references collected), suggested next steps (which skills are now unblocked -- cf_concept_functionality_features informed by competitor gaps, cf_concept_brand_visual informed by design references, cf_concept_ui_screens informed by layout patterns).

## Common Mistakes

| Mistake | Why it happens | What to do instead |
|---------|---------------|-------------------|
| Making claims without web search evidence | The agent guesses instead of searching | Every factual claim must come from web search. Cite sources. |
| Inventing competitor features or pricing | The user didn't mention any, so the agent fabricates | Verify via search. If unknown, say "not publicly available". |
| Generic personas ("busy professional") | The agent uses template personas | Be specific to the domain. Use real job titles, real pain points. |
| Skipping design inspiration | The agent thinks features are enough | Design inspiration is critical input for brand. Always produce it. |
| Writing to `_grounding/` or `02_research/` instead of `_research/` | The agent uses the old output path | Research output goes to `_concept/_research/`. Cross-cutting topics go to `_research/general/`, step-specific to `_research/{step}/`. |

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** `_concept/01_project/` (if exists), active `_concept/` step
- **Feeds into:** all downstream skills via `_concept/_research/` — features, brand, screens all check for research data
- **Feedback loops:** None inbound. Outbound: every skill can optionally read from `_research/`.

## Strict Constraints

- **FORBIDDEN:** Making claims without web search evidence
- **FORBIDDEN:** Inventing competitor features or pricing — verify via search
- **FORBIDDEN:** Generic personas — be specific to the domain
- **FORBIDDEN:** Skipping design inspiration — critical input for brand
- **REQUIRED:** Every competitor entry has a "Relevance to Our App" section
- **REQUIRED:** Every persona has "Design Implications"
- **REQUIRED:** Sources cited or noted for all factual claims
- **REQUIRED:** Screenshots saved to `findings/` when `browser` skill is available
