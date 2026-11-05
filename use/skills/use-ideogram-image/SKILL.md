---
name: "use-ideogram-image"
description: "Use when you need to generate an image from a text prompt - posters, logos, illustrations, photoreal scenes, images with legible text. Calls the Skaile platform capability platform.generate_image, which uses the organization's configured image provider (Ideogram, Recraft, FLUX or another) and saves the result into the session workspace. Needs no API key: the platform holds the provider and its key."
metadata:
  stage: "alpha"
  requires:
    - "contract:use-contract"
keywords:
  - image
  - generation
  - ideogram
  - illustration
  - poster
---

# Generate images (Skaile platform)

Make images with the platform capability **`platform.generate_image`**. The platform
sends the prompt to the organization's image provider, chosen by an admin in the AI
provider settings, and saves the images into this session's workspace. You never see,
need or ask for an API key, and you never call an image provider's API yourself (no
`curl` to Ideogram or any other image service).

## Call

```json
platform.generate_image({
  "prompt": "A vintage travel poster of the Swiss Alps, bold legible title text \"GRINDELWALD\"",
  "negative": "people, watermark",
  "aspect": "2:3",
  "count": 1,
  "name": "alps-poster"
})
```

| Field | Notes |
|---|---|
| `prompt` | **Required.** What the image shows. Quote the exact words that must appear in the image. |
| `negative` | Optional. What the image must not show. |
| `aspect` | Optional, default `1:1`. One of `1:1`, `4:3`, `3:4`, `16:9`, `9:16`, `3:2`, `2:3`. Pick from the intended use: `16:9`/`9:16` for banners and stories, `3:2`/`2:3` for posters. |
| `count` | Optional, default `1`, at most `4`. Each image is billed to the organization: keep `1` unless the user wants options. |
| `name` | Optional short file name; the platform adds a time stamp. |

The result lists each saved image's workspace path, under `generated-images/`:

```json
{ "resourceId": "workspace", "images": [{ "path": "generated-images/20261105-142233-alps-poster.png", "contentType": "image/png", "bytes": 812345 }] }
```

The provider drops unsafe candidates, so you may get fewer images than `count`.

## Show the result

- Inline in the chat: `![Swiss Alps travel poster](generated-images/20261105-142233-alps-poster.png)`.
- Or open it in the user's workspace pane with `platform.open_file({ "path": "generated-images/..." })`.
- Move or rename the file with your shell when the user wants it elsewhere.

## Refusals

A refusal comes back worded for the user. Pass it on and do not retry or look for
another way to reach an image service:

- Image generation is turned off for this organization.
- No image provider is set up: an admin adds one in the AI provider settings.
- Too many images for now: try again in a few minutes.
- The provider returned no usable image: rephrase the prompt, or try again later.

## Prompting tips

- Describe subject, style, composition, lighting and colours; name the medium
  ("flat vector illustration", "35 mm photo", "watercolour").
- Text in the image: quote it exactly and keep it short.
- Iterate with small prompt changes rather than raising `count`.
