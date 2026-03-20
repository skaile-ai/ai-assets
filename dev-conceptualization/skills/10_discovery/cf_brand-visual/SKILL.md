---
name: brand-visual
description: "Use when the project brief is approved but _concept/04_brand/ is empty. Also when user says 'brand', 'colors', 'visual identity', 'design tokens', 'make it look good'."
keywords: [brand, colors, fonts, identity, tokens, design, visual, palette, typography, atmosphere]
user_inputs:
  dialog:
    - id: reference_urls
      label: "Reference websites"
      type: text
      required: false
      hint: "Show a website you love — I'll extract palette and style"
    - id: mood
      label: "Desired feeling"
      type: text
      required: true
      hint: "What feeling should the app give? calm/bold/professional/playful..."
    - id: light_dark
      label: "Color mode"
      type: select
      options: ["light", "dark", "both"]
      required: true
      default: "dark"
      hint: "Light mode, dark mode, or both?"
    - id: font_preferences
      label: "Font preferences"
      type: text
      required: false
      hint: "Any font preferences or constraints?"
  files:
    - "01_project/brief.md"
---

# Brand Visual Identity

## Overview

The brand visual identity skill defines a distinctive visual identity for the app.
It thinks in terms of aesthetic direction, spatial composition, typography as a design
statement, and atmosphere. It produces artifacts that every downstream skill consumes:
design mockups, screen specs, and the visual editor all read from its output.

**This skill writes all artifacts directly — no approval gates.** It gathers inputs,
composes the brand, writes the files, and presents a summary. The user can request
changes afterward.

## When to Use
- Project brief is approved but `_concept/04_brand/` is empty or incomplete
- User asks about brand, colors, fonts, visual style, design tokens
- User says "brand", "colors", "visual identity", "make it look good"

## When NOT to Use
- Project brief has not been written or approved yet (run overview first)
- User wants to define brand voice/copy (use brand_behavioral)
- User wants to tweak existing tokens (edit the file directly)

## Prerequisites

**HARD-GATE (from pipeline.json):**
- `_concept/01_project/brief.md` must exist

If the gate fails, stop immediately:

> "No approved project brief found. Run `cf_concept_overview` first."

## Standalone Mode
This skill can be invoked directly without the orchestrator.
**Gate check:** `_concept/01_project/brief.md` must exist
**If gates fail:** Run `cf_concept_overview` first.
**On completion:** Present summary, then orchestrator suggests next steps.

## Context Budget

**Must read:**
- `_concept/01_project/brief.md`
- `cf__shared/concept_structure.md`
- `cf__shared/frontmatter.md`

**Optional:**
- `_concept/_research/general/design_inspiration.md` — visual references from research
- `_concept/_research/general/colors_fonts.md` — palette and typography research findings
- `_concept/_research/brand_visual/user_input.json` — pre-collected user inputs

**Never load:**
- `_concept/06_datamodel/`
- Source code

## Shared Contracts

Before starting, read:
- `cf__shared/iron_laws.md` — non-negotiable constraints (questions-as-standalone-messages)
- `cf__shared/agent_patterns.md` — communication style, read-context-first, standalone mode

## Design Philosophy

**Before producing any output, commit to a BOLD aesthetic direction.**

- **Tone is a spectrum, not a checkbox.** Don't ask "modern or classic?" Instead,
  explore the full range: brutally minimal, maximalist chaos, retro-futuristic,
  organic/natural, luxury/refined, playful/toy-like, editorial/magazine,
  brutalist/raw, art deco/geometric, soft/pastel, industrial/utilitarian,
  dark and moody, neon-lit, handcrafted, scandinavian clean, swiss precision.

- **Typography is a design statement.** Beautiful, unexpected, characterful fonts.
  Pair a distinctive display font with a refined body font. Never default to
  Inter/Roboto/Arial alone without intentional aesthetic justification.

- **Color carries meaning.** Dominant colors with sharp accents outperform timid,
  evenly-distributed palettes. Commit to a clear palette with purpose — every
  color earns its place.

- **Atmosphere over decoration.** Backgrounds aren't "just white" or "just dark."
  Consider gradient meshes, noise textures, geometric patterns, grain overlays,
  subtle radial gradients. The background sets the entire mood.

- **Differentiation.** What makes this app UNFORGETTABLE visually? What's the one
  thing someone will remember? If the answer is "nothing," the brand isn't done.

**FORBIDDEN:** Generic palettes (primary blue, gray secondary, Inter font).
Every brand must be visually distinct and intentionally crafted for this specific app.

## Workflow

### Step 1: Read Context

Read `_concept/01_project/brief.md`. Understand:
- What the app does
- Who it's for
- What problem it solves
- Comparable products (what to look different from)

Check `_concept/_research/general/design_inspiration.md` and `_concept/_research/general/colors_fonts.md`
for research findings collected during the research phase. Use these to ground
aesthetic direction in real-world evidence.

Check `_concept/_research/brand_visual/user_input.json` for pre-collected user inputs.
If inputs are pre-collected, skip to Step 3.

### Step 2: Gather Inputs

Collect the dialog inputs defined in the frontmatter. Ask the user these questions
**one at a time**, building on each answer:

| # | Question | What it reveals |
|---|----------|----------------|
| 1 | "Show me a website or app you love the look of. (Paste a URL — I'll extract the palette and style.)" | Reference anchor |
| 2 | "What feeling should your app give? Pick from this spectrum or describe your own:" | Emotional direction |
| 3 | "Look at these two extremes. Where does your app sit?" | Calibration |
| 4 | "Do you have existing brand colors, a logo, or a style guide?" | Constraints |
| 5 | "Light mode, dark mode, or both?" | Technical constraint |
| 6 | "Do you have font preferences, or should I suggest something unexpected?" | Typography |

**For question 2**, present a rich spectrum — not a dropdown:

> "What feeling should your app give? Some directions to consider:
>
> - **Calm & trustworthy** — soft colors, generous whitespace, rounded shapes
> - **Bold & energetic** — saturated colors, sharp contrasts, dynamic layout
> - **Professional & precise** — muted palette, tight grid, clear hierarchy
> - **Playful & approachable** — warm colors, rounded fonts, friendly illustrations
> - **Luxury & refined** — dark backgrounds, gold/copper accents, serif typography
> - **Editorial & intellectual** — magazine-like layout, strong typography, monochrome
> - **Futuristic & technical** — dark mode, neon accents, monospace elements
> - **Organic & natural** — earth tones, textured backgrounds, hand-drawn feel
>
> Or describe in your own words — what should people *feel* when they use this?"

**For question 3**, present two opposing examples relevant to the app type:

> "On a scale from A to B, where does your app sit?
>
> **A:** Clean, restrained, Swiss-poster precision
> **B:** Rich, layered, immersive experience
>
> (Or anywhere in between — just tell me.)"

### Step 3: Extract from Reference URLs

When the user provides a URL, use `browser` to:

1. Open the URL and take a screenshot
2. Save screenshot to `_concept/04_brand/references/`
3. Analyze the page for:
   - Dominant colors (background, surface, text, accent)
   - Typography (heading font, body font, sizes, weights)
   - Layout density (spacious vs compact)
   - Elevation style (flat, subtle shadows, heavy depth)
   - Border radius (sharp, slightly rounded, pill-shaped)
   - Overall mood

Present findings to the user:

> "Here's what I extracted from [URL]:
>
> - **Palette:** dark background (#1a1a2e), electric blue accent (#0ea5e9), light text
> - **Typography:** Clash Display for headings, DM Sans for body
> - **Style:** dark, editorial, generous spacing, sharp corners
> - **Mood:** futuristic but approachable
>
> Continuing with brand composition..."

If no URL is provided, continue without extraction.

### Step 4: Compose and Write Immediately

Based on all input, compose the brand identity and **write all artifacts directly**.
Do NOT ask for approval — write the files, then present a summary.

```bash
mkdir -p _concept/04_brand/references
```

**Output: `_concept/04_brand/identity.md`**

```yaml
---
mood: "dark editorial with electric accents"
mode: dark
last_updated: YYYY-MM-DD
---
```

Body contains the full brand guide in natural language:
- Aesthetic direction and reasoning
- Color usage rules (when to use primary vs secondary vs accent)
- Typography hierarchy (H1 size/weight, H2, body, caption, label)
- Spacing system (8px base grid)
- Elevation (shadow depth, glassmorphism rules)
- Atmosphere (background treatment — gradients, textures)
- Tone of voice for UI text (formal/casual, short/descriptive)
- What makes this brand distinctive (the memorable element)
- Do's and don'ts

**Output: `_concept/04_brand/tokens.json`**

Machine-readable design tokens consumed by downstream skills:

```json
{
  "colors": {
    "primary": "#6366f1",
    "secondary": "#0ea5e9",
    "accent": "#f59e0b",
    "background": "#0f172a",
    "surface": "#1e293b",
    "text": "#f8fafc",
    "text_muted": "#94a3b8",
    "border": "#334155",
    "error": "#ef4444",
    "success": "#22c55e",
    "warning": "#f59e0b"
  },
  "fonts": {
    "heading": "Clash Display",
    "body": "DM Sans",
    "mono": "JetBrains Mono"
  },
  "radius": "8px",
  "mode": "dark",
  "spacing_base": "8px",
  "shadows": {
    "sm": "0 1px 2px rgba(0,0,0,0.1)",
    "md": "0 4px 12px rgba(0,0,0,0.15)",
    "lg": "0 10px 40px rgba(0,0,0,0.2)"
  },
  "atmosphere": {
    "type": "radial_gradient",
    "description": "Subtle dark-to-darker radial glow behind main content"
  }
}
```

**Output: `_concept/04_brand/references/`**

Screenshots saved from reference URLs the user provided.

**Output: `_concept/04_brand/brandbook.html`**

A self-contained HTML brandbook that visually showcases the complete brand identity.
This file must be viewable by opening it directly in a browser — no external
dependencies, no build step, no framework. All CSS is inline or in a `<style>` block.

The brandbook **reads values from the composed brand** (same colors, fonts, tokens)
and renders them as a polished visual reference. It includes:

1. **Hero section** — app name, aesthetic tagline, mood description
2. **Color palette** — every color as a swatch with hex, name, and usage rule.
   Background of each swatch IS the color. Include contrast ratios for text colors.
3. **Typography showcase** — heading font at multiple sizes (H1–H4), body font in
   paragraph form, mono font in a code block. Show font names, weights, and sizes.
   Load fonts from Google Fonts via `<link>` (or note if the font isn't on Google Fonts).
4. **Spacing & radius** — visual examples of the spacing grid and border radius
5. **Elevation & shadows** — cards at each shadow level (sm, md, lg) side by side
6. **Atmosphere** — a section whose background demonstrates the atmosphere treatment
   (gradient, texture, etc.) described in the tokens
7. **Component preview** — a few example UI elements styled with the brand tokens:
   - Primary and secondary buttons
   - A card with heading, body text, and a badge
   - An input field with label
   - A simple navigation bar
8. **Do's and Don'ts** — key brand rules rendered visually (green checkmark / red cross)

**Brandbook styling rules:**
- The brandbook itself uses the brand's own tokens — it IS the brand. Background
  color, text color, fonts, radius, shadows all come from the composed tokens.
- Clean, magazine-like layout with generous whitespace
- Responsive (works on desktop and mobile)
- Print-friendly (use `@media print` to hide non-essential chrome)
- Total file size should stay under 50KB (excluding Google Fonts load)

### Step 5: Present Summary

After writing all files, present a brief summary:

> "Brand identity written to `_concept/04_brand/`:
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
> **Next steps:** [which skills are now unblocked]"

## Outputs

| File | Purpose |
|------|---------|
| `_concept/04_brand/identity.md` | Full brand guide in natural language |
| `_concept/04_brand/tokens.json` | Machine-readable design tokens for downstream skills |
| `_concept/04_brand/brandbook.html` | Self-contained visual brand reference (open in browser) |
| `_concept/04_brand/references/` | Screenshots from user-provided reference URLs |

## Common Mistakes

| Rationalization | Reality |
|----------------|---------|
| "Primary blue with gray secondary and Inter font is fine" | FORBIDDEN. Generic palettes are the most common failure mode. Every brand must be visually distinct and intentionally crafted for this specific app. |
| "I'll wait for the user to approve before writing files" | NO. Write files directly after composing. The user can request changes afterward — don't block on approval. |
| "The brandbook is optional, I'll skip it" | The brandbook is a required output. It's the fastest way for stakeholders to evaluate the brand. Always generate it. |
| "I'll use a CDN framework for the brandbook" | The brandbook must be self-contained. No external JS frameworks, no build steps. Only Google Fonts links are allowed as external resources. |
| "Light mode is simpler, I'll default to that" | The user's `light_dark` input is authoritative. If they chose dark, produce dark. If both, produce both mode tokens. |
| "Atmosphere and shadows are nice-to-have" | Downstream skills read `atmosphere` and `shadows` from tokens.json. Missing fields mean design skills fall back to bland defaults. Include all fields. |
| "The brandbook just lists the tokens" | The brandbook must DEMONSTRATE the brand — live component previews, actual colors rendered, typography in use. It's a visual artifact, not a data dump. |

## Research Mode

Research color palettes, typography trends, and design inspiration. Check
`_concept/_research/general/colors_fonts.md` and `_concept/_research/general/design_inspiration.md`
for findings from the research phase. These ground aesthetic choices in real-world
evidence rather than agent assumptions.

## Strict Constraints

- **FORBIDDEN:** Generic palettes (primary blue, gray secondary, Inter font)
- **FORBIDDEN:** Ignoring reference URLs — always extract and analyze when provided
- **FORBIDDEN:** Cookie-cutter brand output that could belong to any app
- **FORBIDDEN:** Asking for approval before writing — write directly, accept changes after
- **REQUIRED:** Every brand must have a stated "memorable element"
- **REQUIRED:** Typography choices must be justified for the aesthetic direction
- **REQUIRED:** Color usage rules (not just hex values — when to use each)
- **REQUIRED:** tokens.json must include all fields the downstream skills expect
- **REQUIRED:** brandbook.html must be generated as a self-contained visual reference

## Integration

- **Called by:** orchestrator or standalone
- **Reads from:** 01_project/, _research/general/design_inspiration.md (optional), _research/general/colors_fonts.md (optional)
- **Writes to:** 04_brand/
- **Consumed by:** `cf_concept_ui_screens`, cf_concept_mock, bootstrap, brand_behavioral

```
[cf_concept_brand_visual] started
  run_id: <uuid>
  reads: 01_project/brief.md

[cf_concept_brand_visual] checkpoint phase=inputs_gathered
  mood: "dark editorial with electric accents"
  reference_urls: 2

[cf_concept_brand_visual] checkpoint phase=artifacts_written
  colors: 7 defined
  fonts: heading=Clash Display, body=DM Sans
  artifacts: identity.md, tokens.json, brandbook.html

[cf_concept_brand_visual] completed
  run_id: <uuid>
  artifacts: 04_brand/identity.md, 04_brand/tokens.json, 04_brand/brandbook.html
  reference_screenshots: 2
```
