#!/usr/bin/env bash
#
# Runs the `migrate-skaile-manifest` smoke fixture: scripts/migrate.py rewrites
# skaile-development/skills/migrate-skaile-manifest/scripts/smoke_fixture/skaile.legacy.yaml
# and the output must equal expected.yaml byte for byte. It is the only thing
# that pins the dependency-ref grammar the skill teaches (`kind:@publisher/name`,
# `bundle:` -> `preset:`), so a change to migrate.py or to the grammar that is not
# mirrored in expected.yaml fails here instead of in a consumer's migration.
#
# Lives in scripts/, not next to the skill: a test inside a skill directory is
# copied wholesale into every consumer repo (see scripts/TESTING.md).
#
# migrate.py declares its dependency (ruamel.yaml) inline (PEP 723), so it needs
# `uv`. CI installs uv; a machine without it skips this file locally but fails
# under CI=true, where a skipped lane would be false confidence.
set -uo pipefail
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

SKILL="$ROOT/skaile-development/skills/migrate-skaile-manifest"
FIXTURE="$SKILL/scripts/smoke_fixture"

if ! command -v uv >/dev/null 2>&1; then
  if [ -n "${CI:-}" ]; then
    fail "uv is not installed, and CI must run the migrate-skaile-manifest smoke test"
    assert_done
  fi
  echo "   - skipped: uv is not installed (https://docs.astral.sh/uv/); CI runs this test"
  exit 0
fi

assert_true "smoke fixture input exists" test -f "$FIXTURE/skaile.legacy.yaml"
assert_true "smoke fixture expectation exists" test -f "$FIXTURE/expected.yaml"

out="$(mktemp)"
trap 'rm -f "$out"' EXIT
(cd "$SKILL" && uv run scripts/migrate.py \
  --in scripts/smoke_fixture/skaile.legacy.yaml \
  --out "$out" \
  --publisher-map '{"_bare_default": "skaile-ai"}') >/dev/null 2>&1
assert_eq 0 "$?" "migrate.py exits 0 on the smoke fixture"

diff_out="$(diff -u "$FIXTURE/expected.yaml" "$out" 2>&1)"
assert_eq "" "$diff_out" "migrated output equals expected.yaml"

assert_done
