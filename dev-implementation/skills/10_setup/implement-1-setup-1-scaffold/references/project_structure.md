# PostXL Project Structure

The `pxl create-project` command generates this directory layout:

```
<app-slug>/
├── backend/
│   ├── apps/api/
│   ├── libs/
│   └── package.json
├── frontend/
│   ├── src/
│   └── package.json
├── e2e/
│   └── package.json
├── docker-compose.yml
├── package.json
├── postxl-schema.json
├── generate.ts
└── tsconfig.json
```

## Notes

- The generated project has **no root-level `build` script**. Build backend and frontend separately.
- `.env.example` files are generated at root, `backend/apps/api/`, `frontend/`, and `e2e/`. The `pnpm run setup` script copies them to `.env`.
- Without running `setup`, the backend fails with Zod validation errors for missing config values.
