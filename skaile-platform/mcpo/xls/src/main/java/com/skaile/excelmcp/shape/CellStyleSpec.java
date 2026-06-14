package com.skaile.excelmcp.shape;

/**
 * POI-free description of the cell formatting to apply over a range (the {@code range.set_style}
 * tool). Every field is independently optional: a {@code null} field means "leave that attribute as
 * it already is on each cell". This merge-not-replace semantics lets callers layer styling (e.g.
 * set a number format in one call, a fill in another) without clobbering earlier work.
 *
 * <p>Colors are {@code "#RRGGBB"} (the leading {@code #} is optional). Engine-side validation
 * rejects malformed colors, border styles, and alignment names with {@code STYLE_INVALID} before
 * any cell is touched.
 */
public record CellStyleSpec(
    String fillColor,
    FontSpec font,
    BorderSpec border,
    String numberFormat,
    String horizontalAlignment,
    String verticalAlignment,
    Boolean wrapText) {

  /** Font attribute overrides. Null fields inherit the cell's existing font value. */
  public record FontSpec(String name, Integer size, Boolean bold, Boolean italic, String color) {
    public boolean isEmpty() {
      return name == null && size == null && bold == null && italic == null && color == null;
    }
  }

  /** Per-edge border overrides. A null edge leaves that edge unchanged. */
  public record BorderSpec(BorderEdge top, BorderEdge bottom, BorderEdge left, BorderEdge right) {
    public boolean isEmpty() {
      return top == null && bottom == null && left == null && right == null;
    }
  }

  /**
   * One border edge. {@code style} is an Excel border style name (thin, medium, thick, dashed,
   * dotted, double, hair, none, ...); {@code color} is an optional {@code "#RRGGBB"} (defaults to
   * automatic/black when omitted).
   */
  public record BorderEdge(String style, String color) {}

  public boolean isEmpty() {
    return fillColor == null
        && (font == null || font.isEmpty())
        && (border == null || border.isEmpty())
        && numberFormat == null
        && horizontalAlignment == null
        && verticalAlignment == null
        && wrapText == null;
  }
}
