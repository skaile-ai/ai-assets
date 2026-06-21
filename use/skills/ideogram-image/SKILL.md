---
name: "ideogram-image"
description: "Use when you need to generate or transform images from a text prompt via Ideogram's REST API - posters, logos, illustrations, photoreal scenes, and especially legible in-image text. Calls the Ideogram 4.0 endpoint with a central, org-wide API key the platform injects as $IDEOGRAM_API_KEY, billed to one shared Ideogram API account. Covers generate (primary), plus edit, upscale, describe/remix and background removal. For per-user, personally-billed image work use the `ideogram` remote MCP instead."
auth:
  type: api-key
  inject: env
  env: IDEOGRAM_API_KEY
  fields:
    - { key: apiKey, label: "Ideogram API key", secret: true }
metadata:
  stage: "alpha"
  requires:
    - "contract:use-contract"
  env_vars:
    IDEOGRAM_API_KEY: "Required. Org-wide Ideogram API key, injected by the platform from the asset's BYO-key auth block. Never hardcode or echo it."
keywords:
  - ideogram
  - image
  - generation
  - edit
  - upscale
  - rest-api
---

# Ideogram Image (REST API)

Generate and transform images with Ideogram's REST API, using a **central org API
key** the platform injects as the `IDEOGRAM_API_KEY` environment variable. This is
the enterprise-default route: one Ideogram API account, central billing, no per-user
login. (The alternative is the `ideogram` remote MCP, which is per-user OAuth and
bills each user's own Ideogram subscription.)

## Setup & Environment Variables

You do **not** configure a key here. The platform's "Bring Your Own Key" capability
collects the key once (an admin pastes it in AI Assets), stores it encrypted, and
provisions it into the session as `$IDEOGRAM_API_KEY` per the `auth` block in this
skill's frontmatter:

```yaml
auth:
  type: api-key
  inject: env
  env: IDEOGRAM_API_KEY
  fields:
    - { key: apiKey, label: "Ideogram API key", secret: true }
```

Rules:

- **Never hardcode a key** and **never echo `$IDEOGRAM_API_KEY`** (no `echo`, no
  putting it in a logged URL, no committing it). Read it only as the env var.
- If `$IDEOGRAM_API_KEY` is unset or empty, do not invent one - tell the user the
  org's Ideogram key is not configured and an admin must set it in AI Assets.

## Generate (primary capability)

`POST https://api.ideogram.ai/v1/ideogram-v4/generate` - Ideogram 4.0, synchronous.
The request is **`multipart/form-data`** (flat form fields; this differs from the
legacy `/generate` endpoint, which took a JSON `image_request` wrapper). Auth is the
`Api-Key` header.

Minimal, runnable example:

```bash
set -euo pipefail
: "${IDEOGRAM_API_KEY:?Ideogram API key not set - ask an admin to add it in AI Assets}"

curl -s -X POST "https://api.ideogram.ai/v1/ideogram-v4/generate" \
  -H "Api-Key: $IDEOGRAM_API_KEY" \
  -F "prompt=A vintage travel poster of the Swiss Alps, bold legible title text \"GRINDELWALD\"" \
  -F "aspect_ratio=3x2" \
  -F "rendering_speed=DEFAULT" \
  -F "style_type=DESIGN" \
  -F "num_images=1" \
| jq '.data[] | {url, resolution, seed, is_image_safe}'
```

### Request fields

| Field | Values / notes |
|---|---|
| `prompt` | **Required.** The text prompt. Ideogram is strong at rendering in-image text - quote the exact words you want to appear. |
| `aspect_ratio` | Optional, default `1x1`. v4 format is `WxH`: `1x1`, `16x9`, `9x16`, `3x2`, `2x3`, `4x3`, `3x4`, `16x10`, `10x16`, `3x1`, `1x3`. Mutually exclusive with `resolution`. |
| `resolution` | Optional explicit pixel size (e.g. `1024x1024`); v4 supports up to native 2K. Use instead of `aspect_ratio`, not both. |
| `rendering_speed` | Optional, default `DEFAULT`. `TURBO` (fastest/cheapest), `DEFAULT` (balanced), `QUALITY` (slowest/best). |
| `style_type` | Optional. `AUTO`, `GENERAL`, `REALISTIC`, `DESIGN`. |
| `magic_prompt` | Optional. `AUTO`, `ON`, `OFF` - Ideogram's prompt-expansion. |
| `num_images` | Optional, default `1` (1-8). Each image is a billed generation - keep small unless the user asks for options. |
| `seed` | Optional integer for reproducibility. |
| `negative_prompt` | Optional - what to avoid. |
| `style_reference_images` | Optional multipart **file** fields to steer style (`-F "style_reference_images=@ref.png"`). |

The exact, authoritative enum lists (and the full 2K resolution table) live at
<https://developer.ideogram.ai> (api-reference → "Generate with Ideogram 4.0"); v4
also accepts a JSON body with `text_prompt` / `json_prompt` for structured prompts.
When in doubt, check the live reference rather than guessing an enum.

### Response

JSON. The generated image(s) are in `data[]`, each with a hosted `url`:

```json
{
  "created": "2026-06-21T12:00:00Z",
  "data": [
    {
      "prompt": "A vintage travel poster of the Swiss Alps ...",
      "resolution": "1248x832",
      "is_image_safe": true,
      "seed": 1234567890,
      "url": "https://ideogram.ai/api/images/ephemeral/....png",
      "style_type": "DESIGN"
    }
  ]
}
```

**Hosted URLs are time-limited** (they expire roughly an hour after generation) -
download anything the user wants to keep promptly.

## Surfacing the result to the user

1. **Inline preview (default).** Emit the returned hosted URL as markdown so it
   renders inline in the Skaile chat:

   ```markdown
   ![Swiss Alps travel poster](https://ideogram.ai/api/images/ephemeral/....png)
   ```

2. **Save into the workspace (when the user wants the file).** Download it into the
   session workspace and reference the saved path:

   ```bash
   url=$(curl -s -X POST "https://api.ideogram.ai/v1/ideogram-v4/generate" \
     -H "Api-Key: $IDEOGRAM_API_KEY" \
     -F "prompt=A vintage travel poster of the Swiss Alps" \
     -F "aspect_ratio=3x2" | jq -r '.data[0].url')
   mkdir -p outputs
   curl -s "$url" -o outputs/alps-poster.png
   ```

   Then reference `outputs/alps-poster.png` (and you can still preview it inline with
   `![...](outputs/alps-poster.png)`). Because hosted URLs expire, prefer saving when
   the image needs to survive past the current turn.

## Other capabilities (brief)

Same `Api-Key` header; all return the same `data[].url` shape. See
<https://developer.ideogram.ai> for exact request fields:

- **Edit / inpaint** - `POST /v1/ideogram-v4/edit`: supply the source image, a mask,
  and a prompt for the masked region (multipart, with `@file` uploads).
- **Upscale** - `POST /v1/ideogram-v3/upscale`: increase resolution of an existing
  image (optionally with a guiding prompt).
- **Describe** - `POST /v1/ideogram-v4/describe`: caption an image (useful before a
  remix).
- **Remix** - `POST /v1/ideogram-v4/remix`: regenerate from a source image + prompt at
  a chosen `image_weight`.
- **Background removal** - `POST /v1/ideogram-v3/replace-background` (and the RMBG
  endpoint): isolate or swap the subject's background.

## Defaults & guidance

- **Aspect ratio:** default `1x1`; pick `16x9`/`9x16` for banners/stories, `3x2`/`2x3`
  for posters. Set it from the user's intended use, not at random.
- **Speed vs quality:** `TURBO` for quick drafts/iteration, `DEFAULT` for normal use,
  `QUALITY` for final/hero assets. Start at `DEFAULT` unless the user signals urgency
  or finality.
- **Batch size:** keep `num_images=1` unless the user wants options - each image is a
  separate billed generation.
- **Text-in-image:** quote the exact words in the prompt; Ideogram's typography is a
  headline strength.

## Error handling

- **401 Unauthorized** - the org's Ideogram API key is missing or invalid. Do not
  retry. Tell the user: *"the org's Ideogram API key is missing or invalid - ask an
  admin to set it in AI Assets."*
- **429 Too Many Requests** - rate-limited. Back off (exponential, a few seconds) and
  retry a bounded number of times; do not hammer. Tell the user if it persists.
- **400 Bad Request** - usually an invalid enum or field combination (e.g. both
  `aspect_ratio` and `resolution`, or `rendering_speed=FLASH`, which v4 does not yet
  accept). Re-check fields against the live reference.
- **5xx** - transient Ideogram-side error; retry with backoff, then surface.

## Billing & privacy

- Usage bills the **org's central Ideogram API account** (the BYO-key account), not
  any individual user - a different account and billing path from the per-user
  `ideogram` remote MCP.
- The key is a shared org secret. Never print it, log it, embed it in a saved file, or
  include it in output. Read it only from `$IDEOGRAM_API_KEY`.
