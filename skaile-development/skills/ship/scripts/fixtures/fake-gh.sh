#!/usr/bin/env bash
#
# Offline stand-in for `gh` (and for the clock and sleep) used by babysit-poll.test.sh.
#
# Serves recorded payloads from $FAKE_SCENARIO, one JSON file per scenario:
#   user      login for `gh api user`; null -> 403, as under a GitHub App token
#   commits   {sha: iso-date} for `gh api repos/.../commits/<sha>`
#   polls     {"<n>": snapshot}, each snapshot holding
#               pr                     `gh pr view --json ...`
#               review_comments        `gh api repos/.../pulls/<n>/comments` (page 1)
#               review_comments_page2  optional page 2, served ONLY under --paginate
#               issue_comments         `gh api repos/.../issues/<n>/comments`
#               fail                   true -> every call in that poll fails
# Poll <n> falls back to the highest snapshot <= n, so a scenario lists only its changes.
#
# $FAKE_STATE holds `poll` (the snapshot index) and `clock` (epoch seconds). `__sleep N`
# advances both — one sleep is one poll interval — and `__now` prints the clock.
set -uo pipefail

st=${FAKE_STATE:?FAKE_STATE unset}
case "${1:-}" in
  __now) cat "$st/clock"; exit 0 ;;
  __sleep)
    echo $(($(cat "$st/clock") + ${2:-0})) >"$st/clock"
    echo $(($(cat "$st/poll") + 1)) >"$st/poll"
    exit 0
    ;;
esac

scen=${FAKE_SCENARIO:?FAKE_SCENARIO unset}
echo "$*" >>"$st/calls"
snap="$(jq -c --argjson n "$(cat "$st/poll")" \
  '[.polls | to_entries[] | select((.key | tonumber) <= $n)] | max_by(.key | tonumber) | .value // empty' "$scen")"
[ -n "$snap" ] || { echo "fake-gh: no snapshot for poll $(cat "$st/poll") in $scen" >&2; exit 1; }
part() { jq -c --arg k "$1" '.[$k] // empty' <<<"$snap"; }

if [ "${1:-}" = api ] && [ "${2:-}" = user ]; then
  user="$(jq -r '.user // empty' "$scen")"
  [ -n "$user" ] && { echo "$user"; exit 0; }
  echo 'gh: Resource not accessible by integration (HTTP 403)' >&2
  exit 1
fi

[ "$(part fail)" = true ] && { echo "fake-gh: injected failure" >&2; exit 1; }

args=" $* "
case "$args" in
  " pr view "*) part pr ;;
  *" api "*"/pulls/"*"/comments "*)
    part review_comments
    case "$args" in *" --paginate "*) part review_comments_page2 ;; esac
    ;;
  *" api "*"/issues/"*"/comments "*) part issue_comments ;;
  *" api "*"/commits/"*)
    sha=${args##*/commits/}
    sha=${sha%% *}
    date="$(jq -r --arg s "$sha" '.commits[$s] // empty' "$scen")"
    [ -n "$date" ] || { echo 'gh: Not Found (HTTP 404)' >&2; exit 1; }
    echo "$date"
    ;;
  *) echo "fake-gh: unhandled call: $*" >&2; exit 1 ;;
esac
