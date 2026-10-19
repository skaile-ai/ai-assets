# Changelog

All notable changes to the skaile-development domain are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/).

## [Unreleased]

### Changed
- Agent `skaile-development` 1.2.0 moves to `spec_version: "0.2"`: `metadata.orchestrates` becomes `composes:` skill items; `routes_to` / `knows` prose moves to `metadata.routing_notes` / `knowledge_notes`.
- `skaile-development.bundle.yaml` → `skaile-development.preset.yaml` (preset `@skaile-ai/skaile-development`). Consumers: replace `bundle:@skaile-ai/skaile-development` with `preset:@skaile-ai/skaile-development` in `dependencies:`.

### Fixes
- `git` `deploy` (retired) now says never to push `deploy`: the platform `deploy` branch is protected by the `deploy-protection` ruleset, and a release deploys when the Version PR merges in the 06:00-07:00 Berlin window (#192). `ship` 1.9.1 `references/platform.md` says the same.
- `git` `finish` merge: a BLOCKED PR is classified from `gh pr checks --required --json name,bucket` (read the JSON, not the exit status), so a red required check stops instead of arming auto-merge. Auto-merge is armed only for a pending required check or a required review; a block nothing will clear (no required checks, or all pass or skipping) stops and names the gate. An UNKNOWN merge state, usual right after the PR is created, gets one re-probe after about 3 s before it stops. Every `git-state.json` write is anchored at `<repo_root>`, and branch mode resolves `repo_root` itself. `DOMAIN.md` and `worktree_patterns.md` say changes land by PR instead of claiming every default branch is protected.
- Every repo is standalone; the archived `skaile-dev` shell is no longer treated as live (#189). `DOMAIN.md` states the rule and no longer marks `references/test_stack_map.md` authoritative (the target repo's `CLAUDE.md` wins). `test_stack_map.md` gives per-repo commands for workspaces, platform, ai-assets, store and brand. `ship` 1.9.0 resolves the target repo as the main checkout of a standalone clone (never a linked worktree), stops reading the shell `CLAUDE.md`, and puts worktrees beside the clone at `<repos_root>/.worktrees/<repo>-<number>`. `git` uses per-repo format commands, a worktree beside the clone, an unpushed-commit guard instead of submodule pushes, its `finish` merge squash-merges on GitHub only (`gh pr merge --squash`, no local squash, never `--admin`, no `--delete-branch`), is safe to re-run (an already merged PR or a recorded `auto_merge` status skips to cleanup), lets `mergeStateStatus` pick the path (merge now when CLEAN, UNSTABLE or HAS_HOOKS; when BLOCKED, stop on a red required check or requested changes and arm auto-merge only for pending required checks or a required review, then never offer `discard`; stop on DIRTY, BEHIND, DRAFT or UNKNOWN), keeps its state at `<repo_root>/_implementation/git-state.json`, and cleans up in a safe order (worktree, default branch, `pull --ff-only`, then the branch), and its `sync` and `deploy` modes now stop with a pointer (the platform deploys when its Version PR merges). `commit-spec.md` drops package-path scopes and states that no `---agent---` block is used. `test`, `implement`, `proposal`, `e2e-platform` 1.3.2, `ready`, `postxl` 1.0.2, `jenkins-debug`, `devlog`, `doc_tiers.md` and `readiness_criteria.md` run commands and write docs in the target repo.
- `ship` 1.7.2: STEP 15 puts `Closes #<issue_number>` in the squash-merge body and then reads the issue state, closing the issue by hand with a comment if the merge left it open. Before, the skill left the keyword out of the squash body, and merged issues stayed open.
- `ship` 1.7.1: STEP 10b treats a `whats-new-fix:`-only changeset as `docs_impact=none`, matching the platform CI guard, instead of demanding a capabilities-doc edit and an ai-assets PR.

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
