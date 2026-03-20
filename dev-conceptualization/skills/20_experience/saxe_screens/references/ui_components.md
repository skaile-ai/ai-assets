# PostXL UI Component Catalog

Screen specs reference `@postxl/ui-components` by exact name.
Use these names in the Component Inventory section of every screen spec.

## Layout
Sidebar, SidebarTab, Sheet, Resizable, Tabs, Stepper, NavigationMenu,
Breadcrumb, ScrollArea

## Data Display
DataGrid (with cell variants: checkbox, date, number, short-text, long-text,
select, multi-select, json, gantt, hierarchy), Card, CardHover, InfoCard,
Avatar, Badge, Carousel, TreeView, Kanban

## Forms
Input, DeferredInput, NumberInput, Textarea, DeferredTextarea, Select,
Combobox, Checkbox, RadioGroup, Switch, Toggle, ToggleGroup, Slider,
DatePicker, Calendar, Field, Label

## Feedback
Alert, AlertDialog, Dialog, Modal, Drawer, Toast (Sonner), Progress,
Spinner, Skeleton, Tooltip, Popover

## Actions
Button, DropdownMenu, ContextMenu, Command, CommandPalette, Menubar

## Content
Accordion, Collapse, Separator, ContentFrame, Comment, MarkValueRenderer,
Pagination, Slicer

## DataGrid Column Spec

For DataGrid screens, specify the cell variant per column:

```
DataGrid — task list
  columns:
    title (short-text)
    status (select)
    assignedTo (short-text)
    dueDate (date)
```
