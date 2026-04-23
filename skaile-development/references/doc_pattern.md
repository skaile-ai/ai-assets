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

---

## Tier 1 — README.md (user-facing, served by Starlight)

### Required structure

```markdown
# Package Name

One-sentence description of what this package does.

## Purpose

Short paragraph (2–4 sentences) explaining what problem this package solves and
where it fits in the skaile-dev monorepo. Who uses it, what it depends on,
what depends on it.

## Installation
# setup / install steps

## Usage
# key commands or code examples for the main use case

## <Feature Heading>
# repeat for major capabilities

→ [Full docs](/slug/)
```

The **Purpose** section is mandatory. It answers: "I've never seen this package —
why does it exist and how does it connect to the rest of the system?"

### Update when
- Public API, CLI commands, or usage patterns change
- The package's role in the monorepo changes

### Do NOT include
- Architecture internals (those go in CLAUDE.md)
- Environment variable tables (link to CLAUDE.md for those)
- Anything a user doesn't need to operate the package

---

## Tier 2 — CLAUDE.md (developer + agent guide, served by Starlight)

### Reminders (structure is defined in the root CLAUDE.md)

- CLAUDE.md is picked up by Starlight glob loaders — stale content = stale docs site
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

---

## Decision Table

| I want to document… | Where it goes |
|---|---|
| What an exported function/type/interface does | TSDoc `/** */` in source (Tier 0 — always first) |
| Package purpose and how it fits in the project | `README.md` — mandatory Purpose section |
| How to install / quick-start / main usage | `README.md` |
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

## Context

This is in the `ai-assets/skaile-development/references/` directory alongside `doc_tiers.md`, `audit_checklists.md`, `readiness_criteria.md`, `sub-agent-dispatch.md`, `test_stack_map.md`.
