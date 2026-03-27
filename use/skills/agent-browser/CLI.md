# Agent Browser CLI Usage

The `agent-browser` skill provides a comprehensive CLI for browser automation, driven by Playwright. You can invoke it either directly as `agent-browser` or via `npx agent-browser:*` depending on your environment setup.

## Core Workflow

The typical workflow involves navigating to a page, taking a snapshot to identify elements, interacting with them, and closing the session. The browser persists in the background between commands.

```bash
# 1. Navigate
npx agent-browser open https://example.com/form

# 2. Get interactive elements (returns references like @e1, @e2)
npx agent-browser snapshot -i

# 3. Interact using references
npx agent-browser fill @e1 "user@example.com"
npx agent-browser click @e3

# 4. Wait for page load
npx agent-browser wait --load networkidle

# 5. Clean up
npx agent-browser close
```

## Essential Commands

### Navigation & Core
- `npx agent-browser open <url>`: Navigate to a URL.
- `npx agent-browser close`: Shut down the background browser session.

### Discovery & Extraction
- `npx agent-browser snapshot -i`: Print the accessibility tree and assign `@e` references to interactive elements.
- `npx agent-browser screenshot [path]`: Capture a screenshot of the viewport.
- `npx agent-browser screenshot --annotate`: Capture a screenshot with numeric labels corresponding to `@e` references.
- `npx agent-browser get text @e1`: Retrieve text content of an element.

### Interaction
- `npx agent-browser click @e1`: Click an element.
- `npx agent-browser fill @e1 "text"`: Clear input and type text.
- `npx agent-browser type @e1 "text"`: Type text without clearing first.
- `npx agent-browser select @e1 "option"`: Select an option in a dropdown.
- `npx agent-browser press Enter`: Simulate a keyboard keypress.

### Synchronization
- `npx agent-browser wait --load networkidle`: Wait until network traffic stops.
- `npx agent-browser wait @e1`: Wait for a specific element to be ready.
- `npx agent-browser wait 2000`: Hard wait for 2 seconds.

## Advanced Usage

### Command Chaining
You can chain commands in a single shell invocation using `&&` for performance:
```bash
npx agent-browser open https://example.com && npx agent-browser wait --load networkidle && npx agent-browser snapshot -i
```

### Session & State Management
You can maintain isolated sessions or persist authentication state across runs:
```bash
# Save authentication state after logging in
npx agent-browser state save auth.json

# Start a named isolated session
npx agent-browser --session task-1 open https://example.com
```

### Script Execution
Evaluate JavaScript directly in the page context. For complex scripts, use `--stdin` to avoid shell escaping issues:
```bash
npx agent-browser eval --stdin <<'EOF'
  return document.querySelectorAll('a').length;
EOF
```

---

## Suggested Subcommands

Here are some suggested additions or wrapper scripts that could enhance the `agent-browser` CLI experience in the future:

1. **`agent-browser assert <ref> <condition> <value>`**
   A dedicated assertion command for End-to-End testing. E.g., `agent-browser assert @e1 contains "Success"` or `agent-browser assert @e2 visible`. This would reduce the need to parse `get text` output with `grep` or external shell scripts.

2. **`agent-browser crawl <url> --depth <N> --output-dir <dir>`**
   A wrapper script for automated site mapping and content extraction. It would recursively visit links up to the specified depth, taking snapshots, capturing text, and saving screenshots of each unique page.

3. **`agent-browser extract-table <ref> --format csv`**
   A specialized command to parse an HTML `<table>` element and output it cleanly as CSV or JSON. This would simplify common tabular data extraction tasks without needing custom `eval` JavaScript injections.
