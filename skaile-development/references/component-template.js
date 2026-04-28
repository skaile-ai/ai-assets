/**
 * skaile-{{name}} - {{description}}
 *
 * Properties:
 *   - {{prop1}} ({{prop1Type}}): {{prop1Description}}
 *
 * Events:
 *   - select: Fired when user selects an item. Detail: { ... }
 *   - interaction: Fired on user actions. Detail: { action: string, value: unknown }
 *   - state_update: Fired when UI state changes. Detail: { store: string, state: object }
 */

class Skaile{{PascalName}} extends HTMLElement {
  static get observedAttributes() {
    return [/* "filter", "sort-by" - simple string/number attributes */];
  }

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
    // Internal state - initialize all properties
    this._items = [];
  }

  // -- Property setters (React 19 sets props this way) --
  // Use for complex types (arrays, objects):
  set items(val) {
    this._items = Array.isArray(val) ? val : [];
    this._render();
  }
  get items() {
    return this._items;
  }

  // -- Lifecycle --
  connectedCallback() {
    this._render();
  }

  disconnectedCallback() {
    // Cleanup: clear timers, abort controllers, remove listeners
  }

  attributeChangedCallback(name, _old, val) {
    // Handle simple attribute changes
    // if (name === "filter") { this._filter = val || "all"; this._render(); }
  }

  // -- Event helpers --
  _emit(name, detail) {
    this.dispatchEvent(
      new CustomEvent(name, {
        detail,
        bubbles: true,
        composed: true, // crosses shadow DOM boundary
      }),
    );
  }

  _emitInteraction(action, value) {
    this._emit("interaction", { action, value });
  }

  _emitStateUpdate() {
    this._emit("state_update", {
      store: "component:" + (this.id || "{{name}}"),
      state: {
        /* UI state to persist: filter, sortBy, etc. */
      },
    });
  }

  // -- Rendering --
  _render() {
    if (!this.shadowRoot) return;

    let h = "";
    // Build component HTML
    h += '<div class="root">';
    h += "  <!-- Component content -->";
    h += "</div>";

    this.shadowRoot.innerHTML =
      "<style>" + Skaile{{PascalName}}._styles + "</style>" + h;
    this._bind();
  }

  _bind() {
    // Attach event listeners after render
    // const root = this.shadowRoot;
    // for (const btn of root.querySelectorAll("[data-action]")) {
    //   btn.addEventListener("click", () => { ... });
    // }
  }

  // -- Utilities --
  _esc(s) {
    return String(s || "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  static _styles = `
:host {
  display: block;
  font-family: var(--font-sans, system-ui, sans-serif);
  color: var(--foreground, #1a1a1a);
  background: var(--background, #fff);
  border: 1px solid var(--border, #e5e5e5);
  border-radius: var(--radius, 8px);
  overflow: hidden;
  font-size: 13px;
  line-height: 1.4;
}
.root {
  padding: 8px 10px;
}
/* Primary button */
.btn-primary {
  all: unset;
  padding: 4px 12px;
  border-radius: var(--radius, 8px);
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  background: var(--primary, #2563eb);
  color: #fff;
}
.btn-primary:hover { opacity: 0.9; }
.btn-primary:focus-visible {
  outline: 2px solid var(--primary, #2563eb);
  outline-offset: 1px;
}
/* Muted text */
.muted {
  color: var(--muted-foreground, #666);
  font-size: 12px;
}
/* Empty state */
.empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--muted-foreground, #666);
}
`;
}

if (!customElements.get("skaile-{{name}}")) {
  customElements.define("skaile-{{name}}", Skaile{{PascalName}});
}

export { Skaile{{PascalName}} };
