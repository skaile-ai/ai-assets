# PostXL Schema Grammar

Portable reference — applies to **any** PostXL project, not just skaile-dev.

Authority is the Zod decoder, not this page. When something here disagrees with
`node_modules/@postxl/schema/dist/`, the decoder wins — read
`model/model.json-decoder.js` and `field/*.js`. Verified against `@postxl/schema` 2.3.0.

> `@postxl/cli` ships `"files": ["dist"]` and no markdown. Guidance pointing at
> `node_modules/@postxl/cli/docs/postxl-for-agents.md` is dead — that file does not exist in
> any installed version. The framework's own copy of that document is also stale in places
> (it still documents `DiscriminatedUnion`, removed from the decoder).

## Where the schema lives

`postxl-schema.json` at the project root is the source of truth. Large schemas may split per
model / per enum under `schema/`. Inline `models` / `enums` and split files are merged at
parse time; defining the same name on both sides is an error.

## Two different strictness regimes — this is the thing to internalise

**Field-level keys are strict. Model-level keys are an open extension surface.** They fail in
opposite ways, and conflating them is how the wrong mental model gets built.

| Level | Decoder | Unknown key |
|---|---|---|
| **Field** | `.strict()` | **Hard error** — `unrecognized_keys` |
| **Model** | `z.object()` (strips) | Silently dropped from the model, **retained in `model.source`** |

A misspelled *field* key is caught for you:

```
{ "type": "String", "bogusKey": 1 }
→ Error decoding field Widget.title: unrecognized_keys ["bogusKey"]
```

A misspelled *model* key is not. `zModelJSON` is `z.preprocess(input => ({...input, source:
{...input}, fields: …}), z.object({…}))` — the preprocess stashes the untouched original on
`source`, then the object decoder strips anything it does not declare.

**That is a feature, not a leak.** Several real model-level keys are absent from `zModelJSON`
on purpose — `repository` and `actions` among them — because the generators that own them
re-parse them off the preserved original (`zRepository.parse(model.source)`,
`zUpdateActions.parse(model.source)`).

So the rule is: **a model-level key does something if and only if some generator reads it off
`model.source`.** To check any key you are unsure about, grep the generator packages for
`.parse(model.source)` — that gives you the real consumer list rather than a guess. Today there
are three:

| Consumer | Reads |
|---|---|
| `backend-repositories/repositories.generator.js:157` | `zRepository` |
| `backend-database-prisma/generators/prisma-schema.generator.js:18` | `zRepository` |
| `backend-update/update.generator.js:182` | `zUpdateActions` |

If nothing reads a key, it parses clean and emits nothing.

### The live instance: `compositeUnique`

`compositeUnique` is not a PostXL key and no generator reads it. It parses without complaint
and produces **no constraint at all** — zero occurrences in `@postxl/schema`, while `indexes`
is decoded in `model.json-decoder.js` and `model.transformer.js`.

Use `indexes`. It is a declared model key, so it also *validates*: a bad field reference is a
hard error (`Index [a,b] in model Widget references unknown field a!`) — which is exactly the
protection `compositeUnique` silently forgoes.

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
`{ "fields": [...] }` and expecting uniqueness is the easy mistake.

Field order is preserved verbatim in the generated Prisma index. For a **single-column**
unique constraint use field-level `isUnique: true`; for a plain single-column index,
`hasIndex: true`.

## Naming rules that are hard errors

These fail at decode time with clear messages, and agents hit them within the first few
minutes because the habits are so ingrained:

| Rule | Failure |
|---|---|
| Model names are **PascalCase** | `"widget"` → *Model name "widget" must be PascalCase, i.e. Widget!* |
| Model names are **not pluralized** | `"Widgets"` → *Model Widgets must not be pluralized!* |
| Relation field names **end in `Id`** | `widget: { type: "Widget" }` → `invalid_union`. Use `widgetId`. |

Naming a collection model in the plural is the single most common first mistake.

### `prismaRelationFieldName` — the error message is backwards

Two relations from one model to the same target need **nothing**. The property defaults per
field to `<Model>.<fieldName>`, so `aId` and `bId` both pointing at `Widget` are distinct by
construction and parse clean.

The duplicate check fires **only when you set it explicitly to the same value twice**:

```
Model Gadget references Widget multiple times (reference name gadgets).
Use 'prismaRelationFieldName' property
```

That message reads as though having multiple relations is the trigger and as though you have
not used the property — when in fact you already did, identically, twice. Do not add
`prismaRelationFieldName` in response to it; change one of the two values, or remove both and
let the defaults apply.

## Field types

`String`, `Int`, `Float`, `Boolean`, `DateTime`, `Json`, plus:

- **Relations** — `"type": "Widget"`, or `"Widget?"` for optional. The field name must end in `Id`.
- **Enum references** — `"type": "<EnumName>"`.
- **Inline enums** — see below.

**`DiscriminatedUnion` no longer exists.** It was removed from the framework and a field
declaring it is a hard `invalid_union` decode error, not a silent no-op. Older documentation
(including the framework's own `postxl-for-agents.md` §3) still lists it with `commonFields`
and `members`; that text is stale.

### Inline enums

The object form's **value is a description, not a duplicate of the name**:

```jsonc
"status": { "type": { "Draft": "not yet published", "Published": "live" } }
```

`{ "Draft": "Draft" }` parses, but sets the description to the literal string "Draft" — which
is why it looks harmless and reads as noise in generated output. The array form is available
when you want names without descriptions, mixed with described members:

```jsonc
"status": { "type": ["Draft", { "value": "Published", "description": "live" }] }
```

An inline enum is named `{ModelName}{PascalCase(fieldName)}` — that is how you reference it
elsewhere.

## Field options

| Key | Purpose |
|---|---|
| `maxLength` | **String only** |
| `min`, `max` | **numeric only**; `min <= max` enforced |
| `defaultValue` | column default — emitted as a Prisma `@default(...)`, not merely applied in the create decoder. Rejected at decode time if outside `min`/`max` |
| `isUnique` | column-level `@unique` |
| `hasIndex` | column-level `@index` |
| `isCreatedAt`, `isUpdatedAt` | auto-managed timestamps |
| `isReadonly` | server-stamped; not writable through the generated update surface |
| `description` | carried into generated JSDoc — write invariants here, they reach the agent |

**`int` and `float` are not keys.** They are derived from the database type (`Int` vs `Float`)
into `validations.type`. Writing either is an `unrecognized_keys` error under field strictness.

Every constraint above is a **hard error**, not a warning — which is the field-level half of
the strictness table at the top.

`standardFields: ["id", "createdAt", "updatedAt"]` opts into the common set instead of
declaring each.

### Relation options

`onDelete` (see `field/relation.js` for the accepted behaviours) and `deepClone`. Worth
knowing: **cascade runs in the generated update service, not in the database**, so it behaves
identically under in-memory repositories — a cascade you rely on is not evidence of a DB-level
foreign-key rule.

## Model-level keys

| Key | Purpose |
|---|---|
| `auth` | `read` / `write` / `create` / `update` / `delete` and per-action rules; `auth.adminUi.visibleFor` gates Admin UI visibility |
| `repository.type` | `DatabaseDirect` \| `DatabaseCached` \| `InMemory` \| `NoRepository`. **Defaults to `DatabaseCached`** — see below |
| `actions` | custom actions; also read off `model.source` |
| `faker` | `seed` + `items` for count, plus per-field expressions (`lorem.slug`, `internet.email`) |
| `labelField` / `keyField` | human-readable identifier used by navigation, Excel import, admin views |
| `defaultSort` | array of field names, applied ascending |
| `indexes` | see above |
| `seed` | literal seed rows |

Top-level `standardModels` opts into built-ins: `User`, `Action`, `ActionOperation`, `File`,
`TableView`, `Comment`, `Config`.

### `repository.type` defaults to `DatabaseCached`

**Omit `repository` and your model silently gets a cached Prisma repository.** The default is
applied in two layers, in `@postxl/generators/dist/backend-repositories/model.types.js` — one
on the type enum, one on the whole object — so both "no `repository` key" and
`"repository": {}` resolve the same way:

| Authored | Resolved |
|---|---|
| no `repository` key | `{ type: "DatabaseCached" }` |
| `"repository": {}` | `{ type: "DatabaseCached" }` |
| `"repository": { "type": "InMemory" }` | `{ type: "InMemory" }` |

**The default lives in the generators, not in `@postxl/schema`.** So `pxl validate` and any
schema-level inspection report `repository: undefined` for a model that will in fact get a
cached repo — the schema layer is not where the answer is. Same shape as the model-level key
trap above: the decoder's silence is not information.

Visible end-to-end in generated output — a model with no `repository` key emits a repository
whose comments describe cached reads, while one pinned to `DatabaseDirect` emits the
uncached form. Note that `model.defaults.js` shows `DatabaseDirect` for `Action` and
`ActionOperation`; those are per-standard-model overrides, not the general default.

## Project-level auth and the public route surface

Model-level `auth` is not the whole story, and the rest of it is security-relevant:

- **`auth.defaultDeny`** — the setting that makes unlisted access denied. Without it, "I did not
  list this" does not mean "this is closed."
- **`auth.roleClaimPath`**, **`auth.scopes`**, and top-level **`schemaAuth`**.
- **`exposeRestApi`** (default `false`) plus deny-by-default environment gates:
  `REST_API_ENABLED`, `OPENAPI_DOC_ENABLED`, `EXCEL_IO_ENABLED`, and the global `APP_GUARD` /
  `@Public()` split.

A skill that tells you to read the migration SQL to confirm a constraint should also tell you
to audit the generated public route surface. Same discipline, higher stakes.

## Splitting a schema

- `schema/<camelCaseName>.model.json` → model `<PascalCaseName>`
- `schema/<camelCaseName>.enum.json` → enum `<PascalCaseName>`
- Contents are the same JSON shape **minus** the `name` field — the filename is the source of
  truth.
- Override the default globs with top-level `modelFiles` / `enumFiles` arrays.

**The filename rule is a first-character case flip, not a general camelCase conversion**
(`fileBasename.charAt(0).toUpperCase() + fileBasename.slice(1)`). `OAuthFlowState` →
`oAuthFlowState.model.json`, *not* `oauthFlowState.model.json`. Getting it wrong makes the
model silently absent from the merge.

The merged collection sorts by name, so splitting an inline block is a generation no-op **only
if the inline models were already alphabetical**. Splitting a non-alphabetical block reorders
models and can shift ordering-sensitive aggregate output.

## Verifying a schema change actually landed

`pxl validate` catches shape errors, not semantic no-ops. For anything that should produce a
database constraint, read the generated migration SQL. A schema that parses is not evidence
that a constraint exists — and at model level, the decoder will not tell you.
