# PostXL Component Gotchas

Common pitfalls with `@postxl/ui-components`, TanStack Router, and PostXL hooks
that cause subtle bugs (blank pages, runtime errors, wrong styling).

## Role Guard Arrays

`hasAnyRole(roles, [])` always returns false, causing a blank page. The page
compiles fine but renders nothing. Always verify role arrays contain actual
role strings before passing them to guards.

## Spinner Uses `classNames` (Plural)

`Spinner` takes `classNames`, not `className`. This deviates from standard
React props:

```tsx
// BAD — silently ignored, spinner renders at default size
<Spinner className="h-5 w-5" />

// GOOD
<Spinner classNames="h-5 w-5" />
```

## Progress Component

`Progress` does not accept `className`. To customize height or styling, wrap
it in a `<div>` or use the `size` prop.

## StepperSeparator Placement

`StepperSeparator` must be rendered INSIDE a `StepperItem`, not between items.
Placing it between items throws a runtime error.

## Underline-Style Tabs

`@postxl/ui-components` TabsList/TabsTrigger only support `"default"` and
`"protocol"` variants. For the common underline pattern (bottom-border active
indicator), apply className overrides:

```tsx
<TabsList className="border-b border-border bg-transparent p-0 rounded-none">
  <TabsTrigger
    className="cursor-pointer rounded-none border-b-2 border-transparent px-4 py-2
               data-[state=active]:border-primary
               data-[state=active]:bg-transparent
               data-[state=active]:shadow-none"
  >
    Tab Label
  </TabsTrigger>
</TabsList>
```

## TanStack Router: Index vs Layout Routes

Use `directory/index.tsx` (index route) instead of `file.tsx` (layout route)
when the route has or may have sibling child routes. `file.tsx` is a layout
route that wraps children with `<Outlet />`, so child routes render inside it.

For nested navigation like `/apps/new` + `/apps/new/templates`, use
`new/index.tsx` for the main page.

## Auth Context for Write Operations

Create/update mutations require the current user ID. Import `useAuth()`:

```tsx
import { useAuth } from '@context-providers/auth-context-provider'

const { viewerData } = useAuth()
// Pass viewerData.user.id as createdById to create mutations
```

## PostXL Hook Return Types — Two Patterns

1. **`use<Entity>(id)` / `use<Entity>ByKey(key)`** — returns `ViewModel | null`
   directly (NOT `{ data }`). Do not destructure as `{ data: entity }`.

2. **`use<Entities>(options)`** — returns an object with `{ list, map,
   filteredList, create, update, ... }`. Use `const { list, create } = useApps()`.

## Sidebar Navigation

Pages do NOT register their own navigation `SidebarTab` — navigation is shared
at the layout level (see `implement-1-setup-2-foundation`). Pages should only register
page-specific sidebar content (detail panels, contextual tools).
