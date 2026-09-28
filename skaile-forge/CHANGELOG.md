# Changelog

All notable changes to the skaile-forge domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Breaking Changes
- Bundle renamed: `bundle:forge-project@skaile-ai` → `bundle:skaile-forge@skaile-ai`. The domain folder moved from `forge-project/` to `skaile-forge/` and the manifest from `forge-project.bundle.yaml` to `skaile-forge/skaile-forge.bundle.yaml`. The old bundle identity no longer resolves.
  - **Migration:** in every consumer `skaile.yaml`, replace `bundle:forge-project@skaile-ai` with `bundle:skaile-forge@skaile-ai` under `dependencies:` (keep any version constraint suffix).
  - **Unchanged:** agent asset names stay `forge-project-assistant`, `forge-project-base-orchestrator`, `forge-project-orchestrator`, and the `ui-rendering` skill keeps its name — `agent:` / `skill:` refs need no change.

### Other
- Domain docs and agent prompts now target the unified `forge/skaile-forge` app (standalone repo `skaile-ai/skaile-forge`), which replaces forge-project, forge-assistant and forge-concept.
