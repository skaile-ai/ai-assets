# Changelog

All notable changes to the skaile-forge domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Changed

- `DOMAIN.md`: spec version, changelog note and the Skills table now match the shipped assets (`code-assistance`, `file-management` and `project-exploration` are indexed).
- Agents `forge-project-assistant` 1.2.0 and `forge-project-orchestrator` 1.2.0 move to `spec_version: "0.2"`: `skills:` becomes `composes:` skill items (`forge-project-base-orchestrator` does the same and ships as 1.2.0, see below). The `session_error` hook (`log-session-error`) of `forge-project-assistant` is now an `event` trigger `report-session-error` in its `agent.yaml`, renamed because it no longer logs anything. The hook pointed at `scripts/log-error.sh`, which never existed, so nothing was logged before; the trigger prompt now asks the agent to tell the user what failed (plain text, no `<slug>` placeholder) and has no async flag because the trigger schema defines none. `project_open` and `agent_definition_change_requested` stay in `hooks/hooks.yaml`.
- Agents `forge-project-assistant` and `forge-project-orchestrator` 1.2.0 declare the same `memory.blocks` as the base orchestrator (`user`, `soul`, `learned`, `self`, all `scope: user`). The Forge assistant channel runs whichever agent a workspace binds, so without them project workspaces would lose memory once Forge drops its overlay files. Each `SOUL.md` gains a "What You Remember" section and each `RULES.md` exempts memory blocks from its file rules; the orchestrator is told to keep recording `user` and `learned` facts when it delegates.
- Agent `forge-project-base-orchestrator` 1.2.0 (its `skills:` also become `composes:`) declares `memory.blocks` `user`, `soul`, `learned`, `self` (all `scope: user`). Without them the assistant gets no `<MEMORY>` section and no `memory.*` capabilities once Forge drops its memory overlay files and `/api/internal/memory`. Its `SOUL.md` and `RULES.md` now say what goes into each block and when to read and write it, rule 4 exempts memory blocks from "never modify files", and `knowledge/forge-project-features.md` maps the imprint "Facts" to the `user` block.
- `skaile-forge.bundle.yaml` → `skaile-forge.preset.yaml` (preset `@skaile-ai/skaile-forge` 1.0.0, versioned by hand: bump it when `items:` changes). Consumers: replace `bundle:skaile-forge@skaile-ai` with `preset:@skaile-ai/skaile-forge` in `dependencies:`; a bundle-era version range does not carry over (presets start at 1.0.0).

### Breaking Changes
- The bundle kind is gone: `bundle:forge-project@skaile-ai` no longer resolves, and neither does `bundle:skaile-forge@skaile-ai`. The domain folder moved from `forge-project/` to `skaile-forge/`, and the former `forge-project.bundle.yaml` is now `skaile-forge/skaile-forge.preset.yaml` (preset `@skaile-ai/skaile-forge` 1.0.0).
  - **Migration:** in every consumer `skaile.yaml`, replace `bundle:forge-project@skaile-ai` (or `bundle:skaile-forge@skaile-ai`) with `preset:@skaile-ai/skaile-forge#^1.0` under `dependencies:`. A bundle-era version constraint does not carry over.
  - **Unchanged:** agent asset names stay `forge-project-assistant`, `forge-project-base-orchestrator`, `forge-project-orchestrator`, and the `ui-rendering` skill keeps its name — `agent:` / `skill:` refs need no change.

### Other
- Domain docs and agent prompts now target the unified `forge/skaile-forge` app (standalone repo `skaile-ai/skaile-forge`). `forge/L4-project` references in skaile-development skills were repointed; `forge/L4-assistant` / `forge/L5-concept` references are not part of this change.
