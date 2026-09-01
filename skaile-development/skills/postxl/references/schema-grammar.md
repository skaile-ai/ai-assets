# PostXL Schema Grammar

Portable reference — applies to **any** PostXL project, not just skaile-dev.

Authority is the Zod decoder, not this page. When something here disagrees with
`node_modules/@postxl/schema/dist/`, the decoder wins — read
`dist/model/model.json-decoder.js` and `dist/field/*.js`.

> `@postxl/cli` ships `"files": ["dist"]` and no markdown. Older guidance pointing at
> `node_modules/@postxl/cli/docs/postxl-for-agents.md` is dead — that file does not exist
> in any installed version. Use the decoders.

## Where the schema lives

`postxl-schema.json` at the project root is the source of truth. Large schemas may split
per model / per enum under `schema/` (see *Splitting* below). Inline `models` / `enums`
and split files are merged at parse time; defining the same name on both sides is an error.

## The unknown-key trap — read this before writing a constraint

**The model decoder is `passthrough()`, not `strict()`.** An unrecognized key is kept in
the parsed object and ignored by every generator. There is no error, no warning, and no
generated output. A misspelled or invented key looks exactly like a working one.

The live instance of this: **`compositeUnique` is not a PostXL schema key.** It appears in
some hand-written schemas and produces no constraint at all —
`grep -c compositeUnique node_modules/@postxl/schema/dist/**/*.js` returns 0, while
`indexes` is decoded in `model.json-decoder.js` and `model.transformer.js`.

Use `indexes`. Verify by reading the generated migration SQL, never by the schema parsing
without complaint — it always parses without complaint.

## Composite indexes and uniqueness

```jsonc
"indexes": [
  { "fields": ["organizationId", "slug"], "unique": true, "map": "widget_org_slug_key" }
]
```

| Form | Meaning |
|---|---|
| `["a", "b"]` (bare array shorthand) | multi-column **UNIQUE** index |
| `{ "fields": [...] }` | plain `@@index` — **`unique` defaults to `false`** |
| `{ "fields": [...], "unique": true }` | multi-column unique constraint |
| `"map": "<name>"` | pins the DB constraint/index name — use it to match an existing constraint so Prisma reports no drift |

The shorthand and the object form default oppositely on `unique`. Writing
`{ "fields": [...] }` and expecting a unique constraint is the easy mistake.

Field order is preserved verbatim in the generated Prisma index.

For a **single-column** unique constraint use the field-level `isUnique: true`; for a plain
single-column index use `hasIndex: true`.

## Field types

`String`, `Int`, `Float`, `Boolean`, `DateTime`, `Json`, plus:

- **Relations** — `"type": "Post"`, or `"Post?"` for optional.
- **Enum references** — `"type": "<EnumName>"`.
- **Inline enums** — an object literal of `{ "Value": "Value" }`.
- **DiscriminatedUnion** — with `commonFields` and `members`.

## Field options

| Key | Purpose |
|---|---|
| `maxLength`, `min`, `max`, `int`, `float` | validation |
| `isUnique` | column-level `@unique` |
| `hasIndex` | column-level `@index` |
| `isCreatedAt`, `isUpdatedAt` | auto-managed timestamps |
| `isReadonly` | server-stamped; not writable through the generated update surface |
| `defaultValue` | column default |
| `description` | carried into generated JSDoc — write invariants here, they reach the agent |

`standardFields: ["id", "createdAt", "updatedAt"]` opts into the common set instead of
declaring each.

## Model-level keys

| Key | Purpose |
|---|---|
| `auth` | `read` / `write` / `create` / `update` / `delete` and per-action rules; `auth.adminUi.visibleFor` gates Admin UI visibility |
| `repository.type` | `DatabaseDirect` \| `DatabaseCached` \| `InMemory` \| `NoRepository` |
| `faker` | `seed` + `items` for count, plus per-field expressions (`lorem.slug`, `internet.email`) |
| `labelField` / `keyField` | human-readable identifier used by navigation, Excel import, admin views |
| `defaultSort` | array of field names, applied ascending |
| `indexes` | see above |
| `seed` | literal seed rows |

Top-level `standardModels` opts into built-ins: `User`, `Action`, `ActionOperation`,
`File`, `TableView`, `Comment`, `Config`.

## Splitting a schema

- `schema/<camelCaseName>.model.json` → model `<PascalCaseName>`
- `schema/<camelCaseName>.enum.json` → enum `<PascalCaseName>`
- Contents are the same JSON shape **minus** the `name` field — the filename is the
  source of truth.
- Override the default globs with top-level `modelFiles` / `enumFiles` arrays.

**The filename rule is a first-character case flip, not a general camelCase conversion.**
`OAuthFlowState` → `oAuthFlowState.model.json`, *not* `oauthFlowState.model.json`. Getting
this wrong makes the model silently absent from the merge — the same failure shape as the
unknown-key trap.

Merging always sorts alphabetically, so splitting an existing inline schema is a
generation no-op if the filenames are right.

## Verifying a schema change actually landed

`pxl validate` catches shape errors, not semantic no-ops. For anything that should produce
a database constraint, read the generated migration SQL. A schema that parses is not
evidence that a constraint exists.
