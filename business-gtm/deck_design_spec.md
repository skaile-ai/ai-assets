# Partner Deck: Design Specification

## Brand Tokens Applied to Presentation

### Color Palette

| Role | Color | Hex | Usage in Deck |
|------|-------|-----|---------------|
| Primary | Deep Teal | `#115E59` | Slide titles, key headlines, section dividers, CTA buttons |
| Primary Bright | Teal | `#14B8A6` | Accent highlights, progress indicators, hover states, icon fills |
| Accent | Amber | `#F59E0B` | Callout numbers, "AI" markers, emphasis on key stats, spark moments |
| Background | Warm White | `#FAFAF9` | Slide background (light slides) |
| Surface | White | `#FFFFFF` | Cards, content panels, elevated elements |
| Dark Background | Warm Charcoal | `#1C1917` | Title slide, section dividers, high-impact slides |
| Dark Surface | Stone 800 | `#292524` | Dark slide content panels |
| Text | Near Black | `#1C1917` | Body text on light slides |
| Text Light | Warm White | `#FAFAF9` | Body text on dark slides |
| Text Muted | Stone 500 | `#78716C` | Subtitles, captions, secondary info |
| Border | Stone 200 | `#E7E5E4` | Divider lines, card borders, table lines |
| Success | Green | `#059669` | Checkmarks, "pass" indicators |
| Error | Red | `#DC2626` | "X" marks on competitor slide, limitation callouts |

### Typography

| Role | Font | Weight | Size Range | Usage |
|------|------|--------|------------|-------|
| Slide Title | Plus Jakarta Sans | Bold (700) | 36–48pt | Main headline per slide |
| Subtitle | Plus Jakarta Sans | SemiBold (600) | 20–24pt | Subheadlines, section labels |
| Body | Inter | Regular (400) | 14–18pt | Descriptive text, bullet points |
| Body Emphasis | Inter | Medium (500) | 14–18pt | Bold key phrases within body |
| Caption | Inter | Regular (400) | 11–12pt | Footnotes, source labels, small annotations |
| Data / Code | JetBrains Mono | Regular (400) | 12–14pt | Statistics, technical details, code snippets |

### Slide Layout System

**Grid:**
- 16:9 aspect ratio (widescreen)
- 64px margins on all sides
- 8px base grid for alignment
- Max 2 columns of content per slide (never 3+ content blocks side-by-side except the competitor comparison slide)

**Slide Types:**

#### 1. Title Slide (dark)
- Background: `#1C1917`
- PAXL logo (teal `#14B8A6`) top-left
- Headline: Plus Jakarta Sans Bold 48pt, white `#FAFAF9`
- Tagline: Inter Regular 20pt, muted `#A8A29E`
- Subtle teal accent line (2px) under headline

#### 2. Statement Slide (light)
- Background: `#FAFAF9`
- Single large headline centered: Plus Jakarta Sans Bold 36pt, `#1C1917`
- Optional supporting text below: Inter Regular 18pt, `#78716C`
- Used for: "The Question" (slide 2), "The Missing Piece" (slide 4)

#### 3. Content Slide (light)
- Background: `#FAFAF9`
- Headline top-left: Plus Jakarta Sans SemiBold 28pt, `#115E59`
- Body content: Inter Regular 16pt, `#1C1917`
- Bullet points with teal dot markers (`#14B8A6`)
- Used for: most content slides (5, 6, 8, 9, 10, 13, 15)

#### 4. Comparison Slide (light)
- Background: `#FAFAF9`
- Four equal columns with white `#FFFFFF` card backgrounds
- Card border: `#E7E5E4`
- Card header: Plus Jakarta Sans SemiBold 16pt
- Card body: Inter Regular 14pt
- Limitation callout: red `#DC2626` text or badge
- Used for: competitor comparison (slide 3)

#### 5. Hero Visual Slide (light or dark)
- Background: `#FAFAF9` or `#1C1917`
- Centered screenshot/mockup with subtle shadow
- Minimal text — let the visual speak
- Caption below: Inter Regular 14pt
- Used for: workspace screenshot (slide 7)

#### 6. Proof Slide (light)
- Background: `#FAFAF9`
- Large callout number in amber `#F59E0B` (e.g., "10") using JetBrains Mono Bold 72pt
- Supporting text: Plus Jakarta Sans SemiBold 24pt
- Logo row with enterprise references below
- Used for: proven technology (slide 11)

#### 7. CTA / Ask Slide (dark)
- Background: `#1C1917`
- Headline: Plus Jakarta Sans Bold 36pt, white
- Contact info: Inter Regular 18pt, `#A8A29E`
- PAXL logo
- Amber accent element (line or dot)
- Used for: closing slide (slide 16)

### Visual Elements

**Icons:**
- Style: Lucide icons (consistent with PostXL UI library)
- Color: `#14B8A6` (teal) on light backgrounds, `#FAFAF9` (white) on dark
- Size: 24–32px in content, 48px for featured icons

**Dividers:**
- Thin horizontal line: `#E7E5E4` (1px) on light, `#44403C` on dark
- Section transition: full-bleed teal `#115E59` bar (4px height)

**Cards / Panels:**
- Background: `#FFFFFF`
- Border: `#E7E5E4` (1px)
- Border radius: 8px (matching `radius_lg` token)
- Shadow: `0 1px 2px rgba(28, 25, 23, 0.05)` (matching `shadows.sm`)

**Charts / Diagrams:**
- Primary data color: `#115E59`
- Secondary: `#14B8A6`
- Accent: `#F59E0B`
- Grid lines: `#E7E5E4`
- Text labels: `#78716C`

**The AI Accent (Memorable Element):**
- Whenever "AI" is mentioned or shown, use amber `#F59E0B` as the accent
- AI-related icons get an amber dot or amber background
- The chat panel mockup should show the amber left-border on AI messages
- This reinforces the brand identity: teal = platform, amber = AI

### Slide-by-Slide Mode Assignment

| Slide | Type | Mode |
|-------|------|------|
| 1. Title | Title | Dark |
| 2. The Question | Statement | Light |
| 3. Current Answers Fall Short | Comparison | Light |
| 4. The Missing Piece | Statement | Light |
| 5. Introducing PAXL | Content | Light |
| 6. How a Consultant Uses PAXL | Content | Light |
| 7. The Workspace | Hero Visual | Light |
| 8. Concept → App | Content | Light |
| 9. Enterprise-Grade Output | Content (split) | Light |
| 10. IT Loves This | Content (icons) | Light |
| 11. Proven Technology | Proof | Light |
| 12. The Team | Content | Light |
| 13. The Partner Opportunity | Content | Light |
| 14. The Vision | Content (diagram) | Light |
| 15. Why Now | Content (checklist) | Light |
| 16. The Ask | CTA | Dark |

### Do's and Don'ts

**Do:**
- Use generous whitespace — slides should breathe (consulting deck feel)
- Limit text to 6 lines of body max per slide
- Use the amber accent sparingly — it should pop when it appears
- Keep diagrams simple with clear labels
- Use real screenshots of PostXL apps (SteerEx) where possible

**Don't:**
- Use gradients on backgrounds (brand rule)
- Mix teal and amber in the same element
- Use more than 2 font weights per slide
- Put walls of text — this is a presentation, not a document
- Use stock photos — use diagrams, screenshots, and clean typography instead
