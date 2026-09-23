# Tests

This repo is mostly prose, but some of that prose has been extracted into
executable scripts under a skill's `scripts/` directory. This is where those
scripts get verified.

```bash
npm test                                      # everything
bash scripts/run-tests.sh path/to/x.test.sh   # one file, while iterating
```

## Where a test file goes

**Next to the thing it covers**, the same way `scripts/*.test.mjs`,
`scripts/resolve-review-scope.test.sh` and `deploy/bin/*.test.sh` sit beside
their subjects in the platform repo.

| subject | test |
| --- | --- |
| `skaile-development/skills/ship/scripts/babysit-poll.sh` | `skaile-development/skills/ship/scripts/babysit-poll.test.sh` |
| something repo-level, like `skaile.yaml` | `scripts/<name>.test.sh` |

Discovery is a repo-wide `find` for `*.test.sh`, pruning `node_modules`, `.git`,
`__pycache__` and `.venv`. Nothing registers a test; putting the file in place is
enough.

### The consequence to know about

A skill directory named in `skaile.yaml` is copied into consumer repos
**wholesale** — `installer.ts` calls `cpSync(dir, dest, { recursive: true })`,
with no filter, and there is no exclude or ignore mechanism anywhere in the
deploy path. So a co-located test ships into every consumer alongside the script
it covers: platform gets `.claude/skills/ship/scripts/babysit-poll.test.sh`.

That is unavoidable while the script itself has to ship — it has to be there for
the skill to run — and it is **not** asset drift, because the deploy rewrites
`skaile.lock.yaml` as it copies. The real cost is smaller and worth knowing: a
test-only edit changes a tracked hash in every consumer repo, so test iteration
produces cross-repo lock churn.

Keeping tests in a separate root would avoid that, at the price of splitting them
from their subjects. This repo chose the convention.

## Writing one

A test is a bash file ending in `.test.sh`. It passes by exiting 0.

```bash
#!/usr/bin/env bash
set -uo pipefail
source "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/lib/assert.sh"

assert_eq "expected" "$actual" "what this pins"
assert_true "the file is there" test -f "$REPO_ROOT/some/path"
assert_done   # required — this is what turns failures into a non-zero exit
```

`$REPO_ROOT` is exported by the runner. `lib/assert.sh` holds `assert_eq`,
`assert_true`, `assert_contains`, `fail` and `assert_done`; helpers record
failures and continue, so one file reports every problem it found.

## Two properties the runner guarantees

- **Zero discovered tests fails the run.** A glob that matches nothing exits 0
  in the naive form, which converts a rename into silent, permanent green.
- **Every test runs even after one fails**, so a single break does not hide the
  ones behind it.

Both are verified by deliberately breaking them — do that again after any change
to `run-tests.sh`. A lane that cannot go red is worse than no lane.

## Runners

**Bash only, today.** The first subject is the `ship` skill's babysit poll,
which is shell and `jq`.

**`node --test` is deliberately not wired.** It would have no subject: the only
JavaScript here is three `.ts` files run by the `doc` skill under `bun` and
`catalogs/build.ts`, none of which `node --test` can execute without a TypeScript
runner. Add it when the first `.mjs` arrives — discovery in `run-tests.sh` is keyed on
the file extension, so it is a few lines.

**Python is deliberately not wired, and this is not an oversight.** Python is by
far the dominant scripting language here — 122 `.py` files, 89 of them inside a
skill's `scripts/` directory — so the obvious move is to glob `test_*.py` and run
`python3 -m unittest`. That would be the wrong lane. 84 of those scripts carry a
PEP 723 `# /// script` block and are launched by `uv run`, and their imports are
overwhelmingly third-party (`rich` in 95 files, `typer`, `pydantic-ai`, `yaml`,
`httpx`, `pdfminer`, `pillow`). A stdlib `unittest` lane would therefore fail
with `ModuleNotFoundError` for the typical subject while reporting green for the
handful of pure-stdlib ones — a runner that is red for the normal case is worse
than no runner, because someone will "fix" it by not writing the test.

The correct Python lane is `uv run`, which resolves each script's own PEP 723
block. It costs an `astral-sh/setup-uv` step in `test.yml` and a second discovery
branch in `run-tests.sh`. Wire it when the first Python subject exists — there are zero
Python tests in this repo today, so wiring it now would ship a runner that has
never been observed to go red.
