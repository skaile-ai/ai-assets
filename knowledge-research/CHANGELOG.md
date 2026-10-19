# Changelog — knowledge-research

## [Unreleased]

### Added
- `researcher` 1.0.0 (`agent:@skaile-ai/researcher`): the first shipped role agent. `lifecycle: role`, input `{topic}`, output `{summary, sources}` (read-only: it follows the collection and synthesis phases of `deep-research` and returns results instead of storing them), composes `deep-research`, `use-exa`, `use-perplexity` and `use-searxng`. The `knowledge-research` preset now includes it and the three `use-*` search skills it composes, so installing the preset alone resolves everything the agent needs.

### Changed
- `DOMAIN.md` frontmatter (here and in `skills/knowledge-writing/`): the `docs:` key is gone (there is no `docs/` folder) and the empty `prompts`/`tools`/`agents` entries all read "TBD".

- `DOMAIN.md` has an `agents/` row and an Agents section for `researcher`; `skills/knowledge-writing/DOMAIN.md` describes the real layout (the three skills sit next to `DOMAIN.md`, no `docs/` or `prompts/` folder) and notes that it is the one nested domain.
- `DOMAIN.md` points to the nested `skills/knowledge-writing/DOMAIN.md`, which is the one index for `book-writer`, `knowledge-paper-podcast` and `knowledge-writer-podcast`; that file called `book-writer` `knowledge-writer-book`, a skill that does not exist, and is corrected.
- `knowledge-research` bundle → `knowledge-research.preset.yaml` (preset `@skaile-ai/knowledge-research` 1.0.0); `knowledge-writing` likewise. Presets are versioned by hand (bump `version:` when `items:` changes); they no longer follow the git tag.

### Fixes
- `knowledge-writer-podcast`: document conversion goes through `use-anydoc`. It no longer points at `use-docling` (now opt-in only) or the nonexistent `pdf-to-markdown` skill (skaile-ai/platform#7323).
