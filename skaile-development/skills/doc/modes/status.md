# Mode: Status — Quick Documentation Report

Run a fast read-only check and display the current state of documentation coverage, staleness, and gaps. No files are written or modified.

---

## Input

- **doc-status.ts output** — markdown report from `bun doc-status.ts --root <monorepo-root>`, providing:
  - Overall coverage percent and file counts
  - Per-package coverage table (sorted lowest coverage first)
  - Stale pages table (pages whose sources changed since `_based_on_commit`)
  - High and medium priority gaps (individually listed)
  - Low priority gap count
  - Broken `@doc:see` references (if any)

---

## Workflow

### Step 1 — Run doc-status.ts

Execute the script and capture its markdown output:

```bash
bun ai-assets/skaile-development/skills/doc/scripts/doc-status.ts \
  --root <monorepo-root> \
  [--scope <path-prefix>] \
  --format markdown
```

Use `--scope` if the user wants to check a specific package only (e.g., `--scope agent-framework/cli`).

### Step 2 — Display the report

Display the full markdown output from doc-status.ts directly, without modification. The script produces a complete, formatted report — do not summarize or reformat it.

### Step 3 — Offer next steps based on findings

After displaying the report, assess the findings and offer the relevant next steps:

**If stale pages exist:**
> "There are N stale page(s). Run `skaile-dev-docs` in `update` mode to bring them back in sync."

**If high priority gaps exist:**
> "There are N high priority gap(s) (undocumented public API or @doc:important files). Run `skaile-dev-docs` in `write` mode to address these."

**If medium priority gaps exist:**
> "There are N medium priority gap(s) (large files or uncovered directories). Review and either document them with `write` mode or mark as `@doc:skip`."

**If broken references exist:**
> "There are N broken @doc:see reference(s). Either create the missing pages or correct the annotations."

**If everything is clean (no stale pages, no high/medium gaps, no broken refs):**
> "Documentation is up to date. No action required."

---

## Output

- The formatted markdown report from doc-status.ts, displayed as-is.
- A short "next steps" block based on the findings.
- No files written or modified.
