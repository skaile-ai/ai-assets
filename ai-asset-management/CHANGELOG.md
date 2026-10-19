# Changelog — ai-asset-management

## [Unreleased]

### Changed
- Agent `skaile` 1.1.0 moves to `spec_version: "0.2"`: `persona:` / `rules:` / `requires:` become `composes:` items. The persona and ruleset parts are now `_agent-parts/personas/skaile-base/PERSONA.md` and `_agent-parts/rules/skaile-safety/RULESET.md` (1.1.0). The delegate agents are catalog refs (`skaileup-conceptualize`, `impl-build-implement`, `quality`); the `architecture` entry is dropped because no such agent is published.
- `ai-asset-management.bundle.yaml` → `ai-asset-management.preset.yaml` (preset `@skaile-ai/ai-asset-management` 1.0.0).
- Agent `skaile`: `metadata.routing_notes` is renamed `metadata.dropped_delegates` (it records why the `architecture` delegate was dropped); `routing_notes` now means "where to route" only, as in `skaile-development`.
- `source-manifest-builder`: the scaffold stub uses a literal publisher plus `publisher_overrides`, since @skaile/workspaces 5.0 rejects the `"@<domain>"` template.
- `source-manifest-builder`, `source-sidecar-curator`: the publisher review question asks about the resolved publisher instead of a folder-derived one, which 5.0 no longer produces.
