/**
 * skaile-gif-display - GIF chat renderer web component.
 *
 * Displays a GIF inline in the chat stream with proper aspect ratio,
 * accessibility, and max-width constraints.
 *
 * Properties:
 *   - url (string): GIF URL
 *   - alt (string): Alt text for accessibility
 *   - width (number): Original width in pixels
 *   - height (number): Original height in pixels
 */

class SkaileGifDisplay extends HTMLElement {
  static get observedAttributes() {
    return ["url", "alt", "width", "height"];
  }

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
    this._url = "";
    this._alt = "";
    this._width = 480;
    this._height = 270;
  }

  connectedCallback() {
    this._render();
  }

  attributeChangedCallback(name, _old, value) {
    if (name === "url") this._url = value || "";
    else if (name === "alt") this._alt = value || "";
    else if (name === "width") this._width = Number(value) || 480;
    else if (name === "height") this._height = Number(value) || 270;
    this._render();
  }

  // React 19 sets properties directly
  set url(val) { this._url = val || ""; this._render(); }
  get url() { return this._url; }

  set alt(val) { this._alt = val || ""; this._render(); }
  get alt() { return this._alt; }

  set width(val) { this._width = Number(val) || 480; this._render(); }
  get width() { return this._width; }

  set height(val) { this._height = Number(val) || 270; this._render(); }
  get height() { return this._height; }

  _escapeHtml(str) {
    return String(str)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  _render() {
    if (!this.shadowRoot) return;
    if (!this._url) {
      this.shadowRoot.innerHTML = "";
      return;
    }

    const ratio = this._height / this._width;
    const paddingBottom = (ratio * 100).toFixed(2);

    this.shadowRoot.innerHTML =
      "<style>" +
      `:host {
        display: block;
        max-width: 400px;
      }
      .gif-container {
        position: relative;
        width: 100%;
        padding-bottom: ${paddingBottom}%;
        border-radius: 8px;
        overflow: hidden;
        background: var(--muted, #f5f5f5);
        border: 1px solid var(--border, #e5e5e5);
      }
      img {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        display: block;
      }
      .alt-text {
        margin-top: 4px;
        font-size: 12px;
        color: var(--muted-foreground, #666);
        font-family: var(--font-sans, system-ui, sans-serif);
      }` +
      "</style>" +
      '<div class="gif-container">' +
      '<img src="' +
      this._escapeHtml(this._url) +
      '" alt="' +
      this._escapeHtml(this._alt) +
      '" role="img" loading="lazy" />' +
      "</div>" +
      (this._alt
        ? '<div class="alt-text">' + this._escapeHtml(this._alt) + "</div>"
        : "");
  }
}

if (!customElements.get("skaile-gif-display")) {
  customElements.define("skaile-gif-display", SkaileGifDisplay);
}

export { SkaileGifDisplay };
