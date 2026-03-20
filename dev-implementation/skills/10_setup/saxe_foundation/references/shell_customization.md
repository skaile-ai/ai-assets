# Shell Customization Patterns

## PostXL Generated Infrastructure

The PostXL generated project already provides most foundation infrastructure:
- ThemeProvider (light/dark mode with localStorage persistence)
- SidebarProvider with DynamicTabbedSidebar
- CommandPalette
- Sonner toast notifications
- Auth guards (redirect to /login)
- Keycloak OIDC configuration
- Error boundaries

The foundation skill's main value-add is:
1. Replacing default `theme-default.css` values with brand tokens from `tokens.json`
2. Adding custom font imports (Google Fonts / Fontshare)
3. Creating a shared sidebar navigation component at the layout level
4. Updating the header with project branding

Steps 3 (auth), 5 (shared services), and 6 (seed data) are largely done by
PostXL and scaffold. Focus effort on Steps 2 (brand tokens) and 4 (app shell
customization).

## HSL Color Conversion

The PostXL `theme-default.css` uses HSL color format (e.g., `hsl(239 84% 67%)`)
for all CSS custom properties. The brand `tokens.json` typically provides hex
values. You must convert hex to HSL when applying brand tokens. The `tailwind`
section in `tokens.json` provides hex values that need HSL conversion for
`theme-default.css`.

### Example: theme-default.css

```css
:root {
  --primary: 239 84% 67%;        /* converted from tokens.json hex */
  --secondary: 168 76% 36%;
  /* ... all tokens from tokens.json, converted to HSL */
}
.dark {
  --primary: 232 89% 76%;
  --secondary: 172 66% 56%;
  /* ... dark mode overrides, converted to HSL */
}
```

## DynamicTabbedSidebar Pattern

PostXL uses a `DynamicTabbedSidebar` where each page registers its own
`SidebarTab` components. The sidebar content is NOT a single static
component — it's composed per-page.

**Navigation must be shared at the layout level, not per-page.** Create a shared
`AppSidebarNavigation` component that renders the common navigation `SidebarTab`
elements (e.g., Apps, Settings, Admin, AI Assistant) and place it in the layout
(`authorized-page-layout.tsx`) using `@custom-start`/`@custom-end` blocks.

Individual pages should NOT register navigation tabs — they should only register
page-specific sidebar content (e.g., detail panels, contextual tools).

Without this, each page will have inconsistent navigation — some showing all
tabs, others showing only one.

## UI Component Usage

Use `@postxl/ui-components` for all UI primitives (Sidebar, Avatar,
DropdownMenu, Button, etc.). Never create custom components when a library
component exists.

## Authentication Configuration

PostXL already provides Keycloak OIDC configuration, auth guards, and login
redirect. The foundation step is about **verifying** the existing setup and
making project-specific adjustments:

1. Verify the auth configuration matches the concept's role requirements
2. Adjust role mappings if the concept defines custom roles beyond the defaults
3. Verify mock auth mode works for development (`AUTH_MOCK=true`)
4. If the concept specifies SSO or social login, configure Keycloak federation

Most projects need only minor adjustments here. Skip if the generated defaults
match the concept.

## Shared Services

PostXL already provides error boundaries, Sonner toast notifications, and auth
navigation guards. Only create custom shared services if the concept requires
something beyond PostXL defaults (e.g., custom notification system, real-time
presence).

## Seed Data

The `implement-1-setup-1-scaffold` skill already copies concept seed data into
`backend/test-data.json` and configures the seed migration. Foundation only
needs to verify:

1. Run the seed: `pnpm run seed`
2. Verify seed data loaded correctly by querying a few records
3. Verify the dev user (mock auth identity `sub: "test"`) has appropriate
   org/workspace memberships to see data on startup

## Storybook Foundation

1. Configure Storybook theme to use brand tokens (background, fonts, colors)
2. Create a theme decorator that wraps all stories with the brand's CSS variables
3. Set up viewport presets matching the shell spec's responsive breakpoints
4. Verify Storybook starts: `pnpm run storybook:start`

## Example E2E Test Updates

PostXL generates a default `example.spec.ts` E2E test that looks for default
dashboard content (e.g., `Hi Test User!`) and theme toggle behavior. Custom
dashboard and shell implementations break these tests.

Update `e2e/specs/example.spec.ts` to match the new foundation content:
- Replace default greeting assertions with actual dashboard content
- Verify theme toggle still works with custom brand tokens
- Verify navigation links from the custom shell
