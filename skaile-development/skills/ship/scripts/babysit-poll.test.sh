#!/usr/bin/env bash
#
# Pins the edges of babysit-poll.sh against recorded `gh` payloads, offline.
#
# Each scenario under fixtures/babysit-poll/ is a sequence of poll snapshots served by
# fixtures/fake-gh.sh, which also stands in for the clock and for sleep, so a 15-minute
# poll runs in milliseconds and its timing is exact. The payload shapes, the spinner body
# and the finished-review body are recorded from platform#4949, the PR whose review rounds
# found these edges in the prose version of this loop.

set -uo pipefail
ROOT="${REPO_ROOT:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && git rev-parse --show-toplevel)}"
source "$ROOT/scripts/lib/assert.sh"

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCRIPT="$HERE/babysit-poll.sh"
FAKE="$HERE/fixtures/fake-gh.sh"
FX="$HERE/fixtures/babysit-poll"
T0=1790077200 # 2026-09-22T11:40:00Z, the push of head A in every scenario

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

export BABYSIT_GH="$FAKE" BABYSIT_NOW="$FAKE __now" BABYSIT_SLEEP="$FAKE __sleep"

# start <scenario> <clock-offset-from-T0> — fresh fake state AND fresh babysit state.
start() {
  export FAKE_SCENARIO="$FX/$1.json"
  export FAKE_STATE="$TMP/$1.fake"
  BS="$TMP/$1.state"
  rm -rf "$FAKE_STATE" "$BS"
  mkdir -p "$FAKE_STATE"
  echo 1 >"$FAKE_STATE/poll"
  at "$2"
}
at() { echo $((T0 + $1)) >"$FAKE_STATE/clock"; }
snapshot() { echo "$1" >"$FAKE_STATE/poll"; }

# run [poll args...] — sets OUT (stdout JSON) and CODE. The default timeout is well past
# every expected break; it only bounds how long a regression takes to report.
run() {
  OUT="$(bash "$SCRIPT" poll --state "$BS" --repo o/r --pr 7 --timeout 200 "$@" 2>"$TMP/stderr")"
  CODE=$?
}
field() { jq -c "$1" <<<"$OUT" 2>/dev/null || echo "<unparseable: $OUT>"; }
bs() { bash "$SCRIPT" "$@" --state "$BS" 2>/dev/null; }

echo "spinner: the placeholder is not a signal; the review edited into its id is"
start spinner-edited-in-place 30
run --pushed-at $T0
assert_eq 0 "$CODE" "exits 0 on a signal"
assert_eq '"signal"' "$(field .result)" "breaks on the finished review"
assert_eq 2 "$(field .polls)" "...on the poll the edit landed, not the placeholder's"
assert_eq '["900"]' "$(field '[.signals[].id]')" "the signal is the comment id the spinner held"
assert_eq '["CI/Build & Test","Claude Code Review/claude-review"]' "$(field .checks.pending)" "breaks WITH checks still running"
run --pushed-at $T0 --timeout 60
assert_eq 4 "$CODE" "a consumed review never re-breaks the poll (it times out instead)"
assert_eq '"timeout"' "$(field .result)" "...and says so"

echo "spinner test: a finished review that QUOTES '- [ ]' is a review"
start finished-review-quotes-checkbox 120
for id in 911 912; do
  assert_contains "$(jq -r --argjson id $id '.polls["1"].issue_comments[] | select(.id == $id) | .body' "$FX/finished-review-quotes-checkbox.json")" '`- [ ]`' \
    "negative control: review $id really contains the substring the old rule keyed on"
done
run --pushed-at $T0
assert_eq '["911","912","913"]' "$(field '[.signals[].id]')" \
  "reviews quoting a box mid-line (with or without the finished header) and a finished one with a box left open are signals; the dead spinner is not"
assert_eq false "$(field .bot_review_in_flight)" "a spinner created BEFORE the push is not in flight"
run --pushed-at $T0
assert_eq '"ci-terminal"' "$(field .result)" "so the finished suite can break the poll"
assert_eq '["CI/Build & Test"]' "$(field .checks.failed)" "and reports what failed"
run --pushed-at $T0 --timeout 40
assert_eq '"timeout"' "$(field .result)" "a terminal rollup is reported once, not on every re-poll"

echo "spinner: one created after the push holds a finished suite open"
start new-spinner-blocks-ci 120
run --pushed-at $T0 --timeout 60
assert_eq '"timeout"' "$(field .result)" "no ci-terminal break while a review is in flight"
assert_eq true "$(field .bot_review_in_flight)" "the timeout names the in-flight review"
start new-spinner-blocks-ci 120
run --timeout 60
assert_eq true "$(field .bot_review_in_flight)" "without --pushed-at the in-flight test uses the commit date, erring toward waiting"

echo "spinner test is Bot-only"
start human-checkbox 30
run --pushed-at $T0
assert_eq '["930"]' "$(field '[.signals[].id]')" "a human's unchecked box is still a review"

echo "SHA scope: original_commit_id, the watermark, and the global consumed-set"
start reanchor 30
run --pushed-at $T0
assert_eq '["PRR_r1","101","102"]' "$(field '[.signals[].id]')" "round 1 delivers the review and both inline comments"
bs own "https://github.com/o/r/pull/7#discussion_r105"
snapshot 2
at 1100
run --pushed-at $((T0 + 1090))
assert_eq "$(jq -r '.polls["2"].review_comments[] | select(.id == 101) | .commit_id' "$FX/reanchor.json")" "$(jq -r .head_sha <<<"$OUT")" \
  "negative control: comment 101 has been re-anchored onto the new head"
assert_eq '["103","106"]' "$(field '[.signals[].id]')" \
  "round 2: not 99 or 101 (re-anchored from older commits; 101 also consumed), not 102 (old commit), not our reply 105; yes the comment that landed mid-fix, and the new-head one"

echo "consumed-set: (id, body hash), not updated_at"
start metadata-bump 30
run --pushed-at $T0
assert_eq '["200"]' "$(field '[.signals[].id]')" "the comment is delivered once"
snapshot 2
at 510
run --pushed-at $T0 --timeout 100
assert_eq 2 "$(field .polls)" "a metadata bump (same body, newer updated_at) does not break"
assert_eq '["200"]' "$(field '[.signals[].id]')" "an edited body under the same id does"

echo "rollup: terminal must hold across polls AND 60s after the push"
start rollup-grows 10
run --pushed-at $T0
assert_eq '"ci-terminal"' "$(field .result)" "the grown suite eventually breaks"
assert_eq 5 "$(field .polls)" "not at poll 2 (under 60s), not at poll 4 (claude-review just registered)"
assert_eq 4 "$(field .checks.total)" "and it saw the late check"
run --pushed-at $T0 --timeout 40
assert_eq '"timeout"' "$(field .result)" "the same finished suite is reported once"

echo "no-checks: 3 empty polls AND 60s"
start no-checks 10
run --pushed-at $T0
assert_eq '"no-checks"' "$(field .result)" "a CI-less repo is detected"
assert_eq 4 "$(field .polls)" "only once 60s have passed, not at the third empty poll"
start no-checks 70
run --pushed-at $T0
assert_eq 3 "$(field .polls)" "and not before the third empty poll, however long ago the push was"
start checks-late 10
run --pushed-at $T0 --timeout 100
assert_eq '"timeout"' "$(field .result)" "a suite that registers late is not mistaken for no CI"

echo "pagination"
start pagination 30
run --pushed-at $T0
assert_eq '["1031"]' "$(field '[.signals[].id]')" "a comment on page 2 is seen"
assert_true "the comment endpoints are paginated" grep -q -- '--paginate repos/o/r/pulls/7/comments' "$FAKE_STATE/calls"

echo "authorship"
start me-resolves 30
bs own "https://github.com/o/r/pull/7#issuecomment-300"
run --pushed-at $T0
assert_eq '["302"]' "$(field '[.signals[].id]')" "own ids and the resolved login are both discarded"
start me-403 30
bs own 300
run --pushed-at $T0
assert_eq '["301","302"]' "$(field '[.signals[].id]')" "a 403 on gh api user falls back to the id list, not to 'nothing is mine'"

echo "failure handling"
start bad-head 30
run --pushed-at $T0
assert_eq 3 "$CODE" "a truncated head SHA is refused"
assert_eq '"error"' "$(field .result)" "...as an error result"
start gh-flaky 30
run --pushed-at $T0
assert_eq '["940"]' "$(field '[.signals[].id]')" "one failed gh call is retried"
start gh-down 30
run --pushed-at $T0
assert_eq 3 "$CODE" "three in a row stop the poll loudly"
assert_eq 3 "$(field .polls)" "...on the third"

echo "declined-set and seen-set"
start me-403 0
assert_eq new "$(bs item 'SKILL.md:42 wording')" "an unknown item is new"
bs decline 'SKILL.md:42 wording'
assert_eq skip "$(bs item 'SKILL.md:42 wording')" "a declined item is skipped"
assert_eq escalate "$(bs item 'SKILL.md:42 wording' --defect)" "re-raised as a defect it escalates"
assert_eq new "$(bs item 'SKILL.md:42 wording')" "...and leaves the declined-set"
bs decline 'SKILL.md:42 wording'
assert_eq escalate "$(bs item 'SKILL.md:42 wording' --blocking)" "an explicit blocking change-request escalates too"
bs fixed 'x.sh:7 off-by-one'
assert_eq repeat "$(bs item 'x.sh:7 off-by-one')" "a fixed item coming back is a repeat"
bash "$SCRIPT" own not-an-id --state "$BS" 2>/dev/null
assert_eq 2 "$?" "own refuses an argument with no comment id"

assert_done
