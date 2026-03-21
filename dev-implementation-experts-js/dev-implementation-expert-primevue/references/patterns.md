# PrimeVue 4 x Nuxt 3 Patterns

## 1. Theming Architecture (Modern Design Tokens)
- **Always prioritize `definePreset`**: Move away from CSS overrides. Use semantic tokens (e.g., `primary.500`, `radius.md`) to propagate styles project-wide.
- **Component Specific Overrides**: Nest component keys (e.g., `button`, `datatable`) inside the preset to fine-tune specific visual properties.
- **Surface Palette**: Customize the `surface` token for light/dark modes to control background and border colors consistently.

## 2. Nuxt 3 Module Integration
- **Module Order Maters**: When using Tailwind, `@nuxtjs/tailwindcss` must come BEFORE `@primevue/nuxt-module`.
- **Automatic Imports**: Leverage the module's automatic component and directive registration to maintain tree-shaking and clean templates.

## 3. High-Control Styling (PassThrough)
- **Unstyled Mode**: Use `unstyled: true` for full Tailwind control. Use the `tailwindcss-primeui` plugin to bridge Tailwind utilities with PrimeVue's internal state (e.g., `hover`, `active`).
- **Global PT**: Define a global `pt` object in `nuxt.config.ts` (or a separate file) to keep component styling DRY.
- **Declarative PT**: Prefer the declarative object syntax for `pt` properties in templates.

## 4. Modern Form Management
- **The `Form` Component**: Use the new `@primevue/forms` for encapsulated state. No more manual `v-model` management for complex forms.
- **Typed Resolvers**: Integrate with `Valibot` or `Zod` using the validation resolver pattern.
- **In-Field Labels**: Use `IftaLabel` for compact, modern form headers that save vertical space.

## 5. Data & Overlays
- **Lazy DataTable**: Always implement `lazy` loading for production datasets.
- **Global Services**: Ensure `ToastService`, `ConfirmationService`, and `DialogService` are added to the modules list for global `useToast()` etc. availability.