# storybook CLI

## Trigger

Invoke with: "storybook", "component library", "design system", "story", or after screens are approved.

## Output

- `_concept/experience/4_storybook/` — complete standalone Storybook project
  - `.storybook/` — config (main, preview, theme)
  - `src/components/` — custom building-block components
  - `src/pages/` — full-page screen compositions
  - `src/stories/` — stories organized in Components/Pages/Journeys layers
  - `package.json` — Storybook + addon deps

## Sub-skills (run in sequence)

1. `storybook-setup` — scaffold, install deps, apply brand tokens
2. `storybook-components` — custom components + stories
3. `storybook-pages` — AppShell + page compositions + stories
4. `storybook-journeys` — clickable journey flows (if stories.json exists)

## Run the Storybook

```bash
cd _concept/experience/4_storybook
<package_manager> install
<package_manager> run storybook
```

## Next Steps

After completion:
- Review the brand applied across all components and screens
- Share `brandbook.html` and Storybook with stakeholders
- Run implementation skills to build the real app
