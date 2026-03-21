
# Recipe: Installing and Using PrimeVue with Nuxt

This guide provides steps to set up PrimeVue with its own styling in a Nuxt project. It also includes an optional section for integrating Tailwind CSS for layout and utility classes.

## 1. Prerequisites

*   A Nuxt 3 project. You can create one by running `npx nuxi@latest init <project-name>`.
*   A terminal or command prompt.

## 2. Core PrimeVue Installation (Styled Mode)

### Step 1: Install Dependencies

Navigate to your project's root directory and run the following command to install PrimeVue, its theme collection, and the Nuxt module:

```bash
# Using npm
npm install primevue @primeuix/themes
npm install --save-dev @primevue/nuxt-module

# Using pnpm
pnpm add primevue @primeuix/themes
pnpm add -D @primevue/nuxt-module
pnpm add @primevue/forms
pnpm add primeicons
```

### Step 2: Configure Nuxt

In your `nuxt.config.ts` file, add `@primevue/nuxt-module` to the `modules` array. Import a theme (e.g., `Aura`) and set it as the preset within the `primevue` configuration object.

```typescript
// nuxt.config.ts
import Aura from '@primeuix/themes/aura';

export default defineNuxtConfig({
  modules: [
    '@primevue/nuxt-module'
  ],
  primevue: {
    options: {
      ripple: true, // Optional: Adds a ripple effect to components
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: ".p-dark", // Add this for dark mode support
        },
      },
    }
  },
  css: [
    'primeicons/primeicons.css' // Add this line for PrimeIcons
  ]
})
```

### Step 3: Use PrimeVue Components

The Nuxt module automatically imports PrimeVue components, so you can use them directly in your Vue files without any extra import statements.

For example, to use a Button and a Toast component, simply add them to a page or component:

```vue
<!-- pages/index.vue -->
<script setup>
import { useToast } from 'primevue/usetoast';
const toast = useToast();

const showToast = () => {
  toast.add({ severity: 'info', summary: 'Hello!', detail: 'This is a PrimeVue Toast', life: 3000 });
};
</script>

<template>
  <div>
    <h1>Welcome to PrimeVue</h1>
    <Toast />
    <Button label="Show a Toast" @click="showToast" />
  </div>
</template>
```

## 3. Optional: Add Tailwind CSS

Follow these steps if you want to use Tailwind CSS for layouts and utility styling alongside PrimeVue's component theme.

### Step 4: Install and Configure the Nuxt Tailwind Module

First, add the official Tailwind CSS module for Nuxt to your project.

```bash
# Using npm
npm install --save-dev @nuxtjs/tailwindcss

# Using pnpm
pnpm add -D @nuxtjs/tailwindcss
pnpm add tailwindcss
```

Next, add the module to your `nuxt.config.ts`. It should be placed **before** the PrimeVue module.

```typescript
// nuxt.config.ts
import Aura from '@primeuix/themes/aura';

export default defineNuxtConfig({
  modules: [
    '@nuxtjs/tailwindcss', // Add this line
    '@primevue/nuxt-module'
  ],
  primevue: {
    options: {
      theme: {
        preset: Aura
      },
      ripple: true,
    }
  }
})
```

### Step 5: Configure Tailwind CSS in nuxt.config.ts

Instead of a separate `tailwind.config.js` file, the current project integrates Tailwind CSS configuration directly into `nuxt.config.ts`. This includes importing `tailwindcss-primeui` and configuring dark mode.

First, ensure `tailwindcss-primeui` is installed:

```bash
# Using npm
npm install --save-dev tailwindcss-primeui

# Using pnpm
pnpm add -D tailwindcss-primeui
```

Then, update your `nuxt.config.ts` to import `PrimeUI` and configure the `tailwindcss` module:

```typescript
// nuxt.config.ts
import Aura from '@primeuix/themes/aura';
import PrimeUI from 'tailwindcss-primeui'; // Add this import

export default defineNuxtConfig({
  modules: [
    '@nuxtjs/tailwindcss',
    '@primevue/nuxt-module'
  ],
  primevue: {
    options: {
      ripple: true,
      theme: {
        preset: Aura,
        options: {
          darkModeSelector: ".p-dark", // Add this for dark mode support
        },
      },
    }
  },
  tailwindcss: { // Add this block
    config: {
      plugins: [PrimeUI],
      darkMode: ["class", ".p-dark"],
    },
  },
  // ... rest of your config
})
```

You can now use Tailwind CSS classes for layout and spacing while relying on PrimeVue for beautifully styled components.