---
name: dashboard-layout
description: Template for data-heavy administrative views or user dashboards.
---

# Dashboard Layout Template

When the user requests a Dashboard or Admin Panel, strictly adhere to this layout structure.

## Core Structure
- **Left Sidebar (Navigation):** Fixed width (e.g., `w-64`), containing primary navigation links (with icons), brand logo at the top, and user profile at the bottom.
- **Top Header:** Flex container for global search, notification bell, and contextual actions. Stick to the top.
- **Main Content Area:** A flexible, scrolling area for widgets, tables, and charts. Use a soft background color (e.g., `bg-slate-50`) to separate it from the white sidebar and header.

## Component Patterns
- **Metric Cards (KPIs):** Display key statistics in a CSS Grid (e.g., `grid-cols-1 md:grid-cols-3 lg:grid-cols-4`) at the top of the main content area. Use `bg-white`, subtle shadows, and a defining icon.
- **Data Tables:** Full width within their container. Ensure headers are distinct (e.g., uppercase, `text-xs`, `text-slate-500`). Row hover states are mandatory (`hover:bg-slate-50`).
- **Charts/Graphs:** If requested, structure a placeholder div with a fixed height (e.g., `h-80`) and an internal centered label indicating "Chart Area".

## Anti-Patterns
- **No Floating Content Without Anchors:** Everything in a dashboard must align to a strict grid.
- **No Clutter:** Hide secondary actions inside a "..." (More Options) dropdown menu within table rows.

## Data Binding Requirements
- Populate with realistic administrative data (e.g., "Active Users", "Revenue", names like "Johannes Schmidt").
