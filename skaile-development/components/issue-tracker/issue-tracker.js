/**
 * skaile-issue-tracker - Interactive issue board web component.
 *
 * Properties:
 *   - issues (Array): Issue objects with {id, title, status, priority, assignee, createdBy, createdAt, sourceMessageId, notes}
 *   - sessionId (string): Current session ID
 *   - filter (string): "all" | "open" | "mine" | "agent"
 *   - sortBy (string): "priority" | "created" | "status"
 *
 * Events:
 *   - interaction: {action, value} on create/update/export
 *   - select: {id, sourceMessageId} when user clicks issue title
 *   - state_update: {store, state} when filter/sort changes
 */

const P_ORD = { high: 0, medium: 1, low: 2 };
const S_ORD = { open: 0, "in-progress": 1, done: 2 };
const S_NEXT = { open: "in-progress", "in-progress": "done", done: "open" };

class SkaileIssueTracker extends HTMLElement {
  static get observedAttributes() {
    return ["filter", "sort-by", "session-id"];
  }

  constructor() {
    super();
    this.attachShadow({ mode: "open" });
    this._issues = [];
    this._sessionId = "";
    this._filter = "all";
    this._sortBy = "priority";
    this._showForm = false;
  }

  connectedCallback() { this._render(); }

  attributeChangedCallback(name, _old, val) {
    if (name === "filter") { this._filter = val || "all"; this._render(); }
    else if (name === "sort-by") { this._sortBy = val || "priority"; this._render(); }
    else if (name === "session-id") { this._sessionId = val || ""; }
  }

  // React 19 direct property setters
  set issues(v) { this._issues = Array.isArray(v) ? v : []; this._render(); }
  get issues() { return this._issues; }
  set sessionId(v) { this._sessionId = v || ""; }
  get sessionId() { return this._sessionId; }
  set filter(v) { this._filter = v || "all"; this._render(); }
  get filter() { return this._filter; }
  set sortBy(v) { this._sortBy = v || "priority"; this._render(); }
  get sortBy() { return this._sortBy; }

  _genId() {
    return (typeof crypto !== "undefined" && crypto.randomUUID)
      ? crypto.randomUUID()
      : "issue-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8);
  }

  _storeKey() { return "component:" + (this.id || "issue-tracker"); }

  _esc(s) {
    return String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;")
      .replace(/>/g, "&gt;").replace(/"/g, "&quot;");
  }

  _trunc(s, n) { return s ? (s.length > n ? s.slice(0, n) + "..." : s) : ""; }

  _fmtDate(d) {
    if (!d) return "";
    try { return new Date(d).toLocaleDateString(undefined, { month: "short", day: "numeric" }); }
    catch { return ""; }
  }

  _items() {
    let a = this._issues.slice();
    const f = this._filter;
    if (f === "open") a = a.filter(i => i.status !== "done");
    else if (f === "mine") a = a.filter(i => i.assignee && i.assignee !== "agent");
    else if (f === "agent") a = a.filter(i => i.assignee === "agent");

    const s = this._sortBy;
    if (s === "priority") a.sort((x, y) => (P_ORD[x.priority] ?? 3) - (P_ORD[y.priority] ?? 3));
    else if (s === "created") a.sort((x, y) => {
      const dx = x.createdAt ? new Date(x.createdAt).getTime() : 0;
      const dy = y.createdAt ? new Date(y.createdAt).getTime() : 0;
      return dy - dx;
    });
    else if (s === "status") a.sort((x, y) => (S_ORD[x.status] ?? 3) - (S_ORD[y.status] ?? 3));
    return a;
  }

  _emit(ev, detail) {
    this.dispatchEvent(new CustomEvent(ev, { bubbles: true, composed: true, detail }));
  }

  _render() {
    if (!this.shadowRoot) return;
    const items = this._items();
    const total = this._issues.length;
    const e = (s) => this._esc(s);
    const sel = (k, v) => this["_" + k] === v ? " selected" : "";
    let h = "";

    // Header
    h += '<div class="hd"><div class="hd-l"><span class="tt">Issues</span><span class="ct">' + total + '</span></div><div class="hd-r">';
    h += '<select class="sl" data-k="filter">';
    for (const [v, l] of [["all","All"],["open","Open"],["mine","Mine"],["agent","Agent"]])
      h += "<option value=\"" + v + "\"" + sel("filter", v) + ">" + l + "</option>";
    h += '</select><select class="sl" data-k="sortBy">';
    for (const [v, l] of [["priority","Priority"],["created","Created"],["status","Status"]])
      h += "<option value=\"" + v + "\"" + sel("sortBy", v) + ">" + l + "</option>";
    h += '</select><button class="ib ab" data-a="tf" title="Add issue">+</button>';
    h += '<button class="ib" data-a="ex" title="Export"><svg width="14" height="14" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M8 2v8M5 7l3 3 3-3M3 12v1.5h10V12"/></svg></button>';
    h += "</div></div>";

    // Add form
    if (this._showForm) {
      h += '<form class="af"><input class="fi" name="title" type="text" placeholder="Issue title..." autocomplete="off" />';
      h += '<div class="fr"><div class="pg">';
      h += '<label class="po"><input type="radio" name="priority" value="high" /><span class="pb ph">H</span></label>';
      h += '<label class="po"><input type="radio" name="priority" value="medium" checked /><span class="pb pm">M</span></label>';
      h += '<label class="po"><input type="radio" name="priority" value="low" /><span class="pb pl">L</span></label>';
      h += '</div><select class="sl" name="assignee"><option value="me">Me</option><option value="agent">Agent</option></select></div>';
      h += '<div class="fa"><button type="submit" class="bn bp">Add</button><button type="button" class="bn bc" data-a="cf">Cancel</button></div></form>';
    }

    // List
    if (items.length === 0) {
      h += '<div class="em">' + (total === 0 ? "No issues yet — click + to add one" : "No issues match this filter") + "</div>";
    } else {
      h += '<div class="il">';
      for (const i of items) {
        const sc = "sd " + (i.status === "in-progress" ? "sip" : i.status === "done" ? "sdn" : "sop");
        const pc = i.priority === "high" ? "ph" : i.priority === "low" ? "pl" : "pm";
        h += '<div class="ic" data-id="' + e(i.id) + '">';
        h += '<button class="' + sc + '" data-a="cs" data-id="' + e(i.id) + '" title="' + e(i.status) + '"></button>';
        h += '<div class="ib2"><button class="it" data-a="sl" data-id="' + e(i.id) + '">' + e(i.title) + "</button>";
        if (i.notes) h += '<div class="in">' + e(this._trunc(i.notes, 60)) + "</div>";
        if (i.createdAt || i.createdBy) {
          h += '<div class="im">';
          if (i.createdBy) h += e(i.createdBy);
          if (i.createdAt) h += (i.createdBy ? " · " : "") + this._fmtDate(i.createdAt);
          h += "</div>";
        }
        h += '</div><div class="ir"><span class="pb ' + pc + '">' + e((i.priority || "medium")[0].toUpperCase()) + "</span>";
        if (i.assignee) h += '<span class="ac">' + e(i.assignee) + "</span>";
        h += "</div></div>";
      }
      h += "</div>";
    }

    this.shadowRoot.innerHTML = "<style>" + SkaileIssueTracker._styles + "</style>" + h;
    this._bind();
  }

  _bind() {
    const r = this.shadowRoot;

    // Filter & sort selects
    for (const sl of r.querySelectorAll("select[data-k]")) {
      sl.addEventListener("change", (ev) => {
        const k = sl.getAttribute("data-k");
        this["_" + k] = ev.target.value;
        this._emit("state_update", { store: this._storeKey(), state: { filter: this._filter, sortBy: this._sortBy } });
        this._render();
      });
    }

    // Buttons by data-a
    for (const btn of r.querySelectorAll("[data-a]")) {
      const a = btn.getAttribute("data-a");
      if (a === "tf") btn.addEventListener("click", () => { this._showForm = !this._showForm; this._render(); });
      else if (a === "ex") btn.addEventListener("click", () => this._emit("interaction", { action: "export", value: { format: "markdown" } }));
      else if (a === "cf") btn.addEventListener("click", () => { this._showForm = false; this._render(); });
      else if (a === "cs") {
        const issue = this._issues.find(i => i.id === btn.getAttribute("data-id"));
        if (issue) btn.addEventListener("click", (ev) => {
          ev.stopPropagation();
          this._emit("interaction", { action: "update", value: { id: issue.id, field: "status", value: S_NEXT[issue.status] || "open" } });
        });
      } else if (a === "sl") {
        const issue = this._issues.find(i => i.id === btn.getAttribute("data-id"));
        if (issue) btn.addEventListener("click", () => this._emit("select", { id: issue.id, sourceMessageId: issue.sourceMessageId }));
      }
    }

    // Form submit
    const form = r.querySelector(".af");
    if (form) {
      form.addEventListener("submit", (ev) => {
        ev.preventDefault();
        const title = form.title.value.trim();
        if (!title) return;
        const priority = form.querySelector('input[name="priority"]:checked')?.value || "medium";
        const assignee = form.assignee.value || "me";
        this._emit("interaction", { action: "create", value: { id: this._genId(), title, priority, assignee } });
        this._showForm = false;
        this._render();
      });
      const ti = form.querySelector('input[name="title"]');
      if (ti) ti.focus();
    }
  }

  static _styles = `
:host{display:block;font-family:var(--font-sans,system-ui,-apple-system,sans-serif);color:var(--foreground,#1a1a1a);background:var(--background,#fff);border:1px solid var(--border,#e5e5e5);border-radius:var(--radius,8px);overflow:hidden;font-size:13px;line-height:1.4}
.hd{display:flex;align-items:center;justify-content:space-between;padding:8px 10px;border-bottom:1px solid var(--border,#e5e5e5);gap:6px;flex-wrap:wrap}
.hd-l{display:flex;align-items:center;gap:6px}
.hd-r{display:flex;align-items:center;gap:4px}
.tt{font-weight:600;font-size:14px}
.ct{display:inline-flex;align-items:center;justify-content:center;min-width:18px;height:18px;padding:0 5px;font-size:11px;font-weight:600;background:var(--accent,#f0f0f0);color:var(--foreground,#1a1a1a);border-radius:9px}
.sl{appearance:none;background:var(--background,#fff);border:1px solid var(--border,#e5e5e5);border-radius:var(--radius,8px);padding:3px 6px;font-size:11px;color:var(--foreground,#1a1a1a);cursor:pointer;outline:none}
.sl:focus{border-color:var(--primary,#2563eb)}
.ib{all:unset;display:inline-flex;align-items:center;justify-content:center;width:24px;height:24px;border-radius:var(--radius,8px);cursor:pointer;color:var(--muted-foreground,#666);transition:background .15s,color .15s}
.ib:hover{background:var(--accent,#f0f0f0);color:var(--foreground,#1a1a1a)}
.ib:focus-visible{outline:2px solid var(--primary,#2563eb);outline-offset:1px}
.ab{font-size:18px;font-weight:300;line-height:1}
.af{padding:10px;border-bottom:1px solid var(--border,#e5e5e5);display:flex;flex-direction:column;gap:8px;animation:sd .15s ease-out}
@keyframes sd{from{opacity:0;transform:translateY(-6px)}to{opacity:1;transform:translateY(0)}}
.fi{width:100%;padding:6px 8px;border:1px solid var(--border,#e5e5e5);border-radius:var(--radius,8px);font-size:13px;color:var(--foreground,#1a1a1a);background:var(--background,#fff);outline:none;box-sizing:border-box}
.fi:focus{border-color:var(--primary,#2563eb)}
.fi::placeholder{color:var(--muted-foreground,#999)}
.fr{display:flex;align-items:center;gap:8px}
.pg{display:flex;gap:4px}
.po{cursor:pointer}
.po input{display:none}
.po input:checked+.pb{outline:2px solid var(--primary,#2563eb);outline-offset:1px}
.fa{display:flex;gap:6px}
.bn{all:unset;padding:4px 12px;border-radius:var(--radius,8px);font-size:12px;font-weight:500;cursor:pointer;text-align:center;transition:background .15s}
.bp{background:var(--primary,#2563eb);color:#fff}
.bp:hover{opacity:.9}
.bc{background:var(--accent,#f0f0f0);color:var(--foreground,#1a1a1a)}
.bc:hover{background:var(--border,#e5e5e5)}
.bn:focus-visible{outline:2px solid var(--primary,#2563eb);outline-offset:1px}
.em{padding:32px 16px;text-align:center;color:var(--muted-foreground,#666);font-size:13px}
.il{overflow-y:auto;max-height:480px}
.ic{display:flex;align-items:flex-start;gap:8px;padding:8px 10px;border-bottom:1px solid var(--border,#e5e5e5);transition:background .12s}
.ic:last-child{border-bottom:none}
.ic:hover{background:var(--accent,#f8f8f8)}
.sd{all:unset;flex-shrink:0;width:10px;height:10px;border-radius:50%;margin-top:4px;cursor:pointer;transition:transform .15s,box-shadow .15s;padding:4px;background-clip:content-box}
.sd:hover{transform:scale(1.3);box-shadow:0 0 0 2px var(--border,#e5e5e5)}
.sd:focus-visible{outline:2px solid var(--primary,#2563eb);outline-offset:2px}
.sop{background-color:#3b82f6}
.sip{background-color:#f59e0b}
.sdn{background-color:#22c55e}
.ib2{flex:1;min-width:0}
.it{all:unset;display:block;font-weight:600;font-size:13px;cursor:pointer;color:var(--foreground,#1a1a1a);word-break:break-word}
.it:hover{color:var(--primary,#2563eb);text-decoration:underline}
.it:focus-visible{outline:2px solid var(--primary,#2563eb);outline-offset:1px;border-radius:2px}
.in{font-size:12px;color:var(--muted-foreground,#666);margin-top:2px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.im{font-size:11px;color:var(--muted-foreground,#999);margin-top:2px}
.ir{display:flex;flex-direction:column;align-items:flex-end;gap:4px;flex-shrink:0}
.pb{display:inline-flex;align-items:center;justify-content:center;width:18px;height:18px;border-radius:4px;font-size:10px;font-weight:700;line-height:1;color:#fff}
.ph{background:var(--destructive,#ef4444)}
.pm{background:#f59e0b}
.pl{background:#9ca3af}
.ac{font-size:10px;padding:1px 6px;border-radius:8px;background:var(--accent,#f0f0f0);color:var(--muted-foreground,#666);white-space:nowrap;max-width:60px;overflow:hidden;text-overflow:ellipsis}
`;
}

if (!customElements.get("skaile-issue-tracker")) {
  customElements.define("skaile-issue-tracker", SkaileIssueTracker);
}

export { SkaileIssueTracker };
