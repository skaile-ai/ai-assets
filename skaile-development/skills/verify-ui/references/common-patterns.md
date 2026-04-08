# Common Verification Patterns

## Tool Mapping

Both tools can accomplish the same tasks. Use whichever is available.

| Action | agent-browser CLI | chrome-devtools MCP |
|--------|------------------|-------------------|
| Open URL | `agent-browser open <url>` | `navigate_page({ url })` |
| Wait for load | `agent-browser wait --load networkidle` | `wait_for({ type: "networkidle" })` |
| Snapshot (interactive) | `agent-browser snapshot -i` | `take_snapshot()` |
| Screenshot | `agent-browser screenshot` | `take_screenshot()` |
| Click element | `agent-browser click @eN` | `click({ ref: "eN" })` or `click({ selector })` |
| Fill input | `agent-browser fill @eN "text"` | `fill({ ref: "eN", value: "text" })` |
| Get URL | `agent-browser get url` | `evaluate_script({ script: "location.href" })` |
| Get page title | `agent-browser get title` | `evaluate_script({ script: "document.title" })` |
| Wait for element | `agent-browser wait "#selector"` | `wait_for({ selector: "#selector" })` |
| Close | `agent-browser close` | `close_page()` |

## Page Load Pattern

Every page verification follows this sequence:

```
1. Navigate to URL
2. Wait for networkidle (or 3s timeout as fallback)
3. Take interactive snapshot
4. Verify expected elements exist in snapshot output
5. Report pass/fail
```

**agent-browser:**
```bash
agent-browser open {url} && agent-browser wait --load networkidle && agent-browser snapshot -i
```

**chrome-devtools:**
```
navigate_page({ url }) -> wait_for({ type: "networkidle" }) -> take_snapshot()
```

## Element Verification

To check if an element exists, take a snapshot and search the output for expected text:

- **Heading check**: Look for heading text in snapshot (e.g., "Dashboard", "Projects", "Settings")
- **Button check**: Look for button labels (e.g., `[button] "Create Project"`)
- **Link check**: Look for link text (e.g., `[link] "Dashboard"`)
- **Input check**: Look for input labels or placeholders (e.g., `[textbox] "Project name"`)
- **Table check**: Look for table-related elements or column headers

If an expected element is NOT found in the snapshot:
1. Take a screenshot for visual inspection
2. Report as WARN or FAIL with the screenshot path
3. Continue to next check

## Navigation Verification

To verify a nav link works:

```
1. Snapshot current page -> find link by text
2. Click the link
3. Wait for networkidle
4. Check URL changed to expected path
5. Snapshot new page -> verify page heading matches
```

## Form Verification (Non-Destructive)

To verify a form renders correctly WITHOUT submitting:

```
1. Navigate to form page
2. Snapshot -> verify expected fields exist (inputs, selects, checkboxes)
3. Optionally fill fields with test data to verify they accept input
4. Verify submit button exists
5. Do NOT click submit
```

## Empty State Detection

Many pages show different UI when no data exists (no projects, no providers, etc.). The verification should handle both:

- **With data**: Table/list renders with rows
- **Empty state**: Empty state message or "Create your first..." prompt

Both are valid -- report what was found, not a failure.

## Error Recovery

If a page fails to load or times out:
1. Take a screenshot
2. Check browser console for errors: `agent-browser eval 'JSON.stringify(window.__CONSOLE_ERRORS__ || [])'`
3. Report the error with screenshot path
4. Navigate back to dashboard before continuing to next check
5. If dashboard also fails, abort remaining checks

## Timing

- After navigation: always wait for networkidle (timeout 5s)
- After click that triggers navigation: wait for URL change, then networkidle
- For dynamic content (modals, dropdowns): wait 500ms after click, then re-snapshot
