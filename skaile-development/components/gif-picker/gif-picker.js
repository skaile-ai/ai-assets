/**
 * skaile-gif-picker - GIF search and selection web component.
 *
 * A lightweight, self-contained web component that provides a GIF search grid.
 * Uses the Klipy API by default (free tier, no API key required).
 *
 * Properties:
 *   - query (string): Search query text
 *   - provider (string): API provider - "klipy" (default), "giphy", "tenor"
 *   - apiKey (string): Provider API key (optional for klipy)
 *
 * Events:
 *   - select: Fired when user clicks a GIF. Detail contains { url, alt, width, height, provider }
 */

const PROVIDERS = {
  klipy: {
    search: async (query, apiKey) => {
      // Klipy API v1: key goes in the URL path, uses per_page instead of limit
      const params = new URLSearchParams({ q: query, per_page: "20" });
      const keySegment = apiKey ? apiKey : "public";
      const res = await fetch(
        "https://api.klipy.com/api/v1/" + keySegment + "/gifs/search?" + params,
      );
      if (!res.ok) throw new Error("Klipy API error: " + res.status);
      const text = await res.text();
      if (!text) return [];
      const data = JSON.parse(text);
      return (data.data?.data || data.data || []).map((g) => ({
        id: g.id,
        url: g.file?.hd?.gif?.url || g.file?.md?.gif?.url || g.url,
        preview: g.file?.sm?.gif?.url || g.file?.xs?.gif?.url || g.url,
        alt: g.title || query,
        width: Number(g.file?.hd?.gif?.width || g.file?.md?.gif?.width || 480),
        height: Number(g.file?.hd?.gif?.height || g.file?.md?.gif?.height || 270),
      }));
    },
  },
  giphy: {
    search: async (query, apiKey) => {
      if (!apiKey) throw new Error("Giphy requires an API key");
      const params = new URLSearchParams({ q: query, limit: "20", api_key: apiKey });
      const res = await fetch("https://api.giphy.com/v1/gifs/search?" + params);
      if (!res.ok) throw new Error("Giphy API error: " + res.status);
      const data = await res.json();
      return (data.data || []).map((g) => ({
        id: g.id,
        url: g.images?.original?.url,
        preview: g.images?.fixed_width?.url || g.images?.preview_gif?.url,
        alt: g.title || query,
        width: Number(g.images?.original?.width || 480),
        height: Number(g.images?.original?.height || 270),
      }));
    },
  },
  tenor: {
    search: async (query, apiKey) => {
      if (!apiKey) throw new Error("Tenor requires an API key");
      const params = new URLSearchParams({ q: query, limit: "20", key: apiKey });
      const res = await fetch("https://tenor.googleapis.com/v2/search?" + params);
      if (!res.ok) throw new Error("Tenor API error: " + res.status);
      const data = await res.json();
      return (data.results || []).map((g) => ({
        id: g.id,
        url: g.media_formats?.gif?.url || g.url,
        preview: g.media_formats?.tinygif?.url || g.media_formats?.gif?.url,
        alt: g.content_description || query,
        width: g.media_formats?.gif?.dims?.[0] || 480,
        height: g.media_formats?.gif?.dims?.[1] || 270,
      }));
    },
  },
};

class SkaileGifPicker extends HTMLElement {
  static get observedAttributes() {
    return ["query", "provider", "api-key"];
  }

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
    this._query = "";
    this._provider = "klipy";
    this._apiKey = "BMysgPJm0DEODCvgX3cMuNHkJ1uOvoN34toKNC1VTnPaXrBkVAsV97Wmhz7Eqg7x";
    this._results = [];
    this._loading = false;
    this._error = null;
    this._debounceTimer = null;
    this._focusIndex = -1;
  }

  connectedCallback() {
    this._render();
    if (this._query) {
      this._search();
    }
    // Keyboard navigation: arrow keys to move, enter/space to select
    this.shadowRoot.addEventListener("keydown", (e) => {
      const items = this.shadowRoot.querySelectorAll(".gif-item");
      if (items.length === 0) return;

      const cols = Math.floor(
        this.shadowRoot.querySelector(".grid")?.clientWidth / 124,
      ) || 1;

      if (e.key === "ArrowRight") {
        e.preventDefault();
        this._moveFocus(Math.min(this._focusIndex + 1, items.length - 1));
      } else if (e.key === "ArrowLeft") {
        e.preventDefault();
        this._moveFocus(Math.max(this._focusIndex - 1, 0));
      } else if (e.key === "ArrowDown") {
        e.preventDefault();
        this._moveFocus(
          Math.min(this._focusIndex + cols, items.length - 1),
        );
      } else if (e.key === "ArrowUp") {
        e.preventDefault();
        this._moveFocus(Math.max(this._focusIndex - cols, 0));
      } else if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        if (this._focusIndex >= 0 && this._focusIndex < items.length) {
          items[this._focusIndex].click();
        }
      }
    });
  }

  disconnectedCallback() {
    if (this._debounceTimer) clearTimeout(this._debounceTimer);
  }

  _moveFocus(index) {
    this._focusIndex = index;
    const items = this.shadowRoot.querySelectorAll(".gif-item");
    if (items[index]) {
      items[index].focus();
    }
  }

  attributeChangedCallback(name, _old, value) {
    if (name === "query") {
      this._query = value || "";
      this._debounceSearch();
    } else if (name === "provider") {
      this._provider = value || "klipy";
    } else if (name === "api-key") {
      this._apiKey = value || "";
    }
  }

  // React 19 sets properties directly
  set query(val) {
    this._query = val || "";
    this._debounceSearch();
  }
  get query() { return this._query; }

  set provider(val) { this._provider = val || "klipy"; }
  get provider() { return this._provider; }

  set apiKey(val) { this._apiKey = val || ""; }
  get apiKey() { return this._apiKey; }

  _debounceSearch() {
    if (this._debounceTimer) clearTimeout(this._debounceTimer);
    this._debounceTimer = setTimeout(() => this._search(), 300);
  }

  async _search() {
    const query = this._query.trim();
    if (!query) {
      this._results = [];
      this._render();
      return;
    }

    this._loading = true;
    this._error = null;
    this._render();

    try {
      const provider = PROVIDERS[this._provider] || PROVIDERS.klipy;
      this._results = await provider.search(query, this._apiKey);
    } catch (err) {
      this._error = err.message || "Search failed";
      this._results = [];
    } finally {
      this._loading = false;
      this._render();
    }
  }

  _handleSelect(gif) {
    this.dispatchEvent(
      new CustomEvent("select", {
        bubbles: true,
        composed: true,
        detail: {
          url: gif.url,
          alt: gif.alt,
          width: gif.width,
          height: gif.height,
          provider: this._provider,
        },
      }),
    );
  }

  _render() {
    if (!this.shadowRoot) return;

    let content = "";

    if (this._loading) {
      content = '<div class="status">Searching...</div>';
    } else if (this._error) {
      content = '<div class="status error">' + this._escapeHtml(this._error) + "</div>";
    } else if (this._results.length === 0 && this._query) {
      content = '<div class="status">No GIFs found</div>';
    } else if (this._results.length === 0) {
      content = '<div class="status">Type to search for GIFs</div>';
    } else {
      content = '<div class="grid">';
      for (const gif of this._results) {
        content +=
          '<button class="gif-item" tabindex="0" data-id="' +
          this._escapeHtml(gif.id) +
          '" title="' +
          this._escapeHtml(gif.alt) +
          '">' +
          '<img src="' +
          this._escapeHtml(gif.preview || gif.url) +
          '" alt="' +
          this._escapeHtml(gif.alt) +
          '" loading="lazy" />' +
          "</button>";
      }
      content += "</div>";
    }

    this.shadowRoot.innerHTML =
      "<style>" + SkaileGifPicker._styles + "</style>" + content;

    // Attach click handlers
    for (const btn of this.shadowRoot.querySelectorAll(".gif-item")) {
      const id = btn.getAttribute("data-id");
      const gif = this._results.find((g) => String(g.id) === id);
      if (gif) {
        btn.addEventListener("click", () => this._handleSelect(gif));
      }
    }

    // Auto-focus first result for immediate keyboard navigation
    if (this._results.length > 0) {
      this._focusIndex = 0;
      const first = this.shadowRoot.querySelector(".gif-item");
      if (first) first.focus();
    }
  }

  _escapeHtml(str) {
    return String(str)
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
      background: var(--surface, #fff);
      border: 1px solid var(--border, #e5e5e5);
      border-radius: 8px;
      overflow: hidden;
      max-height: 320px;
    }
    .status {
      padding: 16px;
      text-align: center;
      color: var(--muted-foreground, #666);
      font-size: 14px;
    }
    .status.error {
      color: var(--destructive, #ef4444);
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
      gap: 4px;
      padding: 4px;
      overflow-y: auto;
      max-height: 316px;
    }
    .gif-item {
      all: unset;
      cursor: pointer;
      border-radius: 4px;
      overflow: hidden;
      aspect-ratio: 16/9;
      background: var(--muted, #f5f5f5);
      transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .gif-item:hover {
      transform: scale(1.03);
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
    }
    .gif-item:focus-visible {
      outline: 2px solid var(--primary, #2563eb);
      outline-offset: 2px;
    }
    .gif-item img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      display: block;
    }
  `;
}

if (!customElements.get("skaile-gif-picker")) {
  customElements.define("skaile-gif-picker", SkaileGifPicker);
}

export { SkaileGifPicker };
