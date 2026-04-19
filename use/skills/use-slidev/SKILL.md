---
name: "use-slidev"
description: "Build slide decks using Slidev (sli.dev) in Skaile's brand design. Produces a slides.md + style.css that renders as a dark-themed, gradient-accented presentation with Plus Jakarta Sans / Inter / JetBrains Mono typography."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "use-contract"
keywords: [slidev, slides, presentation, deck, sli.dev, markdown]
reads_from: []
writes_to: ["slides.md", "style.css", "package.json"]
---

# Use Slidev

Build presentation decks using [Slidev](https://sli.dev/) in Skaile's brand design.

## Output

The skill produces a self-contained Slidev project:

```
<output-dir>/
  slides.md      <- slide content (Slidev markdown)
  style.css      <- Skaile brand theme
  package.json   <- Slidev dependencies
```

## Setup

The output directory needs `pnpm install` before running. Then:

```bash
pnpm dev        # live preview at localhost:3030
pnpm build      # static HTML export
pnpm export     # PDF export (requires playwright)
```

## Brand Design Tokens

All slides use the Skaile brand identity. NEVER deviate from these tokens.

### Colors

```
Background:      #13151A   (dark blue-black)
Surface:         #1A1C22   (raised surface)
Surface raised:  #23262D   (cards, panels)
Border:          #2E303A   (subtle dividers)
Border strong:   #3D404A   (emphasized dividers)

Text:            #F4F4F5   (primary text - white)
Text muted:      #A1A1AA   (secondary text)
Text dim:        #71717A   (tertiary text)

Primary:         #7300FF   (violet - headings, accents)
Primary bright:  #9B40FF   (lighter violet)
Secondary:       #D8377E   (pink)
Accent:          #FF5D01   (orange)

Brand gradient:  linear-gradient(135deg, #FF5D01 0%, #D8377E 50%, #7300FF 100%)
                 (orange -> pink -> violet, used on key elements)

Error:           #EF4444
Success:         #10B981
Warning:         #F59E0B
```

### Typography

```
Headings:  Plus Jakarta Sans (700, 600)
Body:      Inter (400, 500, 600)
Mono:      JetBrains Mono (400, 500)
```

Loaded from Google Fonts in style.css.

### Visual Principles

- Dark background everywhere. No light slides.
- Headings in white (#F4F4F5), NOT in the brand colors.
- Brand gradient ONLY on highlighted/emphasized words, accent lines, and CTA buttons.
- Body text in muted (#A1A1AA).
- Cards use surface-raised (#23262D) with no visible border (or subtle #2E303A).
- List bullets use violet (#7300FF) arrows.
- Generous whitespace. Slides should breathe.
- No drop shadows, no rounded corners larger than 8px.

## Slide Types

### Cover Slide

```markdown
---
layout: cover
---

<div class="cover">
  <h1>Presentation Title</h1>
  <div class="accent-line"></div>
  <p class="tagline">Your expertise, skaile'd.</p>
  <p class="subtitle">Subtitle or context line</p>
</div>
```

### Content Slide (default)

```markdown
---
---

# Slide Title

Body text goes here in regular paragraphs.

- List items use arrow bullets automatically
- Keep to 3-5 items per slide
```

### Two-Column Slide

```markdown
---
---

# Slide Title

<div class="grid-2">
  <div>

  Left column content.

  </div>
  <div>

  Right column content.

  </div>
</div>
```

### Card Grid

```markdown
---
---

# Slide Title

<div class="grid-3">
  <div class="card">
    <h3>Card Title</h3>
    <p>Card description text.</p>
  </div>
  <div class="card">
    <h3>Card Title</h3>
    <p>Card description text.</p>
  </div>
  <div class="card">
    <h3>Card Title</h3>
    <p>Card description text.</p>
  </div>
</div>
```

### Big Number / Stat

```markdown
---
---

<div class="stat-grid">
  <div class="stat">
    <span class="big-number">48x</span>
    <span class="stat-label">ROI on platform investment</span>
  </div>
  <div class="stat">
    <span class="big-number">36K</span>
    <span class="stat-label">Hours freed per year</span>
  </div>
</div>
```

### Quote Slide

```markdown
---
---

> "The quoted text goes here."

**Speaker Name** -- Title, Company
```

### Dark Emphasis Slide (surface background)

```markdown
---
class: bg-surface
---

# Emphasized Content

Use this for key messages that need visual weight.
```

### Closing / CTA Slide

```markdown
---
layout: cover
---

<div class="cover">
  <h1>Call to Action</h1>
  <div class="accent-line"></div>
  <p class="tagline">Your expertise, skaile'd.</p>
  <p class="cta-contact">contact@skaile.ai</p>
</div>
```

## CSS Utility Classes

Available in all slides:

| Class | Effect |
|-------|--------|
| `.grid-2` | 2-column grid |
| `.grid-3` | 3-column grid |
| `.grid-4` | 4-column grid |
| `.card` | Raised surface card with padding |
| `.card-accent` | Card with violet left border |
| `.big-number` | Large mono number (accent colored) |
| `.badge` | Small uppercase label |
| `.badge-violet` | Violet badge |
| `.badge-orange` | Orange badge |
| `.badge-pink` | Pink badge |
| `.accent-line` | Gradient horizontal rule |
| `.text-gradient` | Brand gradient on text |
| `.text-muted` | Muted text color |
| `.text-dim` | Dim text color |
| `.text-violet` | Violet text |
| `.text-orange` | Orange text |
| `.bg-surface` | Surface background on full slide |
| `.check-item` | Checkmark list item |
| `.phase-box` | Pipeline phase indicator |
| `.stat` | Stat block (number + label) |
| `.stat-grid` | Grid for stat blocks |
| `.cover` | Cover slide layout |
| `.workspace` | 3-panel workspace mockup |

## Build Process

STEP 1: Plan the deck
  - Identify the audience, key message, and narrative arc
  - Plan slide count (aim for 1 idea per slide, 10-20 slides total)
  - Map the emotional journey (see narrative_arcs.md if available)

STEP 2: Write slides.md
  - Use Slidev markdown format (slides separated by `---`)
  - Apply the slide types above
  - Keep text minimal - slides are visual aids, not documents
  - Use HTML+CSS classes for layouts, not complex markdown

STEP 3: Generate style.css
  - Copy the reference style.css below verbatim
  - Do NOT modify the brand tokens
  - Add custom styles AFTER the base theme if needed

STEP 4: Generate package.json
  - Use the reference package.json below

STEP 5: Verify
  - Ensure all slides use dark backgrounds
  - Ensure headings are white, not colored
  - Ensure brand gradient is used sparingly (highlights only)
  - Ensure fonts are correct (Plus Jakarta Sans, Inter, JetBrains Mono)

## Reference Files

### package.json

```json
{
  "devDependencies": {
    "@slidev/cli": "^52.14.1",
    "@slidev/theme-default": "^0.25.0",
    "@slidev/types": "^52.14.1"
  },
  "pnpm": {
    "onlyBuiltDependencies": ["esbuild"]
  },
  "scripts": {
    "dev": "slidev",
    "build": "slidev build --base ./",
    "export": "slidev export"
  }
}
```

### style.css

```css
/* Skaile Brand Theme for Slidev */
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Inter:wght@300;400;500;600&family=JetBrains+Mono:wght@400;500&display=swap');

:root {
  --sk-bg: #13151A;
  --sk-surface: #1A1C22;
  --sk-surface-raised: #23262D;
  --sk-border: #2E303A;
  --sk-border-strong: #3D404A;

  --sk-text: #F4F4F5;
  --sk-text-muted: #A1A1AA;
  --sk-text-dim: #71717A;

  --sk-primary: #7300FF;
  --sk-primary-bright: #9B40FF;
  --sk-secondary: #D8377E;
  --sk-accent: #FF5D01;

  --sk-error: #EF4444;
  --sk-success: #10B981;
  --sk-warning: #F59E0B;

  --sk-gradient: linear-gradient(135deg, #FF5D01 0%, #D8377E 50%, #7300FF 100%);

  --slidev-theme-primary: var(--sk-primary);
}

/* Base slide */
.slidev-layout {
  font-family: 'Inter', system-ui, sans-serif;
  color: var(--sk-text);
  background: var(--sk-bg);
  padding: 2.5rem 3rem;
}

/* Headings */
h1, h2, h3 {
  font-family: 'Plus Jakarta Sans', system-ui, sans-serif;
  color: var(--sk-text);
  font-weight: 700;
  letter-spacing: -0.02em;
}
h1 { font-size: 2.5rem; line-height: 1.15; margin-bottom: 1rem; }
h2 { font-size: 1.8rem; line-height: 1.2; margin-bottom: 0.75rem; }
h3 { font-size: 1.3rem; line-height: 1.3; }

/* Body */
p, li {
  font-size: 1.05rem;
  line-height: 1.6;
  color: var(--sk-text-muted);
}

/* Text utilities */
.text-muted { color: var(--sk-text-muted); }
.text-dim { color: var(--sk-text-dim); }
.text-violet { color: var(--sk-primary); }
.text-orange { color: var(--sk-accent); }
.text-gradient {
  background: var(--sk-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* Backgrounds */
.bg-surface { background: var(--sk-surface) !important; }

/* Lists */
ul { list-style: none; padding-left: 0; }
ul li::before {
  content: "\2192";
  color: var(--sk-primary);
  font-weight: 700;
  margin-right: 0.5rem;
}

/* Accent line (gradient) */
.accent-line {
  height: 3px;
  background: var(--sk-gradient);
  border-radius: 2px;
  width: 120px;
  margin: 1.2rem 0;
}

/* Cards */
.card {
  background: var(--sk-surface-raised);
  border: 1px solid var(--sk-border);
  border-radius: 8px;
  padding: 1.25rem;
}
.card h3 { font-size: 1rem; margin-bottom: 0.4rem; }
.card p { font-size: 0.9rem; }
.card-accent { border-left: 3px solid var(--sk-primary); }

/* Grids */
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1.5rem; }
.grid-4 { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 1rem; }

/* Big number */
.big-number {
  font-family: 'JetBrains Mono', monospace;
  font-size: 4rem;
  font-weight: 700;
  background: var(--sk-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
}

/* Stats */
.stat { text-align: center; }
.stat-label {
  display: block;
  font-size: 0.9rem;
  color: var(--sk-text-muted);
  margin-top: 0.5rem;
}
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 2rem;
  align-items: center;
  justify-items: center;
  margin-top: 2rem;
}

/* Badges */
.badge {
  display: inline-block;
  padding: 0.2rem 0.6rem;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.badge-violet { background: rgba(115, 0, 255, 0.2); color: var(--sk-primary-bright); }
.badge-orange { background: rgba(255, 93, 1, 0.2); color: var(--sk-accent); }
.badge-pink { background: rgba(216, 55, 126, 0.2); color: var(--sk-secondary); }

/* Phase box */
.phase-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 600;
  font-size: 0.85rem;
  background: var(--sk-surface-raised);
  border: 1.5px solid var(--sk-primary);
  color: var(--sk-text);
}

/* Check items */
.check-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  margin: 0.5rem 0;
}
.check-icon {
  color: var(--sk-success);
  font-weight: 700;
  font-size: 1.2rem;
  flex-shrink: 0;
}

/* Cover layout */
.cover {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
}
.cover h1 {
  font-size: 3.5rem;
  letter-spacing: -0.03em;
}
.cover .tagline {
  font-size: 1.4rem;
  color: var(--sk-text-muted);
  font-family: 'Plus Jakarta Sans', sans-serif;
}
.cover .subtitle {
  font-size: 1rem;
  color: var(--sk-text-dim);
  margin-top: 1rem;
}
.cover .cta-contact {
  font-size: 1.1rem;
  color: var(--sk-primary-bright);
  margin-top: 1.5rem;
}

/* Workspace mockup */
.workspace {
  display: grid;
  grid-template-columns: 280px 1fr 220px;
  gap: 2px;
  background: var(--sk-border);
  border: 1px solid var(--sk-border);
  border-radius: 8px;
  overflow: hidden;
  height: 360px;
}
.workspace > div {
  background: var(--sk-surface);
  padding: 0.75rem;
  overflow: hidden;
}
.workspace .panel-label {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 600;
  font-size: 0.75rem;
  color: var(--sk-primary);
  margin-bottom: 0.5rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* Blockquotes */
blockquote {
  border-left: 4px solid var(--sk-primary);
  padding-left: 1.5rem;
  margin: 1rem 0;
  font-size: 1.2rem;
  font-style: italic;
  color: var(--sk-text-muted);
}

/* Code */
code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85em;
  background: var(--sk-surface-raised);
  padding: 0.15rem 0.4rem;
  border-radius: 4px;
}

/* Slide number */
.slidev-page-number {
  color: var(--sk-text-dim);
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.75rem;
}
```

## Constraints

MUST  use dark backgrounds on ALL slides (--sk-bg or --sk-surface)
MUST  use Plus Jakarta Sans for headings, Inter for body, JetBrains Mono for code
MUST  keep headings white (#F4F4F5), NOT in brand colors
MUST  use the brand gradient sparingly (highlights, accent lines, big numbers)
MUST  copy the reference style.css verbatim as the base theme
NEVER  use light/white slide backgrounds
NEVER  change the brand color tokens
NEVER  use fonts other than the three specified
NEVER  put more than one key idea per slide
