---
name: doc
description: >
  Use when writing new documentation, updating stale docs, auditing for gaps,
  or checking documentation status in the skaile-dev monorepo. Covers all five
  doc tiers: README.md, CLAUDE.md, Starlight docs, DOMAIN.md/SKILL.md, and _devlog.
metadata:
  version: "1.0.0"
  tags:
    - documentation
    - starlight
    - readme
    - claude-md
    - domain-md
    - skill-md
    - docs-sync
    - docs-audit
    - docs-coverage
  stage: beta
  source: MERGED
  prerequisites:
    inputs_required:
      - key: MODE
        prompt: "Which mode? (write | update | audit | status)"
        description: "Determines the operation to perform"
    inputs_optional:
      - key: SCOPE
        prompt: "Scope to a specific package? (e.g., agent-framework/cli)"
        description: "Monorepo-relative path prefix to limit operations"
      - key: TARGET
        prompt: "Target files or topic for write mode"
        description: "Source files to document or topic to write about (write mode only)"
    reads:
      - docs/
      - "*/README.md"
      - "*/CLAUDE.md"
      - ai-assets/**/DOMAIN.md
      - ai-assets/**/SKILL.md
    produces:
      - docs/src/content/docs/**/*.md
      - "*/README.md"
      - "*/CLAUDE.md"
      - ai-assets/**/DOMAIN.md
      - ai-assets/**/SKILL.md
---

# doc — Skaile Dev Documentation Skill

## Overview

Comprehensive documentation skill for the skaile-dev monorepo. Manages all five documentation
tiers through four distinct operating modes. Use this skill whenever documentation work is needed —
writing new docs, syncing stale content after code changes, auditing coverage gaps, or checking
current status without making changes.

### Documentation Tiers

| Tier | Location | Audience | Purpose |
|------|----------|----------|---------|
| README.md | `<package>/README.md` | End users | Installation, quick-start, feature overview |
| CLAUDE.md | `<package>/CLAUDE.md` | Developers + AI agents | Architecture, conventions, environment setup |
| Starlight docs | `docs/src/content/docs/` | All | In-depth reference (commands, API, config) |
| DOMAIN.md / SKILL.md | `ai-assets/<domain>/` | AI agents + skill authors | Domain purpose, skill inventory, skill behavior |
| _devlog | `_devlog/DEVLOG.md` | Developers | Historical record of what changed and why |

### Modes

| Mode | Purpose | Mutates files? |
|------|---------|----------------|
| `write` | Write fresh documentation for undocumented code or features | Yes |
| `update` | Sync stale docs after a code change (detects drift via git) | Yes |
| `audit` | Identify coverage gaps and stale content without writing | No |
| `status` | Quick summary of doc health across a package or the whole monorepo | No |

---

## When to Use

- After implementing a new feature — to write docs for the new code
- When `implement` finishes — it calls this skill in `update` mode automatically
- As a quality gate before merging — run `audit` mode to surface gaps
- When a stakeholder asks "is the documentation current?" — use `status` mode
- When you have undocumented code and need to write coverage from scratch — use `write` mode

## When NOT to Use

- For auto-generated resource catalog pages under `resources/` — those are managed by ai-resource-loader
- For writing `_devlog` entries directly — this skill delegates to `devlog`
- For non-skaile-dev projects — this skill has monorepo-specific knowledge baked in

---

ROLE  Documentation specialist for the skaile-dev monorepo. You understand all five doc tiers,
      know when each tier applies, and can write, update, audit, or report on any of them.

REFERENCES
  skaileup-shared/contracts/doc_tracking.md               — doc tracking contract (MUST read before any operation)
  ai-assets/skaile-development/references/doc_tiers.md    — five-tier decision table: which surface to update
  ai-assets/skaile-development/references/doc_pattern.md  — TSDoc conventions + README structure: how to write it

MUST  read `skaileup-shared/contracts/doc_tracking.md`, `references/doc_tiers.md`, and `references/doc_pattern.md` before starting any operation
MUST  annotate all added or modified exported TypeScript symbols with TSDoc (per doc_pattern.md) before updating any documentation surface
MUST  run the appropriate helper script first and consume its output before writing anything
MUST  verify all claims against actual source code before rewriting or creating documentation
MUST  use monorepo-relative paths in all `_sources` frontmatter entries
MUST  set `_based_on_commit` to the current HEAD SHA and `_last_synced` to today's date on all Starlight pages
MUST  preserve existing frontmatter fields (title, description, badge, _sources) when updating pages
MUST  delegate `_devlog` entries to `devlog` — never write devlog content directly

NEVER invent documentation for code you have not read
NEVER modify auto-generated resource catalog pages (resources/**)
NEVER scan the codebase blindly — always use a helper script or git to determine scope first
NEVER write _devlog entries directly from this skill
NEVER remove `_sources` entries from Starlight frontmatter without confirming the source file was deleted

---

## Workflow

### Step 0: Read Contracts

Before any other action, read both reference files:

1. `skaileup-shared/contracts/doc_tracking.md` — defines the tracking frontmatter schema and rules
2. `ai-assets/skaile-development/references/doc_tiers.md` — the full tier decision table
3. Read `ai-assets/skaile-development/references/doc_pattern.md`
   - This defines TSDoc conventions (Tier 0) and the mandatory README.md structure
   - If this operation touches TypeScript source: check for unannotated exports first
   - `doc_tiers.md` = which surface; `doc_pattern.md` = how to write it

These govern every decision in the steps that follow.

### Step 0b: Tier 0 Pre-check (TypeScript packages only)

Before determining mode, ask: does this operation affect TypeScript source files?

- **Yes:** Check whether any exported symbols (functions, types, interfaces, constants, classes)
  were added or modified. If so, add TSDoc `/** */` annotations per `doc_pattern.md` first.
  Then run `_scripts/generate-api-docs.ts` if `docs/api-reference.md` exists for this package.
- **No:** Proceed to Step 1.

### Step 1: Determine Mode

Use the `MODE` input to select the operating mode. If MODE was not provided, ask the user.

Confirm the SCOPE if provided (e.g., `agent-framework/cli`) — all path operations will be limited
to that prefix.

### Step 2: Run Helper Script

Run the appropriate helper script from `ai-assets/skaile-development/skills/doc/scripts/`
and consume its JSON output before writing anything.

| Mode | Script | Command |
|------|--------|---------|
| `write` | `doc-tracker.ts` | `bun doc-tracker.ts --root <monorepo-root> [--scope <path>]` |
| `update` | `doc-tracker.ts` | `bun doc-tracker.ts --root <monorepo-root> [--scope <path>]` |
| `audit` | `doc-audit.ts` | `bun doc-audit.ts --root <monorepo-root> [--scope <path>]` |
| `status` | `doc-status.ts` | `bun doc-status.ts --root <monorepo-root> [--scope <path>]` |

The script output determines what to act on. Do not proceed without it.

### Step 3: Load Mode Instructions

Load the mode-specific instruction file before executing:

| Mode | Instruction file |
|------|-----------------|
| `write` | `modes/write.md` |
| `update` | `modes/update.md` |
| `audit` | `modes/audit.md` |
| `status` | `modes/status.md` |

### Step 4: Execute Mode

Follow the loaded mode instructions exactly, using the helper script output as the primary input.

---

## Integration

- **Called by:** `implement` after tests pass (in `update` mode)
- **Calls:** `devlog` — after any write or update operation, delegate devlog entry creation
- **Related:** `update-starlight-docs` (generic Starlight sync skill, no skaile-dev-specific knowledge)
