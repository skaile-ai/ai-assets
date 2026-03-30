# Changelog

All notable changes to the skaile-development domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

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
