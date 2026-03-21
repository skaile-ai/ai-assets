# Recipe: Tiptap Markdown Editor in Nuxt 3

A step-by-step guide to implement Tiptap as a client-side editor for Markdown fields from a database within a Nuxt 3 application.

This recipe uses the `nuxt-tiptap-editor` module for simplified setup and the `tiptap-markdown` extension to handle the conversion between Tiptap's native JSON format and Markdown.

> [!TIP]
> **2024 Best Practice: Module vs. Direct Integration**
> While `nuxt-tiptap-editor` is great for rapid prototyping and minimalist setups, direct integration using `@tiptap/vue-3` and `@tiptap/pm` is recommended for complex or highly customized editors. If you are already using Nuxt UI, consider using their `UEditor` component which is built on Tiptap and provides deep theme integration out-of-the-box.

### Ingredients (Dependencies)

*   A Nuxt 3 project
*   `nuxt-tiptap-editor`
*   `tiptap-markdown`

### The Cooking Process

#### Step 1: Install the Dependencies

Open your terminal in your Nuxt 3 project root and run the following commands. The first command uses `nuxi` to add the `nuxt-tiptap-editor` module and its core dependencies. The second command adds the specific extension for Markdown conversion.

```bash
# Add the Tiptap module for Nuxt
npx nuxi@latest module add tiptap

# Install the Markdown extension
npm install tiptap-markdown
```

#### Step 2: Configure Nuxt

The `nuxi` command should have already added the module to your `nuxt.config.ts`. Verify that your configuration file looks like this:

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: [
    'nuxt-tiptap-editor'
  ],
  // Optional: configuration for the module
  tiptap: {
    prefix: 'Tiptap', // Prepend "Tiptap" to all component names
  },
})
```

#### Step 3: Create the Reusable Editor Component

This component will encapsulate all the Tiptap logic and handle the two-way data binding (`v-model`) for a Markdown string.

Create a new file: `components/MarkdownEditor.vue`

```vue
<!-- components/MarkdownEditor.vue -->
<template>
  <div v-if="editor" class="markdown-editor">
    <!-- A basic toolbar -->
    <TiptapToolbar :editor="editor" :extensions="extensions" />
    
    <!-- The editor content area -->
    <TiptapEditorContent :editor="editor" />
  </div>
</template>

<script setup>
import {
  TiptapEditor,
  TiptapEditorContent,
  TiptapToolbar,
  StarterKit,
  Markdown
} from 'nuxt-tiptap-editor';

// Define props for v-model compatibility
const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
});

// Define emits for v-model compatibility
const emit = defineEmits(['update:modelValue']);

// List of extensions to use in the editor
const extensions = [
  StarterKit,
  // Configure the Markdown extension to handle conversion
  Markdown.configure({
    html: false,        // Disallow HTML input to prevent XSS attacks
    tightLists: true,   // No <p> inside <li>
    linkify: true,      // Auto-detect and convert URLs to links
    breaks: true,       // Render soft line breaks as <br>
  }),
];

// The useTiptap composable manages the editor instance
const { editor } = useTiptap({
  // The 'content' property is used to initialize the editor.
  // We pass the modelValue and use toRef to keep it reactive.
  content: toRef(props, 'modelValue'),
  
  // Pass the defined extensions
  extensions,

  // This function is called whenever the editor's content changes.
  onUpdate: ({ editor }) => {
    // Retrieve the Markdown output from the extension's storage
    const markdown = editor.storage.markdown.getMarkdown();
    // Emit the new value to the parent component
    emit('update:modelValue', markdown);
  },
});

// Always destroy the editor instance when the component is unmounted
onUnmounted(() => {
  if (editor.value) {
    editor.value.destroy();
  }
});
</script>

<style>
.markdown-editor .ProseMirror {
  border: 1px solid #dbdbdb;
  border-radius: 4px;
  padding: 0.5rem 0.75rem;
  min-height: 300px;
  margin-top: 0.5rem;
}

.markdown-editor .ProseMirror:focus {
  outline: none;
  border-color: #333;
  box-shadow: 0 0 0 2px rgba(0,0,0,0.1);
}
</style>
```

#### Step 4: Use the Editor on a Page

Now you can import and use your `MarkdownEditor` component on any page or in another component. Since Tiptap directly manipulates the DOM, you **must** wrap the editor component in `<client-only>` tags.

Here is an example of an edit page that fetches data, binds it to the editor, and simulates saving it.

Create a new file: `pages/edit-post.vue`

```vue
<!-- pages/edit-post.vue -->
<template>
  <div class="page-container">
    <h1>Edit Your Post</h1>
    <p>This page demonstrates how to use the Markdown editor component with `v-model`.</p>

    <form @submit.prevent="submitPost">
      <!-- 
        Wrap the editor in <client-only> to ensure it only renders on the client-side.
        Use v-model to bind the 'postContent' ref to the editor.
      -->
      <client-only>
        <MarkdownEditor v-model="postContent" />
      </client-only>
      
      <button type="submit">Save Content</button>
    </form>
    
    <div class="preview-area">
      <h2>Live Markdown Preview</h2>
      <p>This area shows the raw Markdown string being updated in real-time.</p>
      <pre>{{ postContent }}</pre>
    </div>
  </div>
</template>

<script setup>
// This ref will hold the Markdown content from your database
const postContent = ref('');

// Simulate fetching data from a database when the page loads
onMounted(() => {
  // In a real app, you would fetch this from your API
  postContent.value = `## Hello World!\n\nThis is **Tiptap** editing a *Markdown* field.\n\n*   List item 1\n*   List item 2\n\nCheck out [Nuxt](https://nuxt.com)!`;
});

// Function to handle form submission
function submitPost() {
  // Here, you would send postContent.value to your server API to save it
  console.log('--- Submitting to database ---');
  console.log(postContent.value);
  alert('Post content has been logged to the console!');
}
</script>

<style scoped>
.page-container {
  max-width: 800px;
  margin: 2rem auto;
  padding: 1rem;
}
form {
  margin: 2rem 0;
}
button {
  margin-top: 1rem;
  padding: 0.5rem 1rem;
  font-size: 1rem;
  cursor: pointer;
}
.preview-area {
  margin-top: 2rem;
  padding: 1rem;
  background-color: #f3f4f6;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}
pre {
  background-color: #fff;
  padding: 1rem;
  white-space: pre-wrap;
  word-wrap: break-word;
  border-radius: 4px;
}
</style>
```

You have now successfully implemented a reusable, two-way-bindable Markdown editor in your Nuxt 3 application.