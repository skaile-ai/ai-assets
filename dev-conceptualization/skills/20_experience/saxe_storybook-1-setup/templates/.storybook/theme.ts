import { create } from "storybook/theming";

// ── TEMPLATE ──
// Replace {{PLACEHOLDER}} values with brand tokens from tokens.json and brief.md.

export default create({
  base: "{{MODE}}", // tokens.json mode: "light" or "dark"
  brandTitle: "{{APP_NAME}}", // from brief.md
  brandUrl: "",

  // Colors — from tokens.json
  colorPrimary: "{{colors.primary}}",
  colorSecondary: "{{colors.primary}}",

  // UI
  appBg: "{{colors.background}}",
  appContentBg: "{{colors.surface}}",
  appBorderColor: "{{colors.border}}",
  appBorderRadius: 6,

  // Text
  textColor: "{{colors.text}}",
  textMutedColor: "{{colors.text_muted}}",
  textInverseColor: "{{colors.background}}",

  // Toolbar
  barTextColor: "{{colors.text_muted}}",
  barSelectedColor: "{{colors.primary}}",
  barBg: "{{colors.surface}}",

  // Form
  inputBg: "{{colors.surface}}",
  inputBorder: "{{colors.border}}",
  inputTextColor: "{{colors.text}}",
  inputBorderRadius: 6,

  // Typography — from tokens.json
  fontBase: '"{{fonts.body}}", sans-serif',
  fontCode: '"{{fonts.mono}}", monospace',
});
