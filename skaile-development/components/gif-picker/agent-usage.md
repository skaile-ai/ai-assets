# GIF Picker - Agent Usage

## Sending a GIF in Chat

To send a GIF in the conversation, output a JSON block with the `gif` custom
message marker. The platform will render it as an inline animated GIF using
the `skaile-gif-display` component.

### Format

```
[CUSTOM:gif:{"url":"<gif-url>","alt":"<description>","width":<number>,"height":<number>}]
```

### Example

```
[CUSTOM:gif:{"url":"https://static.klipy.com/ii/example/cat.gif","alt":"funny cat","width":480,"height":270}]
```

### When to Use

- When the user asks for a GIF or a reaction GIF
- When the conversation calls for a visual/humorous response
- When the user shares a GIF and you want to respond in kind

### Finding GIF URLs

You can use the Klipy API to search for GIFs:

```
GET https://api.klipy.com/api/v1/BMysgPJm0DEODCvgX3cMuNHkJ1uOvoN34toKNC1VTnPaXrBkVAsV97Wmhz7Eqg7x/gifs/search?q=<query>&per_page=5
```

The response contains `data.data[]` with each item having:
- `file.hd.gif.url` - full size GIF URL
- `file.sm.gif.url` - small preview URL  
- `title` - description text
- `file.hd.gif.width` / `file.hd.gif.height` - dimensions

### Notes

- The `[CUSTOM:gif:...]` marker must be on its own line
- The JSON must be valid and contain at least `url`
- The marker is consumed by the platform and not shown as text
- Users can also send GIFs via the `/gif` input extension
