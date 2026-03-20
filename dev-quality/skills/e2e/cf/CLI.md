# App E2E CLI Reference

The `cf_test_e2e` skill utilizes the Vercel `agent-browser` CLI to perform comprehensive end-to-end testing, interact with the application UI, and capture visual artifacts. It also relies on standard database CLI tools for data validation.

## Prerequisites & Installation

Before running tests, the skill ensures `agent-browser` is installed and the browser engine is set up:

```bash
# Check version to ensure it's installed
agent-browser --version

# Install globally if missing
npm install -g agent-browser

# Install browser dependencies (required on Linux/WSL, harmless on macOS)
agent-browser install --with-deps
```

## Browser Interaction Commands

The skill uses the following commands to navigate, interact with, and inspect the application. Note that element references (`@eN`) become invalid after navigation or DOM changes and require a new snapshot.

### Navigation & Session

```bash
agent-browser open <url>              # Navigate to a specific page
agent-browser get url                 # Get the current URL
agent-browser close                   # End the browser session and clean up
```

### Element Interaction

To interact with elements, the skill first takes an interactive snapshot to get element references (`@e1`, `@e2`, etc.):

```bash
agent-browser snapshot -i             # Get interactive elements with refs (@e1, @e2...)
agent-browser click @eN               # Click an element by its reference
agent-browser fill @eN "text"         # Clear an input field and type text
agent-browser select @eN "option"     # Select a dropdown option
agent-browser press Enter             # Press a specific keyboard key (e.g., Enter, Escape)
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
agent-browser console                 # Output the browser console logs (useful for checking JS errors)
agent-browser errors                  # Output any uncaught page exceptions
```

## Database Validation

After interacting with the UI, the skill verifies that data flows correctly to the database. It uses standard CLI tools depending on the detected database:

```bash
# Postgres verification
psql "$DATABASE_URL" -c "SELECT * FROM users WHERE email = 'test@example.com'"

# SQLite verification
sqlite3 db.sqlite "SELECT * FROM users WHERE email = 'test@example.com'"
```

## Suggested Subcommands

To enhance the `cf_test_e2e` skill and provide more targeted testing utilities, the following subcommands could be implemented in the future:

1. **`e2e-report`**: A command to parse the generated `e2e-test-report.md` or testing artifacts and output a condensed, colorized summary to the terminal. It could also support exporting results to HTML or JSON formats for better CI/CD integration.
2. **`e2e-record`**: A command that records an entire testing journey into a video file (using `agent-browser` screen recording capabilities or external tools like `ffmpeg`), making it easier to debug flaky interactions or complex UI glitches that are hard to capture in static screenshots.
3. **`e2e-clean`**: A utility command to clear out old testing artifacts (such as the `e2e-screenshots/` directory, outdated test reports, and temporary database records) to ensure a pristine state before starting a fresh test run.
