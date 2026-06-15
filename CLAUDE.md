# Skaile AI Assets

## What This Is

AI agent skills, domains, and shared resources for the Skaile ecosystem. Pure content — no code dependencies.

**GitHub:** `github.com/skaile-ai/skaile-ai-assets`

> **Skaileup skills** (concept, build, quality pipelines) have been extracted to the separate `ai-assets-skaileup/` submodule (`github.com/skaile-ai/ai-assets-skaileup`).

## Structure

Skills are organized into domains. Each domain has a `DOMAIN.md` and contains skills, contracts, and docs.

```
ai-asset-management/              ← meta: skill/domain scaffolding, catalog navigation
skaile-development/               ← skaile-dev monorepo workflow skills (git, test, audit, implement, etc.)
skaile-platform/                  ← skaile platform-specific skills (agent personas)
mcp/                              ← MCP server catalog entries (xls, ppt, github)
forge-project/                    ← forge app project skills
dev-implementation-experts-js/    ← JS/TS framework experts
dev-implementation-experts-python/ ← Python experts
dev-implementation-experts-typst/  ← Typst expert
external/                         ← tracked external resources
knowledge-research/               ← deep research, paper extraction
knowledge-writing/                ← book/podcast generation
use/                              ← external service integrations (exa, perplexity, outline, etc.)
```

## Skill Structure

Every skill lives in its own directory:

```
my-skill/
├── SKILL.md        ← YAML frontmatter + markdown body (the agent prompt)
├── CLI.md          ← Optional: CLI invocation docs
├── references/     ← Optional: reference material
└── validator.py    ← Optional: output validation script
```

## Manifest Schema

Every asset shipped from this repo is identified by the canonical tuple
`(publisher, kind, name, version)`:

| Field | Source of truth | Notes |
|---|---|---|
| `publisher` | `skaile.yaml` at the repo root | `skaile-ai` for everything here |
| `kind` | `<domain>/skills/<name>/SKILL.md` (skill), `<domain>/<domain>.bundle.yaml` (bundle) | filename convention |
| `name` | SKILL.md frontmatter `name:` (must match parent directory) | per-asset |
| `version` | `git describe --tags --abbrev=0` of the resolved commit (repo-wide default); a per-asset frontmatter `version:` **overrides** it for that asset | cut a tag to release, or pin per-asset (see below) |

A per-asset version **may** be declared in SKILL.md frontmatter — as top-level
`version:` **or** `metadata.version:`. The workspaces parser reads both
(`packages/workspaces/core/src/manifest.ts` → `extractVersion()` checks
top-level then `metadata.version`; `CommonMetadataSchema` in
`types/.../manifests/_shared.ts` allows `metadata.version`), uses it for the
catalog entry and flow-node version pins, and it **overrides** the repo-wide
git-tag version for that asset (`core/src/publish-manifest.ts`: "`version:`
overrides the manifest-level version"). Prefer `metadata.version` for skaile
extensions (the agentskills.io convention the parser follows). MCP servers
under `mcp/` source their version from `pom.xml` and mirror it into
`metadata.version` (SKILL.md) and `version` (MCP.md) — see
`mcp/DOMAIN.md`.

Consumer projects pull from this repo via a `sources:` entry in their own
`skaile.yaml`:

```yaml
sources:
  - url: https://github.com/skaile-ai/ai-assets
    pin: v1.4.0     # branch | tag | 40-char sha; defaults to HEAD of default branch
dependencies:
  - bundle:skaile-development@skaile-ai#^2.0
  - skill:audit@skaile-ai#~1.4
```

Full schema:
[`workspaces/.../specs/2026-05-31-manifest-canonical-identity.md`](https://github.com/skaile-ai/workspaces/blob/main/packages/workspaces/_devlog/specs/2026-05-31-manifest-canonical-identity.md)
(canonical) and the migration skill at
`skaile-development/skills/migrate-skaile-manifest/` (for converting legacy
`repositories:` / `ai_resources:` manifests).

## How This Is Consumed

The skaile CLI and platform consume these at **runtime** by reading SKILL.md files from disk. Configure the path via `SKAILE_RESOURCES_PATH` env var or `skaile.yaml`.
