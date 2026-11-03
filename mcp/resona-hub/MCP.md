---
name: resona-hub
description: "Use when an agent needs a private audio transcription or a document
  OCR-to-Markdown conversion: German or English recordings to a transcript with
  timestamps, scanned or digital PDFs and images to Markdown with page numbers and
  tables. Wraps the hosted Resona hub's Streamable HTTP MCP endpoint
  (resona.ecsplico.de/mcp). Jobs run asynchronously on the organization's approved
  workers, private by default, so no external AI provider sees the content. The
  bearer token is an organization-bound Resona client token: the hub derives the
  organization from it, and one organization's jobs are invisible to another's."
version: 0.1.0 # mcp-catalog-version
transport: http
url: https://resona.ecsplico.de/mcp
auth:
  type: bearer
  inject: header
  header: Authorization
  fields:
    - { key: apiKey, label: "Resona organization token", secret: true }
keywords:
  - resona
  - transcription
  - speech-to-text
  - audio
  - ocr
  - pdf
  - markdown
  - documents
  - private
  - mcp
  - remote
---

# Resona Hub MCP Server (remote)

Resona's hosted MCP endpoint for private audio transcription and document
OCR→Markdown. This is a **remote** asset: there is no binary or image to package; the
runner opens a Streamable HTTP connection straight to `https://resona.ecsplico.de/mcp`.
Source, worker setup, hub operation and the job contract:
[`ecsplico/resona`](https://github.com/ecsplico/resona) (`apps/resona-mcp`,
`docs/server/`). This directory is the catalog entry only; bump `version:` here when
adopting a release.

The agent guidance (the upload → submit → poll → page flow, privacy defaults) is sent by
the server itself as its MCP `initialize` instructions, and each tool's description
carries its own arguments and limits. Keep this body short: the platform puts it in the
prompt on every turn.

## Authentication (bound per organization)

`auth` declares a bearer token injected as the `Authorization` header. Paste a
**dedicated Resona client token minted for this SKAILE organization** as the instance
secret (Resona operator: `resona-admin organization create` + `credential issue
--role client`). Never reuse another organization's token: the hub scopes every job,
upload and result to the token's organization, and a foreign job id answers 404.

## Capabilities

9 tools over Streamable HTTP:

<!-- mcp-catalog-tools -->
- **Input (2)** — `create_upload_url`, `upload_content`
- **Jobs (5)** — `transcribe_audio`, `convert_document`, `get_job`, `get_result`, `cancel_job`
- **Hub (2)** — `list_engines`, `get_settings`
<!-- /mcp-catalog-tools -->

## Gotchas

- **Asynchronous.** Submissions return a job id at once; poll `get_job` and page
  `get_result` until `next_offset` is null. A `pending` job with
  `queue_reason=no_capable_worker` waits for an approved worker to come online; it is
  not an error.
- **Remote inputs.** This endpoint accepts no local paths or URLs. Call
  `create_upload_url` and run the returned `curl` command in the shell with the file
  path; the bytes go straight to the hub and the reply carries the `upload_id`. The
  link is single-use and valid 10 minutes. `upload_content` (inline base64 plus the
  file's `sha256`) is for files of a few KB only: models copy long base64 unreliably.
- **Private by default.** `private: false` only works when the organization allows cloud
  processing; otherwise the hub answers `policy_denied`.
