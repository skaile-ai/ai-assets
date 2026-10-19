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
# under CI=true, where a skipped lane would be false confidence. `uv run` resolves
# ruamel.yaml from PyPI on a cold cache, so this is the one test in the suite that
# needs the network: a red run with a resolver error is a network blip, not a regression.
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
err="$(mktemp)"
trap 'rm -f "$out" "$err"' EXIT
(cd "$SKILL" && uv run scripts/migrate.py \
  --in scripts/smoke_fixture/skaile.legacy.yaml \
  --out "$out" \
  --publisher-map '{"_bare_default": "skaile-ai"}') >/dev/null 2>"$err"
status=$?
[ "$status" -eq 0 ] || echo "   migrate.py stderr: $(cat "$err")" >&2
assert_eq 0 "$status" "migrate.py exits 0 on the smoke fixture"

# The fixture's `bundle:skaile-development@skaile#^2.0` loses its bundle-era pin. That is an
# absence in the output, which expected.yaml cannot encode, so the warning is the only signal.
assert_true "migrate.py warns that the bundle-era pin was dropped" grep -q "dropped pin" "$err"

diff_out="$(diff -u "$FIXTURE/expected.yaml" "$out" 2>&1)"
assert_eq "" "$diff_out" "migrated output equals expected.yaml"

assert_done
