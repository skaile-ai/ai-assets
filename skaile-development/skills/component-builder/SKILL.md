---
name: "component-builder"
description: "[skaile-development] Scaffold a new dynamic web component for the Agent App Protocol. Generates COMPONENT.md, web component JS, agent-usage.md, and test harness in the components directory."
metadata:
  version: "1.0.0"
  tags:
    - "component"
    - "scaffold"
    - "web-component"
    - "agent-app-protocol"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "ai-assets/skaile-development/references/component-authoring.md"
        gate: hard
        description: "Component authoring guide - read to understand conventions and checklist"
      - path: "ai-assets/skaile-development/references/component-template.js"
        gate: hard
        description: "Web component starter template with placeholders"
      - path: "ai-assets/skaile-development/references/component-test-harness.html"
        gate: hard
        description: "Test harness template with placeholders"
    inputs_required:
      - id: component_name
        label: "Component name (lowercase, hyphenated, e.g. 'data-chart')"
        type: text
      - id: description
        label: "One-line description of what this component does"
        type: text
    inputs_optional:
      - id: targets
        label: "Render targets (comma-separated)"
        type: text
        default: "preview"
      - id: interactions
        label: "Interaction action names (comma-separated, e.g. 'create,update,export')"
        type: text
        default: ""
      - id: has_input_extension
        label: "Does this have an input extension trigger? (yes/no)"
        type: text
        default: "no"
      - id: input_extension_prefix
        label: "Input extension prefix trigger (e.g. '/search'). Only if has_input_extension=yes"
        type: text
      - id: custom_message_type
        label: "Custom message type name (blank if none, e.g. 'gif')"
        type: text
      - id: props
        label: "Component props (comma-separated key:type pairs, e.g. 'items:array,title:string')"
        type: text
  produces:
    - path: "ai-assets/skaile-development/components/<name>/COMPONENT.md"
      description: "Component manifest with YAML frontmatter"
    - path: "ai-assets/skaile-development/components/<name>/<name>.js"
      description: "Web component ES module"
    - path: "ai-assets/skaile-development/components/<name>/agent-usage.md"
      description: "Agent rendering reference"
    - path: "ai-assets/skaile-development/components/<name>/test-harness.html"
      description: "Standalone browser test page"
  user_inputs:
    dialog:
      - id: "component_name"
        label: "Component name (lowercase, hyphenated)"
        type: "text"
        required: true
      - id: "description"
        label: "One-line description"
        type: "text"
        required: true
      - id: "targets"
        label: "Render targets"
        type: "text"
        required: false
        default: "preview"
      - id: "interactions"
        label: "Interaction action names"
        type: "text"
        required: false
      - id: "has_input_extension"
        label: "Has input extension trigger?"
        type: "select"
        options: ["no", "yes"]
        required: false
        default: "no"
      - id: "custom_message_type"
        label: "Custom message type name (blank if none)"
        type: "text"
        required: false
      - id: "props"
        label: "Props (key:type pairs, e.g. 'items:array,title:string')"
        type: "text"
        required: false
    files: []
---

# Component Builder - Scaffold a New Agent App Protocol Component

## Overview

Generates all files for a new dynamic web component. Reads the authoring guide and
templates, substitutes values, and writes four files to the components directory.

## When to Use

- Creating a new component for the Agent App Protocol
- Prototyping a component to validate the manifest structure

## When NOT to Use

- Modifying an existing component (edit files directly)
- Building a component outside the ai-assets directory

---

ROLE  Component scaffolding tool - generates COMPONENT.md, JS, agent-usage.md, and test harness.

READS
  ai-assets/skaile-development/references/component-authoring.md     -- conventions + checklist
  ai-assets/skaile-development/references/component-template.js      -- JS template
  ai-assets/skaile-development/references/component-test-harness.html -- test harness template

WRITES
  ai-assets/skaile-development/components/{{name}}/COMPONENT.md
  ai-assets/skaile-development/components/{{name}}/{{name}}.js
  ai-assets/skaile-development/components/{{name}}/agent-usage.md
  ai-assets/skaile-development/components/{{name}}/test-harness.html

MUST  read the authoring guide before generating any files
MUST  validate that the component name is lowercase-hyphenated
MUST  validate that tagName follows the skaile-<name> convention
MUST  generate all four files in a single pass
MUST  run the pre-flight checklist against the generated output
NEVER  generate a component with hardcoded colors (must use theme tokens)
NEVER  use attribute serialization for complex props (arrays, objects)

# -- Phase 1: Parse Inputs ------------------------------------------------

STEP 1: Validate and derive values
  - component_name: must be lowercase, hyphenated, no spaces
  - PascalName: convert component_name to PascalCase (e.g. "data-chart" -> "DataChart")
  - tagName: "skaile-" + component_name
  - targets: split comma-separated string into array (default: ["preview"])
  - interactions: split comma-separated string into array (default: [])
  - props: parse key:type pairs into schema properties

  DERIVE provides_type:
    IF has_input_extension = "yes"
      provides_type = "input-extension"
    ELSE IF custom_message_type is not blank
      provides_type = "chat-renderer"
    ELSE
      provides_type = "component"

  DERIVE output_dir:
    output_dir = "ai-assets/skaile-development/components/" + component_name

STEP 2: Read templates
  - Read component-template.js
  - Read component-test-harness.html
  - Read component-authoring.md (for checklist reference)

# -- Phase 2: Generate COMPONENT.md ----------------------------------------

STEP 3: Build COMPONENT.md frontmatter

  The frontmatter structure depends on provides_type:

  IF provides_type = "component":
    ```yaml
    ---
    name: {{name}}
    description: {{description}}
    version: 1.0.0
    keywords: [{{derive from description}}]

    provides:
      - type: component
        component: { kind: web-component, url: ./{{name}}.js, tagName: {{tagName}} }
        schema:
          type: object
          properties:
            {{for each prop: key: { type: propType }}}
        interactions: [{{interactions}}]
        targets: [{{targets}}]
        fallback: |
          {{generate a simple markdown template using prop names}}
    ---
    ```

  IF provides_type = "input-extension":
    ```yaml
    ---
    name: {{name}}
    description: {{description}}
    version: 1.0.0
    keywords: [{{derive from description}}]

    provides:
      - type: input-extension
        trigger: { type: prefix, value: "{{input_extension_prefix}}" }
        component: { kind: web-component, url: ./{{name}}.js, tagName: {{tagName}} }
        produces: { type: message, messageType: {{custom_message_type}} }

      - type: chat-renderer
        messageType: {{custom_message_type}}
        component: { kind: web-component, url: ./{{name}}-renderer.js, tagName: {{tagName}}-display }
        schema:
          type: object
          properties:
            {{for each prop in the selection output}}
        interactions: []
        targets: [chat]
        fallback: "{{simple markdown fallback}}"
    ---
    ```
    NOTE: Input extensions that produce a custom message type need TWO components:
    the picker (input-extension) and the renderer (chat-renderer). Generate both JS files.

  IF provides_type = "chat-renderer":
    ```yaml
    ---
    name: {{name}}
    description: {{description}}
    version: 1.0.0
    keywords: [{{derive from description}}]

    provides:
      - type: chat-renderer
        messageType: {{custom_message_type}}
        component: { kind: web-component, url: ./{{name}}.js, tagName: {{tagName}} }
        schema:
          type: object
          properties:
            {{for each prop: key: { type: propType }}}
        interactions: [{{interactions}}]
        targets: [{{targets}}]
        fallback: "{{simple markdown fallback}}"
    ---
    ```

  After the frontmatter, add a markdown body:
  ```markdown
  # {{PascalName formatted with spaces}}

  {{description}}.

  ## Usage

  {{Describe how the user interacts with this component.}}

  ## Interactions

  | Action | Trigger | Effect |
  |--------|---------|--------|
  {{for each interaction: action | user action | what happens}}
  ```

# -- Phase 3: Generate JS --------------------------------------------------

STEP 4: Generate the web component JS file

  Read component-template.js and substitute all {{placeholders}}:

  - Replace `{{name}}` with component_name
  - Replace `{{PascalName}}` with PascalCase name
  - Replace `{{description}}` with the description
  - Replace `{{prop1}}`, `{{prop1Type}}`, `{{prop1Description}}` with actual prop info

  Configure the component based on inputs:

  FOR EACH prop in props:
    IF prop.type = "array" or prop.type = "object":
      Add a property setter + getter pair:
      ```javascript
      set {{propName}}(val) { this._{{propName}} = ...; this._render(); }
      get {{propName}}() { return this._{{propName}}; }
      ```
    ELSE:
      Add to observedAttributes and attributeChangedCallback

  IF interactions is not empty:
    Keep the _emitInteraction helper
    Add interaction handlers in _bind()

  IF interactions is empty AND this is not an input-extension:
    Remove _emitInteraction (component only emits select)

  IF targets includes "preview":
    Ensure state_update is emitted (preview components persist state)

  Generate appropriate _render() body:
    - Empty state message
    - Data display section
    - Interactive controls (if interactions exist)

# -- Phase 4: Generate agent-usage.md ---------------------------------------

STEP 5: Generate the agent usage reference

  Follow the structure from components/issue-tracker/agent-usage.md:

  ```markdown
  # {{PascalName}} - Agent Usage Reference

  How to render and manage the {{name}} component from within an agent session.

  ## Prerequisites

  The component must be registered. Emit a `component_register` event:

  ```json
  {
    "type": "component_register",
    "components": {
      "{{name}}": {
        "schema": { {{full JSON schema from COMPONENT.md}} },
        "interactions": [{{interactions}}],
        "targets": [{{targets}}],
        "source": {
          "kind": "web-component",
          "url": "asset://skaile-development/components/{{name}}/{{name}}.js",
          "tagName": "{{tagName}}"
        },
        "fallback": "{{fallback template}}"
      }
    }
  }
  ```

  ## Rendering

  Emit a `ui_render` event:

  ```json
  {
    "type": "ui_render",
    "id": "<unique-render-id>",
    "target": "{{primary target}}",
    "persistState": {{true if preview target, false otherwise}},
    "component": {
      "kind": "catalog",
      "name": "{{name}}",
      "props": { {{example props with realistic sample data}} }
    }
  }
  ```

  ## Updating

  Emit `ui_render_update` to patch props:

  ```json
  {
    "type": "ui_render_update",
    "id": "<same-render-id>",
    "patch": { {{example partial update}} }
  }
  ```

  ## Handling User Interactions

  {{Only if interactions is not empty}}

  When the user interacts, you receive a structured prompt:

  ```
  [UI Interaction]
  render_id: <render-id>
  action: {{first interaction}}
  value: {{example value}}
  timestamp: 2026-01-01T00:00:00Z
  ```

  | Action | Value Shape | Agent Should |
  |--------|-------------|--------------|
  {{for each interaction: action | value shape | recommended agent response}}

  ## Clearing

  ```json
  {
    "type": "ui_clear",
    "render_id": "<same-render-id>"
  }
  ```

  ## State Persistence

  {{If persistState is true}}
  When `persistState: true`, component state survives session hibernation.
  The agent does not need to re-render after session resume.
  ```

# -- Phase 5: Generate test-harness.html -----------------------------------

STEP 6: Generate the test harness

  Read component-test-harness.html and substitute all {{placeholders}}:
  - Replace `{{name}}` with component_name

  Customize the `injectSample()` function with realistic sample data for this
  component's props. Example for a list component:

  ```javascript
  function injectSample() {
    el.items = [
      { id: "1", title: "Sample item", status: "open", priority: "high" },
      { id: "2", title: "Another item", status: "done", priority: "low" }
    ];
  }
  ```

  Customize `clearData()` to reset all props to empty/default values.

# -- Phase 6: Validate and Report ------------------------------------------

STEP 7: Run pre-flight checklist

  Check every item from the authoring guide's Pre-Flight Checklist against the
  generated files:

  - [ ] COMPONENT.md has valid YAML frontmatter with all required fields
  - [ ] name matches directory name and uses lowercase-hyphenated format
  - [ ] tagName starts with skaile- and contains a hyphen
  - [ ] Web component uses attachShadow({ mode: "open" })
  - [ ] Complex props use JS property setters (not attributes)
  - [ ] All events use bubbles: true, composed: true
  - [ ] Events use standard names: select, interaction, state_update
  - [ ] CSS uses theme tokens with fallback values
  - [ ] Custom element registration is guarded
  - [ ] agent-usage.md documents all props and interactions
  - [ ] fallback template produces readable markdown
  - [ ] JS file is self-contained

STEP 8: Report output

  > "Component scaffolded at `ai-assets/skaile-development/components/{{name}}/`:
  >
  > - `COMPONENT.md` - manifest with {{provides_type}} provides block
  > - `{{name}}.js` - web component ({{N}} props, {{M}} interactions)
  > - `agent-usage.md` - agent rendering reference
  > - `test-harness.html` - standalone browser test
  >
  > Next steps:
  > 1. Implement the rendering logic in `{{name}}.js`
  > 2. Open `test-harness.html` in a browser to verify
  > 3. Add realistic sample data to `injectSample()`
  > 4. Test at 280px and 800px widths
  > 5. Run the pre-flight checklist before submitting"

# -- Procedures ------------------------------------------------------------

PROCEDURE validate_name(name)
  - Must be lowercase
  - Must contain only letters, numbers, and hyphens
  - Must contain at least one hyphen (web component requirement via skaile- prefix)
  - Must not start or end with a hyphen
  - Must not conflict with existing component names in components/
  RETURN: valid | invalid (with reason)

PROCEDURE to_pascal_case(name)
  - Split on hyphens
  - Capitalize first letter of each segment
  - Join without separator
  - Example: "data-chart" -> "DataChart"
  RETURN: PascalCase string

PROCEDURE generate_fallback(props, provides_type)
  - For array props: use {{#each propName}} ... {{/each}}
  - For string props: use {{propName}} directly
  - For object props: use {{propName.field}}
  - Keep it simple - readable markdown, not a full replica
  RETURN: Handlebars template string

CHECKLIST
  - [ ] Component name validated (lowercase-hyphenated)
  - [ ] All four files generated in correct directory
  - [ ] COMPONENT.md frontmatter is valid YAML
  - [ ] JS file substitutions complete (no remaining {{placeholders}})
  - [ ] Test harness has component-specific sample data
  - [ ] agent-usage.md has realistic example props
  - [ ] Pre-flight checklist passed
