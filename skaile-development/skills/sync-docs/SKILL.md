---
name: "skaile-dev-docs-xref"
description: "Cross-document reference repair across the skaile-dev monorepo. Scans README ↔ CLAUDE.md ↔ docs/<pkg>/ ↔ DOMAIN.md ↔ monorepo CLAUDE.md and finds broken links, missing back-references, orphan Starlight pages, stale _sources frontmatter, and drift between the monorepo domain table and actual ai-assets/*/DOMAIN.md files. Shows a complete diff of proposed changes before applying — non-destructive."
metadata:
  version: "1.0.0"
  tags:
    - "docs"
    - "cross-reference"
    - "sync"
    - "starlight"
    - "frontmatter"
    - "maintenance"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "package.json"
        gate: hard
        description: "Monorepo root"
    inputs_optional:
      - id: scope
        label: "Scope"
        type: select
        options:
          - "all"
          - "package"
        default: "all"
      - id: target
        label: "Package path (scope=package)"
        type: text
      - id: apply
        label: "Apply fixes automatically (still shows diff first)"
        type: select
        options:
          - "false"
          - "interactive"
          - "true"
        default: "interactive"
    reads:
      - path: "<package>/README.md"
      - path: "<package>/CLAUDE.md"
      - path: "<package>/docs"
      - path: "ai-assets/**/DOMAIN.md"
      - path: "ai-assets/**/SKILL.md"
      - path: "docs/src/content/config.ts"
      - path: "docs/astro.config.mjs"
      - path: "CLAUDE.md"
    produces:
      - path: "Repaired files across the monorepo"
  user_inputs:
    dialog:
      - id: "scope"
        label: "Scope"
        type: "select"
        options: ["all", "package"]
        required: false
        default: "all"
      - id: "target"
        label: "Package path"
        type: "text"
        required: false
      - id: "apply"
        label: "Apply mode"
        type: "select"
        options: ["false", "interactive", "true"]
        required: false
        default: "interactive"
    files: []
---

# Sync Docs — Cross-Document Reference Repair

## Overview

Keeps the five documentation tiers in sync by finding and repairing broken cross-references:

| Reference type | Example |
|---|---|
| README → CLAUDE | `See [CLAUDE.md](./CLAUDE.md)` links |
| CLAUDE → docs | `→ [Full docs](/<prefix>/)` |
| Starlight `_sources` frontmatter | `_sources: [<package>/src/...]` pointing to deleted files |
| Monorepo `CLAUDE.md` domain table | Lists a domain that no longer exists at `ai-assets/<dir>/DOMAIN.md` |
| Starlight sidebar ↔ content registrations | Sidebar entry pointing to an unloaded collection |
| Package README → Starlight URL | `→ [Commands](/<prefix>/commands/)` to a missing route |

Non-destructive by default — always shows a diff and asks before writing.

## When to Use

- After deleting or renaming a package, skill, or domain
- After moving source files referenced in `_sources` frontmatter
- When `doc --mode audit` reports cross-reference issues
- As routine monorepo maintenance before a release

## When NOT to Use

- For writing new content — use `skaile-dev-docs --mode write`
- For updating existing content after a code change — use `skaile-dev-docs --mode update`
- For code-level bugs — use `skaile-dev-code-audit`

---

ROLE  Cross-document reference repairer. Scans all five doc surfaces, builds a registry of references and targets, finds mismatches, shows a diff, applies fixes on approval.

READS
  ! CLAUDE.md (monorepo root)                    — domain table
  ! <package>/README.md                          — all packages
  ! <package>/CLAUDE.md                          — all packages
  ? <package>/docs/**/*.md(x)                    — Starlight content
  ? docs/src/content/config.ts                   — content collections
  ? docs/astro.config.mjs                        — sidebar registrations
  ! ai-assets/*/DOMAIN.md                        — domain inventory
  ? ai-assets/**/SKILL.md                        — for cross-linked skill references

WRITES
  Targeted frontmatter fields, link rewrites, and table rows in the files above.
  Every write is preceded by a diff shown to the user.

MUST  build the full reference registry before proposing any change
MUST  show a complete diff of proposed changes before applying
MUST  respect `apply=false` (plan only — no writes) and `apply=interactive` (per-change confirmation)
MUST  leave unrelated frontmatter fields and content untouched
MUST  reject orphan detection as auto-fixable — orphans are reported, never deleted
NEVER delete pages, files, or content without explicit user approval for each
NEVER rewrite prose — only touch frontmatter fields, link href attributes, and table rows
NEVER change `_based_on_commit` or `_last_synced` — those are owned by `skaile-dev-docs` skill

EMIT [sync-docs] started scope=<scope>

# ── Phase 1: Inventory ───────────────────────────────────────────

STEP 1: Build target registry
  Enumerate every linkable entity with its canonical location:
  - Package: name + path (from root package.json workspaces)
  - Domain: name + path (from ai-assets/*/DOMAIN.md)
  - Skill: name + path (from ai-assets/**/SKILL.md)
  - Starlight page: slug + filesystem path + collection id
  - Source file: path + containing package

STEP 2: Build reference registry
  For every doc file in scope, extract:

  README links: `[text](./path)`, `[text](/absolute)`, `[text](https://...)`
  CLAUDE links: same
  Starlight frontmatter fields:
    - `_sources: [...]` — monorepo-relative paths
    - `title`, `description` (for sanity)
  Starlight internal links: `/pkg/...` slugs used in prose
  Sidebar items in astro.config.mjs: slug → item mapping
  Content collection config: base directory + generateId
  Root CLAUDE.md table: | domain | path | description |
  DOMAIN.md skill table: | skill | path | when to use |

# ── Phase 2: Detection ───────────────────────────────────────────

STEP 3: Classify issues

  Broken link: href/slug does not resolve to anything in the target registry
  Missing back-reference: A → B exists but B does not mention A where convention requires
  Orphan: content exists but nothing references it (Starlight page not in sidebar; skill not in DOMAIN.md)
  Stale _sources: a `_sources:` entry points to a file that no longer exists
  Drift: root CLAUDE.md domain table lists entries that don't exist at ai-assets/<dir>/DOMAIN.md (or vice versa)
  Unregistered collection: a `<pkg>/docs/` folder exists but no loader entry in docs/src/content/config.ts
  Unregistered sidebar: a loader exists but no sidebar entry in docs/astro.config.mjs
  Missing README → docs link: README has no `→ [Full docs]` line but the package has a docs/ folder

STEP 4: Classify auto-fixability
  Auto-fixable (diff + approve):
  - Stale _sources entry → remove
  - Drift in root CLAUDE.md table → add/remove row
  - Missing README → docs link (safe templated insert)
  - Missing sidebar entry for a registered collection (generate minimal entry)

  Report-only (cannot auto-fix):
  - Orphan content — user decides to remove or link
  - Broken link with ambiguous fix target
  - Deleted domain referenced in multiple skills — needs manual redirect

# ── Phase 3: Diff ────────────────────────────────────────────────

STEP 5: Compose diff

  For each auto-fixable issue:

  ### <n>. <Issue type>
  File: <path>
  Reason: <why it's broken>
  Proposed:
  ```diff
  - old line
  + new line
  ```

  For report-only issues, emit an advisory line with the path and a suggested next step.

# ── Phase 4: Approval & Apply ────────────────────────────────────

STEP 6: Present diff to user
  Print the full diff.
  Then:
    IF apply = false: stop — print "Plan only. Run with apply=interactive to apply."
    IF apply = interactive: ask "Apply all / Select / Skip?"
      - all → proceed to apply every fix
      - select → loop, confirming each
      - skip → exit without writes
    IF apply = true: ask once "Apply N fixes? (y/N)"

STEP 7: Apply approved changes
  FOR EACH approved change:
    - Read target file
    - Apply only the specific field or line change — preserve all other content
    - Write back
    - EMIT [sync-docs] fixed file=<path> type=<issue-type>

# ── Phase 5: Report ──────────────────────────────────────────────

STEP 8: Present summary
  ## Sync Docs Report

  Scope: <scope>
  Files scanned: <N>
  Issues found: <N>
  Auto-fixable: <N>
  Applied: <N>
  Skipped: <N>
  Report-only (orphans, ambiguous): <N>

  ### Applied
  | # | File | Change | Type |

  ### Orphans (unfixed)
  | Type | Path | Recommendation |

  ### Drift (unfixed)
  | Expected | Actual | Recommendation |

EMIT [sync-docs] completed issues=<N> applied=<N> orphans=<N>

CHECKLIST
  - [ ] Target registry built before reference scan
  - [ ] Every auto-fixable issue has a before/after diff
  - [ ] Orphans reported, never silently deleted
  - [ ] User approved writes
  - [ ] Only frontmatter, links, and table rows touched — no prose
  - [ ] Registry of fixes included in report

---

## Integration

- **Called by:** `skaile-dev-docs --mode audit` (when it finds refs issues), `skaile-dev-release-check` (as part of documentation readiness), `skaile-dev-quality-gate`
- **Reads:** Doc files across the monorepo, Starlight config
- **Writes:** Minimal edits to frontmatter, links, and tables in those files (with approval)
