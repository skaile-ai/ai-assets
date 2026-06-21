---
name: "use-ideogram-image"
description: "Use when you need to generate or transform images from a text prompt via Ideogram's REST API - posters, logos, illustrations, photoreal scenes, and especially legible in-image text. Calls Ideogram's generate endpoints with a central, org-wide API key the platform injects as $IDEOGRAM_API_KEY, billed to one shared Ideogram API account. Defaults to Ideogram 4.0 (2K resolutions) and falls back to 3.0 when you need aspect-ratio / style / multi-image controls. Also covers edit, upscale, describe/remix and background removal. For per-user, personally-billed image work use the `ideogram` remote MCP instead."
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

# Use Ideogram Image (REST API)

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

## Generate

Ideogram exposes a generate endpoint per model version. Both are `multipart/form-data`
POSTs authenticated with the `Api-Key` header, and both return the same response
shape. **The request fields differ by version - do not mix them:**

| | **v4 (default, latest)** | **v3 (richer controls)** |
|---|---|---|
| Endpoint | `POST /v1/ideogram-v4/generate` | `POST /v1/ideogram-v3/generate` |
| Prompt field | `text_prompt` *or* `json_prompt` | `prompt` |
| Sizing | `resolution` (fixed 2K enum) | `aspect_ratio` (`WxH`) or `resolution` |
| Style / variants | not available | `style_type`, `magic_prompt`, `num_images`, `negative_prompt` |

Reach for **v4** by default (best model, native 2K). Drop to **v3** when you need
aspect-ratio control, multiple images per call, an explicit `style_type`, or a
`negative_prompt` - v4 does not accept those fields.

### v4 - default

`POST https://api.ideogram.ai/v1/ideogram-v4/generate` - Ideogram 4.0, synchronous,
`multipart/form-data`. Sizing is by **`resolution`** (an explicit pixel size from a
fixed enum), not `aspect_ratio`.

```bash
set -euo pipefail
: "${IDEOGRAM_API_KEY:?Ideogram API key not set - ask an admin to add it in AI Assets}"

curl -s -X POST "https://api.ideogram.ai/v1/ideogram-v4/generate" \
  -H "Api-Key: $IDEOGRAM_API_KEY" \
  -F "text_prompt=A vintage travel poster of the Swiss Alps, bold legible title text \"GRINDELWALD\"" \
  -F "resolution=1024x1024" \
  -F "rendering_speed=DEFAULT" \
| jq '.data[] | {url, resolution, seed, is_image_safe}'
```

| Field | Values / notes |
|---|---|
| `text_prompt` | The text prompt. Mutually exclusive with `json_prompt`; supplying it auto-enables magic-prompt expansion. Quote the exact words you want rendered in-image - Ideogram's typography is a headline strength. |
| `json_prompt` | A structured prompt conforming to the Ideogram 4.0 JSON contract (used **instead of** `text_prompt`; disables magic-prompt and is consumed by the model directly). |
| `resolution` | The output pixel size, from a fixed enum of supported pairs (Ideogram 4.0 supports up to native 2K, e.g. `1024x1024`, `1280x800`, `2048x2048`). See the live reference for the authoritative enum. |
| `rendering_speed` | `TURBO` (fast/cheap), `DEFAULT` (balanced), `QUALITY` (best). `FLASH` is in the enum but **coming soon** - v4 currently returns 400 for it. |
| `enable_copyright_detection` | Optional. Opt the request into post-generation copyright checks (the effective gate is the OR of this field and the org's setting). |

v4 does **not** accept `aspect_ratio`, `style_type`, `magic_prompt`, `num_images`, or
`negative_prompt` - use v3 below for those.

### v3 - richer controls

`POST https://api.ideogram.ai/v1/ideogram-v3/generate` - Ideogram 3.0, synchronous,
`multipart/form-data`. Use when you need aspect-ratio sizing, multiple images,
`style_type`, or a `negative_prompt`.

```bash
curl -s -X POST "https://api.ideogram.ai/v1/ideogram-v3/generate" \
  -H "Api-Key: $IDEOGRAM_API_KEY" \
  -F "prompt=A vintage travel poster of the Swiss Alps, bold legible title text \"GRINDELWALD\"" \
  -F "aspect_ratio=3x2" \
  -F "rendering_speed=DEFAULT" \
  -F "style_type=DESIGN" \
  -F "num_images=1" \
| jq '.data[] | {url, resolution, seed, is_image_safe}'
```

| Field | Values / notes |
|---|---|
| `prompt` | **Required.** The text prompt. |
| `aspect_ratio` | Optional, default `1x1`. `WxH` form: `1x1`, `16x9`, `9x16`, `3x2`, `2x3`, `4x3`, `3x4`, `16x10`, `10x16`, `3x1`, `1x3`. Mutually exclusive with `resolution`. |
| `resolution` | Optional explicit pixel size (e.g. `1024x1024`). Use instead of `aspect_ratio`, not both. |
| `rendering_speed` | Optional, default `DEFAULT`. `FLASH`, `TURBO`, `DEFAULT`, `QUALITY` (v3 accepts `FLASH`; v4 does not yet). |
| `style_type` | Optional. `AUTO`, `GENERAL`, `REALISTIC`, `DESIGN`. |
| `magic_prompt` | Optional. `AUTO`, `ON`, `OFF` - Ideogram's prompt-expansion. |
| `num_images` | Optional, default `1` (1-8). Each image is a billed generation - keep small unless the user asks for options. |
| `seed` | Optional integer for reproducibility. |
| `negative_prompt` | Optional - what to avoid. |
| `style_reference_images` | Optional multipart **file** fields to steer style (`-F "style_reference_images=@ref.png"`). |

The exact, authoritative field lists, enums and the full 2K resolution table live at
<https://developer.ideogram.ai> (api-reference → "Generate with Ideogram 4.0" /
"Generate with Ideogram 3.0"). When unsure of an enum value, check the live reference
rather than guessing.

### Response (both versions)

JSON. The generated image(s) are in `data[]`, each with a hosted `url`:

```json
{
  "created": "2026-06-21T12:00:00Z",
  "data": [
    {
      "prompt": "A vintage travel poster of the Swiss Alps ...",
      "resolution": "1024x1024",
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
     -F "text_prompt=A vintage travel poster of the Swiss Alps" \
     -F "resolution=1024x1024" | jq -r '.data[0].url')
   mkdir -p outputs
   curl -s "$url" -o outputs/alps-poster.png
   ```

   Then reference `outputs/alps-poster.png` (and you can still preview it inline with
   `![...](outputs/alps-poster.png)`). Because hosted URLs expire, prefer saving when
   the image needs to survive past the current turn.

## Other capabilities (brief)

Same `Api-Key` header; all return the same `data[].url` shape. See
<https://developer.ideogram.ai> for exact request fields and the current model version
of each endpoint:

- **Edit / inpaint** - supply a source image, a mask, and a prompt for the masked
  region (multipart, with `@file` uploads).
- **Upscale** - increase resolution of an existing image (optionally with a guiding
  prompt).
- **Describe** - caption an image (useful before a remix).
- **Remix** - regenerate from a source image + prompt at a chosen `image_weight`.
- **Background removal** - isolate or swap the subject's background.

## Defaults & guidance

- **Version:** default to **v4**; switch to **v3** only when you need `aspect_ratio`,
  `style_type`, `num_images`, or `negative_prompt`.
- **Sizing:** on v4 set `resolution` (fixed enum, up to 2K); on v3 set `aspect_ratio`
  (`WxH`) - pick `16x9`/`9x16` for banners/stories, `3x2`/`2x3` for posters. Size from
  the user's intended use, not at random.
- **Speed vs quality:** `TURBO` for quick drafts/iteration, `DEFAULT` for normal use,
  `QUALITY` for final/hero assets. Start at `DEFAULT` unless the user signals urgency
  or finality.
- **Batch size (v3 only):** keep `num_images=1` unless the user wants options - each
  image is a separate billed generation.
- **Text-in-image:** quote the exact words in the prompt; Ideogram's typography is a
  headline strength (both versions).

## Error handling

- **401 Unauthorized** - the org's Ideogram API key is missing or invalid. Do not
  retry. Tell the user: *"the org's Ideogram API key is missing or invalid - ask an
  admin to set it in AI Assets."*
- **429 Too Many Requests** - rate-limited. Back off (exponential, a few seconds) and
  retry a bounded number of times; do not hammer. Tell the user if it persists.
- **400 Bad Request** - usually a wrong field for the chosen version (e.g. sending
  `aspect_ratio`/`style_type`/`num_images` to the **v4** endpoint, sending both
  `aspect_ratio` and `resolution` on v3, or `rendering_speed=FLASH` on v4, which is
  not yet accepted). Re-check fields against the version's table above / the live
  reference.
- **5xx** - transient Ideogram-side error; retry with backoff, then surface.

## Billing & privacy

- Usage bills the **org's central Ideogram API account** (the BYO-key account), not
  any individual user - a different account and billing path from the per-user
  `ideogram` remote MCP.
- The key is a shared org secret. Never print it, log it, embed it in a saved file, or
  include it in output. Read it only from `$IDEOGRAM_API_KEY`.
