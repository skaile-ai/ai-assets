# Worktree Patterns for the Skaile Repos

Git worktrees let you check out multiple branches of the same repository simultaneously,
each in its own directory. In a skaile repo, this is useful for parallel, unrelated work.

Every skaile repo is standalone (the `skaile-dev` shell is archived; no repo has submodules).
A worktree goes **beside** the clone, never inside it:

```
<repos_root>/                  ← parent directory of the clone
  ├── platform/                ← the main checkout
  └── .worktrees/
      └── platform-<slug>/     ← one worktree per branch
```

Why beside, not inside: the main checkout stays clean without a `.gitignore` entry (brand and
ai-assets do not ignore `.worktrees/`), and it is the same path the `ship` skill and existing
tooling use. The worktree root holds the repo's own `CLAUDE.md`, so agents working there load it.

## When to Use Worktrees

**Use a worktree when:**
- You are working on two genuinely independent changes simultaneously
- A subagent needs its own filesystem context without disturbing the main checkout
- You want to test a branch in isolation while keeping main clean

**Do NOT use a worktree when:**
- The two changes touch the same files — they'll conflict in the worktree too
- The work is sequential (one after the other) — just use a feature branch
- You only need to quickly check something on another branch — use `git stash` + checkout instead

## Setup Pattern

```bash
# From the main checkout of <repo>
git fetch origin
mkdir -p ../.worktrees
# Use an absolute path: `git -C <repo> worktree add <relpath>` resolves relative to <repo>
git worktree add "$(cd .. && pwd)/.worktrees/<repo>-<slug>" -b feature/<pkg>/<slug> origin/main
```

## Working Inside a Worktree

```bash
cd <repos_root>/.worktrees/<repo>-<slug>
bun install           # a fresh worktree has no node_modules; install at the worktree root
# ... make changes ...
git add -p
git commit -m "feat(<pkg>): <description>"
```

The worktree shares the `.git` history with the main checkout. Changes committed in the
worktree are visible in the main checkout via `git log feature/<pkg>/<slug>`.

## Merging Back

After work in the worktree is complete and tests pass:

```bash
# From the main checkout (not inside the worktree)
git merge --squash feature/<pkg>/<slug>
git commit -m "feat(<pkg>): <summary>"
git branch -d feature/<pkg>/<slug>
git worktree remove <repos_root>/.worktrees/<repo>-<slug>
```

On a repo whose `main` is protected (platform, workspaces), push the branch and open a PR
instead; the squash merge happens on GitHub.

## Teardown (without merging)

```bash
# Discard worktree and branch
git worktree remove <repos_root>/.worktrees/<repo>-<slug> --force
git branch -D feature/<pkg>/<slug>
```

## Parallel Subagent Pattern

When dispatching two independent subagents simultaneously (e.g., via `implement-supervised`
with two unrelated task groups):

```
<repos_root>/<repo>/ ← main checkout; the orchestrator lives here, reads shared files
<repos_root>/.worktrees/<repo>-task-a/ ← Subagent A works here (branch: feature/pkg/task-a)
<repos_root>/.worktrees/<repo>-task-b/ ← Subagent B works here (branch: feature/pkg/task-b)
```

After both agents complete:
1. Merge task-a into main checkout feature branch
2. Run full regression tests
3. Merge task-b into main checkout feature branch
4. Run full regression tests again
5. Teardown both worktrees

## Cautions

- **Worktree commits are local-only until pushed.** `git worktree remove` plus
  `git branch -D` destroys unpushed commits permanently. Check before cleanup:

  ```bash
  git -C <worktree> log --oneline @{upstream}..HEAD   # must be empty
  ```

- A fresh worktree has no `node_modules`. Run `bun install` at the worktree root (not at a
  parent directory) before tests, typecheck or lint, and again after merging `origin/main`.
- Worktrees inherit `.env` files from the checked-out branch, not the main checkout.
  Verify environment setup inside the worktree before running services.
- Stale worktrees that were forcefully removed can cause `git worktree list` to show ghost
  entries. Fix with `git worktree prune`.
