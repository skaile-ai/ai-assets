---
name: "skaildev-update-docs"
description: "Post-implementation documentation sync for skaile-dev. Detects changed files via git, resolves which of the five doc tiers need updates (README.md, CLAUDE.md, Starlight docs, DOMAIN.md/SKILL.md, _devlog), checks accuracy, and rewrites stale content. Run after any code change that touches public API, architecture, or AI resource structure."
metadata:
  version: "1.0.0"
  tags:
    - "documentation"
    - "starlight"
    - "readme"
    - "claude-md"
    - "domain-md"
    - "skill-md"
    - "docs-sync"
    - "post-implementation"
  source: "MERGED"
  stage: "beta"
  reads_from:
    - "docs/"
    - "<package>/README.md"
    - "<package>/CLAUDE.md"
    - "ai-resources/<domain>/DOMAIN.md"
    - "ai-resources/<domain>/skills/<skill>/SKILL.md"
  writes_to:
    - "docs/"
    - "<package>/README.md"
    - "<package>/CLAUDE.md"
    - "ai-resources/<domain>/DOMAIN.md"
    - "ai-resources/<domain>/skills/<skill>/SKILL.md"
---

# Update Skaile Docs — Post-Implementation Documentation Sync

## Overview

Keeps all five skaile-dev documentation tiers accurate after code changes. Detects which
source files changed (via git), resolves which doc surfaces are responsible for documenting
them, checks whether documentation is still correct, and rewrites stale content.

This skill extends the generic `update-starlight-docs` with skaile-dev-specific knowledge:
the five-tier documentation model, the AI resource SKILL.md/DOMAIN.md tier, and the
`_devlog` triggering rules.

**Documentation tiers — full decision table:** `references/doc_tiers.md`

| Tier | What it is | Audience |
|------|-----------|----------|
| README.md | User-facing overview, quick-start | End users |
| CLAUDE.md | Developer/agent guide: architecture, conventions | Devs + AI agents |
| Starlight docs | In-depth reference (commands, API, config) | All |
| DOMAIN.md / SKILL.md | AI resource behavior docs | AI agents + skill authors |
| _devlog | What changed + why (plain language, historical) | Developers |

## When to Use

- After completing any implementation step in skaile-dev
- User says: "update docs", "sync docs", "are the docs still accurate"
- A feature branch is about to merge and docs haven't been checked
- A new package was added that has no documentation yet

## When NOT to Use

- For writing the `_devlog` entry — use the `devlog` skill for that
- During active implementation — run docs sync after the code change is stable
- For auto-generated catalog pages under `resources/` — those are managed by ai-resource-loader

---

ROLE  Documentation guardian — ensures all five doc tiers stay accurate after every implementation step in skaile-dev.

READS
  git diff / git log                                    — detect changed files since last sync
  <package>/README.md                                   — current user-facing docs
  <package>/CLAUDE.md                                   — current dev guide
  docs/src/content/docs/**/*.{md,mdx}                   — existing Starlight pages (_sources frontmatter)
  ai-resources/<domain>/DOMAIN.md                       — domain skill inventory
  ai-resources/<domain>/skills/<skill>/SKILL.md         — skill behavior spec
  source files referenced by _sources                   — current state of documented code

WRITES
  <package>/README.md                                   — updated user docs (public API only)
  <package>/CLAUDE.md                                   — updated dev guide
  docs/src/content/docs/**/*.{md,mdx}                   — updated Starlight pages
  ai-resources/<domain>/DOMAIN.md                       — updated domain inventory
  ai-resources/<domain>/skills/<skill>/SKILL.md         — updated skill behavior

REFERENCES
  references/doc_tiers.md                               — tier decision table (what to update when)
  dev-shared/contracts/concept_structure.md             — canonical paths
  docs/astro.config.mjs                                — sidebar structure

MUST  use git to determine change scope — never scan the codebase blindly
MUST  read references/doc_tiers.md before deciding which tiers to update
MUST  verify claims against actual source code before rewriting
MUST  preserve existing frontmatter fields (title, description, badge)
MUST  only add/update _sources, _source_hash, _last_synced in Starlight frontmatter
MUST  check all five tiers, but only update what is stale
NEVER invent documentation for code you haven't read
NEVER modify auto-generated resource catalog pages (resources/**)
NEVER remove README.md or CLAUDE.md content that is still accurate
NEVER trigger _devlog from this skill — devlog is a separate skill

EMIT [skaildev-update-docs] started packages=<list> files_changed=<n>

# ── Step 1: Determine Change Scope ───────────────────────────────

STEP 1: Collect changed files
  - Run `git diff --name-only HEAD~1..HEAD` (or `git diff --name-only main...HEAD` on a feature branch)
  - Exclude: test files, lock files, build artifacts, `.gitignore`
  - Group by package (first meaningful path segment)
  - Identify change type per file: new, modified, deleted, renamed

STEP 2: Consult tier decision table
  - Read `references/doc_tiers.md` decision table
  - For each changed file, determine which tiers need attention:
    - Public API exports → README.md + Starlight docs
    - CLI command changes → README.md + Starlight docs
    - Architecture/module changes → CLAUDE.md
    - Env variable changes → CLAUDE.md + Starlight docs (if configurable)
    - AI skill/domain changes → DOMAIN.md or SKILL.md
    - All meaningful changes → _devlog (handled by devlog skill, flag it here)
  - Build a map: { tier → [changed files that affect it] }

EMIT [skaildev-update-docs] scope_determined tiers=<list> files=<n>

# ── Step 2: README.md Sync ────────────────────────────────────────

FOR EACH package with README.md-relevant changes:

  STEP 3: Check README.md accuracy
    - Read <package>/README.md
    - Read the changed source files
    - Identify sections that may be stale:
      - Installation steps
      - Usage examples and command signatures
      - Feature descriptions
      - Links to the Starlight docs site
    - Classify each as: CURRENT | STALE | MISSING

  STEP 4: Update README.md
    IF STALE or MISSING sections found:
      - Rewrite the stale sections against current source
      - Preserve tone, structure, and level of detail
      - Keep it user-facing: no implementation details
    EMIT [skaildev-update-docs] readme_updated package=<name> sections=<n>

# ── Step 3: CLAUDE.md Sync ───────────────────────────────────────

FOR EACH package with CLAUDE.md-relevant changes:

  STEP 5: Check CLAUDE.md accuracy
    - Read <package>/CLAUDE.md
    - Read changed source files
    - Focus on: Architecture section, Key Conventions, Environment Variables, Common Tasks
    - Classify each section: CURRENT | STALE | MISSING | BROKEN

  STEP 6: Update CLAUDE.md
    IF STALE, MISSING, or BROKEN sections found:
      - Rewrite against current architecture and source code
      - New environment variables: add to env var table with type, default, description
      - New modules: add to Architecture section with one-liner description
      - Changed commands: update Common Tasks section
      - Write for AI agents and developers — assume no prior context
    EMIT [skaildev-update-docs] claude_md_updated package=<name> sections=<n>

# ── Step 4: Starlight Docs Sync ──────────────────────────────────

STEP 7: Find affected doc pages (same as update-starlight-docs)
  - For each changed file, search all doc pages for matching `_sources[].path`
  - Build map: { doc_page → [changed_source_files] }
  - Flag uncovered files for scaffolding

STEP 8: Check and update Starlight pages
  FOR EACH affected doc page:
    - Read the page content + each changed source file in its `_sources`
    - Classify sections: CURRENT | STALE | MISSING | BROKEN
    - For STALE / MISSING / BROKEN: rewrite as in update-starlight-docs

  FOR EACH uncovered file with documentation value:
    - Find the best existing page to extend, or scaffold a new page
    - Add `_sources` entries
    - Register new pages in `docs/astro.config.mjs` sidebar if not auto-generated

  AFTER all updates:
    - Recompute `_source_hash` from current source files
    - Update `_last_synced` to today's date
  EMIT [skaildev-update-docs] starlight_updated pages=<n> pages_created=<n>

# ── Step 5: AI Resource Docs Sync (DOMAIN.md / SKILL.md) ─────────

FOR EACH changed file in ai-resources/:

  STEP 9: Detect AI resource change type
    - SKILL.md changed → check if DOMAIN.md skills table is still accurate
    - New SKILL.md added → update domain's skills table in DOMAIN.md
    - New domain added → check domain is in `ai-resources/README.md` index
    - skill.pack.yaml changed → verify includes list matches actual skills present
    - Skill removed → remove from DOMAIN.md, update skillpack.yaml

  STEP 10: Update DOMAIN.md
    - Skills table: name, path, what it does, when to use — must be accurate
    - If new skills were added: add rows
    - If skills were removed: remove rows
    - If skill descriptions changed: update the row
    EMIT [skaildev-update-docs] domain_md_updated domain=<name> skills_updated=<n>

  STEP 11: Update SKILL.md (if skill behavior changed)
    - SKILL.md documents the skill's own behavior — it should already be accurate
    - Only update if a referenced contract, recipe, or reference path changed
    - Never rewrite SKILL.md based on implementation code — skills ARE the spec

# ── Step 6: Cross-Reference Validation ───────────────────────────

STEP 12: Validate internal links
  - Check all relative links in updated pages (`./path`, `/slug/`)
  - Verify linked pages, files, and sections still exist
  IF broken links found → fix them

STEP 13: Check Starlight sidebar registration
  - For new pages: verify auto-generated or manually registered in astro.config.mjs
  IF unregistered pages found → flag for manual sidebar addition

EMIT [skaildev-update-docs] completed tiers_updated=<list> pages_checked=<n> pages_updated=<n> pages_created=<n>

# ── Completion Report ─────────────────────────────────────────────

Present to user:
- Tiers checked and which were updated
- README.md: sections updated per package
- CLAUDE.md: sections updated per package
- Starlight: pages checked / updated / created
- DOMAIN.md / SKILL.md: resources updated
- Cross-references: broken links found and fixed
- Unregistered new pages (if any) — flag for sidebar

---

## _sources Frontmatter Convention (Starlight)

Same as `update-starlight-docs`:

```yaml
---
title: Page Title
description: One-sentence description.
_sources:
  - path: "skaile-agent-cli/src/commands/run.ts"
    sections: ["## Run Command", "## Flags"]
    description: "run command implementation"
_source_hash: "a1b2c3d4"
_last_synced: "2026-03-25"
---
```

## What to Always Document

- Public API exports and CLI commands
- Configuration options and environment variables
- Component props and emits (Vue/React)
- Database schema changes
- New AI skills/domains (SKILL.md + DOMAIN.md)

## What to Never Document

- Test files, lock files, build artifacts
- Internal implementation details not in the public contract
- Auto-generated files

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Rewriting CLAUDE.md with user-facing info | CLAUDE.md = dev/agent guide. User info goes in README.md. |
| Skipping CLAUDE.md for "small" architecture changes | Even a new env var or module deserves a CLAUDE.md update |
| Forgetting the DOMAIN.md skills table when adding a skill | Always update the domain's inventory table |
| Regenerating the whole README.md | Preserve style and structure — update only stale sections |
| Documenting internal implementation details | Document the public contract, not the internals |
| Confusing _devlog with documentation | Devlog = historical record. Docs = current truth. Both matter. |

## Integration

- **Called by:** `implement-skaile` (after tests pass), or standalone
- **Triggers:** `devlog` skill is NOT triggered from here — do it separately
- **Pairs with:** `verify` (can check docs accuracy as part of readiness gate)
- **References:** `references/doc_tiers.md` for the update decision table
