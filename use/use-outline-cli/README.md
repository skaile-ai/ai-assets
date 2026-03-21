# Outline Wiki Skill

An AI agent skill for searching, reading, and managing documents in [Outline](https://www.getoutline.com/) wiki instances. Works with Claude Code, Gemini CLI, Cursor, OpenAI Codex, Goose, and other AI clients supporting the [Agent Skills Standard](https://agentskills.io).

## Features

- **Search** - Full-text search across all documents and collections
- **Read** - Retrieve document content by URL ID
- **List** - Browse collections and documents
- **Create** - Create new documents with markdown content
- **Update** - Modify existing documents
- **Export** - Export documents as markdown files

Works with both [Outline Cloud](https://www.getoutline.com/) and self-hosted instances. Re-implemented using the official [Doist Outline CLI](https://github.com/Doist/outline-cli).

## Quick Start

### 1. Install outline-cli

```bash
npm install -g github:Doist/outline-cli
```

### 2. Configure authentication

You can authenticate interactively via OAuth:
```bash
ol auth login
```

Alternatively, you can provide an API token.
1. Log into your Outline wiki
2. Go to **Settings** > **API Tokens**
3. Click **Create a token**
4. Copy the generated token

Set environment variables:
```bash
export OUTLINE_API_TOKEN=your-api-key-here

# For self-hosted instances:
export OUTLINE_URL=https://wiki.yourcompany.com
```

### 3. Test connection

```bash
ol auth status
```

## Usage Examples

### Searching

```bash
# Search for documents
ol search "deployment"

# Limit results
ol search "guide" --limit 5

# Filter by collection
ol search "API" --collection abc123
```

### Reading Documents

```bash
# Read a document (rendered for terminal viewing)
ol document get doc-url-id

# Get JSON output
ol document get doc-url-id --json

# Get raw markdown
ol document get doc-url-id --raw
```

### Browsing Collections

```bash
# List all collections
ol collection list

# Get collection details
ol collection get collection-id

# List documents in a collection
ol document list --collection collection-id
```

### Creating Documents

```bash
# Create a placeholder document
ol document create --title "My New Document" --collection collection-id --publish

# Create with file content
ol document create --title "Setup Guide" --collection collection-id --file guide.md --publish

# Create as draft (unpublished)
ol document create --title "Work in Progress" --collection collection-id
```

### Updating Documents

```bash
# Update content from file
ol document update doc-url-id --file updated.md
```

### Exporting

```bash
# Save to file
ol document get doc-url-id --raw > document.md
```

## JSON Output

All commands support `--json` flag for machine-readable output:

```bash
ol search "api" --json
ol collection list --json
```

## Configuration

### Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OUTLINE_API_TOKEN` | No (if using login) | - | Your Outline API token |
| `OUTLINE_URL` | No | `https://app.getoutline.com` | API endpoint URL |

### Self-Hosted Outline

For self-hosted instances, set `OUTLINE_URL` to your instance's base URL:

```bash
export OUTLINE_URL=https://wiki.yourcompany.com
```

## Command Reference

| Command | Description | Required Arguments |
|---------|-------------|-------------------|
| `search <query>` | Search documents | query string |
| `document get <urlId>` | Get document content | document URL ID |
| `collection list` | List all collections | - |
| `document list` | List documents | - |
| `collection get <id>`| Get collection info | collection ID |
| `document create` | Create document | `--title`, `--collection` |
| `document update <urlId>`| Update document | document URL ID |
| `auth status` | Test authentication | - |

### Common Options

| Option | Description |
|--------|-------------|
| `--json` | Output as JSON |
| `--limit N` | Max results |
| `--collection ID` | Filter by collection |

## Troubleshooting

### "Error: Unauthenticated"

Set your API token:
```bash
export OUTLINE_API_TOKEN=your-api-key
```
Or authenticate interactively using `ol auth login`.

### "Connection timeout"

- Check your `OUTLINE_URL` is correct
- Verify network connectivity to the Outline server

### "Document/Collection not found"

- Verify the ID is correct
- Check you have permission to access the resource

## License

Apache 2.0
