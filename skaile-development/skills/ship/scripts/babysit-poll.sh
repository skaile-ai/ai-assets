#!/usr/bin/env bash
#
# The `ship` skill's babysit poll (Phase 12, step (a)), as code rather than prose.
#
# WHY this is a script: the poll is a small state machine whose edges only GitHub's live
# API can confirm, and when it lived as prose seven of eight review rounds on the PR that
# wrote it found a real defect — several of them loops that could never reach their own
# exit. Here each edge is pinned by babysit-poll.test.sh against recorded `gh` payloads.
#
# Subcommands (all take --state <dir>; the dir persists across rounds of one PR):
#
#   poll    --repo <owner/repo> --pr <n> [--pushed-at <iso|epoch>] [--interval 20] [--timeout 900]
#           Polls until the FIRST actionable signal, prints one JSON object, exits.
#           Exit 0: result is "signal", "ci-terminal" or "no-checks".
#           Exit 4: result "timeout" (no signal within --timeout) -> the skill's gate #8.
#           Exit 3: result "error" (gh failed on 3 consecutive polls, or a bad head SHA).
#   own     <id-or-url>...          Record a comment YOU posted, so it is never a signal.
#   item    <fingerprint> [--blocking] [--defect]
#           Prints new | skip | escalate | repeat for a collected review item.
#   decline <fingerprint>           Record a declined nit (it is then settled).
#   fixed   <fingerprint>           Record a fixed item (it coming back is a repeat).
#
# Test seams: BABYSIT_GH (default `gh`), BABYSIT_NOW (command printing epoch seconds),
# BABYSIT_SLEEP (command taking seconds). Needs bash, jq, and sha256sum or shasum.

set -uo pipefail

GH="${BABYSIT_GH:-gh}"
NOW_CMD="${BABYSIT_NOW:-date +%s}"
SLEEP_CMD="${BABYSIT_SLEEP:-sleep}"

# Exit 2 = bad usage or unusable state. `poll` still prints a JSON error for it, so the
# skill's "quote the JSON at gate #8" holds on every non-zero exit.
die() {
  echo "babysit-poll: $*" >&2
  [ "${SUB:-}" = poll ] && jq -n --arg r "$*" '{result:"error", reason:$r}' 2>/dev/null
  exit 2
}
now() { $NOW_CMD; }

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then sha256sum | cut -c1-64; else shasum -a 256 | cut -c1-64; fi
}

STATE_DIR=""
state_file() { printf '%s/state.json' "$STATE_DIR"; }

load_state() {
  mkdir -p "$STATE_DIR" || die "cannot create state dir $STATE_DIR"
  if [ ! -s "$(state_file)" ]; then
    echo '{"consumed":[],"own_ids":[],"declined":[],"fixed":[],"head":null,"me":null,"last_poll_at":null}' >"$(state_file)"
  fi
  jq -e . "$(state_file)" >/dev/null 2>&1 || die "state file $(state_file) is not valid JSON"
}

# state_update <jq-filter> [jq args...] — atomic rewrite of the state file.
state_update() {
  local filter=$1
  shift
  local tmp
  tmp="$(mktemp "$STATE_DIR/.state.XXXXXX")"
  if jq "$@" "$filter" "$(state_file)" >"$tmp"; then mv "$tmp" "$(state_file)"; else rm -f "$tmp"; die "state update failed"; fi
}

to_epoch() { # ISO-8601 (GitHub's Z form) or bare epoch seconds
  case "$1" in
    '' | *[!0-9]*) jq -rn --arg t "$1" '$t | sub("\\.[0-9]+"; "") | fromdateiso8601' 2>/dev/null ;; # non-zero on failure
    *) echo "$1" ;;
  esac
}

# ── set subcommands ──────────────────────────────────────────────────

cmd_own() {
  [ "$#" -gt 0 ] || die "own: need at least one id or URL"
  local arg id
  for arg in "$@"; do
    # `gh pr comment` prints a URL ending in #issuecomment-<id>; `gh api` gives the id.
    id="$(printf '%s' "$arg" | sed -E 's/.*[^0-9]([0-9]+)$/\1/')"
    case "$id" in '' | *[!0-9]*) die "own: no numeric comment id in '$arg'" ;; esac
    state_update '.own_ids = ((.own_ids + [$id]) | unique)' --arg id "$id"
  done
}

cmd_item() {
  local fp="" blocking=false defect=false
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --blocking) blocking=true ;;
      --defect) defect=true ;;
      -*) die "item: unknown flag $1" ;;
      *) [ -z "$fp" ] || die "item: one fingerprint at a time"; fp=$1 ;;
    esac
    shift
  done
  [ -n "$fp" ] || die "item: need a fingerprint"
  local in_declined in_fixed
  in_declined="$(jq -r --arg f "$fp" '.declined | index($f) != null' "$(state_file)")"
  in_fixed="$(jq -r --arg f "$fp" '.fixed | index($f) != null' "$(state_file)")"
  if [ "$in_declined" = true ]; then
    # A decline is OUR severity call. A reviewer contesting it — an explicit blocking
    # change-request, or a correctness/security/data-loss claim — is new information, so it
    # leaves the declined-set and goes to the user instead of being skipped forever.
    if [ "$blocking" = true ] || [ "$defect" = true ]; then
      state_update '.declined -= [$f]' --arg f "$fp"
      echo escalate
    else
      echo skip
    fi
  elif [ "$in_fixed" = true ]; then
    echo repeat
  else
    echo new
  fi
}

cmd_record() { # cmd_record <declined|fixed> <fingerprint>
  [ "$#" -eq 2 ] || die "$1: need exactly one fingerprint"
  state_update ".$1 = ((.$1 + [\$f]) | unique)" --arg f "$2"
}

# ── poll ─────────────────────────────────────────────────────────────

REPO="" PR="" PUSHED_AT="" INTERVAL=20 TIMEOUT=900
MIN_SINCE_PUSH=60 # the rollup is a GROWING set; a just-pushed suite reads "done" for seconds
GH_FAIL_LIMIT=3

fetch() { # fetch <dir> — one snapshot from the three sources; non-zero if any failed
  local d=$1 owner_repo=$REPO
  "$GH" pr view "$PR" --repo "$REPO" \
    --json headRefOid,reviews,reviewDecision,mergeStateStatus,statusCheckRollup >"$d/pr.json" 2>"$d/err" || return 1
  # --paginate: the REST default page is 30, and a long babysit crosses it — the newest
  # comments are exactly the ones on page 2. Pages print as separate arrays; `add` joins them.
  "$GH" api --paginate "repos/$owner_repo/pulls/$PR/comments" 2>>"$d/err" | jq -s 'add // []' >"$d/review-comments.json" || return 1
  "$GH" api --paginate "repos/$owner_repo/issues/$PR/comments" 2>>"$d/err" | jq -s 'add // []' >"$d/issue-comments.json" || return 1
  jq -e 'type == "object"' "$d/pr.json" >/dev/null 2>&1 || return 1
  jq -e 'type == "array"' "$d/review-comments.json" >/dev/null 2>&1 || return 1
  jq -e 'type == "array"' "$d/issue-comments.json" >/dev/null 2>&1 || return 1
}

resolve_me() {
  # Cross-check only. Under a GitHub App installation token this 403s: there is no user.
  # An EMPTY login means "rely on the own-id list", never "nothing is mine".
  local me
  me="$("$GH" api user -q .login 2>/dev/null)" || me=""
  state_update '.me = $me' --arg me "$me"
}

# The core of one poll: pure jq over the snapshot + state. Emits the candidate signals
# (own/out-of-scope/spinners removed, NOT yet checked against the consumed-set, which needs
# a body hash jq cannot compute), plus the rollup and spinner facts.
EVAL_JQ='
def ep: if . == null then null else sub("\\.[0-9]+"; "") | fromdateiso8601 end;
def after($t): $t != null and . != null and (ep > $t);
# On an OLDER commit a review or inline comment is in scope only inside the fix window —
# after the last poll of the previous round, and no later than the push. One posted after the
# push reviews a superseded commit (a review run the push did not cancel) and would re-raise
# what was just fixed, reading as a repeat.
def in_fix_window: after($scope_since) and ((ep) <= $push_hi);
def mine: ($me != "" and .author == $me) or ((.id|tostring) as $i | any($own[]; . == $i));
# A Bot comment ANNOUNCING work: an unchecked box at the START of a line, or the
# reply-triggered placeholder wording. Line-anchored because a finished review can QUOTE
# "- [ ]" mid-line (recorded on platform#4949), and a bare substring test turns that
# review into a permanent spinner. A comment that says it finished is never a spinner.
def spinner: .author_type == "Bot"
  and ((.body | test("^\\s*\\*\\*Claude (finished|encountered an error)")) | not)
  and ((.body | test("(^|\\n)[ \\t]*[-*] \\[ \\] "))
    or (.body | test("is working(…|\\.\\.\\.)|get back to you"; "i")));

$pr[0] as $p | $p.headRefOid as $head
# Reviews come from GraphQL, where a bot login has no [bot] suffix and no type; nothing
# below needs one, since the spinner test applies to top-level comments only.
| ([$p.reviews[]? | {kind:"review", id:(.id|tostring), author:(.author.login // ""),
      state, body:(.body // ""), commit:(.commit.oid // null), at:.submittedAt}
    # An empty-bodied COMMENTED review is only the wrapper GitHub makes for inline comments
    # (a thread reply creates one too, under an id `own` cannot know); the comments
    # themselves are the signals.
    | select((.state == "COMMENTED" and (.body | test("^\\s*$"))) | not)
    | select(.commit == $head or (.at | in_fix_window))]) as $reviews
| ([$rc[0][] | {kind:"review-comment", id:(.id|tostring), author:(.user.login // ""),
      author_type:(.user.type // "User"), body:(.body // ""), path, line:(.line // .original_line),
      url:.html_url, commit:.original_commit_id, at:.created_at, reply_to:(.in_reply_to_id // null)}
    # original_commit_id, NEVER commit_id: GitHub re-anchors commit_id FORWARD per comment.
    | select(.commit == $head or (.at | in_fix_window))]) as $inline
| ([$ic[0][] | {kind:"comment", id:(.id|tostring), author:(.user.login // ""),
      author_type:(.user.type // "User"), body:(.body // ""), url:.html_url,
      created:.created_at, at:.updated_at}]) as $issue_all
| ($issue_all | map(select(.at | after($issue_since)))) as $issue
| ($issue_all | map(select(spinner and (.created | after($push_lo)))) | length > 0) as $in_flight
| ($p.statusCheckRollup // []) as $roll
| ($roll | map(if .__typename == "StatusContext" or has("context") and (has("status")|not)
      then {name:(.context // ""), terminal:((.state // "") | IN("SUCCESS","FAILURE","ERROR")), outcome:(.state // "")}
      else {name:(((.workflowName // "") + "/") + (.name // "")), terminal:((.status // "") == "COMPLETED"), outcome:(.conclusion // .status // "")}
      end)) as $checks
| {
    head: $head,
    review_decision: $p.reviewDecision,
    merge_state: $p.mergeStateStatus,
    candidates: ([$reviews[], $inline[], ($issue[] | select(spinner | not))] | map(select(mine | not))),
    bot_review_in_flight: $in_flight,
    checks: {
      total: ($checks | length),
      all_terminal: (($checks | length) > 0 and ($checks | all(.terminal))),
      signature: ($checks | map(.name) | sort | join("\n")),
      outcomes: ($checks | map(.name + "=" + .outcome) | sort | join("\n")),
      failed: [$checks[] | select(.terminal and (.outcome | IN("FAILURE","ERROR","TIMED_OUT","ACTION_REQUIRED","STARTUP_FAILURE"))) | .name],
      pending: [$checks[] | select(.terminal | not) | .name]
    }
  }
'

emit() { # emit <result> <eval-json> <signals-json> <polls>
  jq -n --arg result "$1" --argjson ev "$2" --argjson signals "$3" --argjson polls "$4" '{
    result: $result, head_sha: $ev.head, polls: $polls, signals: $signals,
    ci_terminal: ($ev.ci_terminal // false),
    checks: ($ev.checks | {total, all_terminal, failed, pending}),
    bot_review_in_flight: $ev.bot_review_in_flight,
    review_decision: $ev.review_decision, merge_state: $ev.merge_state }'
}

cmd_poll() {
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --repo) REPO=${2:-}; shift ;;
      --pr) PR=${2:-}; shift ;;
      --pushed-at) PUSHED_AT=${2:-}; shift ;;
      --interval) INTERVAL=${2:-}; shift ;;
      --timeout) TIMEOUT=${2:-}; shift ;;
      *) die "poll: unknown argument $1" ;;
    esac
    shift
  done
  [ -n "$REPO" ] && [ -n "$PR" ] || die "poll: --repo and --pr are required"
  if [ -n "$PUSHED_AT" ]; then
    local pushed_epoch
    pushed_epoch="$(to_epoch "$PUSHED_AT")" || die "cannot parse --pushed-at '$PUSHED_AT' (want UTC ISO-8601 with Z, or epoch seconds)"
    state_update '.pushed_at = $t' --argjson t "$pushed_epoch"
  fi

  [ "$(jq -r '.me == null' "$(state_file)")" = true ] && resolve_me

  local work start polls=0 gh_fails=0
  work="$(mktemp -d)"
  trap 'rm -rf "$work"' RETURN
  start="$(now)"

  while :; do
    polls=$((polls + 1))
    local t head
    t="$(now)"
    if ! fetch "$work"; then
      gh_fails=$((gh_fails + 1))
      if [ "$gh_fails" -ge "$GH_FAIL_LIMIT" ]; then
        jq -n --arg e "$(cat "$work/err" 2>/dev/null)" --argjson polls "$polls" \
          '{result:"error", reason:"gh failed on consecutive polls", detail:$e, polls:$polls}'
        return 3
      fi
    else
      gh_fails=0
      head="$(jq -r '.headRefOid // ""' "$work/pr.json")"
      # Read in FULL: a truncated or padded SHA matches nothing (or everything) and turns
      # the SHA scope into noise. Refuse rather than poll against it.
      if ! printf '%s' "$head" | grep -Eq '^[0-9a-f]{40}$'; then
        jq -n --arg h "$head" '{result:"error", reason:"headRefOid is not a full 40-hex SHA", head_sha:$h}'
        return 3
      fi

      # A new head: the last poll we made while the PREVIOUS head was current becomes the
      # watermark. Anything created after it on an older commit is a signal we never had a
      # chance to see (it landed while we were fixing), so it stays in scope.
      if [ "$(jq -r '.head // ""' "$(state_file)")" != "$head" ]; then
        state_update '.head = $h | .head_first_seen = $t | .head_commit_date = null
          | .scope_since = .last_poll_at | .terminal_streak = 0 | .prev_signature = null
          | .empty_streak = 0 | .reported_ci = null' \
          --arg h "$head" --argjson t "$t"
      fi
      # Looked up until it succeeds: a failure stored as a stand-in date would outlive the
      # blip for every later round on this head (and could veto a correct --pushed-at).
      if [ "$(jq -r '.head_commit_date == null' "$(state_file)")" = true ]; then
        local cdate
        cdate="$("$GH" api "repos/$REPO/commits/$head" -q .commit.committer.date 2>/dev/null)" || cdate=""
        local cepoch
        if [ -n "$cdate" ] && cepoch="$(to_epoch "$cdate")"; then
          state_update '.head_commit_date = $c' --argjson c "$cepoch"
        fi
      fi

      # Push time. Given explicitly (--pushed-at, ignored once older than the head's commit,
      # i.e. stale from an earlier push), it is exact. Otherwise it is bracketed, and every
      # use takes the side that errs toward WAITING, never toward exiting early or dropping
      # a signal: scoping and the in-flight spinner test use the lower bound (the head's
      # commit date), the 60s settle uses the upper bound (when we first saw this head).
      local push_lo push_hi scope_since issue_since
      # With the commit date unknown, --pushed-at is trusted and the lower bound falls back
      # to the first sighting (then scoping can only narrow, never stall).
      push_lo="$(jq -r 'if .pushed_at != null and (.head_commit_date == null or .pushed_at >= .head_commit_date) then .pushed_at else (.head_commit_date // .head_first_seen) end' "$(state_file)")"
      push_hi="$(jq -r 'if .pushed_at != null and (.head_commit_date == null or .pushed_at >= .head_commit_date) then .pushed_at else .head_first_seen end' "$(state_file)")"
      scope_since="$(jq -r '.scope_since // "null"' "$(state_file)")"
      issue_since="$(jq -rn --argjson a "$push_lo" --argjson b "$scope_since" 'if $b != null and $b < $a then $b else $a end')"

      local ev
      ev="$(jq -n \
        --slurpfile pr "$work/pr.json" --slurpfile rc "$work/review-comments.json" --slurpfile ic "$work/issue-comments.json" \
        --arg me "$(jq -r '.me // ""' "$(state_file)")" --argjson own "$(jq -c '.own_ids' "$(state_file)")" \
        --argjson scope_since "$scope_since" --argjson issue_since "$issue_since" --argjson push_lo "$push_lo" --argjson push_hi "$push_hi" \
        "$EVAL_JQ")" || { jq -n '{result:"error", reason:"evaluating the snapshot failed"}'; return 3; }

      # Consumed-set: GLOBAL (a push must not un-consume a signal whose comment merely
      # re-anchored onto the new head) and keyed on (id, body hash) for comments — a review
      # bot posts a progress placeholder and later edits the finished review into the SAME
      # id, and GitHub bumps updated_at for thread metadata alone. Reviews have no edit
      # signal at all and are keyed on id.
      local signals='[]' n i key c
      n="$(jq '.candidates | length' <<<"$ev")"
      for ((i = 0; i < n; i++)); do
        c="$(jq -c ".candidates[$i]" <<<"$ev")"
        if [ "$(jq -r .kind <<<"$c")" = review ]; then
          key="review:$(jq -r .id <<<"$c")"
        else
          key="$(jq -r '.kind + ":" + .id' <<<"$c"):$(jq -j .body <<<"$c" | sha256)"
        fi
        if [ "$(jq -r --arg k "$key" '.consumed | index($k) != null' "$(state_file)")" != true ]; then
          signals="$(jq -c --argjson c "$c" --arg k "$key" '. + [$c + {key:$k} | del(.commit, .at, .created)]' <<<"$signals")"
        fi
      done

      # Rollup: terminal AND stable — same set of checks, all terminal, on 2 consecutive
      # polls, and the settle time since the push elapsed. Compared as a SET of check names,
      # not a count, so one check swapped for another does not read as stable.
      local sig all_terminal total streak empty_streak since_push ci_ready=false no_checks=false outcomes
      sig="$(jq -r .checks.signature <<<"$ev")"
      outcomes="$(jq -r .checks.outcomes <<<"$ev")"
      all_terminal="$(jq -r .checks.all_terminal <<<"$ev")"
      total="$(jq -r .checks.total <<<"$ev")"
      if [ "$all_terminal" = true ] && [ "$(jq -r --arg s "$sig" '.prev_signature == $s and .terminal_streak > 0' "$(state_file)")" = true ]; then
        streak=$(($(jq -r '.terminal_streak' "$(state_file)") + 1))
      elif [ "$all_terminal" = true ]; then
        streak=1
      else
        streak=0
      fi
      if [ "$total" -eq 0 ]; then empty_streak=$(($(jq -r '.empty_streak // 0' "$(state_file)") + 1)); else empty_streak=0; fi
      since_push=$((t - push_hi))
      local in_flight
      in_flight="$(jq -r .bot_review_in_flight <<<"$ev")"
      local already
      already="$(jq -r --arg o "$outcomes" '.reported_ci == $o' "$(state_file)")"
      # An open post-push Bot spinner means a review is in flight whatever the rollup says.
      # Breaking on CI then would re-break on every re-poll while (e) waits for the bot.
      if [ "$in_flight" != true ] && [ "$since_push" -ge "$MIN_SINCE_PUSH" ]; then
        [ "$streak" -ge 2 ] && ci_ready=true
        [ "$empty_streak" -ge 3 ] && no_checks=true
      fi
      state_update '.terminal_streak = $s | .prev_signature = (if $at then $sig else null end)
        | .empty_streak = $e | .last_poll_at = $t' \
        --argjson s "$streak" --arg sig "$sig" --argjson at "$all_terminal" --argjson e "$empty_streak" --argjson t "$t"

      local result=""
      if [ "$(jq 'length' <<<"$signals")" -gt 0 ]; then
        result=signal
      elif [ "$already" != true ] && [ "$ci_ready" = true ]; then
        result=ci-terminal
      elif [ "$already" != true ] && [ "$no_checks" = true ]; then
        result=no-checks
      fi
      if [ -n "$result" ]; then
        # A terminal rollup is reported ONCE per outcome set: re-polling with nothing new
        # must wait for a new signal, not re-break on the same finished suite forever.
        if [ "$ci_ready" = true ] || [ "$no_checks" = true ]; then
          state_update '.reported_ci = $o' --arg o "$outcomes"
          ev="$(jq -c '.ci_terminal = true' <<<"$ev")"
        fi
        state_update '.consumed = ((.consumed + ($s | map(.key))) | unique)' --argjson s "$signals"
        emit "$result" "$ev" "$signals" "$polls"
        return 0
      fi
    fi

    if [ $(($(now) - start)) -ge "$TIMEOUT" ]; then
      jq -n --argjson polls "$polls" --argjson ev "${ev:-null}" '{result:"timeout", polls:$polls,
        head_sha:($ev.head // null), bot_review_in_flight:($ev.bot_review_in_flight // false),
        checks:(($ev.checks // {}) | {total, all_terminal, failed, pending})}'
      return 4
    fi
    $SLEEP_CMD "$INTERVAL"
  done
}

# ── dispatch ─────────────────────────────────────────────────────────

sub=${1:-}
SUB=$sub
[ -n "$sub" ] || die "usage: babysit-poll.sh <poll|own|item|decline|fixed> --state <dir> ..."
shift
declare -a REST=()
while [ "$#" -gt 0 ]; do
  case "$1" in
    --state) STATE_DIR=${2:-}; shift ;;
    *) REST+=("$1") ;;
  esac
  shift
done
[ -n "$STATE_DIR" ] || die "--state <dir> is required"
command -v jq >/dev/null 2>&1 || die "jq is required"
load_state
set -- ${REST[@]+"${REST[@]}"}

case "$sub" in
  poll) cmd_poll "$@" ;;
  own) cmd_own "$@" ;;
  item) cmd_item "$@" ;;
  decline) cmd_record declined "$@" ;;
  fixed) cmd_record fixed "$@" ;;
  *) die "unknown subcommand $sub" ;;
esac
