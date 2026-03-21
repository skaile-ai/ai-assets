# Recipe: Type Safety with Directus Typeforge

This recipe shows how to use `directus-typeforge` to generate TypeScript types from your Directus schema.

## 1. Installation

```bash
npm install -D directus-typeforge
```

## 2. Generate Types

Run the following command to generate types. Replace the host and token with your values.

```bash
npx directus-typeforge --host http://localhost:8055 --token your-admin-token -o types/directus.ts
```

## 3. Automated Script in `package.json`

Add a script to your `package.json` to easily update your types:

```json
{
  "scripts": {
    "types:sync": "directus-typeforge --host ${DIRECTUS_URL:-http://localhost:8055} --token ${DIRECTUS_TOKEN} -o types/directus.ts"
  }
}
```

## 4. Using the Generated Types

In the generated `types/directus.ts`, you will have a `Schema` interface (or similar name depending on your Directus version). Use this when initializing your Directus SDK client.

```typescript
import type { Schema } from '~/types/directus';
// ...
const client = createDirectus<Schema>(url)...
```

## 5. Tips

- **Run after changes**: Always run the sync script after adding or modifying collections in Directus.
- **Git Commit**: It's good practice to commit the generated types to your repository so other developers have them immediately.
- **Read-only**: Do not edit the generated file manually; it will be overwritten next time you sync.
