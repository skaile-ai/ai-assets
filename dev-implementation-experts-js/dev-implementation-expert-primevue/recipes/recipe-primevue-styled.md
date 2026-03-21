# A Deep Dive into PrimeVue's Styled Mode

This document provides a specific and detailed guide on using the "Styled Mode" theming approach for PrimeVue, focusing on its architecture, configuration, and customization capabilities.

## 1. What is Styled Mode?

Styled Mode is the default and recommended approach for theming PrimeVue components. It provides a complete, professionally designed theme out-of-the-box, which can be easily customized using a powerful system of design tokens. This mode is ideal for developers who want a beautiful and consistent UI without writing extensive CSS from scratch.

The core idea is the separation of component logic from its presentation. The components are shipped without visual styling, which is provided by a separate theme module. [2]

## 2. Core Architecture

Styled Mode's architecture is built on three main concepts: **Themes**, **Presets**, and a **Base**. [2]

*   **Base:** A foundational layer of CSS that applies structural styles (like layout and alignment) to components. It does not contain any colors, spacing, or cosmetic styles.
*   **Preset:** The heart of the theme. A preset is a collection of **Design Tokens** that define the visual appearance, including colors, typography, spacing, borders, and more. PrimeVue offers several pre-built presets like `Aura` (the default), `Lara`, and `Nora`.
*   **Theme:** The combination of the Base and a Preset. When you choose a theme, you are essentially applying a specific set of design tokens (the preset) to the foundational component structure (the base).

### Design Tokens

Design tokens are essentially CSS variables that represent the design properties of a theme. They are organized semantically, making customization intuitive. For example, instead of overriding a specific hex code for a button's background, you override the `primary.500` color token, and this change will propagate consistently across all components that use that primary color.

## 3. Installation and Configuration

To use Styled Mode in a Nuxt application, you need to install the necessary theme packages and configure them in your `nuxt.config.ts`.

### Step 1: Install Dependencies
First, install the `@primeuix/themes` package which contains the pre-built themes and presets.

```bash
# Using npm
npm install @primeuix/themes

# Using yarn
yarn add @primeuix/themes
```

### Step 2: Configure `nuxt.config.ts`
The `@primevue/nuxt-module` makes configuration straightforward. You import your desired theme preset and assign it in the `primevue` configuration object.

Here is a typical setup using the default `Aura` theme:

```typescript
// nuxt.config.ts
import Aura from '@primeuix/themes/aura';

export default defineNuxtConfig({
  modules: ['@primevue/nuxt-module'],

  primevue: {
    theme: {
      preset: Aura,
      options: {
        prefix: 'p', // Optional CSS prefix
        darkModeSelector: 'system', // 'system' or a class name like '.dark'
        cssLayer: false // Defaults to false
      }
    }
  }
});
```

**Configuration Options:**
*   `preset`: The theme preset you want to use (e.g., `Aura`).
*   `prefix`: Adds a prefix to all PrimeVue CSS classes (e.g., `.p-button`) to prevent naming conflicts with other UI libraries.
*   `darkModeSelector`: Configures how dark mode is activated. `'system'` respects the user's OS preference, while a class name like `.dark` allows you to toggle dark mode manually by adding the class to a parent element (usually `<html>`).
*   `cssLayer`: An advanced feature that wraps the theme's CSS in a CSS `@layer`. This can help manage CSS specificity in complex projects but is typically not required.

## 4. Customizing a Theme

The true power of Styled Mode lies in its customizability. You don't have to eject from the system to make significant design changes. Customization is achieved by extending a base preset and overriding its design tokens.

### How to Override Tokens
Let's say you want to change the primary color palette from the default to a shade of purple.

**Step 1: Create a Custom Preset File**
It's good practice to define your custom preset in a separate file, for example, `assets/themes/my-preset.ts`.

```typescript
// assets/themes/my-preset.ts
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

// Extend the Aura preset
export const MyPreset = definePreset(Aura, {
  // Define overrides for semantic tokens
  semantic: {
    // Override the entire primary color palette
    primary: {
      50: '{purple.50}',
      100: '{purple.100}',
      200: '{purple.200}',
      300: '{purple.300}',
      400: '{purple.400}',
      500: '{purple.500}',
      600: '{purple.600}',
      700: '{purple.700}',
      800: '{purple.800}',
      900: '{purple.900}',
      950: '{purple.950}',
    },
    // You can also override specific component tokens
    button: {
      borderRadius: '{radius.xl}' // Make buttons have extra rounded corners
    }
  }
});
```
In this example, we use `definePreset` to extend `Aura`. We re-map the semantic `primary` color to the base `purple` color scale. The system is smart enough to find the corresponding shades. We also override the `borderRadius` for all buttons.

**Step 2: Apply the Custom Preset**
Now, update your `nuxt.config.ts` to use your new custom preset.

```typescript
// nuxt.config.ts
import { MyPreset } from './assets/themes/my-preset'; // Import your custom preset

export default defineNuxtConfig({
  modules: ['@primevue/nuxt-module'],

  primevue: {
    theme: {
      preset: MyPreset // Apply your custom preset
    }
  }
});
```

## 5. Usage in Components

With Styled Mode configured, using components is effortless. You simply place the component in your template, and it will be fully styled according to your chosen (and customized) theme.

```vue
<template>
  <div class="card flex flex-col items-center justify-center gap-4 p-8">
    <h3>Styled Components Example</h3>
    
    <Panel header="User Information" class="w-full md:w-1/2">
      <div class="flex flex-col gap-4">
        <div class="p-fluid">
          <label for="name">Name</label>
          <InputText id="name" />
        </div>
        <div class="p-fluid">
          <label for="city">City</label>
          <Select id="city" :options="cities" optionLabel="name" placeholder="Select a City" />
        </div>
      </div>
    </Panel>
    
    <div class="flex gap-2">
      <Button label="Save Changes" icon="pi pi-check" />
      <Button label="Cancel" severity="secondary" outlined />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const cities = ref([
    { name: 'New York', code: 'NY' },
    { name: 'London', code: 'LDN' },
    { name: 'Paris', code: 'PRS' }
]);
</script>
```

Notice that no `style` tags or `pt` (Pass Through) properties are needed. The components are rendered with the complete theme styles automatically applied, including the custom purple primary color and extra-rounded buttons defined in our custom preset.
