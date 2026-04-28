# GIF - Agent Usage

## Sending a GIF in Chat

The platform provides a `search-gifs` capability. Use it to find GIFs, then
include the chosen one in your response.

### Step 1: Search for GIFs

Invoke the `search-gifs` capability:

```json
{
  "type": "app_action",
  "id": "<uuid>",
  "action": "search-gifs",
  "params": { "query": "thumbs up", "limit": 5 }
}
```

You will receive results via `app_action_result`:

```json
{
  "status": "ok",
  "value": {
    "results": [
      {
        "id": "abc",
        "url": "https://static.klipy.com/hd.gif",
        "preview": "https://static.klipy.com/sm.gif",
        "alt": "thumbs up",
        "width": 480,
        "height": 270
      }
    ]
  }
}
```

### Step 2: Include in Response

Pick the best result and include it in your `finished` event:

```json
{
  "type": "finished",
  "summary": "Here's a thumbs up!",
  "costUsd": 0,
  "customType": "gif",
  "customData": {
    "url": "https://static.klipy.com/hd.gif",
    "alt": "thumbs up",
    "width": 480,
    "height": 270
  }
}
```

### When to Use

- When the user asks for a GIF or a reaction GIF
- When the conversation calls for a visual/humorous response
- When the user shares a GIF and you want to respond in kind

### Legacy Path

The `[CUSTOM:gif:json]` marker format still works for backward compatibility
but the `search-gifs` capability is preferred because the API key is managed
server-side.
