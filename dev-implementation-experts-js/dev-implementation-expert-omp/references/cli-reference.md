# OMP CLI Reference

## Installation

```bash
bun install -g @oh-my-pi/pi-coding-agent
omp --version  # verify
```

## Usage

```bash
omp [flags] [messages...]
```

## Core Flags

### Model Selection
| Flag | Description | Example |
|------|-------------|---------|
| `--model <value>` | Model to use (fuzzy match) | `--model "openrouter/anthropic/claude-sonnet-4"` |
| `--smol <value>` | Fast model for lightweight tasks | `--smol "ollama/gemma3:4b"` |
| `--slow <value>` | Reasoning model for thorough analysis | `--slow "anthropic/claude-opus-4"` |
| `--plan <value>` | Model for architectural planning | `--plan "anthropic/claude-opus-4"` |
| `--provider <value>` | Provider (legacy; prefer --model) | `--provider openrouter` |
| `--api-key <value>` | API key (defaults to env vars) | `--api-key "sk-..."` |

**Model format**: `provider/model-id` — e.g.:
- `openrouter/stepfun/step-3.5-flash:free`
- `ollama/gemma3:4b`
- `anthropic/claude-sonnet-4`
- `openrouter/anthropic/claude-opus-4.6`

**NEVER** use `p-provider/model` — that format does not exist.

### Output Modes
| Flag | Description |
|------|-------------|
| `--mode text` | Default interactive terminal |
| `--mode json` | JSON output |
| `--mode rpc` | RPC mode (stdin/stdout JSON protocol) |
| `-p, --print` | Non-interactive: process prompt and exit |

### Session Management
| Flag | Description |
|------|-------------|
| `--no-session` | Ephemeral mode (no session persistence) |
| `-c, --continue` | Continue previous session |
| `-r, --resume <id>` | Resume session by ID/path/picker |
| `--session-dir <dir>` | Custom session storage directory |
| `--export <file>` | Export session to HTML and exit |

### Tool & Feature Control
| Flag | Description |
|------|-------------|
| `--no-tools` | Disable all built-in tools |
| `--tools <list>` | Comma-separated tool whitelist |
| `--no-lsp` | Disable LSP tools, formatting, diagnostics |
| `--no-pty` | Disable PTY-based interactive bash |
| `--no-skills` | Disable skill discovery |
| `--skills <glob>` | Filter skills by pattern (e.g. `git-*,docker`) |
| `--no-extensions` | Disable extension discovery |
| `--no-rules` | Disable rules discovery |
| `--no-title` | Disable title auto-generation |

### Prompt & System
| Flag | Description |
|------|-------------|
| `--system-prompt <value>` | Override system prompt |
| `--append-system-prompt <value>` | Append text/file to system prompt |
| `--thinking <level>` | Thinking level: minimal, low, medium, high, xhigh |

### Extensions & Hooks
| Flag | Description |
|------|-------------|
| `--hook <file>` | Load a hook/extension file (repeatable) |
| `-e, --extension <file>` | Load an extension file (repeatable) |

### Miscellaneous
| Flag | Description |
|------|-------------|
| `--allow-home` | Allow starting in ~ without auto-switching |
| `--models <list>` | Comma-separated model patterns for Ctrl+P cycling |
| `--list-models <search>` | List available models (fuzzy search) |

## Environment Variables

### API Keys
omp resolves API keys from environment variables in the format `{PROVIDER}_API_KEY`:

| Variable | Provider |
|----------|----------|
| `ANTHROPIC_API_KEY` | Anthropic |
| `OPENAI_API_KEY` | OpenAI |
| `OPENROUTER_API_KEY` | OpenRouter |
| `GOOGLE_API_KEY` | Google |
| `MISTRAL_API_KEY` | Mistral |
| `GROQ_API_KEY` | Groq |
| `DEEPSEEK_API_KEY` | DeepSeek |
| `XAI_API_KEY` | xAI |
| `TOGETHER_API_KEY` | Together |
| `FIREWORKS_API_KEY` | Fireworks |

### Model Roles (env alternatives)
| Variable | Equivalent flag |
|----------|----------------|
| `PI_SMOL_MODEL` | `--smol` |
| `PI_SLOW_MODEL` | `--slow` |
| `PI_PLAN_MODEL` | `--plan` |

### Special
| Variable | Description |
|----------|-------------|
| `PI_CODING_AGENT_DIR` | Point omp at a custom agent directory |

## Configuration Files

### Per-project: `.omp/settings.json`
```json
{
  "skills": {
    "enabled": true,
    "customDirectories": [
      "/path/to/custom/skills"
    ]
  }
}
```

### Global: `~/.omp/settings.json`
Global defaults for model, provider, skill directories, etc.

## Common Usage Patterns

```bash
# Interactive mode
omp

# Interactive with initial prompt
omp "List all .ts files in src/"

# Include files in message
omp @prompt.md @image.png "Analyze this"

# Non-interactive (process and exit)
omp -p "List all .ts files in src/"

# Continue previous session
omp -c

# RPC sidecar for embedding
omp --mode rpc --no-session --model "openrouter/anthropic/claude-sonnet-4"

# Minimal agent (no extras)
omp --no-lsp --no-extensions --no-skills --no-pty

# List available models
omp --list-models "claude"
omp --list-models "openrouter"
```

## Built-in Tools (30+)

| Category | Tools |
|----------|-------|
| **File** | read, write, edit (hashline), glob, grep |
| **Execution** | bash (PTY), python |
| **Navigation** | find, ls |
| **Code Intelligence** | LSP (40+ languages, format-on-write, diagnostics) |
| **Browser** | puppeteer-based |
| **Network** | web_search, fetch, ssh |
| **Agent** | task (parallel subagents, worktree/FUSE isolation) |
| **Management** | todo, ask, notebook |

### Hashline Edit Strategy
omp's edit tool uses content-hash anchors instead of str_replace, providing ~10x reliability for file modifications. The hash is computed from line content, making edits resilient to line number changes.
