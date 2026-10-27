# Skaile AI Assets

## What This Is

AI agent skills, domains, and shared resources for the Skaile ecosystem. Pure content — no code dependencies.

**GitHub:** `github.com/skaile-ai/skaile-ai-assets`

> **Skaileup skills** (concept, build, quality pipelines) live in the separate `ai-assets-skaileup/` submodule (`github.com/skaile-ai/ai-assets-skaileup`); the earlier collection is archived as `ai-assets-skaileup-v1`.

## Structure

Skills are organized into domains. Each domain has a `DOMAIN.md` and contains skills, contracts, and docs.

```
ai-asset-management/              ← meta: skill/domain scaffolding, catalog navigation
skaile-development/               ← skaile-dev monorepo workflow skills (git, test, audit, implement, etc.)
skaile-platform/                  ← skaile platform-specific skills (agent personas)
mcp/                              ← MCP server catalog entries (xls, ppt, github)
skaile-forge/                     ← skaile-forge app agents and skills
dev-implementation-experts-js/    ← JS/TS framework experts
dev-implementation-experts-python/ ← Python experts
dev-implementation-experts-typst/  ← Typst expert
dev-implementation-experts/       ← prog-expert skills with no DOMAIN.md and no preset (pre-existing orphan)
data-extraction/                  ← data extraction skills (MS Access VBA)
forge-mattermost/                 ← Mattermost workspace orchestrator agent
github-ops/                       ← GitHub operations skills and the github-worker agent
testing/                          ← testing skills and flows
_agent-parts/                     ← shared persona and ruleset assets (PERSONA.md, RULESET.md) composed by agents
external/                         ← tracked external resources
knowledge-research/               ← deep research, paper extraction; skills/knowledge-writing/ holds book/podcast generation
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
| `publisher` | `skaile.manifest.yaml` at the repo root | `skaile-ai` for everything here |
| `kind` | `<domain>/skills/<name>/SKILL.md` (skill), `<domain>/agents/<name>/agent.yaml` (agent), `<domain>/<domain>.preset.yaml` (preset), `<name>/PERSONA.md` (persona), `<name>/RULESET.md` (ruleset) | filename convention |
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
under `mcp/` source their version from `pom.xml` and mirror it into the
`version` field of their single `MCP.md` — see `mcp/DOMAIN.md`.

Agents (`agent.yaml`) and presets (`*.preset.yaml`) carry an explicit `version:`
in the file. It is hand-managed: it does not follow the git tag, so bump it (and
add a CHANGELOG entry) whenever the agent's content or the preset's `items:`
change. Presets start at `1.0.0`; they never inherited the old bundle versions, so
a consumer range written for a bundle (such as `#^2.0`) must become `#^1.0`.

Two files carry the word "manifest", and they are different things. The
**publisher** manifest is `skaile.manifest.yaml` in this repo: it names the
publisher and nothing else (no `assets:` list; discovery walks the tree). The
v1 file `.skaile-source.yaml` also declares a publisher (`publisher_default`); it
must agree with the manifest, which `scripts/source-publishers.test.sh` checks. A
**consuming** project's own workspace file stays `skaile.yaml`. Consumers pull
from this repo via a `sources:` entry in it:

```yaml
sources:
  - url: https://github.com/skaile-ai/ai-assets
    pin: v1.4.0     # branch | tag | 40-char sha; defaults to HEAD of default branch
dependencies:
  - preset:@skaile-ai/skaile-development#^1.0
  - skill:@skaile-ai/audit#~1.4
```

Full schema:
[`workspaces/.../specs/2026-05-31-manifest-canonical-identity.md`](https://github.com/skaile-ai/workspaces/blob/main/packages/workspaces/_devlog/specs/2026-05-31-manifest-canonical-identity.md)
(canonical) and the migration skill at
`skaile-development/skills/migrate-skaile-manifest/` (for converting legacy
`repositories:` / `ai_resources:` manifests).

## How This Is Consumed

The skaile CLI and platform consume these at **runtime** by reading SKILL.md files from disk. Configure the path via `SKAILE_RESOURCES_PATH` env var or `skaile.yaml`.

## Agent skills

### Issue tracker

Issues live as markdown files under `.scratch/<feature-slug>/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The five canonical triage roles, label strings equal to their names. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
