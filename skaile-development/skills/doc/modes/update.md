# Mode: Update Stale Documentation

Bring existing documentation back in sync with the current state of the source code.

---

## Input

- **doc-tracker.ts output** — JSON from `bun doc-tracker.ts --root <monorepo-root>`, providing:
  - `pages` — all doc pages; filter for `stale: true` to find pages that need updating
  - `diffSummary` — per-page summary of what changed in the source files since `_based_on_commit`
  - `sourceMap` — which pages document each source file
  - `annotations` — `@doc:important` and `@doc:see` markers in source files

---

## Workflow

### Step 1 — Identify stale pages

From the tracker output, collect all pages where `stale: true`. For each:
- Note `diffSummary` — this is the git diff stat summary since the page was last synced.
- Note `_based_on_commit` and `_last_synced` — these identify how far behind the page is.
- Pages with `diffSummary: "legacy _source_hash — migrate to _based_on_commit"` need migration (see legacy handling below).

If no pages are stale, confirm all source files are covered and exit cleanly.

### Step 2 — Update each stale Starlight page

For each stale page:

1. **Run git diff** to see what changed in the source files:
   ```bash
   git diff <_based_on_commit>..HEAD -- <source-path> [<source-path> ...]
   ```

2. **Read the current page content** to understand its existing structure.

3. **Classify each section** of the page:
   - `CURRENT` — content matches the current source; no change needed
   - `STALE` — content describes behavior that has changed; must be rewritten
   - `MISSING` — the source now has content that the page doesn't cover; must be added
   - `BROKEN` — refers to something that no longer exists; must be removed or corrected

4. **Rewrite only the stale, missing, or broken sections.** Do not rewrite sections classified as CURRENT — preserve stable content.

5. **Update tracking frontmatter:**
   ```yaml
   _based_on_commit: "<new HEAD SHA>"
   _last_synced: "<today YYYY-MM-DD>"
   ```

6. **Migrate legacy `_source_hash`** if present:
   - Remove the `_source_hash` field.
   - Add `_based_on_commit` set to HEAD (treating the page as "synced now").
   - Add `_last_synced` set to today.
   - Add `_sources` entries if they are missing (infer from the page's content and the source file paths).

### Step 3 — Check README.md accuracy

For each package whose source files changed:
- Verify **The Problem** and **What It Does** still accurately describe the package's value proposition.
- Verify the **Features** list matches what is actually available to users. Add new capabilities, remove deprecated ones.
- Verify the **Quick Start** still works with the current code.
- Ensure the tone stays product-first — if any section has drifted into technical/architectural detail, move that content to CLAUDE.md and replace it with a user-benefit description.
- Update only sections that are stale. Do not rewrite sections that are accurate.

### Step 4 — Check CLAUDE.md accuracy

For each package whose source files changed:
- **Architecture section:** verify module names, file paths, and data flow descriptions are current.
- **Environment Variables table:** verify all env vars listed still exist; remove any that were deleted; add any new ones.
- **Key Conventions:** verify naming rules, patterns, and gotchas still apply.
- **Common Tasks:** verify recipes (add a route, run locally, add a test) still work with current code.
- Update only sections that are stale.

### Step 5 — Check AI resource docs (if applicable)

If the changed source files belong to an AI skill or domain under `ai-assets/`:

- **DOMAIN.md skills table:** verify the skill name, path, description, and "when to use" are accurate. Update any row that no longer matches reality.
- **SKILL.md:** verify descriptions, prerequisites (`reads_from`), and outputs (`produces` / `writes_to`) are accurate. Update the workflow body if the skill's behavior changed.

### Step 6 — Cross-reference validation

After updating pages, run these checks:

1. **`_sources` paths exist:** for every `path` in every `_sources` frontmatter, verify the file exists in the monorepo. Remove entries for deleted files; add a `// @doc:important` or `@doc:skip` annotation in the replacement file if appropriate.

2. **`@doc:see` targets exist:** for every `@doc:see "Target"` annotation in source files, verify a Starlight page with that exact `title` exists. Flag broken refs to the user.

3. **Sidebar entries match:** for every updated Starlight page, verify its slug exists in `docs/astro.config.mjs`. Add missing entries; flag entries for deleted pages to remove.

### Step 7 — Delegate _devlog (if substantial updates)

If more than one page was updated, or if a CLAUDE.md architecture section changed, trigger `skaile-dev-devlog` to record a plain-language summary of what was brought back in sync and why it had drifted.

---

## Output

- Updated Starlight pages with refreshed `_based_on_commit` and `_last_synced`.
- Legacy `_source_hash` pages migrated to the `_based_on_commit` / `_sources` format.
- Updated README.md sections (installation, usage, features) where stale.
- Updated CLAUDE.md sections (architecture, env vars, conventions, tasks) where stale.
- Updated DOMAIN.md / SKILL.md where AI resource descriptions drifted.
- A change summary listing: each file updated, sections modified, and a one-line description of what changed.
