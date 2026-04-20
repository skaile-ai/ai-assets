package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.session.SessionStore;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.apache.poi.sl.usermodel.AutoNumberingScheme;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.w3c.dom.Node;

public final class PptAdvancedMutationOperations {
    private final SessionStore store;
    private final ObjectMapper mapper;
    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptPathResolver pathResolver;
    private final Path allowedRoot;

    public PptAdvancedMutationOperations(
            SessionStore store,
            ObjectMapper mapper,
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptPathResolver pathResolver) {
        this.store = store;
        this.mapper = mapper;
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.pathResolver = pathResolver;
        this.allowedRoot = pathResolver.allowedRoot();
    }

    public ToolCallResult setPageSetup(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String preset = requiredString(args, "preset").toLowerCase(Locale.ROOT);
        int width;
        int height;
        switch (preset) {
            case "standard_4_3":
                width = 720;
                height = 540;
                break;
            case "widescreen_16_9":
                width = 960;
                height = 540;
                break;
            case "a4_landscape":
                width = 842;
                height = 595;
                break;
            case "a4_portrait":
                width = 595;
                height = 842;
                break;
            case "custom":
                width = args.path("width").asInt(-1);
                height = args.path("height").asInt(-1);
                if (width < 1 || height < 1) {
                    return error("custom preset requires width and height >= 1");
                }
                break;
            default:
                return error("Unsupported preset: " + preset);
        }

        session.getSlideShow().setPageSize(new Dimension(width, height));
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("preset", preset);
        payload.put("page_width", width);
        payload.put("page_height", height);
        payload.put("message", "Page setup updated");
        return success(payload);
    }

    public ToolCallResult addShape(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        String shapeType = requiredString(args, "shape_type").toLowerCase(Locale.ROOT);
        ShapeType poiShapeType;
        switch (shapeType) {
            case "rectangle":
                poiShapeType = ShapeType.RECT;
                break;
            case "ellipse":
                poiShapeType = ShapeType.ELLIPSE;
                break;
            case "line":
                poiShapeType = ShapeType.LINE;
                break;
            case "arrow":
                poiShapeType = ShapeType.RIGHT_ARROW;
                break;
            default:
                return error("Unsupported shape_type: " + shapeType);
        }

        XSLFAutoShape shape = slide.createAutoShape();
        shape.setShapeType(poiShapeType);
        shape.setAnchor(new Rectangle2D.Double(x, y, width, height));

        if (args.has("fill_color") && !"line".equals(shapeType)) {
            shape.setFillColor(parseColorHex(args.path("fill_color").asText("")));
        }
        if (args.has("border_color")) {
            shape.setLineColor(parseColorHex(args.path("border_color").asText("")));
        }
        if (args.has("border_width")) {
            shape.setLineWidth(args.path("border_width").asDouble());
        }
        if (args.has("text") && !args.path("text").asText("").isBlank()) {
            shape.setText(args.path("text").asText());
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("shape_type", shapeType);
        payload.put("message", "Shape added");
        return success(payload);
    }

    public ToolCallResult addTextBox(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        String text = requiredString(args, "text");
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        XSLFTextShape box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        var paragraph = box.addNewTextParagraph();
        XSLFTextRun run = paragraph.addNewTextRun();
        run.setText(text);
        if (args.has("font_size")) {
            run.setFontSize(args.path("font_size").asDouble());
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("message", "Text box added");
        return success(payload);
    }

    public ToolCallResult addTable(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        int rows = args.path("rows").asInt(0);
        int cols = args.path("cols").asInt(0);
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (rows < 1 || cols < 1) {
            return error("rows and cols must be >= 1");
        }
        if (!isValidRect(x, y, width, height)) {
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

    public ToolCallResult getTableCell(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(-1);
        int colIndex = args.path("col_index").asInt(-1);

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
            return error("shape_index does not point to a table on the selected slide");
        }

        XSLFTableCell cell = getTableCell(table, rowIndex, colIndex);
        if (cell == null) {
            return error("Invalid table cell coordinates");
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("row_index", rowIndex);
        payload.put("col_index", colIndex);
        payload.put("text", cell.getText());
        return success(payload);
    }

    public ToolCallResult setTableCell(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(-1);
        int colIndex = args.path("col_index").asInt(-1);
        String text = requiredString(args, "text");

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
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

    /**
     * Consolidated text mutation for the v1 tool surface.
     *
     * <p>Selects runs based on {@code scope}:
     * <ul>
     *   <li>{@code shape} (default): every run in the shape.</li>
     *   <li>{@code run}: only the run(s) covering one matched occurrence of {@code target_text}.
     *       Existing runs are rewritten so the matched slice becomes its own run.</li>
     *   <li>{@code paragraph}: every run inside the paragraph at {@code paragraph_index}.</li>
     * </ul>
     *
     * <p>Paragraph-level properties (alignment, bullets, spacing) are applied to every paragraph
     * for {@code scope=shape}, the matched paragraph for {@code scope=paragraph}, and ignored for
     * {@code scope=run}. Run-level {@code strikethrough}/{@code rotation}/{@code auto_fit} are
     * accepted by the schema in Phase 1 but their implementations land in Phase 3.
     */
    public ToolCallResult setText(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        if (!(shape instanceof XSLFTextShape textShape)) {
            return error("shape_index does not point to a text-capable shape");
        }

        String scope = args.path("scope").asText("shape").toLowerCase(Locale.ROOT);
        if (!("shape".equals(scope) || "run".equals(scope) || "paragraph".equals(scope))) {
            return error("VALIDATION_ERROR", "scope must be one of: shape, run, paragraph", false);
        }

        Double fontSize = args.has("font_size") ? args.path("font_size").asDouble() : null;
        if (fontSize != null && fontSize <= 0) {
            return error("font_size must be > 0");
        }
        Color fontColor = args.has("font_color") ? parseColorHex(args.path("font_color").asText("")) : null;

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

    private void applyRunStyle(List<XSLFTextRun> runs, JsonNode args, Double fontSize,
            Color fontColor) {
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
            // strikethrough, rotation, and auto_fit accept the schema in Phase 1; their
            // implementations land in Phase 3.
        }
    }

    private ToolCallResult applyParagraphStyle(
            List<org.apache.poi.xslf.usermodel.XSLFTextParagraph> paragraphs, JsonNode args) {
        TextParagraph.TextAlign align = null;
        if (args.has("text_align")) {
            try {
                align = parseTextAlign(args.path("text_align").asText(""));
            } catch (IllegalArgumentException ex) {
                return error("VALIDATION_ERROR", ex.getMessage(), false);
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
                    error("VALIDATION_ERROR", "scope=run requires non-empty target_text", false));
        }
        int occurrence = args.path("occurrence").asInt(1);
        boolean caseSensitive = args.path("case_sensitive").asBoolean(true);
        if (occurrence < 1) {
            return new RunSelection(null, 0, 0,
                    error("VALIDATION_ERROR", "occurrence must be >= 1", false));
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

    public ToolCallResult replaceImage(JsonNode args) throws java.io.IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape originalShape = slide.getShapes().get(shapeIndex);
        if (!(originalShape instanceof XSLFPictureShape originalPicture)) {
            return error("shape_index does not point to a picture shape");
        }

        Rectangle2D anchor = originalPicture.getAnchor();
        if (anchor == null) {
            return error("Selected picture shape has no anchor");
        }

        Path imagePath = resolvePath(requiredString(args, "image_path"), false);
        PictureData.PictureType pictureType = inferPictureType(imagePath);
        byte[] imageBytes = Files.readAllBytes(imagePath);
        slide.removeShape(originalPicture);
        XSLFPictureData pictureData = session.getSlideShow().addPicture(imageBytes, pictureType);
        XSLFPictureShape replacementPicture = slide.createPicture(pictureData);

        boolean keepSize = args.path("keep_size").asBoolean(true);
        if (keepSize) {
            replacementPicture.setAnchor(new Rectangle2D.Double(
                    anchor.getX(),
                    anchor.getY(),
                    anchor.getWidth(),
                    anchor.getHeight()));
        }

        int replacementIndex = indexOfShape(slide, replacementPicture);

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", replacementIndex);
        payload.put("image_path", imagePath.toString());
        payload.put("message", "Image replaced");
        return success(payload);
    }

    public ToolCallResult modifyTableStructure(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        String operation = requiredString(args, "operation").toLowerCase(Locale.ROOT);

        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
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
                for (int r = 0, dst = 0; r < updated.length; r++) {
                    if (r == index) {
                        for (int c = 0; c < oldCols; c++) {
                            updated[r][c] = "";
                        }
                    } else {
                        updated[r] = cells[dst++].clone();
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
        replacement.setAnchor(new Rectangle2D.Double(anchor.getX(), anchor.getY(), anchor.getWidth(), anchor.getHeight()));

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

        XSLFShape oldShape = slide.getShapes().get(shapeIndex);
        slide.removeShape(oldShape);

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

    public ToolCallResult setTableRowHeight(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(-1);
        double height = args.path("height").asDouble(Double.NaN);

        if (Double.isNaN(height) || height <= 0) {
            return error("height must be > 0");
        }

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
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

    public ToolCallResult setTableColumnWidth(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int colIndex = args.path("col_index").asInt(-1);
        double width = args.path("width").asDouble(Double.NaN);

        if (Double.isNaN(width) || width <= 0) {
            return error("width must be > 0");
        }

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
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

    public ToolCallResult setTableHeaderStyle(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        int rowIndex = args.path("row_index").asInt(0);

        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
            return error("shape_index does not point to a table on the selected slide");
        }
        if (rowIndex < 0 || rowIndex >= table.getNumberOfRows()) {
            return error("Invalid row_index: " + rowIndex);
        }

        Color fillColor = args.has("fill_color") ? parseColorHex(args.path("fill_color").asText("")) : null;
        Color fontColor = args.has("font_color") ? parseColorHex(args.path("font_color").asText("")) : null;
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

    public ToolCallResult getTable(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFTable table = resolveTableShape(session.getSlideShow(), slideIndex, shapeIndex);
        if (table == null) {
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
                // row_span / col_span / is_merge_anchor surface once merge_cells ships in
                // Phase 3. Phase 1 always reports a 1x1 unmerged cell.
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
        // Phase 1 has no merge support, so merged_regions is always empty. Phase 3 will populate.
        payload.putArray("merged_regions");
        return success(payload);
    }

    public ToolCallResult editTable(JsonNode args) {
        String operation = args.path("operation").asText("").toLowerCase(Locale.ROOT);
        if (operation.isBlank()) {
            return error("VALIDATION_ERROR", "operation is required", false);
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
                return error("FEATURE_NOT_IMPLEMENTED",
                        "operation=" + operation + " lands in Phase 3.", false);
            default:
                return error("VALIDATION_ERROR",
                        "operation must be one of: set_cell, insert_row, delete_row, insert_col, delete_col, set_row_height, set_col_width, set_header_style, merge_cells, set_cell_border",
                        false);
        }
    }

    private ToolCallResult editTableSetCell(JsonNode args) {
        // Re-shape into the schema setTableCell expects.
        ObjectNode mapped = mapper.createObjectNode();
        copyField(args, mapped, "document_id");
        copyField(args, mapped, "slide_index");
        copyField(args, mapped, "shape_index");
        renameField(args, mapped, "row", "row_index");
        renameField(args, mapped, "col", "col_index");
        copyField(args, mapped, "text");
        return setTableCell(mapped);
    }

    private ToolCallResult editTableRowStructure(JsonNode args, String operation) {
        // modify_table_structure expects "insert_row"/"delete_row" + index, which matches.
        ObjectNode mapped = mapper.createObjectNode();
        copyField(args, mapped, "document_id");
        copyField(args, mapped, "slide_index");
        copyField(args, mapped, "shape_index");
        mapped.put("operation", operation);
        if (args.has("index")) {
            mapped.put("index", args.path("index").asInt());
        }
        return modifyTableStructure(mapped);
    }

    private ToolCallResult editTableColStructure(JsonNode args, String operation) {
        ObjectNode mapped = mapper.createObjectNode();
        copyField(args, mapped, "document_id");
        copyField(args, mapped, "slide_index");
        copyField(args, mapped, "shape_index");
        // edit_table speaks in "insert_col"/"delete_col"; modify_table_structure uses the
        // verbose forms "insert_column"/"delete_column".
        mapped.put("operation", "insert_col".equals(operation) ? "insert_column" : "delete_column");
        if (args.has("index")) {
            mapped.put("index", args.path("index").asInt());
        }
        return modifyTableStructure(mapped);
    }

    private void copyField(JsonNode src, ObjectNode dst, String name) {
        if (src.has(name)) {
            dst.set(name, src.get(name));
        }
    }

    private void renameField(JsonNode src, ObjectNode dst, String from, String to) {
        if (src.has(from)) {
            dst.set(to, src.get(from));
        }
    }

    public ToolCallResult moveShape(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        Rectangle2D anchor = shape.getAnchor();
        if (anchor == null) {
            return error("Selected shape has no anchor");
        }

        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        if (Double.isNaN(x) || Double.isNaN(y)) {
            return error("x and y must be valid numbers");
        }

        if (!setShapeAnchor(shape, x, y, anchor.getWidth(), anchor.getHeight())) {
            return error("Selected shape does not support anchor updates");
        }
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("x", x);
        payload.put("y", y);
        payload.put("width", anchor.getWidth());
        payload.put("height", anchor.getHeight());
        payload.put("message", "Shape moved");
        return success(payload);
    }

    public ToolCallResult cloneShape(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape original = slide.getShapes().get(shapeIndex);
        if (!(original instanceof XSLFTextShape originalText)) {
            return error("clone_shape currently supports only text-capable shapes");
        }

        Rectangle2D anchor = original.getAnchor();
        if (anchor == null) {
            return error("Selected shape has no anchor");
        }
        double offsetX = args.path("offset_x").asDouble(20);
        double offsetY = args.path("offset_y").asDouble(20);

        XSLFTextShape clone = slide.createTextBox();
        clone.setAnchor(new Rectangle2D.Double(
                anchor.getX() + offsetX,
                anchor.getY() + offsetY,
                anchor.getWidth(),
                anchor.getHeight()));
        clone.setText(originalText.getText());

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("source_shape_index", shapeIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("message", "Shape cloned");
        return success(payload);
    }

    public ToolCallResult resizeShape(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        Rectangle2D anchor = shape.getAnchor();
        if (anchor == null) {
            return error("Selected shape has no anchor");
        }

        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (Double.isNaN(width) || Double.isNaN(height) || width <= 0 || height <= 0) {
            return error("width and height must be > 0");
        }

        if (!setShapeAnchor(shape, anchor.getX(), anchor.getY(), width, height)) {
            return error("Selected shape does not support anchor updates");
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("width", width);
        payload.put("height", height);
        payload.put("message", "Shape resized");
        return success(payload);
    }

    public ToolCallResult addHyperlink(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        String url = requiredString(args, "url");
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        if (!(shape instanceof XSLFTextShape textShape)) {
            return error("shape_index does not point to a text-capable shape");
        }

        List<XSLFTextRun> runs = collectTextRuns(textShape);
        if (runs.isEmpty()) {
            return error("Selected text shape has no text runs to hyperlink");
        }

        for (XSLFTextRun run : runs) {
            run.createHyperlink().setAddress(url);
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("url", url);
        payload.put("message", "Hyperlink added");
        return success(payload);
    }

    public ToolCallResult setSlideBackground(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        Color color = parseColorHex(requiredString(args, "color"));
        slide.getBackground().setFillColor(color);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("color", requiredString(args, "color"));
        payload.put("message", "Slide background updated");
        return success(payload);
    }

    public ToolCallResult deleteShape(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        slide.removeShape(shape);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("message", "Shape deleted");
        return success(payload);
    }

    public ToolCallResult getShapeProperties(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        Rectangle2D anchor = shape.getAnchor();

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("shape_type", shape.getClass().getSimpleName());

        if (anchor != null) {
            payload.put("x", anchor.getX());
            payload.put("y", anchor.getY());
            payload.put("width", anchor.getWidth());
            payload.put("height", anchor.getHeight());
        }

        if (shape instanceof XSLFTextShape textShape) {
            payload.put("text", textShape.getText());
        }
        if (shape instanceof XSLFSimpleShape simpleShape) {
            Color fill = simpleShape.getFillColor();
            if (fill != null) {
                payload.put("fill_color", String.format("#%02X%02X%02X", fill.getRed(), fill.getGreen(), fill.getBlue()));
            }
            Color line = simpleShape.getLineColor();
            if (line != null) {
                payload.put("border_color", String.format("#%02X%02X%02X", line.getRed(), line.getGreen(), line.getBlue()));
            }
            payload.put("border_width", simpleShape.getLineWidth());
        }

        return success(payload);
    }

    public ToolCallResult setShapeStyle(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);

        if (args.has("fill_color")) {
            if (!(shape instanceof XSLFSimpleShape simpleShape)) {
                return error("fill_color is only supported for simple shapes");
            }
            simpleShape.setFillColor(parseColorHex(args.path("fill_color").asText("")));
        }

        if (args.has("border_color")) {
            if (!(shape instanceof XSLFSimpleShape simpleShape)) {
                return error("border_color is only supported for simple shapes");
            }
            simpleShape.setLineColor(parseColorHex(args.path("border_color").asText("")));
        }

        if (args.has("border_width")) {
            if (!(shape instanceof XSLFSimpleShape simpleShape)) {
                return error("border_width is only supported for simple shapes");
            }
            simpleShape.setLineWidth(args.path("border_width").asDouble());
        }

        if (args.has("text_align")) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                return error("text_align is only supported for text-capable shapes");
            }
            TextParagraph.TextAlign align = parseTextAlign(args.path("text_align").asText(""));
            for (var paragraph : textShape.getTextParagraphs()) {
                paragraph.setTextAlign(align);
            }
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("message", "Shape style updated");
        return success(payload);
    }

    public ToolCallResult setDocumentMetadata(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        XMLSlideShow show = session.getSlideShow();

        if (args.has("title")) {
            show.getProperties().getCoreProperties().setTitle(args.path("title").asText(""));
        }
        if (args.has("author")) {
            show.getProperties().getCoreProperties().setCreator(args.path("author").asText(""));
        }
        if (args.has("subject")) {
            show.getProperties().getCoreProperties().setSubjectProperty(args.path("subject").asText(""));
        }
        if (args.has("keywords")) {
            show.getProperties().getCoreProperties().setKeywords(args.path("keywords").asText(""));
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("message", "Document metadata updated");
        return success(payload);
    }

    public ToolCallResult setSlideLayout(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        String layoutType = args.path("layout_type").asText("");

        XMLSlideShow show = session.getSlideShow();
        XSLFSlide source = getSlideByIndex(show, slideIndex);
        if (source == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (show.getSlideMasters().isEmpty()) {
            return error("Presentation has no slide masters");
        }

        SlideLayout targetLayout;
        switch (layoutType.toLowerCase(Locale.ROOT)) {
            case "blank":
                targetLayout = SlideLayout.BLANK;
                break;
            case "title":
            case "title_only":
                targetLayout = SlideLayout.TITLE;
                break;
            case "title_content":
                targetLayout = SlideLayout.TITLE_AND_CONTENT;
                break;
            default:
                return error("Unknown layout_type: " + layoutType);
        }

        XSLFSlideMaster master = show.getSlideMasters().get(0);
        XSLFSlideLayout layout = master.getLayout(targetLayout);
        if (layout == null) {
            return error("Requested layout is not available in this template: " + layoutType);
        }

        XSLFSlide replacement = show.createSlide(layout);
        replacement.importContent(source);
        show.setSlideOrder(replacement, slideIndex);
        show.removeSlide(slideIndex + 1);

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("layout_type", layoutType);
        payload.put("message", "Slide layout updated");
        return success(payload);
    }

    public ToolCallResult setShapeZOrder(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        String position = requiredString(args, "position").toLowerCase(Locale.ROOT);

        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return error("Invalid shape_index: " + shapeIndex);
        }

        XSLFShape shape = slide.getShapes().get(shapeIndex);
        Node shapeNode = shape.getXmlObject().getDomNode();
        Node parent = shapeNode.getParentNode();
        if (parent == null) {
            return error("Cannot reorder shape without parent node");
        }

        List<Node> zNodes = collectZOrderNodes(parent);
        int current = zNodes.indexOf(shapeNode);
        if (current < 0) {
            return error("Could not resolve shape z-order node");
        }

        int target;
        switch (position) {
            case "front":
                target = zNodes.size() - 1;
                break;
            case "back":
                target = 0;
                break;
            case "forward":
                target = Math.min(current + 1, zNodes.size() - 1);
                break;
            case "backward":
                target = Math.max(current - 1, 0);
                break;
            default:
                return error("Invalid position: " + position);
        }

        if (target != current) {
            List<Node> remaining = new ArrayList<>(zNodes);
            remaining.remove(shapeNode);
            parent.removeChild(shapeNode);

            int insertAt = Math.min(target, remaining.size());
            if (insertAt >= remaining.size()) {
                parent.appendChild(shapeNode);
            } else {
                parent.insertBefore(shapeNode, remaining.get(insertAt));
            }
            session.touch(true);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("position", position);
        payload.put("message", "Shape z-order updated");
        return success(payload);
    }

    private PptDocumentSession requireSession(JsonNode args) {
        String id = requiredString(args, "document_id");
        Optional<PptDocumentSession> maybe = store.get(id);
        return maybe.orElse(null);
    }

    private XSLFSlide getSlideByIndex(XMLSlideShow show, int index) {
        if (index < 0 || index >= show.getSlides().size()) {
            return null;
        }
        return show.getSlides().get(index);
    }

    private String requiredString(JsonNode args, String key) {
        return argumentValidator.requiredString(args, key);
    }

    private XSLFTable resolveTableShape(XMLSlideShow show, int slideIndex, int shapeIndex) {
        XSLFSlide slide = getSlideByIndex(show, slideIndex);
        if (slide == null) {
            return null;
        }
        if (shapeIndex < 0 || shapeIndex >= slide.getShapes().size()) {
            return null;
        }
        XSLFShape shape = slide.getShapes().get(shapeIndex);
        if (shape instanceof XSLFTable table) {
            return table;
        }
        return null;
    }

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

    private List<Node> collectZOrderNodes(Node parent) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < parent.getChildNodes().getLength(); i++) {
            Node child = parent.getChildNodes().item(i);
            String nodeName = child.getNodeName();
            String localName = child.getLocalName();
            if ("p:sp".equals(nodeName) || "sp".equals(localName)
                    || "p:grpSp".equals(nodeName) || "grpSp".equals(localName)
                    || "p:graphicFrame".equals(nodeName) || "graphicFrame".equals(localName)
                    || "p:cxnSp".equals(nodeName) || "cxnSp".equals(localName)
                    || "p:pic".equals(nodeName) || "pic".equals(localName)
                    || "p:contentPart".equals(nodeName) || "contentPart".equals(localName)) {
                nodes.add(child);
            }
        }
        return nodes;
    }

    private Color parseColorHex(String input) {
        String value = input == null ? "" : input.strip();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() != 6) {
            throw new IllegalArgumentException("font_color must be in #RRGGBB format");
        }
        try {
            int rgb = Integer.parseInt(value, 16);
            return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("font_color must be in #RRGGBB format", ex);
        }
    }

    private boolean isValidRect(double x, double y, double w, double h) {
        return argumentValidator.isValidRect(x, y, w, h);
    }

    private boolean setShapeAnchor(XSLFShape shape, double x, double y, double w, double h) {
        if (shape instanceof XSLFSimpleShape simpleShape) {
            simpleShape.setAnchor(new Rectangle2D.Double(x, y, w, h));
            return true;
        }
        return false;
    }

    private Path resolvePath(String rawPath, boolean forWrite) {
        return pathResolver.resolvePath(rawPath, forWrite);
    }

    private PictureData.PictureType inferPictureType(Path imagePath) {
        return pathResolver.inferPictureType(imagePath);
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

    private ToolCallResult error(String code, String message, boolean retriable) {
        return responseFactory.error(code, message, retriable);
    }
}
