# Documentation Pattern — TSDoc + Starlight

This reference defines *how to write* documentation in the skaile-dev monorepo.
Read `doc_tiers.md` first to determine *which* documentation surface to update.
This document extends `doc_tiers.md` by adding TSDoc as a prerequisite Tier 0
and prescribing the mandatory README.md structure.

---

## Tier 0 — TSDoc (TypeScript source files, prerequisite)

Before updating any documentation surface, annotate all added or modified
exported TypeScript symbols with TSDoc. This is a prerequisite — not an
optional step.

### Conventions

**Single-line — simple symbols:**
```typescript
/** Short description. */
export const MAX_RETRIES = 3;
```

**Field-level — above each property, NOT @param:**
```typescript
export interface ConnectorConfig {
  /** Unique identifier for this connector instance. */
  id: string;
  /** Maximum concurrent connections. Defaults to 5. */
  maxConnections?: number;
}
```

**Full block — functions, classes, complex types:**
```typescript
/**
 * Resolves skill requirements from a manifest declaration.
 *
 * @param manifest - Parsed skill manifest containing requirement declarations
 * @param context - Runtime context providing workspace and connector availability
 * @returns Resolved requirement set, or null if any hard requirement is unmet
 * @throws {RequirementError} When a required connector type is not registered
 * @example
 * const resolved = resolveRequirements(manifest, ctx);
 * if (!resolved) throw new Error('missing requirements');
 */
export function resolveRequirements(
  manifest: SkillManifest,
  context: RuntimeContext,
): ResolvedRequirements | null { ... }
```

**Rule:** *If it's exported, it has a TSDoc block.* Private implementation details
do not need docs. Internal helpers not exported from the package are exempt.

**After annotating:** run `_scripts/generate-api-docs.ts` if `docs/api-reference.md`
exists for this package to regenerate the Starlight API reference page.
The generated `docs/api-reference.md` is never hand-edited — all changes flow from
TSDoc annotations in source.

---

## Tier 1 — README.md (human entry point — product-first, not code-first)

The README is the front door of a repo. A human arriving here has never seen the
project. They want to know: **what is this, what problem does it solve, why should
I care, and what can it do?** Technical setup details are secondary — link to them,
don't lead with them.

### Required structure

```markdown
# Package Name

One-sentence hook: what this is and who it's for.

## The Problem

What pain point or gap does this package address? Why does it need to exist?
Write for someone who has the problem but doesn't know this tool yet.

## What It Does

The approach: how does this package solve the problem? What makes it different
from alternatives? This is the USP — keep it concrete and benefits-oriented.

## Features

Bullet list of key capabilities. Each bullet is a feature name + one sentence
explaining the user benefit. Order by importance, not by implementation date.

## Quick Start

Minimal steps to go from zero to working. Just enough to prove it works.
Link to full installation/configuration docs for details.

## Learn More

Links to detailed docs: installation guide, configuration, API reference,
CLAUDE.md (for contributors), etc.
```

The **Problem** and **What It Does** sections are mandatory. They answer: "I've
never seen this package — why does it exist and why would I choose it?"

### Tone
- Write for a human, not a machine. No jargon in the first three sections.
- Lead with benefits, not implementation details.
- Use concrete examples of what the user can accomplish, not abstract descriptions.

### Update when
- The product positioning, value proposition, or key features change
- New user-facing capabilities are added
- The package's role in the ecosystem changes

### Do NOT include
- Architecture internals (those go in CLAUDE.md)
- Environment variable tables (link to CLAUDE.md for those)
- Exhaustive API references (link to Starlight docs for those)
- Installation/configuration details beyond a minimal Quick Start
- Anything that reads like a developer guide rather than a product introduction

---

## Tier 2 — CLAUDE.md (developer + agent guide)

### Reminders (structure is defined in the root CLAUDE.md)

- CLAUDE.md is the authoritative developer guide — keep it current so agents get correct context
- Cross-package contract change: update *both* the losing and gaining package's CLAUDE.md
- Write for an AI agent that has never seen the codebase

### Update when
- Architecture or module structure changes
- A new non-obvious convention is established
- Environment variables are added or changed
- A gotcha or pitfall is discovered

---

## Tier 3 — Starlight pages (`<pkg>/docs/`)

### API reference

The `docs/api-reference.md` page is **always generated** by
`_scripts/generate-api-docs.ts` from TSDoc comments. Never hand-edit it.

Regenerate it after any exported symbol changes:
```bash
bun _scripts/generate-api-docs.ts --package <pkg>
```

### Registration

New packages must be registered in:
1. `docs/src/content/config.ts` — add a glob loader entry
2. `docs/astro.config.mjs` — add a sidebar entry

Follow the existing `arm` (agent-framework) pattern in both files.

> **New package with no `docs/` directory yet:** Only create a `docs/` directory when the
> package warrants three or more pages (see root `CLAUDE.md` Writing Documentation rules).
> For smaller packages, TSDoc in source + `README.md` + `CLAUDE.md` are sufficient.
> If `docs/api-reference.md` does not yet exist, `_scripts/generate-api-docs.ts` will
> create it automatically — you still must register the package in `config.ts` and
> `astro.config.mjs`.

---

## Decision Table

| I want to document… | Where it goes |
|---|---|
| What an exported function/type/interface does | TSDoc `/** */` in source (Tier 0 — always first) |
| What problem this solves and why it exists | `README.md` — The Problem + What It Does sections |
| Key capabilities and user benefits | `README.md` — Features section |
| Architecture, conventions, env vars, internal recipes | `CLAUDE.md` |
| Full API reference | `docs/api-reference.md` generated from TSDoc — never hand-edit |
| Concept explanation or mental model | `docs/concepts.md` |
| End-to-end tutorial | `docs/tutorials/<name>.md` |
| Cross-package contract change | Both `CLAUDE.md` files (losing and gaining package) |

---

## Workflow Integration

When the `implement` skill runs Phase 5 (Documentation Sync):

1. **TSDoc first:** Did this change add or modify exported symbols?
   - Yes → Annotate all changed exports with TSDoc before anything else
   - Run `_scripts/generate-api-docs.ts` if `docs/api-reference.md` exists
2. **Surface tiers:** Consult `doc_tiers.md` decision table for which surface to update
3. **Run `doc --mode update`** to sync all affected surfaces

---

---

*See also: `references/doc_tiers.md` (which tier to update), `skills/doc/SKILL.md` (how the doc skill works).*
