# Design Philosophy

Adapted from frontend-design aesthetic principles.

## Core Principle

**Before producing any output, commit to a BOLD aesthetic direction.**

## Guidelines

### Tone is a spectrum, not a checkbox

Don't ask "modern or classic?" Instead, explore the full range:
brutally minimal, maximalist chaos, retro-futuristic, organic/natural,
luxury/refined, playful/toy-like, editorial/magazine, brutalist/raw,
art deco/geometric, soft/pastel, industrial/utilitarian, dark and moody,
neon-lit, handcrafted, scandinavian clean, swiss precision.

### Typography is a design statement

Beautiful, unexpected, characterful fonts. Pair a distinctive display font
with a refined body font. Never default to Inter/Roboto/Arial alone without
intentional aesthetic justification.

### Color carries meaning

Dominant colors with sharp accents outperform timid, evenly-distributed
palettes. Commit to a clear palette with purpose — every color earns its place.

### Atmosphere over decoration

Backgrounds aren't "just white" or "just dark." Consider gradient meshes,
noise textures, geometric patterns, grain overlays, subtle radial gradients.
The background sets the entire mood.

### Differentiation

What makes this app UNFORGETTABLE visually? What's the one thing someone
will remember? If the answer is "nothing," the brand isn't done.

## PostXL Design System Context

The target application uses `@postxl/ui-components` — 60+ React components
built on Radix UI primitives with Tailwind CSS v4. Brand tokens translate to
CSS custom properties that theme these pre-built components.

Available components include: DataGrid, Sidebar, Dialog, Card, Kanban,
CommandPalette, Breadcrumb, Tabs, and more. The brand should consider how
these components look with the chosen palette — you are theming a component
library, not building from scratch.
