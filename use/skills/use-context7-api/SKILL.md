---
name: "use-context7-api"
description: "Use when you need to search for programming library documentation or fetch up-to-date code examples via the Context7 REST API. Provides direct CLI access to Context7 without requiring the MCP server — search libraries by name and retrieve markdown documentation with code examples. Use when the user asks about library APIs, how to use a package, or needs current documentation for a specific library version."
metadata:
  tags:
    - "context7"
    - "documentation"
    - "libraries"
    - "api"
    - "code-examples"
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "use-contract"
  env_vars:
    CONTEXT7_API_KEY: "Optional. Increases rate limits and provides access to private repositories."
---

# Use Context7 API

A standalone CLI for the Context7 REST API. Searches programming libraries and fetches live documentation with code examples — no MCP server required.

## Setup & Environment Variables

```bash
cd skills/use-context7-api
uv tool install .
```

```bash
export CONTEXT7_API_KEY="ctx7sk_..."   # optional, increases rate limits
```

## Configuration

No specific application-level configuration files are required for this skill.

## Usage

### Search for a library

```bash
use-context7-api search react
use-context7-api search express --query "How to create middleware"
```

### Fetch documentation

```bash
use-context7-api docs /facebook/react
use-context7-api docs /facebook/react --query "How to use useState"
use-context7-api docs /expressjs/express/v4.18.0 --query "routing"
```

### Check configuration

```bash
use-context7-api info
```

## Instructions

- Consult the [Use Cases](resources/use_cases.md) to see how to approach different workflows with this tool.
- Run `use-context7-api search <name>` to get the Library ID.
- Run `use-context7-api docs <library-id> --query "<question>"` to get relevant docs.
- Use the returned markdown directly in your response or save to a file.

## References

- Full CLI reference: `skills/use-context7-api/README.md`
- Source: `skills/use-context7-api/src/`
