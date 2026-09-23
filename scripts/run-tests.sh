#!/usr/bin/env bash
#
# Test runner for skaile-ai/ai-assets.
#
# Discovers every `*.test.sh` in the repository and runs each one in its own
# subshell. A test passes by exiting 0 and fails by exiting non-zero.
#
# Two rules this runner exists to enforce:
#
#   1. Finding zero tests is a FAILURE, not a pass. A discovery glob that
#      matches nothing exits 0 in the naive form, which turns the lane into a
#      generator of false confidence the moment a rename or a moved directory
#      makes the pattern miss.
#   2. One failing test fails the run, but every test still runs, so a single
#      break does not hide the ones behind it.
#
# A test sits NEXT TO the thing it covers, the way `scripts/*.test.mjs` and
# `deploy/bin/*.test.sh` do in the platform repo. So a test for
# `skaile-development/skills/ship/scripts/babysit-poll.sh` is that script's
# neighbour, and a test for something repo-level lives here in `scripts/`.
#
# Know the one consequence: a skill directory named in `skaile.yaml` is copied
# into consumer repos wholesale (`cpSync` recursive, no filter and no exclude
# mechanism anywhere in the installer), so a co-located test ships alongside the
# script it covers. That is unavoidable while the script itself has to ship, and
# it is not drift — the deploy rewrites the lock — but a test-only edit does
# change a hash in every consumer's `skaile.lock.yaml`.
#
# Usage:
#   npm test                       # all tests
#   bash scripts/run-tests.sh      # same
#   bash scripts/run-tests.sh <path>   # one test file, for iterating

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export REPO_ROOT

declare -a FILES=()
if [ "$#" -gt 0 ]; then
  for arg in "$@"; do
    if [ ! -f "$arg" ]; then
      echo "runner: no such test file: $arg" >&2
      exit 2
    fi
    FILES+=("$arg")
  done
else
  # Repo-wide, because tests live beside their subjects. The prunes keep the
  # walk off dependency and VCS trees; everything else is fair game.
  while IFS= read -r f; do
    FILES+=("$f")
  done < <(
    find "$REPO_ROOT" \
      \( -name node_modules -o -name .git -o -name __pycache__ -o -name .venv \) -prune \
      -o -type f -name '*.test.sh' -print | sort
  )
fi

if [ "${#FILES[@]}" -eq 0 ]; then
  echo "runner: discovered 0 tests in the repository — refusing to report success." >&2
  echo "runner: a lane that passes with nothing to run is worse than no lane." >&2
  exit 1
fi

passed=0
declare -a FAILED=()

for f in "${FILES[@]}"; do
  rel="${f#"$REPO_ROOT"/}"
  echo "── $rel"
  if bash "$f"; then
    passed=$((passed + 1))
  else
    status=$?
    FAILED+=("$rel (exit $status)")
    echo "   FAIL: $rel exited $status" >&2
  fi
done

echo
echo "runner: ${#FILES[@]} test file(s), $passed passed, ${#FAILED[@]} failed"

if [ "${#FAILED[@]}" -gt 0 ]; then
  for entry in "${FAILED[@]}"; do
    echo "  failed: $entry" >&2
  done
  exit 1
fi
