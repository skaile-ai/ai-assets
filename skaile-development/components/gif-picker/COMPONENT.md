---
name: gif-picker
description: Search and select animated GIFs
version: 1.0.0
keywords: [gif, animation, media, sticker]

config:
  provider:
    description: "GIF search provider"
    default: klipy
    enum: [klipy, giphy, tenor]
  api_key:
    description: "Provider API key (required for giphy; klipy has a free tier)"
    required: false

provides:
  - type: input-extension
    trigger: { type: prefix, value: "/gif" }
    component: { kind: web-component, url: ./gif-picker.js, tagName: skaile-gif-picker }
    produces: { type: message, messageType: gif }

  - type: chat-renderer
    messageType: gif
    component: { kind: web-component, url: ./gif-renderer.js, tagName: skaile-gif-display }
    schema:
      type: object
      properties:
        url: { type: string }
        alt: { type: string }
        width: { type: number }
        height: { type: number }
        provider: { type: string }
    interactions: []
    targets: [chat]
    fallback: "![{{alt}}]({{url}})"
---

# GIF Picker

Adds animated GIF search and display to the agent chat. Provider-agnostic -
supports Klipy (free, default), Giphy, and Tenor via a configuration option.

## Usage

Type `/gif <search term>` in the chat input to search for GIFs.
Select one to insert it into the conversation.

## How It Works

The picker component calls the configured provider's search API, shows a
grid of results, and emits a `gif` message type on selection. The chat
renderer displays the GIF inline with alt text.

Clients without the gif components see: `![alt text](url)`

## Providers

| Provider | Free tier | API key required | Notes |
|----------|-----------|------------------|-------|
| Klipy | Yes (default) | Optional | Built by ex-Tenor team, near-identical API |
| Giphy | No (paid since 2025) | Yes | Largest catalog |
| Tenor | Deprecated (shutdown June 2026) | Yes | Legacy support only |
