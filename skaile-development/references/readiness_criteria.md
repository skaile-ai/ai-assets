# Readiness Criteria

Used by the `ready` skill. Each package is evaluated against the criteria table below. A package is **ready** only when every `required` criterion passes.

---

## Per-Package Required Criteria

| # | Criterion | Check | How to verify |
|---|---|---|---|
| 1 | README.md exists | File present at `<pkg>/README.md` | `fs.existsSync` |
| 2 | README.md has product sections | Has `## The Problem` and `## What It Does` (or legacy `## Purpose`) | Regex on H2 headings |
| 3 | CLAUDE.md exists | File present at `<pkg>/CLAUDE.md` | `fs.existsSync` |
| 4 | CLAUDE.md has required sections | Must have: `## What This Is`, `## Architecture`, `## Tech Stack`, `## Key Conventions` | Regex on H2 headings |
| 5 | package.json exists | Required for every TS package | `fs.existsSync` |
| 6 | Test config exists | `vitest.config.ts`, `jest.config.*`, or listed in root `vitest.config` projects | Filesystem + workspace check |
| 7 | At least one test file | `tests/**/*.test.ts` or equivalent has > 0 entries | Glob |
| 8 | Tests pass | `bun x --bun vitest run --project <name>` exits 0 | Subprocess |
| 9 | Build passes | `bun run build` inside package (if script exists) exits 0 | Subprocess |
| 10 | Lint clean | `bun run lint` or `bun x biome check` exits 0 | Subprocess |
| 11 | Typecheck clean | `bun run typecheck` or `tsc --noEmit` exits 0 | Subprocess |
| 12 | No uncommitted changes | `git status --porcelain <pkg>` empty | Subprocess |

## Per-Package Soft Criteria (warnings only)

| # | Criterion | Check |
|---|---|---|
| S1 | CHANGELOG.md exists | For releasable packages |
| S2 | `docs/<pkg>/` registered in Starlight | If `<pkg>/docs/` exists, must be in `docs/src/content/config.ts` + `docs/astro.config.mjs` |
| S3 | Environment variables table in CLAUDE.md | If package reads `process.env.*` at runtime |
| S4 | README-to-docs link | README should link to `→ [Full docs](/<prefix>/)` when docs/ exists |
| S5 | REVIEW.md present | Repo-specific review rules for `review` to consume |

## Domain (ai-assets) Specific Criteria

Applied only to directories under `ai-assets/<domain>/`:

| # | Criterion | Check |
|---|---|---|
| D1 | DOMAIN.md exists | `fs.existsSync(ai-assets/<domain>/DOMAIN.md)` |
| D2 | DOMAIN.md frontmatter valid | Has `name`, `description`, `type: domain`, `stage` |
| D3 | Every skill has SKILL.md | `<domain>/skills/**/SKILL.md` |
| D4 | Every SKILL.md frontmatter valid | `name`, `description`, `metadata.version`, `metadata.source` |
| D5 | Compiled validators up to date | If SKILL.md has MUST/NEVER and `validator.py` exists, check mtime |

## Monorepo Global Criteria

Evaluated once, not per-package:

| # | Criterion | Check |
|---|---|---|
| G1 | Root `package.json` workspaces include every submodule | Workspace glob check |
| G2 | All submodules cleanly pointing at their tracked commits | `git submodule status` no `+` or `-` prefix |
| G3 | `bun run format` produces no diff | `bun run format --check` |
| G4 | Root `CLAUDE.md` domain table matches `ai-assets/*/DOMAIN.md` | Table vs. filesystem cross-check |
| G5 | `skaile.yaml` references exist on disk | Path validation |

---

## Package Categories

Not every criterion applies to every package. The `ready` skill categorizes packages before checking:

| Category | Criteria applied | Examples |
|---|---|---|
| **app** | 1-12, S1-S5 | `forge/project`, `forge/concept`, `platform/backend`, `platform/frontend` |
| **library** | 1, 3-12, S1-S3 | `agent-framework/runner`, `agent-framework/bridge`, `forge/common-backend` |
| **internal-library** | 3, 5-12 (README optional) | `agent-framework/core`, `agent-framework/types`, `agent-framework/transport` |
| **cli** | 1-12, S1, S2 | `agent-framework/cli` |
| **docs-site** | 1, 3, 5 (tests/build per Astro) | `docs/` |
| **ai-domain** | D1-D5 | `ai-assets/skaile-development/`, `ai-assets/skaileup-evaluate/`, etc. |
| **theme** | 1, 3, 5 | `theme/` |

The category is inferred from:
- `package.json` `private`, `main`, `bin`, `type: module`
- Presence of `nuxt.config.ts` / `vite.config.*` → app or frontend
- Path prefix (`ai-assets/*` → ai-domain)
- Explicit override in `<pkg>/.skaile-category` if present

---

## Report Template

```markdown
# Readiness Report — <date>

## Global
- [x] G1 Root workspaces complete
- [ ] G2 Submodules tracked cleanly — forge has uncommitted changes
- ...

## Packages

| Package | Category | Required | Soft | Status |
|---|---|---|---|---|
| forge/project | app | 11/12 | 4/5 | BLOCKED |
| forge/concept | app | 12/12 | 5/5 | READY |
| agent-framework/runner | library | 12/12 | 3/3 | READY |
| ...

## Blockers

### forge/project
- [!] 8 Tests fail — 3 failing tests in tests/e2e/chat.spec.ts
  Fix: investigate recent chat refactor

### ai-assets/skaile-development
- [!] D5 Stale validator.py — skills/audit/validator.py older than SKILL.md
  Fix: bun run skill compile-validators audit

## Warnings

### forge/project
- [~] S1 CHANGELOG.md missing — create one before releasing

## Verdict

- Packages ready: 14/16
- Packages blocked: 2/16
- Global checks: 4/5 passing

Recommendation: fix blockers before `release bump`.
```
