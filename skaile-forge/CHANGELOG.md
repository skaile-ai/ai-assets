# Changelog

All notable changes to the skaile-forge domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Changed
- Agents `forge-project-assistant`, `forge-project-base-orchestrator`, `forge-project-orchestrator` move to `spec_version: "0.2"` and 1.1.0: `skills:` becomes `composes:` skill items. The `session_error` hook (`log-session-error`) of `forge-project-assistant` is now an `event` trigger in its `agent.yaml`; `project_open` and `agent_definition_change_requested` stay in `hooks/hooks.yaml`.
- `skaile-forge.bundle.yaml` → `skaile-forge.preset.yaml` (preset `@skaile-ai/skaile-forge`). Consumers: replace `bundle:` with `preset:` in `dependencies:`.

### Breaking Changes
- Bundle renamed: `bundle:forge-project@skaile-ai` → `bundle:skaile-forge@skaile-ai`. The domain folder moved from `forge-project/` to `skaile-forge/` and the manifest from `forge-project.bundle.yaml` to `skaile-forge/skaile-forge.bundle.yaml`. The old bundle identity no longer resolves.
  - **Migration:** in every consumer `skaile.yaml`, replace `bundle:forge-project@skaile-ai` with `bundle:skaile-forge@skaile-ai` under `dependencies:` (keep any version constraint suffix).
  - **Unchanged:** agent asset names stay `forge-project-assistant`, `forge-project-base-orchestrator`, `forge-project-orchestrator`, and the `ui-rendering` skill keeps its name — `agent:` / `skill:` refs need no change.

### Other
- Domain docs and agent prompts now target the unified `forge/skaile-forge` app (standalone repo `skaile-ai/skaile-forge`). `forge/L4-project` references in skaile-development skills were repointed; `forge/L4-assistant` / `forge/L5-concept` references are not part of this change.
