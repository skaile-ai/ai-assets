---
name: huggingface
description: "Use when an agent needs live data from the Hugging Face Hub - search
  and compare models, datasets and Spaces, read model and dataset cards, licenses
  and repo files, inspect dataset configs, splits and preview rows, browse trending
  models and daily papers, or search the Hugging Face docs. Wraps Hugging Face's
  official hosted MCP server (huggingface.co/mcp) over streamable HTTP. Open server:
  works with no sign-in and no key at anonymous rate limits, read-only, so it is
  safe to put in any job description or agent fleet."
version: 0.1.0
transport: http
url: https://huggingface.co/mcp
keywords:
  - huggingface
  - models
  - datasets
  - papers
  - documentation
  - mcp
  - remote
  - read-only
---

# Hugging Face MCP Server (remote)

Hugging Face's official, Hugging Face-hosted MCP server over the Hub. This is a
**remote** asset: there is no binary or Docker image to package - the runner opens a
streamable-HTTP connection straight to `https://huggingface.co/mcp`. It needs **no
credentials**: without a token it serves public Hub data anonymously.

## When to reach for this

- The user wants to find or compare models, datasets or Spaces (by task, tag,
  downloads, likes, trending) or check a model's license, base model or card.
- The agent needs a dataset's configs, splits, schema or a few preview rows before
  writing code against it.
- The agent needs a paper's text or metadata from Hugging Face Papers, the daily
  papers, or the Hugging Face library docs.

Prefer it over web search for live Hub data: results come from the Hub API, not a
crawled snapshot. Because it is open and read-only, it is a good default in **job
descriptions** for agents that pick or evaluate models and datasets.

## Tool surface

The server advertises its toolset at connect time; probed anonymously on
2026-11-14 it offered four tools, all annotated `readOnlyHint: true`,
`destructiveHint: false`:

- `hf_fs` - one filesystem-style tool over `hf://` URIs with `ls`, `cat`, `attach`
  (images), `stat`, `find` and `search` commands, several per call: repos and their
  files, model and dataset cards, collections, papers (`hf://papers/<id>/paper.md`),
  trending listings (`hf://models/trending`), and docs search (`hf://docs`). The
  server's own instructions say to start here.
- `hub_repo_search` - search models, datasets and Spaces with one query, sorted by
  downloads, likes or trending.
- `hub_repo_details` - card metadata, license, downloads, files, Space hardware and
  status; for datasets `overview`, `dataset_structure` (configs, splits, schema)
  and `dataset_preview` (rows).
- `hf_whoami` - reports the authentication context; anonymously it says there is
  no user.

The toolset is configurable per Hugging Face account (hf.co/settings/mcp), so a
token-bearing connection can see a different list - for example Space tools that
run Gradio apps. Enumerate the live list rather than hard-coding it.

## Non-obvious gotchas the agent must respect

- **Anonymous access is rate-limited.** Without a token the server's `initialize`
  instructions say so itself: "The Hugging Face tools are being used anonymously
  and rate limits apply." Heavy use (many `hf_fs` reads, dataset previews) can hit
  HTTP 429; back off rather than retrying in a loop, and batch several operations
  into one `hf_fs` call.
- **An optional token raises the limits.** A Hugging Face access token
  (hf.co/settings/tokens) sent as `Authorization: Bearer <token>` raises the limits,
  adds the account's private and gated repos, and applies the account's tool
  settings. This entry deliberately sends **no** token, so every session sees only
  public data and no one's account is involved. Do not ask the user to paste a token
  into chat.
- **Public data only.** Anonymously, private repos and gated models' files are not
  readable; a gated model reports access errors rather than content.
- **Read-only.** None of the anonymous tools write to the Hub; there is no side
  effect to confirm with the user.
- **Link what you cite.** Return repo and paper links (`https://huggingface.co/...`)
  with results; the tool descriptions ask for it.
- **Network egress required.** The session sandbox must be able to reach
  `huggingface.co`. Under locked-down session networking the connection fails fast;
  that is a config issue, not a tool bug.

## Transport and authentication

`transport: http` is the runtime literal the runner branches on to open a
`StreamableHTTPClientTransport` - the same path `github/` and `ideogram/` ship on.
The server returns an `mcp-session-id` on `initialize`; the client SDK carries it on
later requests.

This manifest carries **no** `auth` block and **no** `Authorization` header: the
endpoint works anonymously, and the entry is meant for open use in agent fleets.

Vendor documentation: <https://huggingface.co/docs/hub/agents-mcp>
