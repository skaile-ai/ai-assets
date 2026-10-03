# Changelog

All notable changes to the skaile-development domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Enhancements
- `ship` 1.7.0: the platform capability-docs sync moved from post-merge Phase 13b (conditional, "only if accessible") to Phase 8b, before the commit. The capabilities doc now rides in the same PR, the `platform-guide` skill gets its own ai-assets PR (cloned when no checkout exists) merged with the platform PR, and the PR body carries a `## Docs` section that platform CI parses. Headless or forked runs stop before the merge gate and never reached the old step, which is why the docs were never updated.

### Docs
- Skill references and examples now point at `forge/skaile-forge` (standalone repo `skaile-ai/skaile-forge`), which replaced `forge/L4-project`; branch-slug map entry `forge/skaile-forge` → `skaile-forge`.
- `references/test_stack_map.md`: `forge/skaile-forge` row lists unit `test/unit/`, integration `tests/integration/`, e2e `tests/e2e/`; new `forge/forge-common` row. Test skills describe skaile-forge's synthetic-h3 Nitro route tests as unit-level (`test/unit/`), not integration tests.

## [0.4.2] — 2026-04-30

### Fixes
- Restore short skill names across all cross-references (462 occurrences in 35 files): SOUL.md, DOMAIN.md, references/, and SKILL.md bodies. Prior revert (e33b41c) only touched SKILL.md `name:` frontmatter, leaving every cross-reference pointing at non-existent prefixed names — silent dispatch failure for the agent. Map: `skaile-dev-test` → `test`, `skaile-dev-code-audit` → `audit`, `skaile-dev-quality-gate` → `quality`, `skaile-dev-release-check` → `ready`, `skaile-dev-docs-xref` → `sync-docs`, `skaile-dev-docs` → `doc`, `skaile-dev-review-diff` → `review`, `skaile-dev-skill-validators` → `compile-validators`, `skaile-dev-session-retro` → `session-review`, `skaile-dev-session-optimize` → `session-analysis`, `skaile-dev-platform-e2e` → `e2e-platform`, `skaile-dev-design-spec` → `proposal`, plus simple prefix-strip for `implement`, `devlog`, `git`, `release`, `notify`, `faq`, `verify-ui`, `kill-backend`, `test-plan/unit/integration/e2e`. `skaile-dev-ops` references in devlog narrative examples preserved as historical context.

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
