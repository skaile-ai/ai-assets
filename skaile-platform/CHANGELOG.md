# Changelog — skaile-platform

## [Unreleased]

### Changed
- The `platform-guide` skill moved to `skaile-ai/platform` (`features/platform-guide/`), which embeds it into the backend and ships it with each deploy. `skills/platform-guide/` is removed here; `DOMAIN.md` points at the new home. Edit the guide in the same platform PR as the change it describes.
- `auto-ship` 0.3.0: a new non-negotiable says a platform changeset with a `whats-new:`, `whats-new-highlight:` or `whats-new-agent:` marker carries the guide and capabilities-doc edits in the same PR, or a decline marker with a reason.
- Agent `assistant` (`skaile-assistant`) 1.1.0 moves to `spec_version: "0.2"`. No composition content changes. `knowledge/index.yaml` (empty on purpose) loses its "loaded at session start" description and its unread `retrieval: load_all` block.
