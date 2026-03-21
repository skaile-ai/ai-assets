# Storybook Patterns for PostXL Components

How to create Storybook stories for features built with `@postxl/ui-components`.

## Story File Convention

Place stories next to the component they document:

```
frontend/src/
├── pages/<group>/
│   ├── <Page>.tsx
│   └── <Page>.stories.tsx      ← page-level story
├── components/<group>/
│   ├── <Component>.tsx
│   └── <Component>.stories.tsx ← component-level story
```

Naming: `<Component>.stories.tsx` — matches the component file name.

## Basic Story Structure

```typescript
import type { Meta, StoryObj } from '@storybook/react'
import { LoginForm } from './LoginForm'

const meta: Meta<typeof LoginForm> = {
  component: LoginForm,
  title: 'Features/AuthWorkspace/LoginForm',
  tags: ['autodocs'],
  parameters: {
    layout: 'centered', // or 'fullscreen' for pages
  },
}
export default meta

type Story = StoryObj<typeof LoginForm>

export const Default: Story = {
  args: {
    onSubmit: async (data) => console.log('submit', data),
  },
}

export const Loading: Story = {
  args: {
    ...Default.args,
    isSubmitting: true,
  },
}

export const WithError: Story = {
  args: {
    ...Default.args,
    error: 'Invalid email or password',
  },
}
```

## Story Categories

Organize by feature group in the Storybook sidebar:

```
Features/
├── AuthWorkspace/
│   ├── LoginForm
│   ├── OrgSetupWizard
│   └── WorkspaceSettings
├── AppConcept/
│   ├── ConceptWizard
│   └── ConceptReview
├── AppBuilder/
│   ├── ChatPanel
│   ├── PreviewPanel
│   └── BuilderToolbar
└── Layout/
    ├── AppShell
    ├── Sidebar
    └── Header
```

## Required State Variants

For every component, create stories matching the screen spec's `## States` section:

| Screen state | Story name | What to show |
|-------------|-----------|-------------|
| Default | `Default` | Normal rendering with populated data |
| Loading | `Loading` | Skeleton/spinner states |
| Error | `WithError` | Error messages, retry buttons |
| Empty | `Empty` | Zero-data state, onboarding prompts |
| Success | `Success` | Post-action confirmation (if applicable) |

## Using Seed Data in Stories

Import seed data from the concept for realistic content:

```typescript
import seedData from '../../../../_concept/3_blueprint/3_datamodel/seed.json'

const populatedApps = seedData.scenarios.populated.data.App

export const WithApps: Story = {
  args: {
    apps: populatedApps,
  },
}

export const Empty: Story = {
  args: {
    apps: [],
  },
}

export const EdgeCases: Story = {
  args: {
    apps: seedData.scenarios.edge_cases.data.App,
  },
}
```

## Data Grid Stories

For components using `@postxl/ui-components` DataGrid:

```typescript
export const DataGridDefault: Story = {
  args: {
    data: populatedApps,
    columns: [
      { accessorKey: 'name', header: 'App Name' },
      { accessorKey: 'status', header: 'Status' },
      { accessorKey: 'createdAt', header: 'Created' },
    ],
  },
}

export const DataGridEmpty: Story = {
  args: {
    data: [],
    emptyMessage: 'No apps yet. Create your first app to get started.',
  },
}

export const DataGridLoading: Story = {
  args: {
    data: [],
    isLoading: true,
  },
}
```

## Form Stories

For components using TanStack Form:

```typescript
export const FormDefault: Story = {
  args: {
    defaultValues: {
      name: '',
      description: '',
    },
    onSubmit: async (values) => {
      await new Promise(r => setTimeout(r, 1000))
      console.log('submitted', values)
    },
  },
}

export const FormPrefilled: Story = {
  args: {
    defaultValues: {
      name: 'Existing App',
      description: 'An app that already exists',
    },
  },
}

export const FormWithValidationErrors: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    // Trigger validation by submitting empty form
    await userEvent.click(canvas.getByRole('button', { name: 'Submit' }))
  },
}
```

## Interactive Stories with Play Functions

Use play functions for interactive state demonstrations:

```typescript
import { within, userEvent, expect } from '@storybook/test'

export const SubmitFlow: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)

    await userEvent.type(canvas.getByLabelText('Email'), 'test@example.com')
    await userEvent.type(canvas.getByLabelText('Password'), 'password123')
    await userEvent.click(canvas.getByRole('button', { name: 'Sign In' }))

    await expect(canvas.getByText('Welcome')).toBeInTheDocument()
  },
}
```

## Theme Decorator

The Storybook theme decorator (set up by `implement-1-setup-2-foundation`) wraps all stories
with the brand's CSS custom properties. Stories automatically inherit the
correct colors, fonts, and spacing.

To test dark mode:

```typescript
export const DarkMode: Story = {
  parameters: {
    backgrounds: { default: 'dark' },
  },
  decorators: [
    (Story) => (
      <div data-theme="dark">
        <Story />
      </div>
    ),
  ],
}
```

## Responsive Stories

Use viewport parameters to test responsive behavior:

```typescript
export const Mobile: Story = {
  parameters: {
    viewport: { defaultViewport: 'mobile1' },
  },
}

export const Tablet: Story = {
  parameters: {
    viewport: { defaultViewport: 'tablet' },
  },
}
```
