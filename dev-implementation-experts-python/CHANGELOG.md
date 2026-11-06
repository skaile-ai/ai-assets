# Changelog — dev-implementation-experts-python

## [Unreleased]

### Added
- Skill `uv-cli-implementer` moved here from the retired `ai-asset-management` domain (same name, still published as `skill:@skaile-ai/uv-cli-implementer`). Its `metadata.requires: skill-builder-contract` is gone with that contract; it deliberately does not take `impl-experts-python-contract` (the preset ships it regardless), because the skill does not follow the expert-skill protocol that contract describes. Preset `@skaile-ai/dev-implementation-experts-python` 1.1.0 lists it.

### Changed
- `DOMAIN.md`: the `docs:` key and the `docs/` Building Blocks row are gone (there is no `docs/` folder here).
