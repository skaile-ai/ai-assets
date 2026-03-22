---
name: "brand-visual"
description: "Use when the project brief is approved but no visual brand exists. Discovers aesthetic direction through plain-language questions, extracts palettes from reference URLs, and writes identity.md + tokens.json + brandbook.html to _concept/1_discovery/2_brand/."
metadata:
  version: "1.0.0"
  tags:
    - "brand"
    - "colors"
    - "fonts"
    - "identity"
    - "tokens"
    - "design"
    - "visual"
    - "palette"
    - "typography"
    - "atmosphere"
    - "brandbook"
  source: "MERGED"
  prerequisites:
    files:
      - path: "_concept/1_discovery/1_overview/brief.md"
        gate: hard
        description: "Project brief must exist before defining visual identity"
    inputs_required:
      - id: mood
        label: "Desired feeling"
        type: text
        hint: "What feeling should the app give? calm/bold/professional/playful..."
      - id: light_dark
        label: "Color mode"
        type: select
        options:
          - light
          - dark
          - both
        default: dark
        hint: "Light mode, dark mode, or both?"
    inputs_optional:
      - id: reference_urls
        label: "Reference websites"
        type: text
        hint: "Show a website you love — I'll extract palette and style"
      - id: font_preferences
        label: "Font preferences"
        type: text
        hint: "Any font preferences or constraints?"
    reads:
      - path: "_concept/_grounding/general/design_inspiration.md"
        description: "Design inspiration from research phase (if available)"
    produces:
      - path: "_concept/1_discovery/2_brand/identity.md"
        description: "Brand identity: palette, typography, atmosphere"
      - path: "_concept/1_discovery/2_brand/tokens.json"
        description: "Design tokens consumed by all downstream skills"
      - path: "_concept/1_discovery/2_brand/brandbook.html"
        description: "Visual preview of the complete brand"
  user_inputs:
    dialog:
      - id: "reference_urls"
        label: "Reference websites"
        type: "text"
        required: false
        hint: "Show a website you love — I'll extract palette and style"
      - id: "mood"
        label: "Desired feeling"
        type: "text"
        required: true
        hint: "What feeling should the app give? calm/bold/professional/playful..."
      - id: "light_dark"
        label: "Color mode"
        type: "select"
        options:
          - "light"
          - "dark"
          - "both"
        required: true
        default: "dark"
        hint: "Light mode, dark mode, or both?"
      - id: "font_preferences"
        label: "Font preferences"
        type: "text"
        required: false
        hint: "Any font preferences or constraints?"
    files: []
---

# Brand Visual Identity

## Overview

The **brand-visual** skill defines a distinctive visual identity for the app through
plain-language discovery questions. It extracts palettes from reference URLs, proposes
a complete brand, and upon approval writes the artifacts that every downstream skill
consumes: screen specs, mockups, and implementation setup all read `tokens.json`.

The `tailwind` section in tokens.json provides CSS custom properties that map directly
to any CSS/Tailwind-based theming system — downstream skills use these without
re-inventing color names.

## When to Use

- Project brief is approved but `_concept/1_discovery/2_brand/` is empty or incomplete
- User says "brand", "colors", "fonts", "visual identity", "design tokens", "make it look good"
- User wants to change or re-evaluate the visual brand

## When NOT to Use

- Project brief has not been written or approved yet — run `overview` first
- User wants to define brand voice/copy — use `brand-behavioral`
- User wants to tweak existing tokens — edit the file directly

## Prerequisites

**REQUIRED BACKGROUND:** Read `dev-shared/contracts/concept_structure.md` and
`dev-shared/contracts/frontmatter.md` before proceeding.

**Hard gate:** `_concept/1_discovery/1_overview/brief.md` must exist.

If the gate fails, stop:
> "No project brief found. Run `overview` first."

## Context Budget

| Action | Path | Required |
|---|---|---|
| Must read | `_concept/1_discovery/1_overview/brief.md` | Yes |
| Check if present | `_concept/_grounding/general/design_inspiration.md` | No (visual references) |
| Check if present | `_concept/_grounding/general/colors_fonts.md` | No (palette research) |
| Check if present | `_concept/_grounding/brand-visual/user_input.json` | No (pre-collected inputs) |
| Never load | `_concept/3_blueprint/3_datamodel/` | — |
| Never load | Source code | — |

## Standalone Mode

**Gate check:** `_concept/1_discovery/1_overview/brief.md` must exist.
**On completion:** Present summary and suggest next steps (screens, mock, brand-behavioral).

---

ROLE  Brand Identity Designer — discovers aesthetic direction through questions,
      extracts from reference URLs, proposes brand identity, and writes
      `_concept/1_discovery/2_brand/` artifacts upon approval.

READS
  _concept/1_discovery/1_overview/brief.md              — app name, audience, problem, comparables
  ? _concept/_grounding/general/design_inspiration.md  — color approaches, typography trends
  ? _concept/_grounding/general/colors_fonts.md        — palette and typography research findings
  ? _concept/_grounding/brand-visual/user_input.json   — pre-collected user inputs

WRITES
  _concept/1_discovery/2_brand/identity.md             — full brand guide (aesthetic, colors, typography, atmosphere)
  _concept/1_discovery/2_brand/tokens.json             — machine-readable design tokens + CSS custom properties
  _concept/1_discovery/2_brand/brandbook.html          — self-contained visual brand reference (open in browser)
  _concept/1_discovery/2_brand/references/             — screenshots from user-provided reference URLs

REFERENCES
  dev-shared/contracts/concept_structure.md      — valid _concept/ paths
  dev-shared/contracts/frontmatter.md            — brand frontmatter fields
  references/design_philosophy.md                — aesthetic principles, boldness guidelines
  references/discovery_questions.md              — interview questions and presentation templates
  references/tokens_schema.md                    — tokens.json structure and output templates

REQUIRES
  soft: browser (reference URL extraction deferred without it)

MUST  produce a brand that is visually distinct and intentionally crafted for this specific app
MUST  include all fields downstream skills expect in tokens.json (see references/tokens_schema.md)
MUST  include tailwind section with CSS custom properties in tokens.json
MUST  include brandbook.html — self-contained visual reference
MUST  justify typography choices for the aesthetic direction
MUST  define color usage rules — not just hex values but when to use each
MUST  state the "memorable element" — what makes this app unforgettable visually
NEVER produce generic brand output (primary blue, gray secondary, Inter font)
NEVER write tokens.json without discussing aesthetic direction first
NEVER ignore reference URLs — always extract and analyze when provided
NEVER produce cookie-cutter brand that could belong to any app

EMIT  [brand-visual] started run_id=<uuid>

STEP 1: Read context
  - Read brief.md for app name, audience, problem, comparables
  IF _grounding/general/design_inspiration.md exists
    - Read for color approaches, typography trends, visual references
  IF _grounding/general/colors_fonts.md exists
    - Read for palette and typography research findings
  IF _grounding/brand-visual/user_input.json exists
    - Load pre-collected inputs, skip those questions in STEP 2

STEP 2: Discover aesthetic direction
  - Ask questions ONE AT A TIME, building on each answer
  - See references/discovery_questions.md for the six questions and presentation templates
  - Q1: Reference URL  Q2: Feeling/mood spectrum  Q3: Calibration extremes
  - Q4: Existing brand assets  Q5: Light/dark/both  Q6: Font preferences
  MUST present a rich mood spectrum for Q2 — not a simple dropdown
  MUST present two opposing examples for Q3, relevant to the app type
  - Skip questions already answered in user_input.json

STEP 3: Extract from reference URLs
  IF user provides a URL
    - Open URL with browser tool and take screenshot
    - Save screenshot to _concept/1_discovery/2_brand/references/
    - Analyze: dominant colors, typography, layout density, elevation, border radius, mood
    - Present findings using the URL extraction template (see references/discovery_questions.md)
    - Ask: use as-is, adapt, or go in a different direction?
  EMIT  [brand-visual] checkpoint phase=aesthetic_direction mood=<mood> reference_urls=<N>

STEP 4: Compose brand proposal
  - Synthesize all input into a complete brand proposal
  - Present using the Brand Proposal Presentation Template (see references/discovery_questions.md):
    > "Here's the brand identity I'm proposing for [App Name]:
    > [aesthetic, colors with purpose, typography with rationale, details, memorable element]
    > Approve this, or tell me what to change?"
  MUST include a stated "memorable element" — what makes this app unforgettable visually
  MUST justify typography choices for the aesthetic direction
  MUST define color usage rules — not just hex values but when to use each
  EMIT  [brand-visual] checkpoint phase=brand_composed colors=<N> fonts=heading=<H>,body=<B>

CHECKPOINT brand_approved
  UNTIL user explicitly approves
    - Accept changes, re-present updated proposal
    - On approval: proceed to STEP 5

STEP 5: Write artifacts
  $ mkdir -p _concept/1_discovery/2_brand/references

  OUTPUT _concept/1_discovery/2_brand/identity.md
    ---
    mood: "<aesthetic-description>"
    mode: <light|dark|both>
    last_updated: <YYYY-MM-DD>
    ---
    <brand guide: aesthetic direction + reasoning, color usage rules, typography hierarchy,
     spacing system, elevation, atmosphere, tone of voice for UI text, memorable element, do's/don'ts>

  OUTPUT _concept/1_discovery/2_brand/tokens.json
    See references/tokens_schema.md for the complete required structure.
    Required top-level keys: colors, fonts, radius, mode, spacing_base, shadows, atmosphere, tailwind
    Required color keys: primary, secondary, accent, background, surface, text, text_muted, border, error, success, warning
    Required tailwind keys: --color-primary, --color-primary-foreground, --color-secondary,
      --color-background, --color-surface, --color-foreground, --color-muted, --color-border,
      --color-destructive, --color-success, --color-warning, --radius

  OUTPUT _concept/1_discovery/2_brand/brandbook.html
    Self-contained HTML (no external JS frameworks, Google Fonts links allowed).
    Must include: hero section, color palette swatches, typography showcase, spacing/radius
    examples, elevation/shadows, atmosphere demonstration, component previews (buttons, card,
    input, nav), do's and don'ts. The brandbook uses its own brand tokens — it IS the brand.
    Total file size under 50KB. Responsive. Print-friendly.

EMIT  [brand-visual] checkpoint phase=artifacts_written artifacts=identity.md,tokens.json,brandbook.html

STEP 6: Present summary and hand off
  > "Brand identity written to `_concept/1_discovery/2_brand/`:
  >
  > - **identity.md** — full brand guide ([aesthetic summary])
  > - **tokens.json** — design tokens ([N] colors, fonts: [heading]/[body])
  > - **brandbook.html** — visual brand reference (open in browser)
  > - **references/** — [N] screenshots
  >
  > **Key decisions:** [1-2 line summary of the distinctive choices]
  >
  > To adjust: tell me what to change and I'll update the files.
  >
  > Next steps:
  > - Run `screens` to spec UI screens (now colors and fonts are defined)
  > - Run `brand-behavioral` to define tone of voice and copy guidelines
  > - Run `concept-orchestrator` to continue the full pipeline"

EMIT  [brand-visual] completed run_id=<uuid> tech_stack_skill=n/a additional_artifacts=3

CHECKLIST
  - [ ] Aesthetic direction discussed and agreed with user
  - [ ] Brand has a stated memorable element
  - [ ] Typography choices justified for aesthetic direction
  - [ ] Color usage rules defined (not just hex values)
  - [ ] tokens.json includes all required sections (colors, fonts, radius, mode, shadows, atmosphere, tailwind)
  - [ ] identity.md has correct frontmatter (mood, mode, last_updated) — no status field
  - [ ] brandbook.html is self-contained (no external JS frameworks)
  - [ ] Reference URLs extracted and screenshots saved (if provided)
  - [ ] User has explicitly approved the brand proposal

---

## Design Philosophy

**Before producing any output, commit to a BOLD aesthetic direction.**
See `references/design_philosophy.md` for full guidelines.

**FORBIDDEN:** Generic palettes (primary blue, gray secondary, Inter font).
Every brand must be visually distinct and intentionally crafted for this specific app.

## Common Mistakes

| Mistake | What to do instead |
|---|---|
| Generic blue primary + gray secondary + Inter font | FORBIDDEN. Every brand must be visually distinct and crafted for this specific app. |
| Writing tokens.json without discussing direction first | Always present the brand proposal and get approval before writing files. |
| Skipping brandbook.html | Required output — fastest way for stakeholders to evaluate the brand. |
| Using a CDN framework in brandbook.html | Must be self-contained. Only Google Fonts links allowed. Inline all CSS. |
| Ignoring reference URLs | Always extract and analyze when provided — this is the user's taste anchor. |
| Missing atmosphere or shadows in tokens.json | Downstream skills fall back to bland defaults without these fields. Include all required fields. |
| Writing a data dump instead of a visual brandbook | The brandbook must demonstrate the brand — live component previews, actual colors rendered, typography in use. |

## Integration

- **Called by:** `concept-orchestrator` or standalone (parallel track after overview)
- **Requires:** `_concept/1_discovery/1_overview/brief.md`
- **Feeds into:** `screens`, `mock`, `brand-behavioral` — all read tokens.json
