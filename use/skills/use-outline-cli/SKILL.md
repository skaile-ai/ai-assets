---
name: "use-outline-cli"
description: "Search, read, and manage Outline wiki documents. Use when: (1) searching
  wiki for documentation, (2) reading wiki pages or articles, (3) listing wiki collections
  or documents, (4) creating or updating wiki content, (5) exporting documents as
  markdown. Works with any Outline wiki instance (self-hosted or cloud)."
license: "Apache-2.0"
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
  - "contract:use-contract"
  author: "sanjay3290"
---

# Outline Wiki Skill

Search, read, create, and manage documents in any Outline wiki instance using the official Doist Outline CLI. Works with all AI clients supporting the Agent Skills Standard.

## Setup

1. This skill requires the `ol` CLI to be installed globally.
   ```bash
   npm install -g github:Doist/outline-cli
   ```

2. Get your API key from your Outline wiki:
   - Go to **Settings > API Tokens**
   - Create a new token with appropriate permissions

3. Set up authentication:
   You can either run an interactive login to set up OAuth:
   ```bash
   ol auth login
   ```
   Or set the environment variables (e.g., in a `.env` file or exported in your shell):
   ```bash
   export OUTLINE_API_TOKEN=your-api-key-here
   # Optional: for self-hosted instances
   export OUTLINE_URL=https://your-wiki.example.com
   ```

## Usage

### Search documents
```bash
ol search "deployment guide"
ol search "API documentation" --limit 10
ol search "onboarding" --collection <id>
```

### Read a document
```bash
ol document get <document-url-id>
ol document get <document-url-id> --raw
```

### List collections
```bash
ol collection list
```

### List documents in a collection
```bash
ol document list --collection <id>
```

### Get collection details
```bash
ol collection get <collection-id>
```

### Create a document
```bash
ol document create --title "New Guide" --collection <id> --publish
ol document create --title "Guide" --collection <id> --file content.md --publish
ol document create --title "Draft" --collection <id>
```

### Update a document
```bash
ol document update <document-url-id> --file updated.md
```

### Delete a document
```bash
ol document delete <document-url-id> --confirm
```

### Export document as markdown
```bash
ol document get <document-url-id> --raw > doc.md
```

### Test authentication
```bash
ol auth status
```

## JSON Output

Add `--json` flag to any command for machine-readable parsing (e.g. by other agents):
```bash
ol search "query" --json
ol document get <id> --json
```

## Operations Reference

| Command | Description |
|---------|-------------|
| search | Full-text search |
| document get | Get document content |
| collection list | List all collections |
| document list | List docs (optionally in collection) |
| collection get | Get collection details |
| document create | Create new document |
| document update | Update existing document |
| document delete | Delete a document |
| document get --raw | Get raw markdown |
| auth status | Check status of API connection |

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| OUTLINE_API_TOKEN | No (if using login) | - | Your Outline API token |
| OUTLINE_URL | No | https://app.getoutline.com | Outline instance base URL |

## Troubleshooting

| Error | Solution |
|-------|----------|
| Token not found | Set OUTLINE_API_TOKEN environment variable or run `ol auth login` |
| Authentication failed | Verify API token is valid and not expired |
| Connection timeout| Verify OUTLINE_URL and network connectivity |
| Document not found| Verify you are using the document URL ID |
| ol: command not found | Run `npm install -g github:Doist/outline-cli` |

## Exit Codes

- **0**: Success
- **1**: Error (auth failed, not found, invalid request)

## Workflow

1. Run `ol auth status` to verify connection
2. Run `ol collection list` to see available collections
3. Run `ol search` or `ol document list` to find content
4. Run `ol document get` to get full document content
5. Use `ol document create`/`ol document update` to modify wiki content
