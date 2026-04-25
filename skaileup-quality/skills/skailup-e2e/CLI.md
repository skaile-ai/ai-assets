# e2e CLI Reference

## Trigger

Invoke with: `/e2e`, "test the app", "E2E tests", "browser testing", "test the user journey"

## Prerequisites & Installation

Before running tests, the skill ensures `agent-browser` is installed:

```bash
# Check version
agent-browser --version

# Install globally if missing
npm install -g agent-browser

# Install browser dependencies (required on Linux/WSL, harmless on macOS)
agent-browser install --with-deps
```

## Browser Interaction Commands

Element references (`@eN`) become invalid after navigation or DOM changes — always
re-snapshot after page changes.

### Navigation & Session

```bash
agent-browser open <url>              # Navigate to a specific page
agent-browser get url                 # Get the current URL
agent-browser close                   # End the browser session and clean up
```

### Element Interaction

```bash
agent-browser snapshot -i             # Get interactive elements with refs (@e1, @e2...)
agent-browser click @eN               # Click an element by its reference
agent-browser fill @eN "text"         # Clear an input field and type text
agent-browser select @eN "option"     # Select a dropdown option
agent-browser press Enter             # Press a specific keyboard key
agent-browser get text @eN            # Get the text content of a specific element
```

### Visual & Layout Testing

```bash
agent-browser screenshot <path>       # Save a screenshot to the specified path
agent-browser screenshot --annotate   # Save a screenshot with numbered element labels
agent-browser set viewport W H        # Set viewport dimensions (e.g., 375 812 for mobile)
```

### Synchronization & Debugging

```bash
agent-browser wait --load networkidle # Wait for the page network activity to settle
agent-browser console                 # Output browser console logs
agent-browser errors                  # Output uncaught page exceptions
```

## Database Validation

After UI interactions, verify data flows correctly to the database:

```bash
# Postgres
psql "$DATABASE_URL" -c "SELECT * FROM users WHERE email = 'test@example.com'"

# SQLite
sqlite3 db.sqlite "SELECT * FROM users WHERE email = 'test@example.com'"
```

## Recommended Workflow Position

```
implement → audit → ready → e2e → verify
                              ↑
                         Run this here
```
