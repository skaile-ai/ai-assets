# Branch Naming for skaile-dev

## Format

```
<type>/<package-abbrev>/<slug>
```

- `<type>` — change category (see table below)
- `<package-abbrev>` — abbreviated package name (omit for monorepo-wide changes)
- `<slug>` — kebab-case description of the specific change, max 40 chars, imperative language

## Branch Types

| Type | When | Commit prefix |
|------|------|---------------|
| `feature` | New capability or user-visible feature | `feat` |
| `fix` | Bug fix in any package | `fix` |
| `refactor` | Code restructuring without behavior change | `refactor` |
| `docs` | Documentation-only changes | `docs` |
| `skill` | New or updated AI skill or domain | `feat` |
| `chore` | Dependencies, CI, tooling, build | `chore` |
| `test` | Adding or fixing tests without production code change | `test` |

## Package Abbreviations

| Package | Abbreviation |
|---------|--------------|
| `forge/project` | `forge-project` |
| `forge/concept` | `forge-concept` |
| `forge/common-backend` | `forge-common-be` |
| `forge/common-ui` | `forge-common-ui` |
| `platform/backend` | `platform-be` |
| `platform/frontend` | `platform-fe` |
| `platform` (both) | `platform` |
| `agent-framework/cli` | `cli` |
| `agent-framework/runner` | `runner` |
| `agent-framework/bridge` | `bridge` |
| `agent-framework/flow-engine` | `flow-engine` |
| `agent-framework/resolver` | `resolver` |
| `agent-framework/lab` | `lab` |
| `ai-resources/<domain>` | `ai-<domain-abbrev>` |
| `docs/` | `docs` |
| Multiple packages | omit package segment |

## Examples

```
feature/forge-project/workspace-rename
feature/forge-concept/collaborative-cursor
fix/platform-be/session-token-expiry
fix/cli/run-command-exit-code
refactor/bridge/extract-driver-interface
refactor/cli/separate-domain-parser
docs/cli/add-resource-type-reference
docs/cli/document-flow-commands
skill/skaile-development/add-devlog-skill
skill/ai-skaileup-quality/merge-cf-saxe-audit
chore/bump-bun-1-4
chore/platform-be/update-prisma
test/forge-project/add-auth-unit-tests
feature/add-skaile-development-domain    (monorepo-wide: no package segment)
```

## Slug Rules

- Lowercase, hyphens only (no underscores, no slashes inside slug)
- Imperative verb preferred: `add-`, `remove-`, `fix-`, `refactor-`, `update-`
- Avoid filler: not `changes-to-x` or `various-fixes` — be specific
- Max 40 characters for the slug segment

## Protected Branches

| Branch | Protection |
|--------|-----------|
| `main` | No direct push. PR + passing tests required. |
| `implement/*` | Created by `implement` orchestrator. Merge via `finish-branch` only. |

All other branches are fair game — create, delete, squash-merge as needed.
