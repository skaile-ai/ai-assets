# Deliver: watching the PR (step 9)

The goal: CI green, and no open finding that relates to the change. Read each review
the moment it lands. A review that arrives while tests run is actionable now, and the
push of its fix replaces the running checks anyway.

## The poll

The poll is a script. Its edge cases are pinned by `babysit-poll.test.sh` beside it:
do not re-implement them by hand, and change them only there, with a test.

```bash
POLL=<this skill's directory>/scripts/babysit-poll.sh
STATE="${TMPDIR:-/tmp}/babysit-<owner>-<repo>-<pr>"   # one per PR, for all rounds
"$POLL" poll --state "$STATE" --repo <slug> --pr <pr> --timeout 480 \
    [--pushed-at <`date -u +%s` taken right after YOUR last push>] > <file>
```

How to run it:

- **Foreground, `--timeout 480`.** The script is silent while it waits. A shell tool
  call ends at 600 seconds, and a background agent that prints nothing for 600 seconds
  is killed. A background run has returned an empty file.
- **Write the output to a file and read all of it.** A truncated result once hid an
  important finding for four rounds. `checks.pending` and `signals` can be null on a
  timeout: use `(.x // [])` in `jq`.
- **Keep one `--state` directory per PR.** A new directory returns every old comment
  again as a signal.
- **Never write your own wait loop**, and never use `gh pr checks --watch` (it blocks
  until the whole suite ends).

The result is one JSON object:

| `result` | Meaning | Do |
|---|---|---|
| `signal` | New reviews or comments in `signals[]`; checks can still run | Handle them now |
| `ci-terminal` | The check suite finished and was stable | Read `checks.failed` |
| `no-checks` | The repo has no CI | Treat as green |
| `timeout` (exit 4) | 480 seconds passed. Not a failure | Read `checks.pending` and `failed`, poll again |
| `error` (exit 3 or 2) | The script could not read the PR | Stop gate 8, quote the JSON |

Before you act on a signal, compare `head_sha` with the commit you pushed. A signal for
an older head, or a bot summary edited in place that says nothing new, is settled with
`decline` and another poll. A `cancelled` check left by a replaced push is normal.

**A gate that waits for a person.** When `failed` is empty and the only pending checks
are ones that wait for a person's review or approval, CI is green and the poll never
reaches `ci-terminal`. Do not poll for a person:

- Unattended: hand back at once, and name the pending check.
- Interactive: go on to the follow-up sweep; the finish step reports it.

**Budget.** No signal and no check result for about 60 minutes of polls is stop gate 8.
A long suite alone is not: platform CI takes 25 to 40 minutes.

## Bookkeeping commands

Give every item a fingerprint: `<file>:<line> <rule or short text>`.

| Command | When |
|---|---|
| `"$POLL" item --state "$STATE" "<fp>" [--blocking] [--defect]` | Before you handle an item. Prints `new`, `skip`, `repeat` or `escalate` |
| `"$POLL" fixed --state "$STATE" "<fp>"` | After the fix is pushed |
| `"$POLL" decline --state "$STATE" "<fp>"` | When you decline it. Without this the item returns in every poll |
| `"$POLL" own --state "$STATE" <id-or-url>` | For every comment or reply you post. Without this your reply reads as a new signal |

- `skip`: declined earlier. Settled.
- `repeat`: fixed earlier and it came back. Do not fix it again blindly: stop gate 8,
  with the item and your reasoning.
- `escalate`: you declined it and the reviewer now calls it blocking or a defect. Stop
  gate 8, with your reason and the objection.

## Each round

1. **Collect** the failed required checks, the review change-requests and the inline
   comments. Ask `item` about each.
2. **Classify.**
   - *Related:* the change caused or exposed it. A check that is green on the default
     branch and red on this branch is related, whichever file fails.
   - *Unrelated:* also red on the default branch, flaky infrastructure, a comment on
     code the change did not touch, or a known flaky signature from the profile
     reference that passes its discrimination test. Record it for the report. Do not
     fix it, do not re-run it to confirm, do not add a retry.
3. **Check for churn.** Count the items that fault lines added by the previous fix
   commit (`git diff HEAD~1 HEAD`). Three rounds in a row of mostly fixes-of-fixes
   means the design is wrong: stop gate 8, report the chain, offer to restructure.
4. **Apply the nit cutoff**, counted in fix pushes.
   - Fix pushes one to three: fix every related item, nits included.
   - From the fourth: fix only a failing required check, a correctness, security,
     data-loss or performance defect, an explicit blocking change-request, or a
     public-API, contract or migration problem. Decline the rest with one reply on the
     thread, and record it with `decline`. Do not argue the merit.
   - Unattended runs stop after the second fix push and hand back the open findings.
5. **Fix and push**, when anything survived. If a fix renamed something or changed a
   stated rule, grep the changed files for the old form first: a producer updated
   without its consumer is the most common review finding. Run lint and the affected
   tests, commit `fix(<scope>): address review feedback (#<n>)`, push, record `fixed`
   and `own`, and poll again with the new `--pushed-at`.
   If a fix changed behaviour a user can notice, re-run the profile reference's docs
   check for that delta.
6. **If nothing survived**, push nothing and go to the exit check. Another poll would
   only wait for a signal that cannot come.

## Exit

Leave the watch when all of these hold:

- Every required check is green, or the result was `no-checks`, or the only red checks
  are unrelated and recorded, or only a gate for a person is pending.
- The review bots finished for the latest commit, or the repo has none.
- No related finding is open. Declined items and items the reviewer marked as fine to
  keep count as closed.

Do not wait for a person who has not reviewed.

## Stop gate 8: the watch does not converge

- The poll returned `error`, or nothing happened for about 60 minutes.
- CI stays red on something unrelated that blocks the merge.
- `item` answered `repeat` or `escalate`.
- Three rounds of churn.
- More than 10 rounds in total.

Report the state and the evidence, keep the worktree, and ask how to proceed.
