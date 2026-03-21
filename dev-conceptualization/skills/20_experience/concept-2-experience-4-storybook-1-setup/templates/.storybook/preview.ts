import type { Preview } from "@storybook/react";
import "../src/styles/brand.css";

const preview: Preview = {
  parameters: {
    layout: "fullscreen",
    viewport: {
      viewports: {
        mobile: { name: "Mobile", styles: { width: "375px", height: "812px" } },
        tablet: { name: "Tablet", styles: { width: "1024px", height: "768px" } },
        desktop: { name: "Desktop", styles: { width: "1440px", height: "900px" } },
      },
    },
    a11y: {
      config: { rules: [{ id: "color-contrast", enabled: true }] },
    },
  },
  globalTypes: {
    theme: {
      description: "Theme mode",
      toolbar: {
        title: "Theme",
        icon: "paintbrush",
        items: [
          { value: "light", title: "Light", icon: "sun" },
          { value: "dark", title: "Dark", icon: "moon" },
        ],
        dynamicTitle: true,
      },
    },
  },
  initialGlobals: { theme: "light" },
  decorators: [
    (Story, context) => {
      const theme = context.globals.theme || "light";
      document.documentElement.classList.toggle("dark", theme === "dark");
      document.body.style.backgroundColor = theme === "dark" ? "#1C1917" : "#FAFAF9";
      document.body.style.color = theme === "dark" ? "#FAFAF9" : "#1C1917";
      return Story();
    },
  ],
};

export default preview;
