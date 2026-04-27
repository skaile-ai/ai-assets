# doc CLI

## Basic Usage

Invoke from the CLI with one of four modes: `status`, `update`, `audit`, or `write`.

### Mode: status

Quick read-only check of documentation coverage and gaps across the monorepo (or a specific package).

```bash
# Check entire monorepo
skaile skill run doc --input MODE=status

# Check a specific package
skaile skill run doc --input MODE=status --input SCOPE=agent-framework/cli
```

**Output:** A formatted markdown report showing:
- Overall coverage percentage
- Per-package coverage table (sorted by lowest coverage first)
- List of stale pages (where source files changed since last sync)
- High and medium priority documentation gaps
- Broken documentation references

**No files are written.**

### Mode: update

Sync stale documentation after code changes. Detects which docs are out of sync using git history and updates them.

```bash
# Update all stale docs across the monorepo
skaile skill run doc --input MODE=update

# Update stale docs in a specific package
skaile skill run doc --input MODE=update --input SCOPE=forge/common-backend
```

**Output:**
- Updated Starlight pages with `_last_synced` set to today's date
- Updated README.md / CLAUDE.md sections (if architectural changes detected)
- Summary of all files touched

**Typical use:** After `skaile-dev-implement` finishes (it calls this automatically) or when a developer has made code changes that affect public behavior.

### Mode: audit

Identify documentation gaps and coverage issues without writing anything.

```bash
# Audit the entire monorepo
skaile skill run doc --input MODE=audit

# Audit a specific package
skaile skill run doc --input MODE=audit --input SCOPE=agent-framework/resolver
```

**Output:** A detailed audit report listing:
- Undocumented source files and their priority level (@doc:important vs. normal)
- Missing Starlight pages for public APIs
- Architecture or convention changes without CLAUDE.md updates
- Broken cross-references in documentation
- Recommendations for each issue

**No files are written.** Use the output to prioritize documentation work.

### Mode: write

Write fresh documentation for source files, features, or packages that have no coverage yet.

```bash
# Write docs for a specific package
skaile skill run doc --input MODE=write --input TARGET=agent-framework/cli

# Write docs for specific source files
skaile skill run doc --input MODE=write --input TARGET=forge/L4-project/src/auth.ts

# Write docs for a feature or topic
skaile skill run doc --input MODE=write --input TARGET="session lifecycle"
```

**Output:**
- New Starlight pages with complete frontmatter (`_sources`, `_based_on_commit`, `_last_synced`)
- New or updated README.md / CLAUDE.md sections
- New DOMAIN.md / SKILL.md for AI resources
- A summary listing all files created and what each covers

---

## Helper Scripts (Direct Invocation)

For quick checks or integration into custom scripts, run the helper scripts directly with `bun`:

### doc-status.ts

Fast read-only report of documentation coverage:

```bash
# Check entire monorepo
bun ai-assets/skaile-development/skills/doc/scripts/doc-status.ts --root .

# Check a specific package with markdown output
bun ai-assets/skaile-development/skills/doc/scripts/doc-status.ts \
  --root . \
  --scope agent-framework/cli \
  --format markdown

# Check with JSON output (for programmatic use)
bun ai-assets/skaile-development/skills/doc/scripts/doc-status.ts \
  --root . \
  --format json
```

**Flags:**
- `--root <path>` — monorepo root (required)
- `--scope <path>` — limit to this package prefix (optional)
- `--format <markdown|json>` — output format, defaults to markdown

### doc-audit.ts

Identify documentation gaps without writing:

```bash
# Audit entire monorepo
bun ai-assets/skaile-development/skills/doc/scripts/doc-audit.ts --root .

# Audit a specific package
bun ai-assets/skaile-development/skills/doc/scripts/doc-audit.ts \
  --root . \
  --scope agent-framework/core

# Export audit results as JSON
bun ai-assets/skaile-development/skills/doc/scripts/doc-audit.ts \
  --root . \
  --format json
```

**Flags:**
- `--root <path>` — monorepo root (required)
- `--scope <path>` — limit to this package prefix (optional)
- `--format <markdown|json>` — output format, defaults to markdown

### doc-tracker.ts

Map source files to documentation coverage (used by `write` and `update` modes):

```bash
# Track entire monorepo
bun ai-assets/skaile-development/skills/doc/scripts/doc-tracker.ts --root .

# Track a specific package
bun ai-assets/skaile-development/skills/doc/scripts/doc-tracker.ts \
  --root . \
  --scope forge/common-ui

# Output raw JSON (for scripting)
bun ai-assets/skaile-development/skills/doc/scripts/doc-tracker.ts \
  --root . \
  --format json
```

**Flags:**
- `--root <path>` — monorepo root (required)
- `--scope <path>` — limit to this package prefix (optional)
- `--format <markdown|json>` — output format, defaults to markdown

---

## Integration with implement

The `skaile-dev-implement` skill automatically calls `skaile-dev-docs` in `update` mode after tests pass and verification succeeds. This ensures documentation stays in sync with code changes.

**Why it matters:** You do not need to manually run `skaile-dev-docs` after a feature is implemented — the orchestrator handles it.

**If you need to run it manually:** Follow the workflow instructions in this CLI under **Mode: update**.

---

## Typical Workflows

### After Implementing a Feature

1. **Run `skaile-dev-implement`** to scaffold and build the feature.
2. **Run tests and verification** to ensure everything works.
3. **`skaile-dev-implement` calls `skaile-dev-docs update` automatically.**
4. *Done.* Documentation is now in sync.

If you need to write docs for undocumented code, run:

```bash
skaile skill run doc --input MODE=write --input TARGET=<package-or-file>
```

### Before a Release

1. **Run status check** to see overall documentation health:
   ```bash
   skaile skill run doc --input MODE=status
   ```

2. **Review the output** for stale pages and gaps.

3. **If stale pages exist:** Run `update` mode to bring them in sync:
   ```bash
   skaile skill run doc --input MODE=update
   ```

4. **If gaps exist:** Run `audit` mode to see what needs to be written:
   ```bash
   skaile skill run doc --input MODE=audit
   ```

5. **Address high-priority gaps** using `write` mode:
   ```bash
   skaile skill run doc --input MODE=write --input TARGET=<package-or-file>
   ```

### Documenting an Existing Package

If a package exists but is undocumented, use `write` mode with the package path:

```bash
skaile skill run doc --input MODE=write --input TARGET=agent-framework/runner
```

The skill will:
- Scan all source files in the package for `@doc:important` annotations
- Determine which documentation tiers apply (README.md, CLAUDE.md, Starlight, etc.)
- Write all required documentation
- Register Starlight pages in the sidebar
- Report what was created

### Quality Gate: Pre-Merge Checks

Before merging a feature branch to `main`:

1. **Run audit** to catch gaps early:
   ```bash
   skaile skill run doc --input MODE=audit --input SCOPE=<your-package>
   ```

2. **Fix any high-priority issues** before requesting review.

3. **After merge**, run status check to confirm everything is clean:
   ```bash
   skaile skill run doc --input MODE=status
   ```

---

## When to Use Each Mode

| Mode | Use when | Writes files? |
|------|----------|---------------|
| `status` | You want a quick report of doc health | No |
| `update` | Code changed and docs need to sync | Yes |
| `audit` | You want to identify gaps without writing | No |
| `write` | You have undocumented source code to cover | Yes |

---

## Related Skills

- **`skaile-dev-implement`** — calls `skaile-dev-docs update` automatically after implementation succeeds
- **`skaile-dev-devlog`** — record what was documented in the project devlog (called after write/update)
- **`update-starlight-docs`** — generic Starlight sync skill (no skaile-dev-specific knowledge)
