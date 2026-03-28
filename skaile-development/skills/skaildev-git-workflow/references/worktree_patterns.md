# Worktree Patterns for skaile-dev

Git worktrees let you check out multiple branches of the same repository simultaneously,
each in its own directory. In skaile-dev, this is useful for parallel, unrelated work.

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
# 1. Create the feature branch (don't check it out in current directory)
git checkout -b feature/<pkg>/<slug>
git checkout -   # back to previous branch

# 2. Create the worktree
mkdir -p .worktrees
git worktree add .worktrees/<slug> feature/<pkg>/<slug>

# 3. (Ignore worktrees in git — they are local only)
echo ".worktrees/" >> .gitignore
git add .gitignore && git commit -m "chore: ignore local worktrees"
```

## Working Inside a Worktree

```bash
cd .worktrees/<slug>
bun install           # installs in the worktree context
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
git worktree remove .worktrees/<slug>
```

## Teardown (without merging)

```bash
# Discard worktree and branch
git worktree remove .worktrees/<slug> --force
git branch -D feature/<pkg>/<slug>
```

## Parallel Subagent Pattern

When dispatching two independent subagents simultaneously (e.g., via `implement-supervised`
with two unrelated task groups):

```
Main checkout (./) ← orchestrator lives here, reads shared files
  ├── .worktrees/task-a/ ← Subagent A works here (branch: feature/pkg/task-a)
  └── .worktrees/task-b/ ← Subagent B works here (branch: feature/pkg/task-b)
```

After both agents complete:
1. Merge task-a into main checkout feature branch
2. Run full regression tests
3. Merge task-b into main checkout feature branch
4. Run full regression tests again
5. Teardown both worktrees

## Cautions

- `node_modules/` and `bun.lock` are shared via the workspace root; running `bun install`
  in a worktree may cause lock file conflicts. Prefer running install from the main checkout.
- Worktrees inherit `.env` files from the checked-out branch, not the main checkout.
  Verify environment setup inside the worktree before running services.
- Stale worktrees that were forcefully removed can cause `git worktree list` to show ghost
  entries. Fix with `git worktree prune`.
