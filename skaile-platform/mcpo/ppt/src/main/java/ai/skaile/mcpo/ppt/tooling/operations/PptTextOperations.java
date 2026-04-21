package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.ColorParser;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.sl.usermodel.AutoNumberingScheme;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.sl.usermodel.TextShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.STTextStrikeType;

/**
 * The {@code ppt.set_text} unified text mutation handler. Applies run-level styling
 * (bold/italic/underline/strikethrough/font_*) to the selected runs, paragraph-level
 * styling (text_align/line_spacing/bullet_*) to the matching paragraphs, and
 * shape-level body settings (rotation, auto_fit) to the containing text shape.
 *
 * <p>Lived on {@code PptTableOperations} through Phase 2 because text edits are most
 * often invoked on table cells; split out in Phase 4 so that adding rotation /
 * strikethrough / auto_fit didn't push the table class past the 800-line cap.
 */
public final class PptTextOperations {

    private final ToolResponseFactory responseFactory;
    private final PptShapeFinder shapeFinder;

    public PptTextOperations(ToolResponseFactory responseFactory, PptShapeFinder shapeFinder) {
        this.responseFactory = responseFactory;
        this.shapeFinder = shapeFinder;
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of("ppt.set_text", this::setText);
    }

    ToolCallResult setText(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTextShape textShape)) {
            return responseFactory.error("shape_index does not point to a text-capable shape");
        }

        String scope = args.path("scope").asText("shape").toLowerCase(Locale.ROOT);
        if (!("shape".equals(scope) || "run".equals(scope) || "paragraph".equals(scope))) {
            return responseFactory.error("VALIDATION_ERROR",
                    "scope must be one of: shape, run, paragraph", false);
        }

        Double fontSize = args.has("font_size") ? args.path("font_size").asDouble() : null;
        if (fontSize != null && fontSize <= 0) {
            return responseFactory.error("font_size must be > 0");
        }
        Color fontColor = args.has("font_color")
                ? ColorParser.parseHex(args.path("font_color").asText(""))
                : null;

        ObjectNode payload = responseFactory.okPayload();
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
                return responseFactory.error("Invalid paragraph_index: " + paragraphIndex);
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
        // Shape-body settings (rotation, auto_fit) apply once per call regardless of scope.
        applyShapeBodyStyle(textShape, args);

        session.touch(true);
        payload.put("message", "Text updated");
        return responseFactory.success(payload);
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
            if (args.has("strikethrough")) {
                CTTextCharacterProperties rPr = run.getRPr(true);
                rPr.setStrike(args.path("strikethrough").asBoolean()
                        ? STTextStrikeType.SNG_STRIKE
                        : STTextStrikeType.NO_STRIKE);
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

    private void applyShapeBodyStyle(XSLFTextShape textShape, JsonNode args) {
        if (args.has("rotation")) {
            double degrees = args.path("rotation").asDouble();
            CTTextBodyProperties bodyPr = bodyPropertiesOf(textShape);
            if (bodyPr != null) {
                // OOXML stores text body rotation in 60000ths of a degree.
                bodyPr.setRot((int) Math.round(degrees * 60000));
            }
        }
        if (args.has("auto_fit")) {
            String mode = args.path("auto_fit").asText("").toLowerCase(Locale.ROOT);
            // POI's TextAutofit only exposes NONE / NORMAL (shrink text on overflow) /
            // SHAPE (resize shape to text). The schema accepts "shrink" as an alias for the
            // shape-resize mode since that's what PowerPoint labels "Resize shape to fit text".
            switch (mode) {
                case "none":
                    textShape.setTextAutofit(TextShape.TextAutofit.NONE);
                    break;
                case "normal":
                    textShape.setTextAutofit(TextShape.TextAutofit.NORMAL);
                    break;
                case "shrink":
                    textShape.setTextAutofit(TextShape.TextAutofit.SHAPE);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "auto_fit must be one of: none, normal, shrink");
            }
        }
    }

    private static CTTextBodyProperties bodyPropertiesOf(XSLFTextShape textShape) {
        XmlObject xml = textShape.getXmlObject();
        XmlObject[] match = xml.selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main'; "
                        + "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main'; "
                        + "./p:txBody/a:bodyPr | ./*/p:txBody/a:bodyPr | ./a:txBody/a:bodyPr");
        if (match.length > 0 && match[0] instanceof CTTextBodyProperties props) {
            return props;
        }
        // Fallback: locate any txBody and add a bodyPr.
        XmlObject[] bodies = xml.selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main'; "
                        + "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main'; "
                        + "./p:txBody | ./*/p:txBody | ./a:txBody");
        if (bodies.length > 0 && bodies[0] instanceof CTTextBody body) {
            // bodyPr is mandatory in the OOXML schema, so getBodyPr() never returns null.
            CTTextBodyProperties bodyPr = body.getBodyPr();
            return bodyPr != null ? bodyPr : body.addNewBodyPr();
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
                    responseFactory.error("VALIDATION_ERROR",
                            "scope=run requires non-empty target_text", false));
        }
        int occurrence = args.path("occurrence").asInt(1);
        boolean caseSensitive = args.path("case_sensitive").asBoolean(true);
        if (occurrence < 1) {
            return new RunSelection(null, 0, 0,
                    responseFactory.error("VALIDATION_ERROR", "occurrence must be >= 1", false));
        }
        String sourceText = textShape.getText();
        if (sourceText == null || sourceText.isBlank()) {
            return new RunSelection(null, 0, 0,
                    responseFactory.error("Selected text shape is empty"));
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
                    responseFactory.error("Could not find target_text occurrence in shape text"));
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

    private static List<XSLFTextRun> collectTextRuns(XSLFTextShape textShape) {
        List<XSLFTextRun> runs = new ArrayList<>();
        textShape.getTextParagraphs().forEach(paragraph -> runs.addAll(paragraph.getTextRuns()));
        return runs;
    }

    private static TextParagraph.TextAlign parseTextAlign(String raw) {
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
                throw new IllegalArgumentException(
                        "text_align must be one of: left, center, right, justify");
        }
    }

}
