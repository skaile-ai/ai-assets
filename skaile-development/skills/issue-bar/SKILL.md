---
name: "issue-bar"
description: "[skaile-development] The bar a ticket must clear to exist: five impact
  questions (user harm, money, stability, security, team drag), each answered with
  evidence. Use before filing an issue an agent found on its own (a ship follow-up,
  a review leftover, an audit finding), when triaging incoming issues, or when
  sweeping an existing backlog for tickets that can be closed. Returns KEEP / CLOSE /
  ASK per ticket with the evidence, and a ready-to-post close comment."
version: 1.0.0
metadata:
  tags:
  - "issue-bar"
  - "triage"
  - "backlog"
  - "follow-ups"
  - "skaile-development"
  source: "NEW"
---

# Issue bar

An issue is a promise that someone will spend time on it. It earns that only when it
names a **consequence**: something bad that happens to a user, to our money, to
uptime, to security or to the team's ability to ship. A true observation about the
code is not a consequence. "This could be more robust", "a second check would be
nice", "these two names disagree" are all true and all fail the bar.

Apply the bar the same way to a ticket you are about to file and to one that already
exists.

## The five questions

Answer every question **YES** or **NO**. A YES needs its **evidence** in one line.
Without evidence, the answer is NO.

| # | Question | YES looks like |
|---|----------|----------------|
| 1 | **User harm.** Does a real user get a wrong result, lose data or work, get blocked, or see something broken, or have to work around it? | A user report, a prod log line, a repro, or a code path that normal use reaches |
| 2 | **Money.** Does it cost us money we can estimate (infra, LLM tokens, paid API calls, support hours), or hold up revenue (a deal, a customer rollout)? | A rough figure or a named deal or customer. "Costs something" is not a figure. |
| 3 | **Stability.** Will it crash, hang, wedge a session, corrupt data, crash-loop the backend, or break a deploy? | It has happened (log, incident, devlog), or you can show the trigger normal operation reaches |
| 4 | **Security.** Is there an exploitable path now: authz bypass, data leaking across user or org, exposed credentials, injection? | The attack path, step by step, with the controls it gets past |
| 5 | **Team drag.** Does it block or repeatedly slow the team: a red or flaky required check, a trap that already cost someone a real run? | The run, PR or devlog where it has already bitten someone. A trap that could bite later does not count. |

**Verdict:**

- **KEEP**: at least one YES with evidence. Put the question number and the evidence
  in the issue body.
- **CLOSE**: five NOs.
- **ASK**: you cannot answer one question from what is reachable (a security path you
  cannot rule out, prod data you cannot read, a product call). Say which question is
  open and what would settle it. Guessing either way is how the backlog fills up, and
  also how real bugs get closed.

## Likelihood × severity

A rare trigger is still a YES when the damage is **severe**: data loss, a cross-tenant
leak, credential exposure, corruption you cannot recover from. A rare trigger with
**mild** damage, like a retry, one error toast or one wasted request, is NO. Name the
trigger and say how often it fires ("every wake", "once per deploy", "needs two admins
editing the same row within 50 ms").

## Answering NO by default

These kinds of ticket answer NO on all five unless they carry specific evidence that
overturns it. Recognise them by their **shape**, even when the wording sounds urgent:

- **Defense in depth.** The primary control holds, and the ticket adds a second layer
  behind it. Q4 is YES only if you can show the primary control failing.
- **Hypothetical edges.** "If X ever happens…" where nothing in normal operation
  produces X.
- **Cleanup.** Refactors, renames, dead code, consistency, symmetry ("A validates,
  so B should too"), comment and docs polish.
- **Test gaps** on code with no known bug. A missing test is a YES only when the
  untested behaviour already broke in prod, or keeps breaking.
- **Observability wishes.** "Log more", "add a metric". YES only when a real incident
  was hard to diagnose *for lack of exactly that signal*.
- **Restatements.** Already covered by another open issue, already fixed on main, or
  already written in the PR that produced it.

## Assessing a ticket

1. Read the whole ticket: title, body and comments. For an existing issue, check
   whether main has already fixed it or another issue covers it:
   `gh issue view <n> --comments`, `gh search issues --repo <slug> "<key terms>"`,
   `git log origin/main --oneline -S "<symbol>"`.
2. Answer the five questions against the **current** code and data, not what the
   ticket assumed when it was written. A ticket written before a fix landed is a NO
   today.
3. Give the verdict, and write the record:

   ```
   #<n> <title>
   Verdict: KEEP | CLOSE | ASK
   1 User harm:  YES|NO — <evidence or "no reachable path">
   2 Money:      YES|NO — <…>
   3 Stability:  YES|NO — <…>
   4 Security:   YES|NO — <…>
   5 Team drag:  YES|NO — <…>
   Open question (ASK only): <which question, and what would settle it>
   ```

Done means every ticket in scope has a record, and every YES cites something a reader
can check.

## Closing

Closing is **outward-facing**, so the default is a report. Close only what the invoker
authorised: this ticket, this list, or "everything that scored CLOSE". Then close
through the tracker's own "not planned" state and post the evidence, so the decision
can be read and reversed:

```bash
gh issue close <n> --repo <slug> --reason "not planned" --comment "$(cat <<'EOF'
Closing against the issue bar (all five impact questions answered NO):
- User harm: <…>
- Money: <…>
- Stability: <…>
- Security: <…>
- Team drag: <…>
Reopen with evidence if any of these changes.
EOF
)"
```

A ticket filed by a **human** reporting something they saw: when it scores five NOs,
the verdict is ASK. The reporter may know about harm the code cannot show you, so
their word counts as evidence until they withdraw it.

Feature requests and roadmap items are outside the bar. Their question is "do we
want this", and that is a product decision. Mark them `ASK — product decision` and
move on.

## Before filing

The bar applies to a ticket that does not exist yet as well. One more check comes
first: if the work is small, fits the change already in flight and needs no design
decision, **fix it now** and skip the ticket. For a ticket you do file, write the
YES and its evidence into the body, so a later sweep does not have to rebuild the
argument. Reference related work as `Refs #<n>`. A closing keyword next to another
issue's number closes that issue.
