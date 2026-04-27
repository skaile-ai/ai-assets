# Mode: Write New Documentation

Write fresh documentation for source files, features, or packages that have no coverage yet.

---

## Input

- **doc-tracker.ts output** — JSON from `bun doc-tracker.ts --root <monorepo-root>`, providing:
  - `untracked` — source files not yet referenced in any `_sources` frontmatter
  - `annotations` — `@doc:important` files that must be documented
  - `sourceMap` / `reverseMap` — existing coverage for context
- **TARGET** — what to document. One of:
  - A list of specific file paths (e.g., `agent-framework/cli/src/commands/run.ts`)
  - A topic or feature name (e.g., "session lifecycle", "auth middleware")
  - A package path (e.g., `agent-framework/runner`)

---

## Workflow

### Step 1 — Identify what to document

If TARGET is provided:
- Use it directly to scope the work.

If TARGET is not provided:
- From `untracked`, identify files flagged `@doc:important` — these are the highest priority.
- Group remaining untracked files by package to find natural documentation boundaries.
- Ask the user to confirm scope before proceeding if the untracked list is large (>20 files).

### Step 2 — Determine tiers via doc_tiers.md

Read `ai-assets/skaile-development/references/doc_tiers.md` for the full decision table.

Quick-reference for common targets:

| Target type | README | CLAUDE.md | Starlight page | DOMAIN/SKILL.md | _devlog |
|---|---|---|---|---|---|
| New user-facing CLI command | Yes | Maybe | Yes | No | Yes |
| New env variable | No | Yes | Yes (if configurable) | No | Yes |
| New package (user-facing) | Yes | Yes | Yes | No | Yes |
| New package (internal only) | No | Yes | No | No | Yes |
| New AI skill | No | No | No | Yes (SKILL.md + DOMAIN.md update) | Yes |
| New public API route | No | Maybe | Yes | No | Yes |
| New config file or format | No | Yes | Maybe | No | Yes |
| Architecture addition | No | Yes | Maybe | No | Yes |

When in doubt, apply the most conservative tier that satisfies the audience. Do not write Starlight pages for internal implementation details.

### Step 3 — Read source files and annotations

For each file in scope:
- Read the file content.
- Look for `// @doc:important` — these sections must be covered.
- Look for `// @doc:see "Target"` — cross-reference the target page to understand relationships.
- Ignore files marked `// @doc:skip`.
- Identify the public interface: exported functions, class methods, CLI commands, API routes, env vars.

### Step 4 — Write Starlight pages (if tier requires it)

For each new Starlight page:

1. Place it under `docs/src/content/docs/<pkg>/` following the package docs structure:
   ```
   <pkg>/docs/
   ├── index.md            ← overview + quick-start
   ├── concepts.md         ← mental model
   ├── commands/<cmd>.md   ← one file per command group (CLI tools)
   └── developer-guide/
       └── index.md        ← architecture internals
   ```
2. Write frontmatter with tracking fields:
   ```yaml
   ---
   title: Page Title
   description: One-sentence description.
   _sources:
     - path: "relative/path/from/monorepo-root/to/source.ts"
       sections: ["## Section Heading"]
       description: "what this source covers on this page"
   _based_on_commit: "<HEAD commit SHA>"
   _last_synced: "<today YYYY-MM-DD>"
   ---
   ```
3. Do not write an H1 heading — the `title` frontmatter provides it.
4. Cover all `@doc:important` sections in the source files.
5. Use code blocks with explicit language fences for all examples.

**Only create a new page** when there is enough material (3+ significant topics). Single-topic additions belong in an existing page or in README/CLAUDE.md.

### Step 5 — Register in sidebar (if new Starlight page)

If a new Starlight page was created:
1. Add a content loader entry in `docs/src/content/config.ts` if this is a new package.
2. Add a sidebar entry in `docs/astro.config.mjs` under the appropriate package section.
3. If this is the first page for a package, add a package card to `docs/src/content/docs/index.md`.

### Step 6 — Write README.md sections (if tier requires it)

If the target is user-facing and README.md does not yet exist or is missing this feature:

README is the human entry point — product-first, not code-first. See `references/doc_pattern.md` for the full required structure (The Problem, What It Does, Features, Quick Start, Learn More).

- If README does not exist: create it with the full required structure. Lead with the problem and value proposition, not installation steps.
- If README exists but is missing coverage for this feature: add a bullet to the **Features** section describing the user benefit. Update **What It Does** if the feature changes the package's value proposition.
- Do not add technical implementation details. Link to Starlight or CLAUDE.md for depth.
- Write for a human evaluating the tool, not a developer implementing with it.

### Step 7 — Write CLAUDE.md sections (if tier requires it)

If the target changes architecture, conventions, or environment variables:
- Add to the relevant section in the package's `CLAUDE.md`.
- Required sections: What This Is, Architecture, Tech Stack, Key Conventions, Environment Variables, Common Tasks.
- If CLAUDE.md does not exist for the package, create it with all required sections.

### Step 8 — Write DOMAIN.md / SKILL.md (if AI resource changes)

If the target is a new or updated AI skill or domain:
- Update the `Skills` table in the domain's `DOMAIN.md`.
- Create or update the skill's `SKILL.md` with accurate `reads_from`, `writes_to`, and workflow body.
- Follow the SKILL.md frontmatter schema from `skaileup-shared/contracts/skill_template.md`.

### Step 9 — Delegate _devlog to devlog

After completing documentation work, trigger `skaile-dev-devlog` to record what was written. This is a short-entry-only change unless the documentation reveals a new architectural pattern.

---

## Output

- New Starlight page(s) with complete `_sources` frontmatter, `_based_on_commit = HEAD SHA`, and `_last_synced = today`.
- Updated `docs/astro.config.mjs` sidebar registration (if new pages).
- Updated or new `README.md` sections (if user-facing).
- Updated or new `CLAUDE.md` sections (if architectural).
- Updated `DOMAIN.md` skills table and/or new `SKILL.md` (if AI resource).
- A brief summary listing every file written and what each covers.
