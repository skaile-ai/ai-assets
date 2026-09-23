# Tests

This repo is mostly prose, but some of that prose has been extracted into
executable scripts under a skill's `scripts/` directory. This is where those
scripts get verified.

```bash
npm test                          # everything
bash tests/run.sh tests/x.test.sh # one file, while iterating
```

## Where a test file goes

Under `tests/`, mirroring the subject's path — **not** next to the subject.

Every `<domain>/skills/<name>/` directory named in `skaile.yaml` is an asset
root, and a consumer repo receives that directory wholesale: platform's
`.claude/skills/doc/` is a byte copy of `skaile-development/skills/doc/`,
`scripts/` included. A test file placed inside an asset root would ship to every
consumer and register there as asset drift.

So a test for `skaile-development/skills/ship/scripts/babysit-poll.sh` belongs at
`tests/skaile-development/skills/ship/babysit-poll.test.sh`.

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
to `run.sh`. A lane that cannot go red is worse than no lane.

## Runners

**Bash only, today.** The first subject is the `ship` skill's babysit poll,
which is shell and `jq`.

**`node --test` is deliberately not wired.** It would have no subject: the only
JavaScript here is three `.ts` files run by the `doc` skill under `bun` and
`catalogs/build.ts`, none of which `node --test` can execute without a TypeScript
runner. Add it when the first `.mjs` arrives — discovery in `run.sh` is keyed on
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
branch in `run.sh`. Wire it when the first Python subject exists — there are zero
Python tests in this repo today, so wiring it now would ship a runner that has
never been observed to go red.
