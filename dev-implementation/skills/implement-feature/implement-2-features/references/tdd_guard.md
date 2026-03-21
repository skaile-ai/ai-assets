# TDD Guard

> Source: https://codeleash.dev/docs/tdd-guard/

TDD Guard is a state machine enforced through Claude Code hooks that ensures agents follow the Red-Green-Refactor cycle by blocking file edits and tracking test outcomes. The implementation is entirely Python-based.

## State Machine

Four states in a cycle:

```
initial ──→ writing_tests ──→ red ──→ making_tests_pass ──→ initial
```

- **initial** — No active TDD cycle; only Red declarations permitted
- **writing_tests** — Agent declared test expectations; test files editable only
- **red** — Test executed and failed as expected; Green declarations permitted
- **making_tests_pass** — Agent declared changes and target files; only declared prod files editable

## State Derivation

State is determined by scanning the TDD log file backwards. The last significant line determines current state:

```python
def read_state(log_path: Path) -> str:
    """Scan log bottom-up for the last significant line to derive state."""
    lines = log_path.read_text().strip().splitlines()
    for i, line in enumerate(reversed(lines)):
        stripped = line.rstrip()
        if stripped.startswith("[test]") and stripped.endswith("- SUCCEEDED"):
            return "initial"
        if stripped.startswith("[test]") and "- FAILED" in stripped:
            preceding = _find_preceding_declaration(lines, len(lines) - 1 - i)
            if preceding == "green":
                return "making_tests_pass"
            return "red"
        if stripped.startswith("## Red"):
            return "writing_tests"
        if stripped.startswith("## Green"):
            return "making_tests_pass"
    return "initial"
```

Rules:

- `[test] ... - SUCCEEDED` → `initial`
- `[test] ... - FAILED` after `## Green` → `making_tests_pass`
- `[test] ... - FAILED` after `## Red` → `red`
- `## Red ...` → `writing_tests`
- `## Green ...` → `making_tests_pass`

## CLI Interface (`tdd_log`)

Agents interact via `scripts/tdd_log.py`:

### Red Phase Declaration

```bash
uv run python -m scripts.tdd_log --log "tdd-abc123.log" red \
  --test "path/to/test_file" \
  --expects "test_name fails because ..."
```

### Green Phase Declaration

```bash
uv run python -m scripts.tdd_log --log "tdd-abc123.log" green \
  --change "what you plan to do" \
  --file "path/to/file1.py" --file "path/to/file2.py"
```

### Skip Red Cycle (refactoring, linting, coverage)

```bash
uv run python -m scripts.tdd_log --log "tdd-abc123.log" green \
  --skip-red --reason=refactoring --change "description" \
  --file "path/to/file.py"
```

### Green Validation Rules

- Without `--skip-red`: requires state to be `red` (test failed) or `making_tests_pass` (re-logging)
- With `--skip-red`: requires `--reason` from `{refactoring, lint-only, adding-coverage}`

### Override

Red or Green declarations override current state at any time, useful for stuck states. Overrides are logged for audit purposes.

## Pre-Edit Hook (`tdd_pre_edit.py`)

Runs on every `Edit` or `Write` tool call, reading current state from TDD log to permit or block edits.

### File Classification Patterns

```python
PROD_PATTERNS = [
    r"^src/",
    r"^app/",
    r"^scripts/.*\.py$",
    r"^main\.py$",
]
```

Categories:

- **e2e_test** (`tests/e2e/`) — TDD bypass
- **test** (`*.test.{ts,tsx,js,jsx}`, `test_*.py`, `tests/`, `conftest.py`) — TDD enforced
- **prod** (`src/`, `app/`, `scripts/*.py`, `main.py`) — TDD enforced
- **other** (everything else) — TDD bypass

### Permission Matrix

| State               | Test Files  | Prod Files                |
| ------------------- | ----------- | ------------------------- |
| `initial`           | Blocked     | Blocked                   |
| `writing_tests`     | **Allowed** | Blocked                   |
| `red`               | Blocked     | Blocked                   |
| `making_tests_pass` | Blocked\*   | Allowed (if in allowlist) |

\* Test files allowed only if Green was logged with `--skip-red`

### Green Allowlist Mechanism

During Green phase, only files declared in `--file` arguments are editable. The hook scans backwards from the last `## Green` header, collecting `File:` entries to build the allowlist. Undeclared files block edits with a message showing permitted files. Warning issued if allowlist exceeds 5 files.

## Post-Bash Hook (`tdd_post_bash.py`)

Runs on every `Bash` tool call, classifying commands and recording outcomes:

| Pattern                        | Tag                | State Effect       |
| ------------------------------ | ------------------ | ------------------ |
| `npm run test:e2e*`            | `ignored e2e test` | None               |
| `npm test*` or `npm run test*` | `test`             | Drives transitions |
| Everything else                | `bash`             | Logged, no change  |

Test commands tagged `test` with `SUCCEEDED` reset to `initial`. Failed tests during writing-tests phase confirm `red` state.

### Example TDD Log

```
## Red - 2026-02-24 10:30:00
Test: tests/unit/services/test_greeting_service.py
Expects: test_create_greeting fails because create() method doesn't exist yet
[test] npm run test:python -- tests/unit/services/test_greeting_service.py -v - FAILED

## Green - 2026-02-24 10:32:00
Change: Add create() method to GreetingService
File: app/services/greeting.py
[test] npm run test:python -- tests/unit/services/test_greeting_service.py -v - SUCCEEDED
```

## Plan Exit Hook (`plan_exit_hook.py`)

Runs as `PreToolUse` hook on `ExitPlanMode`. On first invocation per session:

1. Outputs TDD Planning Checklist to stderr
2. Invokes nested Claude CLI instance to review plan for TDD coverage gaps
3. Blocks tool call (exit 2), forcing agent to address feedback

Second invocation allows exit. State tracked per session ID in temp file.

## Session Start Hook (`tdd_session_start.py`)

Runs at `SessionStart` and outputs:

- TDD log filename (derived from transcript path hash)
- Copy-pasteable Red, Green, and skip-red command examples with correct `--log` value

## Per-Agent Isolation

Each Claude Code session gets a unique TDD log based on MD5 hash of transcript path:

```python
def get_log_path(input_data: dict) -> Path:
    transcript = input_data.get("transcript_path", "")
    if transcript:
        key = hashlib.md5(transcript.encode()).hexdigest()[:8]
        return Path(f"tdd-{key}.log")
    return Path("tdd.log")
```

Multiple agents in same repo (different worktrees/parallel sessions) maintain separate TDD states. All `tdd-*.log` files are gitignored.

## Key Files

- `scripts/tdd_common.py` — State derivation logic, file classification patterns
- `scripts/tdd_log.py` — CLI for Red/Green declarations
- `scripts/tdd_pre_edit.py` — Edit blocking mechanism
- `scripts/tdd_post_bash.py` — Test outcome tracking
- `scripts/plan_exit_hook.py` — Plan review enforcement
- `scripts/tdd_session_start.py` — Session initialization
