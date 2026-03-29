# Documentation Tracking Contract

> **Scope:** This contract defines conventions for tracking relationships between source code files and
> documentation pages. It specifies the frontmatter schema, inline annotation syntax, staleness
> detection protocol, gap detection heuristics, and coverage calculation formula used by all
> documentation-aware skills.

---

## Consuming Skills

| Skill | Domain | Role |
|---|---|---|
| `skaildev-doc` | `skaile-development` | Generates and maintains documentation from source; reads `_sources` frontmatter and `@doc:` annotations to produce or update doc pages |
| `update-starlight-docs` | `dev-implementation` | Syncs existing Starlight doc pages when source files change; uses staleness detection to identify pages that need updating |

---

## `_sources` Frontmatter Schema

Documentation pages that track source files include a `_sources` block in their YAML frontmatter.

### Example

```yaml
---
title: Session Manager
description: Manages git worktrees, hibernation, and wake cycles for agent sessions.
_sources:
  - path: agent-framework/runner/src/session-manager.ts
    sections:
      - SessionManager class
      - hibernate()
      - wake()
    description: Core session lifecycle logic — hibernation, wake, and worktree management.
  - path: agent-framework/runner/src/session-store.ts
    description: In-memory session registry; read to understand state transitions.
_based_on_commit: a3f9c12
_last_synced: 2026-03-29
---
```

### Fields

| Field | Type | Required | Description |
|---|---|---|---|
| `_sources` | array | required | List of source files this doc page is based on |
| `_sources[].path` | string | required | Path to the source file, monorepo-relative from the `skaile-dev/` root |
| `_sources[].sections` | string[] | optional | Named sections, classes, or functions within the file that this page covers |
| `_sources[].description` | string | required | What aspect of the source this entry covers; used in gap reports |
| `_based_on_commit` | string | required | Short git SHA (7 chars) of the commit the doc was last generated from |
| `_last_synced` | string | required | ISO date (`YYYY-MM-DD`) when the doc was last verified against source |

### Path Convention

All `_sources[].path` values are **monorepo-relative from the `skaile-dev/` root**. Do not use
absolute paths, `./` prefixes, or paths relative to the doc file itself.

```yaml
# Correct
path: agent-framework/runner/src/session-manager.ts

# Wrong — do not use
path: /mnt/vault-munin/workBench/CF-PROJECT/skaile-dev/agent-framework/runner/src/session-manager.ts
path: ../../runner/src/session-manager.ts
path: ./session-manager.ts
```

### Legacy Migration

Pages with a `_source_hash` field (legacy format) are treated as **legacy untracked**. They appear
in gap reports as untracked files but are not treated as errors. On first update by a consuming
skill, the `_source_hash` field is replaced with the full `_sources` / `_based_on_commit` /
`_last_synced` structure defined above.

---

## `@doc:` Annotation Syntax

Source files use inline `@doc:` annotations to declare documentation intent. Consuming skills read
these annotations to build gap reports and link source to docs.

### Verbs

| Verb | Effect |
|---|---|
| `important` | This symbol or block should be documented. Flagged as a high-priority gap if no doc page references this file or section. |
| `skip` | Explicitly excluded from gap reports. Use for internal helpers, generated code, or intentionally undocumented symbols. |
| `see "<Page Title>"` | Links this symbol to a named doc page. Broken references (page title not found in the docs site) are flagged in gap reports. |

### Syntax

```
// @doc: <verb> [- <freeform note>]
// @doc: see "<Page Title>" [- <note>]
```

For Python and shell files, use `#` instead of `//`:

```
# @doc: <verb> [- <freeform note>]
```

### TypeScript Examples

```typescript
// @doc: important - public entry point; must be covered in the Session Manager page
export class SessionManager {
  // ...
}

// @doc: see "Session Manager" - hibernate() is covered under the Lifecycle section
async hibernate(sessionId: string): Promise<void> {
  // ...
}

// @doc: skip - internal retry helper, not user-facing
function withRetry<T>(fn: () => Promise<T>, retries: number): Promise<T> {
  // ...
}
```

### Python / Shell Example

```python
# @doc: important - CLI entry point; must appear in the CLI Commands page
def main() -> None:
    ...

# @doc: skip - internal config normalizer
def _normalize_config(raw: dict) -> dict:
    ...
```

---

## Staleness Detection Protocol

Consuming skills use the following 5-step protocol to determine whether a doc page is stale.

1. **Read `_based_on_commit`** from the doc page frontmatter. If the field is absent, treat the
   page as **untracked** and skip to step 5.

2. **Run `git diff <_based_on_commit> HEAD -- <path>`** for each file listed in `_sources`. Use the
   monorepo root (`skaile-dev/`) as the working directory.

3. **Non-empty diff = stale.** The doc page must be reviewed and updated before `_based_on_commit`
   is advanced.

4. **Commit not found** (e.g., shallow clone, force-pushed history, or a typo in the SHA) — treat
   the page as **untracked**. Report the page in the gap report with reason `commit_not_found`.

5. **Missing `_sources` entries** — any source file referenced in annotations (`@doc: see`) that
   does not appear in the `_sources` array is treated as **untracked coverage**. Add it to the
   array and set `_based_on_commit` to the current HEAD short SHA.

### After Updating

Once a doc page has been reviewed and updated, set:

```yaml
_based_on_commit: <current HEAD short SHA>   # 7 chars, e.g. b2e1d97
_last_synced: <today's date>                 # ISO format, e.g. 2026-03-29
```

---

## Gap Detection Heuristics

| Condition | Priority | Notes |
|---|---|---|
| File or symbol annotated `@doc:important` with no doc page referencing it in `_sources` | High | Flagged as a documentation gap regardless of file size or location |
| Public API route (e.g., `app.get`, `router.post`, tRPC procedure) with no doc page `_sources` reference | High | Routes are considered user-facing by default |
| Source file >150 lines with no `_sources` reference in any doc page | Medium | Large files are likely significant enough to warrant documentation |
| Directory containing 3 or more source files where zero files appear in any `_sources` array | Medium | Entire uncovered directories are flagged as a coverage cluster |
| Minor source file with no `@doc:skip` annotation and no `_sources` reference | Low | Low-priority reminder; suppressed when overall coverage is above 80% |

Files annotated with `@doc:skip` are **excluded from all gap reports**, regardless of size, route
presence, or directory clustering.

---

## Coverage Calculation

### Formula

```
coverage = tracked_files / (total_files - skipped_files) * 100
```

### Definitions

| Term | Definition |
|---|---|
| `tracked_files` | Source files that appear in at least one doc page's `_sources` array |
| `skipped_files` | Source files that contain at least one `@doc:skip` annotation, or match an excluded pattern (see below) |
| `total_files` | All source files within the package or monorepo scope being measured |

### Scope

Coverage is calculated both **per-package** and **monorepo-wide**:

- **Per-package:** restricted to files under a single package directory (e.g., `agent-framework/runner/src/`)
- **Monorepo-wide:** aggregated across all packages, excluding the patterns listed below

### Excluded Patterns

The following file patterns are excluded from coverage calculations (treated as if they were
`@doc:skip`):

```
*.test.ts
*.spec.ts
**/__tests__/**
**/node_modules/**
**/dist/**
**/.nuxt/**
**/.output/**
*.config.ts           # unless the file exceeds 150 lines
*.config.js           # unless the file exceeds 150 lines
```

Additionally, **generated resource pages** (Starlight pages auto-generated from skill SKILL.md
files or domain DOMAIN.md files) are excluded from coverage tracking — they are considered
self-documenting by convention.
