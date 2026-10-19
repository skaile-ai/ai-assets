# Changelog — knowledge-research

## [Unreleased]

### Added
- `researcher` 1.0.0 (`agent:@skaile-ai/researcher`): the first shipped role agent. `lifecycle: role`, input `{topic}`, output `{summary, sources}` (read-only: it follows the collection and synthesis phases of `deep-research` and returns results instead of storing them), composes `deep-research`, `use-exa`, `use-perplexity` and `use-searxng`. The `knowledge-research` preset now includes it and the three `use-*` search skills it composes, so installing the preset alone resolves everything the agent needs.

### Changed
- `knowledge-research` bundle → `knowledge-research.preset.yaml` (preset `@skaile-ai/knowledge-research` 1.0.0); `knowledge-writing` likewise. Presets are versioned by hand (bump `version:` when `items:` changes); they no longer follow the git tag.

### Fixes
- `knowledge-writer-podcast`: document conversion goes through `use-anydoc`. It no longer points at `use-docling` (now opt-in only) or the nonexistent `pdf-to-markdown` skill (skaile-ai/platform#7323).
