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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.sl.usermodel.AutoNumberingScheme;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

/**
 * Table and text-styling tool handlers: {@code ppt.add_table}, {@code ppt.get_table},
 * {@code ppt.edit_table}, {@code ppt.set_text}. Internally dispatches the multi-operation
 * {@code edit_table} surface through to the relevant cell / structure mutation path.
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
                "ppt.edit_table", this::editTable,
                "ppt.set_text", this::setText);
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
        for (int r = 0; r < rows; r++) {
            ArrayNode row = cellRows.addArray();
            XSLFTableRow tableRow = table.getRows().get(r);
            for (int c = 0; c < cols && c < tableRow.getCells().size(); c++) {
                XSLFTableCell cell = tableRow.getCells().get(c);
                ObjectNode cellNode = row.addObject();
                cellNode.put("text", cell.getText() == null ? "" : cell.getText());
                cellNode.put("row_span", 1);
                cellNode.put("col_span", 1);
                cellNode.put("is_merge_anchor", false);
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
        payload.putArray("merged_regions");
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
            case "set_cell_border":
                return responseFactory.error("FEATURE_NOT_IMPLEMENTED",
                        "operation=" + operation + " lands in Phase 4.", false);
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
        int rowIndex = args.path("row_index").asInt(-1);
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
        int colIndex = args.path("col_index").asInt(-1);
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

    ToolCallResult setText(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTextShape textShape)) {
            return error("shape_index does not point to a text-capable shape");
        }

        String scope = args.path("scope").asText("shape").toLowerCase(Locale.ROOT);
        if (!("shape".equals(scope) || "run".equals(scope) || "paragraph".equals(scope))) {
            return responseFactory.error("VALIDATION_ERROR",
                    "scope must be one of: shape, run, paragraph", false);
        }

        Double fontSize = args.has("font_size") ? args.path("font_size").asDouble() : null;
        if (fontSize != null && fontSize <= 0) {
            return error("font_size must be > 0");
        }
        Color fontColor = args.has("font_color") ? ColorParser.parseHex(args.path("font_color").asText("")) : null;

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("scope", scope);

        List<XSLFTextRun> targetRuns;
        List<org.apache.poi.xslf.usermodel.XSLFTextParagraph> targetParagraphs;

        if ("run".equals(scope)) {
            RunSelection selection = selectRunByTargetText(textShape, args);
            if (selection.error != null) {
                return selection.error;
            }
            targetRuns = List.of(selection.styledRun);
            targetParagraphs = List.of();
            payload.put("start", selection.start);
            payload.put("end", selection.end);
        } else if ("paragraph".equals(scope)) {
            int paragraphIndex = args.path("paragraph_index").asInt(-1);
            if (paragraphIndex < 0 || paragraphIndex >= textShape.getTextParagraphs().size()) {
                return error("Invalid paragraph_index: " + paragraphIndex);
            }
            var paragraph = textShape.getTextParagraphs().get(paragraphIndex);
            targetRuns = new ArrayList<>(paragraph.getTextRuns());
            if (targetRuns.isEmpty()) {
                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText("");
                targetRuns.add(run);
            }
            targetParagraphs = List.of(paragraph);
            payload.put("paragraph_index", paragraphIndex);
        } else {
            targetRuns = collectTextRuns(textShape);
            if (targetRuns.isEmpty()) {
                var paragraph = textShape.addNewTextParagraph();
                XSLFTextRun run = paragraph.addNewTextRun();
                run.setText("");
                targetRuns.add(run);
            }
            targetParagraphs = new ArrayList<>(textShape.getTextParagraphs());
        }

        applyRunStyle(targetRuns, args, fontSize, fontColor);
        if (!targetParagraphs.isEmpty()) {
            ToolCallResult paragraphError = applyParagraphStyle(targetParagraphs, args);
            if (paragraphError != null) {
                return paragraphError;
            }
        }

        session.touch(true);
        payload.put("message", "Text updated");
        return success(payload);
    }

    private void applyRunStyle(List<XSLFTextRun> runs, JsonNode args, Double fontSize, Color fontColor) {
        for (XSLFTextRun run : runs) {
            if (args.has("bold")) {
                run.setBold(args.path("bold").asBoolean());
            }
            if (args.has("italic")) {
                run.setItalic(args.path("italic").asBoolean());
            }
            if (args.has("underline")) {
                run.setUnderlined(args.path("underline").asBoolean());
            }
            if (fontSize != null) {
                run.setFontSize(fontSize);
            }
            if (fontColor != null) {
                run.setFontColor(fontColor);
            }
            if (args.has("font_family") && !args.path("font_family").asText("").isBlank()) {
                run.setFontFamily(args.path("font_family").asText());
            }
        }
    }

    private ToolCallResult applyParagraphStyle(
            List<org.apache.poi.xslf.usermodel.XSLFTextParagraph> paragraphs, JsonNode args) {
        TextParagraph.TextAlign align = null;
        if (args.has("text_align")) {
            try {
                align = parseTextAlign(args.path("text_align").asText(""));
            } catch (IllegalArgumentException ex) {
                return responseFactory.error("VALIDATION_ERROR", ex.getMessage(), false);
            }
        }
        Double lineSpacing = args.has("line_spacing") ? args.path("line_spacing").asDouble() : null;
        Double spaceBefore = args.has("space_before") ? args.path("space_before").asDouble() : null;
        Double spaceAfter = args.has("space_after") ? args.path("space_after").asDouble() : null;
        Double leftMargin = args.has("left_margin") ? args.path("left_margin").asDouble() : null;
        Double indent = args.has("indent") ? args.path("indent").asDouble() : null;
        boolean setBullet = args.has("bullet_enabled");
        boolean bulletEnabled = args.path("bullet_enabled").asBoolean(false);
        boolean numbered = args.path("numbered").asBoolean(false);
        String bulletChar = args.path("bullet_character").asText("");
        Integer bulletLevel = args.has("bullet_level") ? args.path("bullet_level").asInt() : null;

        for (var paragraph : paragraphs) {
            if (align != null) {
                paragraph.setTextAlign(align);
            }
            if (numbered) {
                paragraph.setBulletAutoNumber(AutoNumberingScheme.arabicPeriod, 1);
            } else if (setBullet) {
                paragraph.setBullet(bulletEnabled);
            }
            if (!bulletChar.isBlank()) {
                paragraph.setBulletCharacter(bulletChar);
            }
            if (bulletLevel != null) {
                double margin = Math.max(0, bulletLevel) * 18.0;
                paragraph.setLeftMargin(margin);
                paragraph.setIndent(Math.max(0, margin - 12.0));
            }
            if (leftMargin != null) {
                paragraph.setLeftMargin(leftMargin);
            }
            if (indent != null) {
                paragraph.setIndent(indent);
            }
            if (lineSpacing != null) {
                paragraph.setLineSpacing(lineSpacing);
            }
            if (spaceBefore != null) {
                paragraph.setSpaceBefore(spaceBefore);
            }
            if (spaceAfter != null) {
                paragraph.setSpaceAfter(spaceAfter);
            }
        }
        return null;
    }

    private static final class RunSelection {
        final XSLFTextRun styledRun;
        final int start;
        final int end;
        final ToolCallResult error;

        private RunSelection(XSLFTextRun styledRun, int start, int end, ToolCallResult error) {
            this.styledRun = styledRun;
            this.start = start;
            this.end = end;
            this.error = error;
        }
    }

    private RunSelection selectRunByTargetText(XSLFTextShape textShape, JsonNode args) {
        String targetText = args.path("target_text").asText("");
        if (targetText.isEmpty()) {
            return new RunSelection(null, 0, 0,
                    responseFactory.error("VALIDATION_ERROR", "scope=run requires non-empty target_text", false));
        }
        int occurrence = args.path("occurrence").asInt(1);
        boolean caseSensitive = args.path("case_sensitive").asBoolean(true);
        if (occurrence < 1) {
            return new RunSelection(null, 0, 0,
                    responseFactory.error("VALIDATION_ERROR", "occurrence must be >= 1", false));
        }
        String sourceText = textShape.getText();
        if (sourceText == null || sourceText.isBlank()) {
            return new RunSelection(null, 0, 0, error("Selected text shape is empty"));
        }
        String haystack = caseSensitive ? sourceText : sourceText.toLowerCase(Locale.ROOT);
        String needle = caseSensitive ? targetText : targetText.toLowerCase(Locale.ROOT);
        int start = -1;
        int from = 0;
        int seen = 0;
        while (true) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                break;
            }
            seen++;
            if (seen == occurrence) {
                start = idx;
                break;
            }
            from = idx + Math.max(1, needle.length());
        }
        if (start < 0) {
            return new RunSelection(null, 0, 0,
                    error("Could not find target_text occurrence in shape text"));
        }
        int end = start + targetText.length();
        String before = sourceText.substring(0, start);
        String target = sourceText.substring(start, end);
        String after = sourceText.substring(end);

        textShape.clearText();
        var paragraph = textShape.addNewTextParagraph();
        if (!before.isEmpty()) {
            paragraph.addNewTextRun().setText(before);
        }
        XSLFTextRun styledRun = paragraph.addNewTextRun();
        styledRun.setText(target);
        if (!after.isEmpty()) {
            paragraph.addNewTextRun().setText(after);
        }
        return new RunSelection(styledRun, start, end, null);
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

    private List<XSLFTextRun> collectTextRuns(XSLFTextShape textShape) {
        List<XSLFTextRun> runs = new ArrayList<>();
        textShape.getTextParagraphs().forEach(paragraph -> runs.addAll(paragraph.getTextRuns()));
        return runs;
    }

    private TextParagraph.TextAlign parseTextAlign(String raw) {
        String value = raw == null ? "" : raw.strip().toLowerCase(Locale.ROOT);
        switch (value) {
            case "left":
                return TextParagraph.TextAlign.LEFT;
            case "center":
                return TextParagraph.TextAlign.CENTER;
            case "right":
                return TextParagraph.TextAlign.RIGHT;
            case "justify":
                return TextParagraph.TextAlign.JUSTIFY;
            default:
                throw new IllegalArgumentException("text_align must be one of: left, center, right, justify");
        }
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
