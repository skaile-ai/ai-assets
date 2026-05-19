---
name: "jenkins-debug"
description: "[skaile-development] Debug a recently failed Skaile deployment on Jenkins. Defaults to the `skaile_platform` job; pass `target=store` to debug the `skaile_store` job instead. Reads the job over the local SSH tunnel (http://localhost:8090, anonymous read), finds the relevant build (last failed by default, or current/explicit), extracts the failure region from the console log, cross-references recent commits in skaile-dev around the build timestamp, and reports a structured summary with a fix hypothesis. Use when the user says \"why did the deploy fail\", \"debug the jenkins failure\", \"what broke on jenkins\", \"the platform deploy is red\", \"the store deploy is red\", \"check the last platform-deploy\", or otherwise asks about a Jenkins deployment failure."
metadata:
  version: "1.0.0"
  tags:
    - "jenkins"
    - "deploy"
    - "ci"
    - "failure"
    - "debug"
    - "platform"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: ".git"
        gate: hard
        description: "Must run from the skaile-dev shell repo (used for commit cross-reference)."
    inputs_optional:
      - id: target
        label: "Deployment to debug"
        type: select
        options:
          - "platform"
          - "store"
        default: "platform"
        hint: "platform = the `skaile_platform` job | store = the `skaile_store` job"
      - id: mode
        label: "Build selection mode"
        type: select
        options:
          - "last-failed"
          - "current"
        default: "last-failed"
        hint: "last-failed = walk recent builds backward until a FAILURE is found | current = inspect lastBuild only and report green if green"
      - id: build
        label: "Explicit build number"
        type: text
        hint: "If set, overrides `mode` and inspects exactly this build (e.g. 252)."
    reads:
      - path: "http://localhost:8090/job/<skaile_platform|skaile_store>/api/json"
        description: "Recent build list for the selected deploy job (anonymous Jenkins read)."
      - path: "http://localhost:8090/job/<skaile_platform|skaile_store>/<n>/consoleText"
        description: "Console log of the target build."
      - path: ".git/logs"
        description: "Local git history for ±2h commit cross-reference (skaile-dev shell + submodules)."
    produces: []
  user_inputs:
    dialog:
      - id: "target"
        label: "Deployment to debug (platform | store)"
        type: "select"
        options: ["platform", "store"]
        required: false
        default: "platform"
      - id: "mode"
        label: "Build selection mode"
        type: "select"
        options: ["last-failed", "current"]
        required: false
        default: "last-failed"
      - id: "build"
        label: "Explicit build number (overrides mode)"
        type: "text"
        required: false
    files: []
---

## Overview

Investigates a failed Skaile deployment by reading a Jenkins job over the local SSH tunnel
and producing a structured failure summary the user (or another agent) can act on without
copy-pasting from the Jenkins UI.

Two jobs are supported, selected by the `target` input:

| `target`           | Jenkins job       | Deploys                        |
| ------------------ | ----------------- | ------------------------------ |
| `platform` (default) | `skaile_platform` | the enterprise platform        |
| `store`            | `skaile_store`    | the public AI Asset Catalog    |

The tunnel forwards the Jenkins controller's port 8080 to `localhost:8090`. Anonymous read is
enabled, so this skill never needs credentials. It is strictly read-only — it never triggers
builds, modifies Jenkins config, or writes to any Jenkins endpoint.

## When to Use

Invoke when the user wants to know what broke on Jenkins, including phrases like:

- "why did the deploy fail"
- "debug the jenkins failure"
- "what broke on jenkins"
- "the platform deploy is red"
- "the store deploy is red" (pass `target=store`)
- "check the last platform-deploy"
- "show me the jenkins error"

## When NOT to Use

- **The user wants to retrigger a build.** This skill is read-only. Use the Jenkins UI (or a
  separate skill purpose-built for that) to re-run jobs.
- **The failure is in a non-deploy job.** Only `skaile_platform` and `skaile_store` are
  supported. Other jobs (`portfolex`, `alma`, etc.) are not in scope.
- **The user is debugging a local `bun run dev` or test failure.** Use `test`, `kill-backend`,
  or the relevant package's diagnostic skill instead — Jenkins is not involved.
- **The SSH tunnel is not up.** Step 1 fails fast and tells the user; do not attempt to start
  the tunnel automatically.
- **The user wants the Jenkins UI.** Direct them to `http://localhost:8090/job/<job>/`
  in a browser.

## Configuration

Hardcoded for this monorepo:

| Setting          | Value                                                         |
| ---------------- | ------------------------------------------------------------- |
| Jenkins base URL | `http://localhost:8090`                                       |
| Job name         | `skaile_platform` (default) or `skaile_store` (`target=store`) |
| Repo for commits | `.` (the skaile-dev shell repo)                               |

Resolve the job name from the `target` input at the start of the procedure:

```bash
case "${TARGET_DEPLOY:-platform}" in
  store)    JOB="skaile_store"    ;;
  platform) JOB="skaile_platform" ;;
  *) echo "[fail] unknown target '${TARGET_DEPLOY}' (expected platform|store)"; exit 1 ;;
esac
echo "[ok] debugging job: $JOB"
```

Use `$JOB` everywhere a job name appears below. If the tunnel uses a different port or a job
is renamed, edit this section before running.

## Procedure

Run these steps in order. Each step prints the data it collects so the user can see the chain
of reasoning, not just the final summary.

### Step 1 - Verify the tunnel

```bash
curl -sf --max-time 3 http://localhost:8090/api/json > /dev/null \
  && echo "[ok] jenkins reachable on :8090" \
  || { echo "[fail] jenkins not reachable on :8090 - is the SSH tunnel up?"; exit 1; }
```

If this fails, stop and tell the user to bring the tunnel up. Do not attempt to start it
automatically.

### Step 2 - Pick the target build

Set `BUILD` (the build number to inspect) based on inputs:

Note: the `target` input is read into `TARGET_DEPLOY` (the `JOB` resolver above); `TARGET`
below is the build *number*. Do not conflate them.

```bash
BASE="http://localhost:8090/job/$JOB"

if [ -n "$BUILD" ]; then
  TARGET="$BUILD"
elif [ "${MODE:-last-failed}" = "current" ]; then
  TARGET=$(curl -s "$BASE/lastBuild/api/json?tree=number" | python3 -c 'import json,sys;print(json.load(sys.stdin)["number"])')
else
  # last-failed: walk recent builds backward until we hit FAILURE
  TARGET=$(curl -s "$BASE/api/json?tree=builds%5Bnumber,result%5D%7B0,30%7D" \
    | python3 -c '
import json, sys
data = json.load(sys.stdin)
for b in data["builds"]:
    if b.get("result") == "FAILURE":
        print(b["number"]); break
')
fi

[ -n "$TARGET" ] || { echo "[fail] no matching build found"; exit 1; }
echo "[ok] inspecting build #$TARGET"
```

If `MODE=current` and `lastBuild.result == SUCCESS`, report "deploy is currently green
(build #N succeeded at <time>)" and exit. Do not dump logs for a passing build.

### Step 3 - Fetch build metadata

```bash
curl -s "$BASE/$TARGET/api/json?tree=number,result,timestamp,duration,url,actions%5BlastBuiltRevision%5BSHA1%5D%5D" \
  | python3 -m json.tool
```

Extract: result, timestamp (ms epoch), duration (ms), and (when present) the SHA1 the build
attempted to check out. Convert timestamp to UTC ISO-8601 for the report.

### Step 4 - Fetch the console log and extract the failure region

```bash
LOG=$(mktemp)
curl -s "$BASE/$TARGET/consoleText" > "$LOG"
wc -l "$LOG"
```

Extract the failure region using these heuristics, in order, picking the first that yields
useful context:

1. **Pipeline stage failure marker.** Search for the last occurrence of `[Pipeline] }` followed
   within ~30 lines by `Finished: FAILURE`. Use the surrounding stage block.
2. **Generic ERROR / fatal markers.** Grep for the last occurrences of `^ERROR:`, `^fatal:`,
   `Caused: `, `hudson.AbortException`, `Build step .* marked build as failure`. Take the
   ~80 lines preceding the first such line and through `Finished: FAILURE`.
3. **Tail fallback.** If neither matched, take the last 120 lines.

```bash
python3 - "$LOG" <<'PY'
import re, sys
path = sys.argv[1]
with open(path) as f:
    lines = f.readlines()

markers = re.compile(r'^(ERROR:|fatal:|Caused: |hudson\.AbortException|Build step .* marked build as failure)')
hits = [i for i, l in enumerate(lines) if markers.search(l)]
if hits:
    start = max(0, hits[0] - 20)
    end = min(len(lines), hits[-1] + 30)
    print(f"--- failure region (lines {start+1}-{end}) ---")
    sys.stdout.writelines(lines[start:end])
else:
    print("--- last 120 lines (no marker hit) ---")
    sys.stdout.writelines(lines[-120:])
PY
```

Identify and report:

- **Stage** (if a `[Pipeline] { (Stage Name)` line is visible above the failure)
- **The single error line** that best summarises the failure (usually the first `fatal:` or `ERROR:` after the last successful step)

### Step 5 - Cross-reference recent commits

Use the build timestamp (Step 3) to find commits in the skaile-dev shell within ±2 hours.
This catches the typical cause: a recent commit broke the deploy.

```bash
# TS_MS from Step 3, e.g. 1778154129968
TS_S=$(( TS_MS / 1000 ))
SINCE=$(date -u -r $((TS_S - 7200)) +'%Y-%m-%dT%H:%M:%SZ')
UNTIL=$(date -u -r $((TS_S + 7200)) +'%Y-%m-%dT%H:%M:%SZ')

git log --since="$SINCE" --until="$UNTIL" --pretty=format:'%h %ai %s' -n 20
git -C platform log --since="$SINCE" --until="$UNTIL" --pretty=format:'%h %ai %s' -n 20 2>/dev/null
git -C workspaces log --since="$SINCE" --until="$UNTIL" --pretty=format:'%h %ai %s' -n 20 2>/dev/null
# For target=store, the store submodule is the most likely culprit:
git -C store log --since="$SINCE" --until="$UNTIL" --pretty=format:'%h %ai %s' -n 20 2>/dev/null
```

If the build metadata included a SHA1, also run `git log -1 <sha>` to identify the exact
commit Jenkins tried to build.

### Step 6 - Produce the report

Print a single structured summary to chat in this exact shape (markdown):

```markdown
## Jenkins `<JOB>` — Build #<N> <RESULT>

**When:** <ISO-8601 UTC> (<duration>s)
**Stage:** <stage name or "pre-pipeline / SCM checkout" if before any stage>
**URL:** http://localhost:8090/job/<JOB>/<N>/console
**Commit attempted:** <sha if known, else "—">

### Error

\`\`\`
<the single most informative error line, or 2-5 line block>
\`\`\`

### Failure region (excerpt)

\`\`\`
<10-30 line excerpt from Step 4 — enough context, not the whole log>
\`\`\`

### Recent commits around this build (±2h)

- <sha> <date> <subject>     ← in skaile-dev / platform / agent-framework
- ...

### Likely cause

<one or two sentences — the agent's hypothesis based on the error pattern>

### Suggested fix

<concrete next step: a file to edit, a command to run, or a question to investigate>
```

Keep the failure-region excerpt short. The full console URL is in the header — readers can
click through if they need the whole log.

### Step 7 - Cleanup

```bash
rm -f "$LOG"
```

## Common Failure Patterns and Hypotheses

The skill should pattern-match the error line against these known shapes and tailor the
"Likely cause" / "Suggested fix" section accordingly:

| Error pattern                                                    | Likely cause                                                                                                          | Suggested fix                                                                                              |
| ---------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `fatal: No url found for submodule path '<X>' in .gitmodules`    | Submodule path tracked in the index without a matching `[submodule]` entry in `.gitmodules`.                          | Either commit the missing `.gitmodules` entry, or `git rm --cached <X>` and remove the orphan path.        |
| `fatal: unable to access '<repo>': Could not resolve host`       | Jenkins agent lost DNS / outbound network.                                                                            | Check the Jenkins controller's network; rerun once it is back.                                             |
| `Permission denied (publickey)` during `git fetch`               | Jenkins SSH credential rotated or removed.                                                                            | Restore the deploy key in Jenkins credentials.                                                             |
| `docker: Error response from daemon: ... no such image`          | Build image was not pushed to the registry, or tag mismatch between build and deploy stages.                          | Verify the build stage pushed the tag the deploy stage expects.                                            |
| `bun: command not found` / `node: command not found`             | Toolchain missing on the Jenkins executor.                                                                            | Install the toolchain on the agent, or move the step into a container that has it.                        |
| `ssh: connect to host <X>: Connection refused/timed out`         | Target deploy host (e.g. `alma-countries`) is down or firewalled.                                                     | Check the host directly; the issue is likely outside this repo.                                            |
| `prisma: Migration failed`                                       | Schema drift or a destructive migration ran against prod.                                                             | Inspect the migration in `platform/backend/prisma/migrations/`; coordinate before retrying.                |
| `Maximum checkout retry attempts reached`                        | Repeated SCM-step failure (look at the inner `Caused:` for the real reason).                                          | Address the inner cause; the retry wrapper is just noise.                                                  |

If none match, say so explicitly in the "Likely cause" section rather than inventing one.

## Output to the User

A single markdown block per Step 6. Do not also dump the full console log into chat — link to
the console URL instead. Keep the report self-contained: the user should be able to act on it
without opening the Jenkins UI.

## Integration

How this skill relates to others in the skaile-development domain:

- **`git`** — once the likely culprit commit is identified in Step 5, the user may want to
  revert or amend it. Hand off to `git` for the actual change.
- **`devlog`** — if the investigation surfaces a non-trivial pattern (e.g. a recurring class of
  Jenkinsfile bug, a Jenkins config drift), record it via `devlog` so future agents can learn
  from it. Skip the devlog for one-off transient failures.
- **`audit scope=diff`** — if the failure is caused by a recently-merged commit, run `audit`
  against that diff to check whether other defects rode along.
- **`release`** — when a deploy fails mid-release, the release skill consumes this skill's
  conclusions to decide whether to roll back or retry.

This skill does **not** call any other skill automatically. It produces a report and lets the
user (or the orchestrating agent) decide the next step.

## Known Limitations

- **Two-job scope.** Supports `skaile_platform` and `skaile_store` via the `target` input.
  Other jobs require generalisation.
- **Anonymous read only.** Cannot fetch build artifacts that are credential-gated, cannot
  trigger builds, cannot read job config. None of these are needed for the debug use case.
- **Pre-pipeline failures show no `wfapi` stages.** When checkout fails before any pipeline
  stage runs, `wfapi/describe` returns an empty `stages` array and the skill reports the stage
  as `pre-pipeline / SCM checkout`. This is correct, not a bug.
- **±2h commit window can miss late pushes.** If a deploy was queued long after a push, the
  cross-reference may not surface the relevant commit. The build's SHA1 (when present) is the
  authoritative answer.
- **No log persistence.** The console log is fetched into a tempfile and deleted at the end.
  If the user wants to keep it, they should `tee` it to a file before running the skill, or
  open the console URL.

## Mistakes to Avoid

- **Do not modify Jenkins.** Read-only. Never `POST` to Jenkins endpoints from this skill.
- **Do not paste the full console log into chat.** It is often thousands of lines of pipeline
  noise. Excerpt the failure region; link to the URL for the rest.
- **Do not invent a "Likely cause" when no pattern matches.** Say "none of the known patterns
  matched; manual inspection needed" — wrong hypotheses send the user down false trails.
- **Do not interpret transient network errors as code defects.** DNS failures, SSH timeouts to
  the deploy target, and registry outages are infrastructure issues, not regressions.
- **Do not start the SSH tunnel automatically.** If Step 1 fails, surface that to the user and
  stop. The tunnel is a user-managed concern.
