---
name: Slash Commands
description: Implementing a Notion-style popup menu triggered by typing "/".
libraries_used: @tiptap/suggestion, tippy.js
---

# Slash Commands

## Objective
Provide a quick way for users to insert blocks or trigger actions by typing a command prefix.

## Prerequisites
- `@tiptap/suggestion`
- `tippy.js` (for the popup)

## Instructions

### 1. Configure the Suggestion Extension
Define the triggering character (usually `/`) and a `render` function to show the popup.

### 2. Implement the Popup Component
Create a Vue component that shows a list of commands and handles keyboard navigation.

### 3. Handle Command Execution
The `command` property in the suggestion configuration defines what happens when an item is selected.

## Code Example Snippet

```javascript
import { Extension } from '@tiptap/core'
import Suggestion from '@tiptap/suggestion'

export default Extension.create({
  name: 'slashCommand',
  addProseMirrorPlugins() {
    return [
      Suggestion({
        editor: this.editor,
        char: '/',
        command: ({ editor, range, props }) => {
          props.command({ editor, range })
        },
        items: ({ query }) => {
          // Return filtered list of commands
        },
        render: () => {
          // Integration with Tippy.js or a Vue component
        },
      }),
    ]
  },
})
```
