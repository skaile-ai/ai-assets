---
name: Design Tokens & Custom Presets
description: How to use definePreset to create custom themes in PrimeVue 4.
libraries_used: @primeuix/themes
---

# Design Tokens & Custom Presets

## Objective
PrimeVue 4 uses a design-token-first approach. This recipe shows how to extend the base `Aura` theme with a custom semantic palette.

## Prerequisites
- PrimeVue 4
- `@primeuix/themes`

## Instructions

### 1. Define the Preset
Use `definePreset` to override semantic colors or component-specific tokens.

### 2. Register in `nuxt.config.ts`
Import the preset and apply it to the `primevue` module configuration.

## Code Example

```typescript
// themes/MyPreset.ts
import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

export const MyPreset = definePreset(Aura, {
    semantic: {
        primary: {
            50: '{indigo.50}',
            100: '{indigo.100}',
            200: '{indigo.200}',
            300: '{indigo.300}',
            400: '{indigo.400}',
            500: '{indigo.500}',
            600: '{indigo.600}',
            700: '{indigo.700}',
            800: '{indigo.800}',
            900: '{indigo.900}',
            950: '{indigo.950}'
        },
        colorScheme: {
            light: {
                surface: {
                    0: '#ffffff',
                    50: '{slate.50}',
                    // ... customize slate shades
                    950: '{slate.950}'
                }
            }
        }
    }
});
```
```typescript
// nuxt.config.ts
import { MyPreset } from './themes/MyPreset';

export default defineNuxtConfig({
    modules: ['@primevue/nuxt-module'],
    primevue: {
        theme: {
            preset: MyPreset
        }
    }
});
```
