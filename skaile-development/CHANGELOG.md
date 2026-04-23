# Changelog

All notable changes to the skaile-development domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

## [0.3.0] — 2026-04-23

### Features
- New `session-review` skill: reads Claude Code session JSONL to report token usage, cache efficiency, estimated cost, workflow adherence, optimization tips, and skillset improvement suggestions
- New `references/doc_pattern.md`: three-tier documentation convention — TSDoc (Tier 0, prerequisite), mandatory README.md Purpose section, Starlight API reference generation

### Enhancements
- `implement` skill: three new MUST rules (TSDoc annotations, README Purpose, CLAUDE.md updates); Phase 5 TSDoc pre-check; session-review suggestion after devlog phase
- `doc` skill: reads `doc_pattern.md` alongside `doc_tiers.md`; Tier 0 pre-check for TypeScript packages
- `references/doc_tiers.md`: cross-reference to `doc_pattern.md`; TSDoc prerequisite note added to decision table
- `agents/skaile-development/SOUL.md`: proactive session-review suggestion on wrap-up signals and post-implement

## [0.2.0] — 2026-03-30

### Breaking Changes
- All skills renamed: `skaildev-*` prefix removed. Old names no longer exist.
- `commit-message` skill absorbed into `git mode=commit`. Standalone `commit-message` deleted.
- `skaildev-update-docs` deleted (was already deprecated).

### Features
- New `git mode=sync` for fetching/merging all submodules
- New `release` skill: changelog management, semantic versioning, git tagging
- `notify` skill (renamed from `skaildev-mattermost`) gains structured templates: plan, breaking-change, release, devlog-summary
- `faq` skill broadened from agent-framework-only to all monorepo packages
- `implement` skill gains optional Phase 6b: auto-notify on breaking changes and large implementations

### Renames
- `skaildev-git-workflow` + `commit-message` → `git`
- `skaildev-implement` → `implement`
- `skaildev-run-tests` → `test`
- `skaildev-doc` → `doc`
- `skaildev-devlog` → `devlog`
- `skaildev-mattermost` → `notify`
- `skaildev-faq` → `faq`

## [0.1.0] — 2026-03-22

Initial release.
