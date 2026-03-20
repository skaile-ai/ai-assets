---
name: concept-1-discovery-3-brand
description: "Step 3: Brand identity. Guides the user through defining visual identity — aesthetic direction, colors, typography, tone of voice. Extracts palettes from reference URLs. Produces identity.md + tokens.json (with Tailwind CSS custom properties for PostXL/Radix/Tailwind theming) in _concept/1_discovery/3_brand/. Downstream skills consume these tokens — no more inventing colors."
keywords: brand, design, colors, fonts, typography, aesthetic, tone, tokens, identity
---

ROLE  Brand Identity Designer — defines visual identity, produces tokens consumed by all downstream skills.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, problem, comparables
  ? _concept/1_discovery/2_research/design_inspiration.md — color approaches, typography trends, references

WRITES
  _concept/1_discovery/3_brand/identity.md             — full brand guide (aesthetic, colors, typography, atmosphere)
  _concept/1_discovery/3_brand/tokens.json             — machine-readable design tokens + Tailwind CSS custom properties
  _concept/1_discovery/3_brand/references/             — screenshots from user-provided reference URLs

REFERENCES
  shared/contracts/concept_structure.md              — valid paths (1_discovery/3_brand/)
  shared/contracts/frontmatter.md                    — brand frontmatter fields
  references/design_philosophy.md           — aesthetic principles, boldness guidelines
  references/discovery_questions.md         — interview questions and presentation templates
  references/tokens_schema.md               — tokens.json structure and output templates

REQUIRES
  soft: agent-browser (reference URL extraction deferred without it)

STEP 1: Read context
  - Read brief.md for app name, audience, problem, comparables
  IF _concept/1_discovery/2_research/ exists
    - Read design_inspiration.md for color, typography, and brand references
  EMIT [concept-1-discovery-3-brand] started run_id=<uuid> reads=1_discovery/1_overview/brief.md

STEP 2: Explore aesthetic direction
  - Ask discovery questions ONE AT A TIME, building on each answer
  - See references/discovery_questions.md for the six questions and presentation templates
  - Q1: Reference URL  Q2: Feeling/mood spectrum  Q3: Calibration extremes
  - Q4: Existing brand assets  Q5: Light/dark/both  Q6: Font preferences
  MUST present a rich mood spectrum for Q2 — not a simple dropdown
  MUST present two opposing examples for Q3, relevant to the app type

STEP 3: Extract from reference URLs
  IF user provides a URL
    - Open URL with agent-browser and take screenshot
    - Save screenshot to _concept/1_discovery/3_brand/references/
    - Analyze: dominant colors, typography, layout density, elevation, border radius, mood
    - Present findings and ask: use as-is, adapt, or go different direction
  EMIT [concept-1-discovery-3-brand] checkpoint phase=aesthetic_direction mood=<mood> reference_urls=<N>

STEP 4: Compose brand identity
  - Synthesize all input into a complete brand proposal
  - Present to user with: aesthetic, colors (with purpose per color), typography (with rationale),
    details (radius, mode, atmosphere), and the ONE memorable element
  - See references/tokens_schema.md for the presentation format
  MUST include a stated "memorable element" — what makes this app unforgettable visually
  MUST justify typography choices for the aesthetic direction
  MUST define color usage rules — not just hex values but when to use each
  EMIT [concept-1-discovery-3-brand] checkpoint phase=brand_composed colors=<N> fonts=heading=<H>,body=<B>

CHECKPOINT brand_proposal
  > "Review the brand identity above. Approve to write artifacts, or tell me what to change."

STEP 5: Write artifacts
  $ mkdir -p _concept/1_discovery/3_brand/references
  - Write identity.md with frontmatter (status: draft, mood, mode, last_updated)
  - Write tokens.json with all required sections
  - See references/tokens_schema.md for identity.md body sections and tokens.json structure
  MUST include tailwind section in tokens.json with CSS custom properties for Radix/Tailwind theming

OUTPUT _concept/1_discovery/3_brand/identity.md
  ---
  status: draft
  mood: "<aesthetic-description>"
  mode: <light|dark>
  last_updated: <YYYY-MM-DD>
  ---
  <brand guide: aesthetic direction, color usage rules, typography hierarchy,
   spacing, elevation, atmosphere, tone of voice, memorable element, do's/don'ts>

OUTPUT _concept/1_discovery/3_brand/tokens.json
  {
    "colors": { "primary": "#...", "secondary": "#...", "accent": "#...",
                "background": "#...", "surface": "#...", "text": "#...",
                "text_muted": "#...", "border": "#...",
                "error": "#...", "success": "#...", "warning": "#..." },
    "fonts": { "heading": "...", "body": "...", "mono": "..." },
    "radius": "...", "mode": "...", "spacing_base": "8px",
    "shadows": { "sm": "...", "md": "...", "lg": "..." },
    "atmosphere": { "type": "...", "description": "..." },
    "tailwind": { "--color-primary": "#...", "--color-primary-foreground": "#...",
                  "--color-secondary": "#...", "--color-background": "#...",
                  "--color-surface": "#...", "--color-foreground": "#...",
                  "--color-muted": "#...", "--color-border": "#...",
                  "--color-destructive": "#...", "--color-success": "#...",
                  "--color-warning": "#...", "--radius": "..." }
  }

STEP 6: Emit completion
  EMIT [concept-1-discovery-3-brand] completed run_id=<uuid> artifacts=1_discovery/3_brand/identity.md,1_discovery/3_brand/tokens.json reference_screenshots=<N>

CHECKPOINT brand_final
  > "Review _concept/1_discovery/3_brand/. The identity guide and design tokens will be used by every
  >  mockup and screen spec from here on. Approve, or tell me what to change."

STEP 7: Hand off
  - Inform user tokens will be consumed by:
    concept-2-experience-3-screens (color/font refs in screen specs),
    app-design (CSS variables and Tailwind theme for @postxl/ui-components),
    implement-1-setup-1-scaffold (initial CSS custom property setup)
  - Suggest next: concept-3-blueprint-1-techstack or continue concept pipeline

NEVER produce generic brand output (primary blue, gray secondary, Inter font)
NEVER write tokens.json without discussing aesthetic direction first
NEVER ignore reference URLs — always extract and analyze when provided
NEVER produce cookie-cutter brand that could belong to any app
MUST include all fields downstream skills expect in tokens.json
MUST include tailwind section with CSS custom properties in tokens.json

CHECKLIST
  - [ ] Aesthetic direction discussed and agreed with user
  - [ ] Brand has a stated memorable element
  - [ ] Typography choices justified for aesthetic direction
  - [ ] Color usage rules defined (not just hex values)
  - [ ] tokens.json includes all required sections (colors, fonts, radius, mode, shadows, atmosphere, tailwind)
  - [ ] identity.md has correct frontmatter (status, mood, mode, last_updated)
  - [ ] Reference URLs extracted and screenshots saved (if provided)
