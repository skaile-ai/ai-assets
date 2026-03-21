# TDD Guard

> Source: https://codeleash.dev/docs/tdd-guard/

TDD Guard is a state machine enforced through Claude Code hooks that ensures agents follow the Red-Green-Refactor cycle by blocking file edits and tracking test outcomes. The implementation is Python-based.

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

State is determined by scanning the TDD log file backwards:

```python
def read_state(log_path: Path) -> str:
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

## CLI Interface (`tdd_log`)

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
  --file "path/to/file1" --file "path/to/file2"
```

### Skip Red Cycle (refactoring, linting, coverage)

```bash
uv run python -m scripts.tdd_log --log "tdd-abc123.log" green \
  --skip-red --reason=refactoring --change "description" \
  --file "path/to/file"
```

## Permission Matrix

| State               | Test Files  | Prod Files                |
| ------------------- | ----------- | ------------------------- |
| `initial`           | Blocked     | Blocked                   |
| `writing_tests`     | **Allowed** | Blocked                   |
| `red`               | Blocked     | Blocked                   |
| `making_tests_pass` | Blocked\*   | Allowed (if in allowlist) |

\* Test files allowed only if Green was logged with `--skip-red`

## File Classification

- **test** (`*.test.*`, `*.spec.*`, `test_*.py`, `tests/`, `conftest.py`) — TDD enforced
- **prod** (`src/`, `app/`, `scripts/*.py`, `main.py`) — TDD enforced
- **e2e** (`tests/e2e/`) — TDD bypass (integration context)
- **other** — TDD bypass

## Example TDD Log

```
## Red - 2026-02-24 10:30:00
Test: tests/unit/services/test_greeting_service.py
Expects: test_create_greeting fails because create() method doesn't exist yet
[test] run tests -- tests/unit/ - FAILED

## Green - 2026-02-24 10:32:00
Change: Add create() method to GreetingService
File: app/services/greeting.py
[test] run tests -- tests/unit/ - SUCCEEDED
```

## Per-Agent Isolation

Each Claude Code session gets a unique TDD log based on MD5 hash of transcript path.
Multiple agents in same repo maintain separate TDD states.
All `tdd-*.log` files are gitignored.

## Key Files

- `scripts/tdd_common.py` — State derivation, file classification
- `scripts/tdd_log.py` — CLI for Red/Green declarations
- `scripts/tdd_pre_edit.py` — Edit blocking (PreToolUse hook)
- `scripts/tdd_post_bash.py` — Test outcome tracking (PostToolUse hook)
- `scripts/tdd_session_start.py` — Session initialization
