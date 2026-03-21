---
name: Custom Node Views
description: Building interactive custom elements as Tiptap nodes using Vue components
libraries_used: "@tiptap/vue-3"
---

# Custom Node Views

## Objective
Create highly interactive and visually distinct editor elements (e.g., callouts, interactive charts, or custom media players) by rendering Vue components directly within Tiptap.

## Prerequisites
- Tiptap Vue 3 integration

## Instructions
1.  **Define Extension**: Create a new `Node` extension.
2.  **Add Attributes**: Define the state/properties your node needs to persist.
3.  **Specify `NodeView`**: Use `VueNodeViewRenderer` to link the extension to a Vue component.
4.  **Vue Component Implementation**: Use `node-view-wrapper` to wrap your component and `node-view-content` for editable areas inside the node.

## Code Example

### `CalloutExtension.ts`
```typescript
import { Node, mergeAttributes } from '@tiptap/core'
import { VueNodeViewRenderer } from '@tiptap/vue-3'
import CalloutComponent from './CalloutComponent.vue'

export const Callout = Node.create({
  name: 'callout',
  group: 'block',
  content: 'inline*', // allows editable text inside
  draggable: true,
  
  addAttributes() {
    return {
      type: { default: 'info' }, // 'info' | 'warning' | 'error'
    }
  },

  parseHTML() {
    return [{ tag: 'div[data-type="callout"]' }]
  },

  renderHTML({ HTMLAttributes }) {
    return ['div', mergeAttributes(HTMLAttributes, { 'data-type': 'callout' }), 0]
  },

  addNodeView() {
    return VueNodeViewRenderer(CalloutComponent)
  },
})
```

### `CalloutComponent.vue`
```vue
<script setup lang="ts">
import { nodeViewProps, NodeViewWrapper, NodeViewContent } from '@tiptap/vue-3'

const props = defineProps(nodeViewProps)

const setType = (type: string) => {
  props.updateAttributes({ type })
}
</script>

<template>
  <NodeViewWrapper class="callout-node" :class="props.node.attrs.type">
    <div class="controls" contenteditable="false">
      <button @click="setType('info')">Info</button>
      <button @click="setType('warning')">Warning</button>
    </div>
    <div class="icon">ℹ️</div>
    <NodeViewContent class="content" />
  </NodeViewWrapper>
</template>

<style scoped>
.callout-node { border-left: 4px solid blue; padding: 1rem; }
.callout-node.warning { border-left-color: orange; }
</style>
```
