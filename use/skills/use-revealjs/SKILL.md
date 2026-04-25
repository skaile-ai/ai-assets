---
name: "use-revealjs"
description: "Build slide decks using Reveal.js in Skaile's brand design. Produces an index.html + skaile-theme.css that renders as a dark-themed, gradient-accented presentation with Plus Jakarta Sans / Inter / JetBrains Mono typography."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "use-contract"
keywords: [revealjs, reveal.js, slides, presentation, deck, html]
reads_from: []
writes_to: ["index.html", "skaile-theme.css"]
---

# Use Reveal.js

Build presentation decks using [Reveal.js](https://revealjs.com/) in Skaile's brand design.

## Output

The skill produces a self-contained Reveal.js presentation:

```
<output-dir>/
  index.html         <- slides (single HTML file with embedded Reveal.js from CDN)
  skaile-theme.css   <- Skaile brand theme
```

The output is a single HTML file that loads Reveal.js from CDN. No build step, no npm install - just open index.html in a browser or serve it from any static host.

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

Loaded from Google Fonts in the HTML head.

### Visual Principles

- Dark background everywhere. No light slides.
- Headings in white (#F4F4F5), NOT in the brand colors.
- Brand gradient ONLY on highlighted/emphasized words, accent lines, and CTA buttons.
- Body text in muted (#A1A1AA).
- Cards use surface-raised (#23262D) with no visible border (or subtle #2E303A).
- List markers use violet (#7300FF) arrows.
- Generous whitespace. Slides should breathe.
- No drop shadows, no rounded corners larger than 8px.

## Slide Structure

Reveal.js slides use `<section>` elements. Vertical slides nest `<section>` inside `<section>`.

### Cover Slide

```html
<section class="cover">
  <h1>Presentation Title</h1>
  <div class="accent-line"></div>
  <p class="tagline">Your expertise, skaile'd.</p>
  <p class="subtitle">Subtitle or context line</p>
</section>
```

### Content Slide (default)

```html
<section>
  <h2>Slide Title</h2>
  <p>Body text goes here.</p>
  <ul>
    <li>List items use arrow markers automatically</li>
    <li>Keep to 3-5 items per slide</li>
  </ul>
</section>
```

### Two-Column Slide

```html
<section>
  <h2>Slide Title</h2>
  <div class="grid-2">
    <div>
      <p>Left column content.</p>
    </div>
    <div>
      <p>Right column content.</p>
    </div>
  </div>
</section>
```

### Card Grid

```html
<section>
  <h2>Slide Title</h2>
  <div class="grid-3">
    <div class="card">
      <h3>Card Title</h3>
      <p>Card description.</p>
    </div>
    <div class="card">
      <h3>Card Title</h3>
      <p>Card description.</p>
    </div>
    <div class="card">
      <h3>Card Title</h3>
      <p>Card description.</p>
    </div>
  </div>
</section>
```

### Big Number / Stat

```html
<section>
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
</section>
```

### Quote Slide

```html
<section>
  <blockquote>"The quoted text goes here."</blockquote>
  <p><strong>Speaker Name</strong> -- Title, Company</p>
</section>
```

### Dark Emphasis Slide (surface background)

```html
<section class="bg-surface">
  <h2>Emphasized Content</h2>
  <p>Use for key messages that need visual weight.</p>
</section>
```

### Closing / CTA Slide

```html
<section class="cover">
  <h1>Call to Action</h1>
  <div class="accent-line"></div>
  <p class="tagline">Your expertise, skaile'd.</p>
  <p class="cta-contact">contact@skaile.ai</p>
</section>
```

### Fragment Animations

Reveal.js supports step-by-step reveals with the `fragment` class:

```html
<section>
  <h2>Progressive Reveal</h2>
  <ul>
    <li class="fragment">First point appears</li>
    <li class="fragment">Then second</li>
    <li class="fragment">Then third</li>
  </ul>
</section>
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
| `.big-number` | Large mono number (gradient colored) |
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
  - Map the emotional journey

STEP 2: Write index.html
  - Use the reference index.html skeleton below
  - Add slides as `<section>` elements inside `<div class="slides">`
  - Apply the slide types and CSS classes above
  - Keep text minimal - slides are visual aids, not documents

STEP 3: Generate skaile-theme.css
  - Copy the reference CSS below verbatim
  - Do NOT modify the brand tokens
  - Add custom styles AFTER the base theme if needed

STEP 4: Verify
  - Open index.html in a browser to preview
  - Ensure all slides use dark backgrounds
  - Ensure headings are white, not colored
  - Ensure brand gradient is used sparingly

## Reference Files

### index.html (skeleton)

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>DECK TITLE - Skaile</title>
  <link rel="preconnect" href="https://fonts.googleapis.com" />
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
  <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&family=Inter:wght@300;400;500;600&family=JetBrains+Mono:wght@400;500&display=swap" rel="stylesheet" />
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/reveal.js@5/dist/reveal.css" />
  <link rel="stylesheet" href="skaile-theme.css" />
</head>
<body>
  <div class="reveal">
    <div class="slides">

      <!-- SLIDES GO HERE -->

    </div>
  </div>
  <script src="https://cdn.jsdelivr.net/npm/reveal.js@5/dist/reveal.js"></script>
  <script>
    Reveal.initialize({
      hash: true,
      slideNumber: true,
      transition: 'slide',
      width: 1280,
      height: 720,
      margin: 0,
      center: false,
    });
  </script>
</body>
</html>
```

### skaile-theme.css

```css
/* Skaile Brand Theme for Reveal.js */

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
}

/* ── Base ────────────────────────────────────── */

.reveal {
  font-family: 'Inter', system-ui, sans-serif;
  font-size: 32px;
  color: var(--sk-text);
}

.reveal .slides section {
  background: var(--sk-bg);
  padding: 2.5rem 3rem;
  text-align: left;
  box-sizing: border-box;
  height: 100%;
}

/* ── Typography ──────────────────────────────── */

.reveal h1, .reveal h2, .reveal h3 {
  font-family: 'Plus Jakarta Sans', system-ui, sans-serif;
  color: var(--sk-text);
  font-weight: 700;
  letter-spacing: -0.02em;
  text-transform: none;
  margin: 0 0 0.75rem 0;
}
.reveal h1 { font-size: 2.2em; line-height: 1.15; }
.reveal h2 { font-size: 1.4em; line-height: 1.2; }
.reveal h3 { font-size: 1em; line-height: 1.3; }

.reveal p, .reveal li {
  font-size: 0.65em;
  line-height: 1.6;
  color: var(--sk-text-muted);
}

.reveal strong { color: var(--sk-text); }

/* ── Text utilities ──────────────────────────── */

.text-muted { color: var(--sk-text-muted) !important; }
.text-dim { color: var(--sk-text-dim) !important; }
.text-violet { color: var(--sk-primary) !important; }
.text-orange { color: var(--sk-accent) !important; }
.text-gradient {
  background: var(--sk-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* ── Backgrounds ─────────────────────────────── */

.reveal .slides section.bg-surface {
  background: var(--sk-surface);
}

/* ── Lists ───────────────────────────────────── */

.reveal ul { list-style: none; padding-left: 0; margin: 0.5em 0; }
.reveal ul li { margin: 0.4em 0; }
.reveal ul li::before {
  content: "\2192";
  color: var(--sk-primary);
  font-weight: 700;
  margin-right: 0.5em;
}
.reveal ol { color: var(--sk-text-muted); padding-left: 1.5em; }

/* ── Accent line (gradient) ──────────────────── */

.accent-line {
  height: 3px;
  background: var(--sk-gradient);
  border-radius: 2px;
  width: 120px;
  margin: 1rem 0;
}
.cover .accent-line { margin: 1rem auto; }

/* ── Cards ───────────────────────────────────── */

.card {
  background: var(--sk-surface-raised);
  border: 1px solid var(--sk-border);
  border-radius: 8px;
  padding: 1.25em;
}
.card h3 {
  font-size: 0.7em;
  margin-bottom: 0.3em;
}
.card p {
  font-size: 0.55em;
}
.card-accent {
  border-left: 3px solid var(--sk-primary);
}

/* ── Grids ───────────────────────────────────── */

.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.grid-3 { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 1.5rem; }
.grid-4 { display: grid; grid-template-columns: 1fr 1fr 1fr 1fr; gap: 1rem; }

/* ── Big number ──────────────────────────────── */

.big-number {
  font-family: 'JetBrains Mono', monospace;
  font-size: 3em;
  font-weight: 700;
  background: var(--sk-gradient);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  line-height: 1;
  display: block;
}

/* ── Stats ───────────────────────────────────── */

.stat { text-align: center; }
.stat-label {
  display: block;
  font-size: 0.55em;
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

/* ── Badges ──────────────────────────────────── */

.badge {
  display: inline-block;
  padding: 0.15em 0.5em;
  border-radius: 4px;
  font-size: 0.45em;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.badge-violet { background: rgba(115, 0, 255, 0.2); color: var(--sk-primary-bright); }
.badge-orange { background: rgba(255, 93, 1, 0.2); color: var(--sk-accent); }
.badge-pink { background: rgba(216, 55, 126, 0.2); color: var(--sk-secondary); }

/* ── Phase box ───────────────────────────────── */

.phase-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 0.4em 0.8em;
  border-radius: 6px;
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 600;
  font-size: 0.55em;
  background: var(--sk-surface-raised);
  border: 1.5px solid var(--sk-primary);
  color: var(--sk-text);
}

/* ── Check items ─────────────────────────────── */

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 0.5em;
  margin: 0.4em 0;
  font-size: 0.6em;
  color: var(--sk-text-muted);
}
.check-icon {
  color: var(--sk-success);
  font-weight: 700;
  font-size: 1.2em;
  flex-shrink: 0;
}

/* ── Cover layout ────────────────────────────── */

.reveal .slides section.cover {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}
.cover h1 {
  font-size: 2.5em;
  letter-spacing: -0.03em;
}
.cover .tagline {
  font-size: 0.8em;
  color: var(--sk-text-muted);
  font-family: 'Plus Jakarta Sans', sans-serif;
}
.cover .subtitle {
  font-size: 0.6em;
  color: var(--sk-text-dim);
  margin-top: 0.8em;
}
.cover .cta-contact {
  font-size: 0.65em;
  color: var(--sk-primary-bright);
  margin-top: 1em;
}

/* ── Workspace mockup ────────────────────────── */

.workspace {
  display: grid;
  grid-template-columns: 240px 1fr 200px;
  gap: 2px;
  background: var(--sk-border);
  border: 1px solid var(--sk-border);
  border-radius: 8px;
  overflow: hidden;
  height: 320px;
  font-size: 0.5em;
}
.workspace > div {
  background: var(--sk-surface);
  padding: 0.75em;
  overflow: hidden;
}
.workspace .panel-label {
  font-family: 'Plus Jakarta Sans', sans-serif;
  font-weight: 600;
  font-size: 0.7em;
  color: var(--sk-primary);
  margin-bottom: 0.5em;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ── Blockquotes ─────────────────────────────── */

.reveal blockquote {
  border-left: 4px solid var(--sk-primary);
  padding: 0.5em 0 0.5em 1em;
  margin: 1em 0;
  font-size: 0.8em;
  font-style: italic;
  color: var(--sk-text-muted);
  background: none;
  box-shadow: none;
  width: auto;
}

/* ── Code ────────────────────────────────────── */

.reveal code {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.85em;
  background: var(--sk-surface-raised);
  padding: 0.1em 0.3em;
  border-radius: 4px;
  color: var(--sk-text);
}

.reveal pre code {
  padding: 1em;
  border-radius: 8px;
  font-size: 0.5em;
  line-height: 1.5;
}

/* ── Slide number ────────────────────────────── */

.reveal .slide-number {
  color: var(--sk-text-dim);
  font-family: 'JetBrains Mono', monospace;
  font-size: 14px;
  background: none;
}

/* ── Progress bar ────────────────────────────── */

.reveal .progress span {
  background: var(--sk-gradient);
}

/* ── Fragment transitions ────────────────────── */

.reveal .fragment {
  transition: all 0.3s ease;
}
.reveal .fragment.visible {
  opacity: 1;
}
```

## Constraints

MUST  use dark backgrounds on ALL slides (--sk-bg or --sk-surface)
MUST  use Plus Jakarta Sans for headings, Inter for body, JetBrains Mono for code
MUST  keep headings white (#F4F4F5), NOT in brand colors
MUST  use the brand gradient sparingly (highlights, accent lines, big numbers)
MUST  copy the reference skaile-theme.css verbatim as the base theme
MUST  load Reveal.js from CDN (no local install needed)
MUST  set `center: false` in Reveal.initialize (left-aligned slides)
NEVER  use light/white slide backgrounds
NEVER  change the brand color tokens
NEVER  use fonts other than the three specified
NEVER  use Reveal.js built-in themes (moon, black, etc.) - use skaile-theme.css only
NEVER  put more than one key idea per slide
