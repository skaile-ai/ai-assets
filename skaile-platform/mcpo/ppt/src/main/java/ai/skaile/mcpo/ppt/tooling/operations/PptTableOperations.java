package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.ColorParser;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextRun;

/**
 * Table tool handlers: {@code ppt.add_table}, {@code ppt.get_table}, {@code ppt.edit_table}.
 * Internally dispatches the multi-operation {@code edit_table} surface through to the relevant
 * cell / structure / style mutation path. The merge and per-cell border operations land on
 * the underlying {@link org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell} XML
 * directly because POI doesn't expose all of these knobs at the {@link XSLFTableCell} level.
 */
public final class PptTableOperations {

    private final ObjectMapper mapper;
    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;

    public PptTableOperations(
            ObjectMapper mapper,
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptShapeFinder shapeFinder,
            PptLimits limits) {
        this.mapper = mapper;
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.shapeFinder = shapeFinder;
        this.limits = limits;
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of(
                "ppt.add_table", this::addTable,
                "ppt.get_table", this::getTable,
                "ppt.edit_table", this::editTable);
    }

    ToolCallResult addTable(JsonNode args) {
        ToolCallResult shapeLimit = limits.enforceShapeLimit(args);
        if (shapeLimit != null) {
            return shapeLimit;
        }

        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        int rows = args.path("rows").asInt(0);
        int cols = args.path("cols").asInt(0);
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (rows < 1 || cols < 1) {
            return error("rows and cols must be >= 1");
        }
        if (!argumentValidator.isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        XSLFTable table = slide.createTable();
        table.setAnchor(new Rectangle2D.Double(x, y, width, height));
        double rowHeight = height / rows;
        double colWidth = width / cols;
        for (int r = 0; r < rows; r++) {
            XSLFTableRow row = table.addRow();
            row.setHeight(rowHeight);
            for (int c = 0; c < cols; c++) {
                XSLFTableCell cell = row.addCell();
                cell.setText("");
                table.setColumnWidth(c, colWidth);
            }
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("rows", rows);
        payload.put("cols", cols);
        payload.put("message", "Table added");
        return success(payload);
    }

    ToolCallResult getTable(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }

        int rows = table.getNumberOfRows();
        int cols = table.getNumberOfColumns();

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("rows", rows);
        payload.put("cols", cols);

        ArrayNode cellRows = payload.putArray("cells");
        ArrayNode mergedRegions = mapper.createArrayNode();
        for (int r = 0; r < rows; r++) {
            ArrayNode row = cellRows.addArray();
            XSLFTableRow tableRow = table.getRows().get(r);
            for (int c = 0; c < cols && c < tableRow.getCells().size(); c++) {
                XSLFTableCell cell = tableRow.getCells().get(c);
                org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell ct =
                        (org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell) cell.getXmlObject();
                int colSpan = ct.isSetGridSpan() ? (int) ct.getGridSpan() : 1;
                int rowSpan = ct.isSetRowSpan() ? (int) ct.getRowSpan() : 1;
                boolean hMerged = ct.isSetHMerge() && ct.getHMerge();
                boolean vMerged = ct.isSetVMerge() && ct.getVMerge();
                // An anchor carries gridSpan/rowSpan > 1; continuation cells carry hMerge
                // and/or vMerge. A cell with neither flag nor span is a standalone cell.
                boolean isAnchor = (colSpan > 1 || rowSpan > 1) && !hMerged && !vMerged;
                ObjectNode cellNode = row.addObject();
                cellNode.put("text", cell.getText() == null ? "" : cell.getText());
                cellNode.put("row_span", rowSpan);
                cellNode.put("col_span", colSpan);
                cellNode.put("is_merge_anchor", isAnchor);
                if (hMerged) {
                    cellNode.put("h_merge", true);
                }
                if (vMerged) {
                    cellNode.put("v_merge", true);
                }
                if (isAnchor) {
                    ObjectNode region = mergedRegions.addObject();
                    region.put("start_row", r);
                    region.put("start_col", c);
                    region.put("end_row", r + rowSpan - 1);
                    region.put("end_col", c + colSpan - 1);
                }
            }
        }

        ArrayNode rowHeights = payload.putArray("row_heights");
        for (int r = 0; r < rows; r++) {
            rowHeights.add(table.getRows().get(r).getHeight());
        }
        ArrayNode colWidths = payload.putArray("col_widths");
        for (int c = 0; c < cols; c++) {
            colWidths.add(table.getColumnWidth(c));
        }
        payload.set("merged_regions", mergedRegions);
        return success(payload);
    }

    ToolCallResult editTable(JsonNode args) {
        String operation = args.path("operation").asText("").toLowerCase(Locale.ROOT);
        if (operation.isBlank()) {
            return responseFactory.error("VALIDATION_ERROR", "operation is required", false);
        }
        switch (operation) {
            case "set_cell":
                return editTableSetCell(args);
            case "insert_row":
            case "delete_row":
                return editTableRowStructure(args, operation);
            case "insert_col":
            case "delete_col":
                return editTableColStructure(args, operation);
            case "set_row_height":
                return setTableRowHeight(args);
            case "set_col_width":
                return setTableColumnWidth(args);
            case "set_header_style":
                return setTableHeaderStyle(args);
            case "merge_cells":
                return mergeCells(args);
            case "set_cell_border":
                return setCellBorder(args);
            default:
                return responseFactory.error("VALIDATION_ERROR",
                        "operation must be one of: set_cell, insert_row, delete_row, insert_col, delete_col, set_row_height, set_col_width, set_header_style, merge_cells, set_cell_border",
                        false);
        }
    }

    private ToolCallResult editTableSetCell(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row").asInt(-1);
        int colIndex = args.path("col").asInt(-1);
        String text = requiredString(args, "text");

        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }
        XSLFTableCell cell = getTableCell(table, rowIndex, colIndex);
        if (cell == null) {
            return error("Invalid table cell coordinates");
        }

        cell.setText(text);
        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row_index", rowIndex);
        payload.put("col_index", colIndex);
        payload.put("text", text);
        payload.put("message", "Table cell updated");
        return success(payload);
    }

    private ToolCallResult editTableRowStructure(JsonNode args, String operation) {
        return modifyTableStructure(args, operation);
    }

    private ToolCallResult editTableColStructure(JsonNode args, String operation) {
        String mapped = "insert_col".equals(operation) ? "insert_column" : "delete_column";
        return modifyTableStructure(args, mapped);
    }

    private ToolCallResult modifyTableStructure(JsonNode args, String operation) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }

        int oldRows = table.getNumberOfRows();
        int oldCols = table.getNumberOfColumns();
        int index = args.has("index") ? args.path("index").asInt(-1) : -1;

        String[][] cells = extractTableText(table);
        String[][] updated;

        switch (operation) {
            case "insert_row":
                if (index < 0) {
                    index = oldRows;
                }
                if (index > oldRows) {
                    return error("index out of range for insert_row");
                }
                updated = new String[oldRows + 1][oldCols];
                for (int r = 0, src = 0; r < updated.length; r++) {
                    if (r == index) {
                        for (int c = 0; c < oldCols; c++) {
                            updated[r][c] = "";
                        }
                    } else {
                        updated[r] = cells[src++].clone();
                    }
                }
                break;
            case "delete_row":
                if (oldRows <= 1) {
                    return error("Cannot delete the last table row");
                }
                if (index < 0 || index >= oldRows) {
                    return error("index out of range for delete_row");
                }
                updated = new String[oldRows - 1][oldCols];
                for (int r = 0, dst = 0; r < oldRows; r++) {
                    if (r == index) {
                        continue;
                    }
                    updated[dst++] = cells[r].clone();
                }
                break;
            case "insert_column":
                if (index < 0) {
                    index = oldCols;
                }
                if (index > oldCols) {
                    return error("index out of range for insert_column");
                }
                updated = new String[oldRows][oldCols + 1];
                for (int r = 0; r < oldRows; r++) {
                    for (int c = 0, src = 0; c < oldCols + 1; c++) {
                        if (c == index) {
                            updated[r][c] = "";
                        } else {
                            updated[r][c] = cells[r][src++];
                        }
                    }
                }
                break;
            case "delete_column":
                if (oldCols <= 1) {
                    return error("Cannot delete the last table column");
                }
                if (index < 0 || index >= oldCols) {
                    return error("index out of range for delete_column");
                }
                updated = new String[oldRows][oldCols - 1];
                for (int r = 0; r < oldRows; r++) {
                    for (int c = 0, dst = 0; c < oldCols; c++) {
                        if (c == index) {
                            continue;
                        }
                        updated[r][dst++] = cells[r][c];
                    }
                }
                break;
            default:
                return error("Unsupported operation: " + operation);
        }

        Rectangle2D anchor = table.getAnchor();
        if (anchor == null) {
            return error("Table has no anchor");
        }

        XSLFTable replacement = slide.createTable();
        replacement.setAnchor(new Rectangle2D.Double(
                anchor.getX(), anchor.getY(), anchor.getWidth(), anchor.getHeight()));
        double rowHeight = anchor.getHeight() / updated.length;
        double colWidth = anchor.getWidth() / updated[0].length;
        for (int r = 0; r < updated.length; r++) {
            XSLFTableRow row = replacement.addRow();
            row.setHeight(rowHeight);
            for (int c = 0; c < updated[r].length; c++) {
                XSLFTableCell newCell = row.addCell();
                newCell.setText(updated[r][c]);
                replacement.setColumnWidth(c, colWidth);
            }
        }
        slide.removeShape(shape);

        int newShapeIndex = indexOfShape(slide, replacement);
        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", newShapeIndex);
        payload.put("rows", replacement.getNumberOfRows());
        payload.put("cols", replacement.getNumberOfColumns());
        payload.put("operation", operation);
        payload.put("message", "Table structure updated");
        return success(payload);
    }

    private ToolCallResult setTableRowHeight(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        // Accept either `row_index` (legacy, matches setTableHeaderStyle) or the generic
        // `index` used by insert_row / delete_row / insert_col / delete_col so callers don't
        // have to remember which table op wants which spelling.
        int rowIndex = args.has("row_index")
                ? args.path("row_index").asInt(-1)
                : args.path("index").asInt(-1);
        double height = args.path("height").asDouble(Double.NaN);
        if (Double.isNaN(height) || height <= 0) {
            return error("height must be > 0");
        }

        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }
        if (rowIndex < 0 || rowIndex >= table.getNumberOfRows()) {
            return error("Invalid row_index: " + rowIndex);
        }
        table.getRows().get(rowIndex).setHeight(height);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row_index", rowIndex);
        payload.put("height", height);
        payload.put("message", "Table row height updated");
        return success(payload);
    }

    private ToolCallResult setTableColumnWidth(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        // Accept either `col_index` (legacy) or the generic `index` used by the row/col
        // structural ops — see setTableRowHeight for the same rationale.
        int colIndex = args.has("col_index")
                ? args.path("col_index").asInt(-1)
                : args.path("index").asInt(-1);
        double width = args.path("width").asDouble(Double.NaN);
        if (Double.isNaN(width) || width <= 0) {
            return error("width must be > 0");
        }

        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }
        if (colIndex < 0 || colIndex >= table.getNumberOfColumns()) {
            return error("Invalid col_index: " + colIndex);
        }
        table.setColumnWidth(colIndex, width);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("col_index", colIndex);
        payload.put("width", width);
        payload.put("message", "Table column width updated");
        return success(payload);
    }

    private ToolCallResult setTableHeaderStyle(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(0);

        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }
        if (rowIndex < 0 || rowIndex >= table.getNumberOfRows()) {
            return error("Invalid row_index: " + rowIndex);
        }

        Color fillColor = args.has("fill_color") ? ColorParser.parseHex(args.path("fill_color").asText("")) : null;
        Color fontColor = args.has("font_color") ? ColorParser.parseHex(args.path("font_color").asText("")) : null;
        boolean bold = args.path("bold").asBoolean(true);

        XSLFTableRow row = table.getRows().get(rowIndex);
        for (XSLFTableCell cell : row.getCells()) {
            if (fillColor != null) {
                cell.setFillColor(fillColor);
            }
            for (var paragraph : cell.getTextParagraphs()) {
                for (XSLFTextRun run : paragraph.getTextRuns()) {
                    run.setBold(bold);
                    if (fontColor != null) {
                        run.setFontColor(fontColor);
                    }
                }
            }
        }
        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row_index", rowIndex);
        payload.put("message", "Table header style updated");
        return success(payload);
    }

    private ToolCallResult mergeCells(JsonNode args) {
        int startRow = args.path("start_row").asInt(-1);
        int startCol = args.path("start_col").asInt(-1);
        int endRow = args.path("end_row").asInt(-1);
        int endCol = args.path("end_col").asInt(-1);
        if (startRow < 0 || startCol < 0 || endRow < startRow || endCol < startCol) {
            return responseFactory.error("VALIDATION_ERROR",
                    "merge_cells requires 0 <= start_row <= end_row and 0 <= start_col <= end_col",
                    false);
        }
        if (startRow == endRow && startCol == endCol) {
            return responseFactory.error("VALIDATION_ERROR",
                    "merge_cells range must cover at least 2 cells", false);
        }

        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }
        if (endRow >= table.getNumberOfRows() || endCol >= table.getNumberOfColumns()) {
            return responseFactory.error("VALIDATION_ERROR",
                    "merge_cells range exceeds table dimensions", false);
        }

        for (int r = startRow; r <= endRow; r++) {
            XSLFTableRow row = table.getRows().get(r);
            for (int c = startCol; c <= endCol; c++) {
                if (c >= row.getCells().size()) {
                    continue;
                }
                XSLFTableCell cell = row.getCells().get(c);
                org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell ct =
                        (org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell) cell.getXmlObject();
                if ((ct.isSetGridSpan() && ct.getGridSpan() > 1)
                        || (ct.isSetRowSpan() && ct.getRowSpan() > 1)
                        || (ct.isSetHMerge() && ct.getHMerge())
                        || (ct.isSetVMerge() && ct.getVMerge())) {
                    return responseFactory.error("MERGE_CONFLICT",
                            "Cells in merge range already participate in another merge",
                            false);
                }
            }
        }

        int rowSpan = endRow - startRow + 1;
        int colSpan = endCol - startCol + 1;
        XSLFTableCell anchor = table.getRows().get(startRow).getCells().get(startCol);
        org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell anchorCt =
                (org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell) anchor.getXmlObject();
        if (colSpan > 1) {
            anchorCt.setGridSpan(colSpan);
        }
        if (rowSpan > 1) {
            anchorCt.setRowSpan(rowSpan);
        }
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (r == startRow && c == startCol) {
                    continue;
                }
                XSLFTableCell cell = table.getRows().get(r).getCells().get(c);
                org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell ct =
                        (org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell) cell.getXmlObject();
                if (c > startCol) {
                    ct.setHMerge(true);
                }
                if (r > startRow) {
                    ct.setVMerge(true);
                }
            }
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("start_row", startRow);
        payload.put("start_col", startCol);
        payload.put("end_row", endRow);
        payload.put("end_col", endCol);
        payload.put("row_span", rowSpan);
        payload.put("col_span", colSpan);
        payload.put("message", "Cells merged");
        return success(payload);
    }

    private ToolCallResult setCellBorder(JsonNode args) {
        int rowIndex = args.path("row").asInt(-1);
        int colIndex = args.path("col").asInt(-1);
        JsonNode sidesNode = args.path("sides");
        if (!sidesNode.isArray() || sidesNode.size() == 0) {
            return responseFactory.error("VALIDATION_ERROR",
                    "set_cell_border requires non-empty sides array", false);
        }
        Color color = args.has("color")
                ? ColorParser.parseHex(args.path("color").asText(""))
                : null;
        Double width = args.has("width") ? args.path("width").asDouble() : null;
        if (width != null && width < 0) {
            return responseFactory.error("VALIDATION_ERROR", "width must be >= 0", false);
        }
        String dashStyle = args.path("dash_style").asText("").toLowerCase(Locale.ROOT);

        java.util.EnumSet<org.apache.poi.sl.usermodel.TableCell.BorderEdge> edges =
                java.util.EnumSet.noneOf(org.apache.poi.sl.usermodel.TableCell.BorderEdge.class);
        for (JsonNode sideNode : sidesNode) {
            String side = sideNode.asText("").toLowerCase(Locale.ROOT);
            switch (side) {
                case "top":
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.top);
                    break;
                case "bottom":
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.bottom);
                    break;
                case "left":
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.left);
                    break;
                case "right":
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.right);
                    break;
                case "all":
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.top);
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.bottom);
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.left);
                    edges.add(org.apache.poi.sl.usermodel.TableCell.BorderEdge.right);
                    break;
                default:
                    return responseFactory.error("VALIDATION_ERROR",
                            "sides entries must be one of: top, bottom, left, right, all",
                            false);
            }
        }

        org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal.Enum dashVal = null;
        if (!dashStyle.isEmpty()) {
            switch (dashStyle) {
                case "solid":
                    dashVal = org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal.SOLID;
                    break;
                case "dash":
                    dashVal = org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal.DASH;
                    break;
                case "dot":
                    dashVal = org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal.DOT;
                    break;
                case "dashdot":
                    dashVal = org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal.DASH_DOT;
                    break;
                default:
                    return responseFactory.error("VALIDATION_ERROR",
                            "dash_style must be one of: solid, dash, dot, dashdot", false);
            }
        }

        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTable table)) {
            return error("shape_index does not point to a table on the selected slide");
        }
        XSLFTableCell cell = getTableCell(table, rowIndex, colIndex);
        if (cell == null) {
            return error("Invalid table cell coordinates");
        }

        for (org.apache.poi.sl.usermodel.TableCell.BorderEdge edge : edges) {
            if (color != null) {
                cell.setBorderColor(edge, color);
            }
            if (width != null) {
                cell.setBorderWidth(edge, width);
            }
            if (dashVal != null) {
                applyBorderDash(cell, edge, dashVal);
            }
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row", rowIndex);
        payload.put("col", colIndex);
        payload.put("sides_applied", edges.size());
        payload.put("message", "Cell border updated");
        return success(payload);
    }

    private static void applyBorderDash(
            XSLFTableCell cell,
            org.apache.poi.sl.usermodel.TableCell.BorderEdge edge,
            org.openxmlformats.schemas.drawingml.x2006.main.STPresetLineDashVal.Enum dashVal) {
        // POI's XSLFTableCell does not expose dash style. Drop into the underlying CTTableCell
        // and locate (or create) the matching <a:lnT/B/L/R> + <a:prstDash val="..."/>.
        org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell ct =
                (org.openxmlformats.schemas.drawingml.x2006.main.CTTableCell) cell.getXmlObject();
        org.openxmlformats.schemas.drawingml.x2006.main.CTTableCellProperties tcPr =
                ct.isSetTcPr() ? ct.getTcPr() : ct.addNewTcPr();
        org.openxmlformats.schemas.drawingml.x2006.main.CTLineProperties line;
        switch (edge) {
            case top:
                line = tcPr.isSetLnT() ? tcPr.getLnT() : tcPr.addNewLnT();
                break;
            case bottom:
                line = tcPr.isSetLnB() ? tcPr.getLnB() : tcPr.addNewLnB();
                break;
            case left:
                line = tcPr.isSetLnL() ? tcPr.getLnL() : tcPr.addNewLnL();
                break;
            case right:
                line = tcPr.isSetLnR() ? tcPr.getLnR() : tcPr.addNewLnR();
                break;
            default:
                return;
        }
        org.openxmlformats.schemas.drawingml.x2006.main.CTPresetLineDashProperties prst =
                line.isSetPrstDash() ? line.getPrstDash() : line.addNewPrstDash();
        prst.setVal(dashVal);
    }

    // -- helpers --

    private XSLFTableCell getTableCell(XSLFTable table, int rowIndex, int colIndex) {
        if (rowIndex < 0 || rowIndex >= table.getNumberOfRows()) {
            return null;
        }
        XSLFTableRow row = table.getRows().get(rowIndex);
        if (colIndex < 0 || colIndex >= row.getCells().size()) {
            return null;
        }
        return row.getCells().get(colIndex);
    }

    private int indexOfShape(XSLFSlide slide, XSLFShape shape) {
        List<XSLFShape> shapes = slide.getShapes();
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i) == shape) {
                return i;
            }
        }
        return -1;
    }

    private String[][] extractTableText(XSLFTable table) {
        int rows = table.getNumberOfRows();
        int cols = table.getNumberOfColumns();
        String[][] out = new String[rows][cols];
        for (int r = 0; r < rows; r++) {
            XSLFTableRow row = table.getRows().get(r);
            for (int c = 0; c < cols; c++) {
                XSLFTableCell cell = c < row.getCells().size() ? row.getCells().get(c) : null;
                out[r][c] = cell == null ? "" : cell.getText();
            }
        }
        return out;
    }

    private String requiredString(JsonNode args, String key) {
        return argumentValidator.requiredString(args, key);
    }

    private ObjectNode okPayload() {
        return responseFactory.okPayload();
    }

    private ToolCallResult success(ObjectNode payload) {
        return responseFactory.success(payload);
    }

    private ToolCallResult error(String message) {
        return responseFactory.error(message);
    }
}
