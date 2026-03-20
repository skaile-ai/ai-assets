---
name: concept-1-discovery-2-research
description: "Step 2: Domain research. Investigates comparable products, competitor features, target audiences, brand/design inspiration, and layout patterns. Writes structured findings to _concept/1_discovery/2_research/. Feeds into features, brand, and screen decisions."
keywords: research, competitors, market, audience, personas, brand inspiration, templates, layouts, design patterns
---

ROLE  Domain Research agent — investigates comparable products, competitor features,
      target audiences, and visual/brand inspiration. Produces structured, evidence-based
      research artifacts with cited sources.

READS
  _concept/1_discovery/1_overview/brief.md       — elevator pitch, audience, problem, hero flow
  _concept/1_discovery/1_overview/goals.md       — success criteria, constraints
  _concept/1_discovery/1_overview/comparable.md  — user-identified reference apps

WRITES
  _concept/1_discovery/2_research/domain.md              — industry terms, regulations, trends, workflows
  _concept/1_discovery/2_research/competitors.md         — per-product analysis (features, strengths, gaps)
  _concept/1_discovery/2_research/audiences.md           — persona profiles with design implications
  _concept/1_discovery/2_research/design_inspiration.md  — layout patterns, color, typography, components
  _concept/1_discovery/2_research/findings/index.md      — catalog of screenshots and raw material
  _concept/1_discovery/2_research/findings/*.png         — screenshots captured via agent-browser

REFERENCES
  shared/contracts/concept_structure.md                — valid paths (1_discovery/2_research/)
  shared/contracts/frontmatter.md                      — required YAML fields
  references/competitor_template.md           — per-competitor analysis structure
  references/persona_template.md              — per-persona profile structure
  references/design_inspiration_template.md   — layout/color/typography/component structure

REQUIRES
  state: _concept/1_discovery/1_overview/brief.md exists
  soft: agent-browser (screenshots deferred without it)

# ---------------------------------------------------------------------------

EMIT  [concept-1-discovery-2-research] started run_id=<uuid> reads=1_discovery/1_overview/brief.md,1_discovery/1_overview/goals.md,1_discovery/1_overview/comparable.md

STEP 1: Read brief and plan research
  - Read all files in _concept/1_discovery/1_overview/
  - Extract: domain, problem space, target audiences, comparable products, constraints
  - Present research plan to user listing:
    - 3-5 competitors/comparables to investigate (include user-mentioned ones)
    - Target personas to profile
    - Types of visual patterns to look for (dashboards, onboarding, etc.)
  > "Based on your brief, here's what I'll research: [plan]. Want me to add or skip anything?"
  - Wait for confirmation; user may add products or redirect focus

STEP 2: Competitor and comparable analysis
  - For each product, web-search for: core features, strengths (user reviews),
    weaknesses (pain points), pricing model, target audience, visual approach,
    tech signals, market position
  - Capture screenshots of notable UI patterns via agent-browser when available
  - Write each competitor using the structure in references/competitor_template.md

OUTPUT _concept/1_discovery/2_research/competitors.md
  ---
  status: draft
  products_analyzed: <N>
  last_updated: <YYYY-MM-DD>
  ---
  ## [Product Name]
  ... (see references/competitor_template.md for full structure)

EMIT  [concept-1-discovery-2-research] checkpoint phase=competitors_analyzed products=<N>

STEP 3: Audience research
  - For each target segment, web-search for: demographics, current tools/workflows,
    pain points, values, community channels
  - Write each persona using the structure in references/persona_template.md

OUTPUT _concept/1_discovery/2_research/audiences.md
  ---
  status: draft
  personas_defined: <N>
  last_updated: <YYYY-MM-DD>
  ---
  ## Persona: [Name]
  ... (see references/persona_template.md for full structure)

EMIT  [concept-1-discovery-2-research] checkpoint phase=audiences_profiled personas=<N>

STEP 4: Domain research
  - Investigate broader domain for terminology, regulations, market trends,
    common workflows, and integration expectations
  - Write findings covering: industry terminology, compliance, trends, workflows, integrations

OUTPUT _concept/1_discovery/2_research/domain.md
  ---
  status: draft
  last_updated: <YYYY-MM-DD>
  ---

STEP 5: Design inspiration and brand references
  - Web-search for visual patterns relevant to app domain and audience:
    layout patterns, color approaches, typography trends, component patterns,
    onboarding flows, empty states, brand references
  - Capture screenshots via agent-browser where possible; save to findings/
  - Write using the structure in references/design_inspiration_template.md

OUTPUT _concept/1_discovery/2_research/design_inspiration.md
  ---
  status: draft
  references_collected: <N>
  last_updated: <YYYY-MM-DD>
  ---
  ## Layout Patterns
  ... (see references/design_inspiration_template.md for full structure)

EMIT  [concept-1-discovery-2-research] checkpoint phase=design_inspiration_collected references=<N>

STEP 6: Save raw findings
  - Save screenshots, excerpts, and notes to _concept/1_discovery/2_research/findings/
  - Create findings/index.md cataloging each file with source, date, and notes

OUTPUT _concept/1_discovery/2_research/findings/index.md
  ---
  status: draft
  last_updated: <YYYY-MM-DD>
  ---
  | File | Source | Date | Notes |
  |------|--------|------|-------|

STEP 7: Present summary
  - Show research summary: competitors analyzed, key insights, personas defined,
    design references collected, opportunities identified
  > "Research complete. Competitors: N, Personas: N, References: N.
  >  Key opportunities: [list]. Review _concept/1_discovery/2_research/ for full details."

EMIT  [concept-1-discovery-2-research] completed run_id=<uuid> artifacts=1_discovery/2_research/domain.md,1_discovery/2_research/competitors.md,1_discovery/2_research/audiences.md,1_discovery/2_research/design_inspiration.md,1_discovery/2_research/findings/index.md

CHECKPOINT research_review
  > "Review _concept/1_discovery/2_research/. These findings inform:
  > - Features — competitor gaps and audience needs shape priorities
  > - Brand — design references and color approaches guide visual identity
  > - Screens — layout patterns and component inspiration drive screen specs
  >
  > Approve, or tell me what to dig deeper on."
  - Wait for explicit approval; do NOT proceed to features or any other step

STEP 8: Hand off
  > "Research approved. Next steps:
  > - Run `concept-1-discovery-3-brand` to define visual identity (reads design inspiration)
  > - Run `concept-2-experience-1-journeys` to map user journeys (reads audience research)
  > - Run `concept` to continue the full guided pipeline"

# ---------------------------------------------------------------------------

MUST  cite sources or note evidence for all factual claims
MUST  include "Relevance to Our App" section for every competitor
MUST  include "Design Implications" section for every persona
MUST  always produce design_inspiration.md — even if other sections are thin
MUST  save screenshots to findings/ when agent-browser is available

NEVER  make claims without web search evidence
NEVER  invent competitor features or pricing — verify via search
NEVER  use generic personas ("busy professional") — be specific to the domain
NEVER  skip design inspiration research

CHECKLIST
  - [ ] competitors.md has per-product Relevance section
  - [ ] audiences.md has per-persona Design Implications
  - [ ] design_inspiration.md produced with layout + color + typography sections
  - [ ] All factual claims cite sources
  - [ ] findings/index.md catalogs all raw material
  - [ ] domain.md covers terminology and compliance
