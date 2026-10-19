#!/usr/bin/env bash
#
# skaile.manifest.yaml is the publication half of this repo's canonical-identity
# contract: it names the one publisher every asset here ships under, so a
# consumer resolving `<kind>:@skaile-ai/<name>` finds it.
#
# It must NOT carry an `assets:` inventory. An inventory switches discovery to
# manifest mode, where only the listed assets exist and the glob walk is skipped
# entirely: every skill, agent, preset, persona and ruleset left off the list
# vanishes from the catalog. This repo relies on the walk, so the test pins a
# publisher-only manifest.

set -uo pipefail
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

MANIFEST="$ROOT/skaile.manifest.yaml"

assert_true "skaile.manifest.yaml exists" test -f "$MANIFEST"
[ -f "$MANIFEST" ] || assert_done

publisher="$(awk -F: '/^publisher:/{gsub(/[[:space:]"'"'"']/, "", $2); print $2; exit}' "$MANIFEST")"
assert_eq "skaile-ai" "$publisher" "publisher is skaile-ai (no leading @)"

if grep -qE '^assets:' "$MANIFEST"; then
  fail "skaile.manifest.yaml declares an assets: inventory — discovery would skip every asset not listed"
else
  pass "no assets: inventory, so discovery walks the whole tree"
fi

assert_true "the legacy publication half skaile.yaml is gone" test ! -f "$ROOT/skaile.yaml"

assert_done
