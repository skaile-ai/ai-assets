# Mode: Audit — Find Documentation Gaps

Scan the codebase for undocumented source files, stale pages, and broken references. Produce a prioritized gap report without writing any documentation.

---

## Input

- **doc-audit.ts output** — JSON from `bun doc-audit.ts --root <monorepo-root>`, providing:
  - `coverage` — overall tracked/total/skipped/percent stats
  - `coverageByPackage` — per-package breakdown
  - `gaps` — undocumented files with priority (`high` / `medium` / `low`) and reason
  - `stalePages` — pages whose source files changed since `_based_on_commit`
  - `brokenRefs` — `@doc:see` annotations pointing to non-existent pages

---

## Workflow

### Step 1 — Present the coverage summary

Display the overall documentation coverage as a brief headline:

```
Documentation coverage: <percent>%
  Tracked:  <tracked> files
  Total:    <total> files
  Skipped:  <skipped> files (marked @doc:skip)
```

Then show the per-package breakdown table, sorted ascending by coverage percent (lowest coverage first). This surfaces the packages most in need of attention.

### Step 2 — Present findings by priority

**High priority gaps** (list individually):
- `@doc:important` files not referenced in any doc page `_sources`
- Public API files (routes, controllers, `/api/` paths) with no coverage

For each high priority gap, show:
- File path
- Reason (as provided by doc-audit.ts)
- Suggested action (see Step 3)

**Medium priority gaps** (list individually):
- Large files (>150 lines) with no doc coverage
- Directories with 3+ source files and zero coverage

For each medium priority gap, show:
- File or directory path
- Reason
- Suggested action

**Low priority gaps** (count only):
- Show the total count of remaining untracked files with no coverage.
- Do not list them individually — the count is sufficient for low-priority items.
- Example: "47 additional untracked source files with no doc coverage."

### Step 3 — Suggest specific actions per gap

For each high and medium gap, provide a concrete next step. Use this decision table:

| Gap type | Suggested action |
|---|---|
| `@doc:important` file without coverage | Run `skaildev-doc` in `write` mode targeting this file |
| Public API route without coverage | Write a Starlight page under `docs/src/content/docs/<pkg>/commands/` |
| Large file (>150 lines) without coverage | Evaluate: does it export a public API? If yes → write docs. If internal only → add `@doc:skip` |
| Directory with 3+ uncovered files | Run `skaildev-doc` in `write` mode targeting the package |
| Low priority untracked file | Add `// @doc:skip` if intentionally undocumented; otherwise add to write backlog |

### Step 4 — Optional auto-fix: add @doc:skip annotations

If the user explicitly asks to auto-fix confirmed non-doc files:
- For each low-priority untracked file that the user confirms is intentionally undocumented:
  - Add `// @doc:skip - intentionally undocumented` as the first comment in the file.
- Do NOT add `@doc:skip` to high or medium priority files without explicit user confirmation.
- After adding annotations, note that re-running the audit will exclude these files from the untracked count.

### Step 5 — Stale page report

Present the stale pages table (from `stalePages` in the audit output):

| Page | Last Synced | Diff Summary |
|---|---|---|
| `docs/...` | YYYY-MM-DD | `3 files changed, 47 insertions(+)` |

If stale pages exist, suggest: "Run `skaildev-doc` in `update` mode to bring these pages back in sync."

If broken references (`brokenRefs`) exist, list them:
- File and line number
- The `@doc:see "Target"` annotation
- Why it is broken ("no doc page found with title X")
- Suggested action: create the missing page or correct the annotation

---

## Output

- Coverage summary (overall percent + per-package table)
- Prioritized gap list (high and medium individually; low as count only)
- Specific action recommendation per gap
- Stale page table with suggested remediation
- Broken reference list (if any)
- No documentation files are written or modified in this mode
