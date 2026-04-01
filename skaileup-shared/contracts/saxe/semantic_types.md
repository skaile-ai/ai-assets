# PostXL Type System

The data model (`3_blueprint/3_datamodel/postxl-schema.json`) uses PostXL field types. PostXL uses Prisma-based types directly — there is no
semantic abstraction layer. Skills write the exact types that the generator consumes.

## Scalar Types

| Type        | Meaning                                              | Example fields         |
| ----------- | ---------------------------------------------------- | ---------------------- |
| `String`    | Short or long text (use `maxLength` for constraints) | name, title, bio, body |
| `String?`   | Optional string                                      | subtitle, nickname     |
| `Int`       | Whole number                                         | count, sortOrder       |
| `Int?`      | Optional whole number                                | rating, retryCount     |
| `Float`     | Decimal number                                       | price, latitude        |
| `Float?`    | Optional decimal                                     | discount, longitude    |
| `Decimal`   | Precise decimal (financial, scientific)              | amount, balance        |
| `Decimal?`  | Optional precise decimal                             | taxRate                |
| `Boolean`   | True/false                                           | isActive, isPublished  |
| `Boolean?`  | Optional boolean                                     | isArchived             |
| `DateTime`  | Date + time                                          | dueDate, publishedAt   |
| `DateTime?` | Optional date + time                                 | deletedAt, completedAt |
| `Json`      | Arbitrary structured data                            | settings, metadata     |
| `Json?`     | Optional structured data                             | preferences            |

The `?` suffix on any type makes the field optional (nullable).

## Relation Types

Relations are expressed as regular fields. Two conventions identify a relation:

1. **Field name** ends with the `Id` suffix (e.g. `ownerId`, `projectId`, `categoryId`).
2. **Type** is the referenced model name (e.g. `User`, `Project`, `Category?`).

Optional relations use the `?` suffix on the model type (e.g. `Category?` means
the relation is nullable).

The inverse side of the relation is generated automatically — you do not need to
declare it in the referenced model.

```jsonc
// Example: Task belongs to a Project, optionally assigned to a User
{
  "name": "projectId",
  "type": "Project",
  "label": "Project",
  "description": "The project this task belongs to"
}
{
  "name": "assigneeId",
  "type": "User?",
  "label": "Assignee",
  "description": "User assigned to this task"
}
```

## Enum Types

Enums are defined inline as an object mapping PascalCase values to descriptions:

```jsonc
{
  "name": "status",
  "type": {
    "Draft": "Initial state",
    "InReview": "Awaiting approval",
    "Published": "Live",
  },
  "label": "Status",
  "description": "Publication status",
}
```

The generator auto-creates a top-level enum named `{ModelName}{PascalCase(fieldName)}`.
For the example above on a `Post` model, the generated enum is `PostStatus`.

Alternatively, reference a shared top-level enum by its name string when multiple
models reuse the same value set.

## Standard Fields

Every model declares a `standardFields` array that auto-generates common fields.
Do **not** define these manually in the fields list:

| Standard field | Generated as                  | Notes              |
| -------------- | ----------------------------- | ------------------ |
| `id`           | `String @id @default(uuid())` | Primary key        |
| `createdAt`    | `DateTime @default(now())`    | Auto-set on create |
| `updatedAt`    | `DateTime @updatedAt`         | Auto-set on update |

Typical declaration: `"standardFields": ["id", "createdAt", "updatedAt"]`

## Model Metadata

Each model supports these top-level properties:

| Property         | Purpose                                                          |
| ---------------- | ---------------------------------------------------------------- |
| `labelField`     | The field used as the display name in UI lists and dropdowns     |
| `keyField`       | URL-friendly unique identifier field (for slugs / readable URLs) |
| `standardFields` | Array of auto-generated fields (`id`, `createdAt`, `updatedAt`)  |

## Field Properties

Every field in the `fields` array supports these properties:

| Property      | Type             | Required | Purpose                                    |
| ------------- | ---------------- | -------- | ------------------------------------------ |
| `name`        | string           | yes      | camelCase field name                       |
| `type`        | string or object | yes      | PostXL type (see above)                    |
| `label`       | string           | yes      | Human-readable label                       |
| `description` | string           | no       | Explanation of the field's purpose         |
| `isUnique`    | boolean          | no       | Enforce uniqueness constraint              |
| `isReadonly`  | boolean          | no       | Prevent editing after creation             |
| `hasIndex`    | boolean          | no       | Create a database index                    |
| `isCreatedAt` | boolean          | no       | Mark as creation timestamp                 |
| `isUpdatedAt` | boolean          | no       | Mark as update timestamp                   |
| `maxLength`   | number           | no       | Maximum character length for String fields |
| `placeholder` | string           | no       | UI placeholder text                        |

## Migration Reference

Mapping from the old semantic type system to PostXL types:

| Old Semantic Type | PostXL Type                    | Notes                                                        |
| ----------------- | ------------------------------ | ------------------------------------------------------------ |
| `string`          | `String`                       | Use `maxLength` if needed                                    |
| `text`            | `String`                       | No separate text type; omit `maxLength` for long text        |
| `richtext`        | `String`                       | Store as HTML or markdown                                    |
| `slug`            | `String`                       | Add `isUnique: true`, use as `keyField`                      |
| `email`           | `String`                       | Validation at application layer                              |
| `url`             | `String`                       | Validation at application layer                              |
| `password`        | `String`                       | Hashing at application layer                                 |
| `integer`         | `Int`                          |                                                              |
| `float`           | `Float` or `Decimal`           | Use `Decimal` for financial data                             |
| `boolean`         | `Boolean`                      |                                                              |
| `date`            | `DateTime`                     | PostXL uses `DateTime` for both date and datetime            |
| `datetime`        | `DateTime`                     |                                                              |
| `enum`            | inline enum object             | `{"Draft": "desc", "Published": "desc"}`                     |
| `json`            | `Json`                         |                                                              |
| `uuid`            | —                              | Handled by `standardFields` for `id`; use `String` elsewhere |
| `image`           | `String` or relation to `File` | URL string, or `imageId` → `File` model                      |
| `file`            | `String` or relation to `File` | URL string, or `fileId` → `File` model                       |
| `relation` (m2o)  | `ModelName`                    | Field name ends in `Id` (e.g. `ownerId` type `User`)         |
| `relation` (o2m)  | —                              | Inverse is automatic; do not declare                         |
| `relation` (m2m)  | junction model                 | Create an explicit join model with two relation fields       |

## Rules for Skills

1. **Model names** are PascalCase singular (e.g. `Task`, `ProjectMember`).
2. **Field names** are camelCase (e.g. `firstName`, `isActive`, `projectId`).
3. **Use PostXL types directly** — no semantic abstraction. Write `String`, `Int`,
   `Boolean`, etc. exactly as listed above.
4. **Enums** use PascalCase values (e.g. `Draft`, `InProgress`, `Done`).
   Never use SCREAMING_SNAKE or lowercase for enum values.
5. **Every model** should declare `standardFields: ["id", "createdAt", "updatedAt"]`.
   Do not add `id`, `createdAt`, or `updatedAt` to the fields array.
6. **Relations** are expressed as fields ending in `Id` with type set to the
   target model name. The inverse is generated automatically.
7. **Use the `faker` property** for test data generation on every field where
   realistic data matters (names, emails, dates, descriptions).
8. **Set `labelField`** on every model so the UI knows which field to display.
9. **Set `keyField`** when the model has a human-readable unique slug.
10. **Append `?`** to make any type optional — do not use a separate `required`
    property.
