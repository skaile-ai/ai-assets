# Changelog — ai-asset-management

## [Unreleased]

### Changed
- `source-manifest-builder`: the scaffold stub uses a literal publisher plus `publisher_overrides`, since @skaile/workspaces 5.0 rejects the `"@<domain>"` template.
- `source-manifest-builder`, `source-sidecar-curator`: the publisher review question asks about the resolved publisher instead of a folder-derived one, which 5.0 no longer produces.
