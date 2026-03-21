# Recipe: Nuxt 3 Project Setup with `src` Directory and ESLint

This guide outlines a recommended setup for a Nuxt 3 project, utilizing a `src` directory for application code, organizing common Nuxt features like pages and layouts, and configuring ESLint for code quality.

## 1. Project Initialization

Start by creating a new Nuxt 3 project.

```bash
npx nuxi@latest init <project-name>
cd <project-name>
pnpm install
```

## 2. Configure `src` Directory

It's a common practice to place your application's source code within a `src` directory (e.g., `app/` or `src/`). This helps in organizing the project, especially in larger applications.

### Step 1: Create the `app` Directory

Create a directory named `app` at the root of your project.

```bash
mkdir app
```

### Step 2: Move Nuxt Application Files

Move your core Nuxt application files (like `app.vue`, `pages/`, `layouts/`, `components/`, etc.) into this new `app` directory.

### Step 3: Update `nuxt.config.ts`

Configure Nuxt to use the `app/` directory as its source directory.

**File:** `nuxt.config.ts`

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  srcDir: "app/", // Specify your source directory here
  // ... other configurations
});
```

## 3. Project Structure Overview

With the `srcDir` configured, your project will typically have the following structure within the `app/` directory:

```
app/
├── assets/         # For static assets like CSS, images, fonts
│   └── css/
│       └── tailwind.css
├── components/     # Vue components
├── composables/    # Reusable Vue composables
├── layouts/        # Layouts for pages
│   └── default.vue
├── middleware/     # Nuxt middleware
├── pages/          # Vue pages (file-based routing)
│   ├── about.vue
│   ├── index.vue
└── utils/          # Utility functions

server/           # Server routes and API (Must be at project root, NOT in app/)
├── api/
└── routes/
```

## 4. ESLint Setup

Integrating ESLint ensures consistent code style and helps catch potential errors.

### Step 1: Install ESLint Module

Install the official Nuxt ESLint module:

```bash
# Using pnpm
pnpm add -D @nuxt/eslint
```

### Step 2: Configure Nuxt for ESLint

Add `@nuxt/eslint` to your `modules` array in `nuxt.config.ts` and configure stylistic rules.

**File:** `nuxt.config.ts`

```typescript
// nuxt.config.ts
export default defineNuxtConfig({
  modules: [
    '@nuxt/eslint', // Add this module
    // ... other modules
  ],
  eslint: {
    config: {
      stylistic: {
        semi: true,
        quotes: "double",
        commaDangle: "always-multiline",
        indent: "tab",
      },
    },
  },
  // ... rest of your config
});
```

### Step 3: ESLint Configuration File

Nuxt generates a base ESLint configuration. Your `eslint.config.mjs` will typically import this generated config.

**File:** `eslint.config.mjs`

```javascript
// @ts-check
import withNuxt from "./.nuxt/eslint.config.mjs";

export default withNuxt(
  // Your custom configs here (if any)
);
```

### Step 4: VS Code Settings for ESLint

To integrate ESLint seamlessly with VS Code, create or update your `.vscode/settings.json` file. This ensures formatting and linting on save.

**File:** `.vscode/settings.json`

```json
{
  "prettier.enable": false,
  "editor.formatOnSave": false,
  "editor.codeActionsOnSave": {
    "source.organizeImports": "always",
    "source.fixAll.eslint": "explicit"
  },
  "eslint.codeActionsOnSave.rules": null
}
```

This setup provides a solid foundation for a well-structured and maintainable Nuxt 3 project.