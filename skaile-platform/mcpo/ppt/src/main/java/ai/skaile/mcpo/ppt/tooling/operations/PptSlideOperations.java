package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.PptSlideBuilder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

/**
 * Slide-content tool handlers: {@code ppt.get_slide_content}, slide add/duplicate/delete,
 * text search-and-replace, textbox creation, and slide notes.
 */
public final class PptSlideOperations {

    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;

    public PptSlideOperations(
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptShapeFinder shapeFinder,
            PptLimits limits) {
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.shapeFinder = shapeFinder;
        this.limits = limits;
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of(
                "ppt.get_slide_content", this::getSlideContent,
                "ppt.add_slide", this::addSlide,
                "ppt.duplicate_slide", this::duplicateSlide,
                "ppt.delete_slides", this::deleteSlides,
                "ppt.update_text", this::updateText,
                "ppt.replace_text_globally", this::replaceTextGlobally,
                "ppt.add_textbox", this::addTextBox,
                "ppt.get_slide_notes", this::getSlideNotes,
                "ppt.set_slide_notes", this::setSlideNotes);
    }

    public ToolCallResult getSlideContent(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("text", PptSlideBuilder.collectSlideText(slide));

        ArrayNode shapes = payload.putArray("shapes");
        int i = 0;
        for (XSLFShape shape : slide.getShapes()) {
            ObjectNode shapeNode = shapes.addObject();
            shapeNode.put("shape_index", i++);
            shapeNode.put("shape_type", shape.getClass().getSimpleName());
            if (shape instanceof XSLFTextShape textShape) {
                shapeNode.put("text", PptSlideBuilder.visibleText(textShape));
            }
            if (shape instanceof XSLFTable table) {
                shapeNode.put("rows", table.getNumberOfRows());
                shapeNode.put("cols", table.getNumberOfColumns());
            }
        }

        return success(payload);
    }

    public ToolCallResult addSlide(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        ToolCallResult limit = limits.enforceSlideLimit(session);
        if (limit != null) {
            return limit;
        }

        String title = args.path("title").asText("");
        XSLFSlide slide = PptSlideBuilder.createDefaultSlide(session.getSlideShow());
        if (!title.isBlank()) {
            XSLFTextShape titleShape = slide.createTextBox();
            titleShape.setAnchor(new Rectangle2D.Double(40, 30, 840, 80));
            titleShape.setText(title);
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", session.getSlideShow().getSlides().size() - 1);
        payload.put("slide_count", session.getSlideShow().getSlides().size());
        return success(payload);
    }

    public ToolCallResult duplicateSlide(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        ToolCallResult limit = limits.enforceSlideLimit(session);
        if (limit != null) {
            return limit;
        }

        XMLSlideShow show = session.getSlideShow();
        int sourceSlideIndex = args.path("source_slide_index").asInt(-1);
        XSLFSlide source = shapeFinder.requireSlide(session, sourceSlideIndex);

        int targetIndex = args.has("target_index")
                ? args.path("target_index").asInt(show.getSlides().size())
                : Math.min(sourceSlideIndex + 1, show.getSlides().size());
        if (targetIndex < 0 || targetIndex > show.getSlides().size()) {
            return error("Invalid target_index: " + targetIndex);
        }

        XSLFSlide copy = show.createSlide();
        copy.importContent(source);
        show.setSlideOrder(copy, targetIndex);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("source_slide_index", sourceSlideIndex);
        payload.put("slide_index", targetIndex);
        payload.put("slide_count", show.getSlides().size());
        payload.put("message", "Slide duplicated");
        return success(payload);
    }

    public ToolCallResult deleteSlides(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        XMLSlideShow show = session.getSlideShow();
        List<XSLFSlide> slides = show.getSlides();
        List<Integer> indices = argumentValidator.parseShapeIndices(args.path("slide_indices"));
        if (indices.isEmpty()) {
            return error("slide_indices must contain at least one index");
        }

        int slideCount = slides.size();
        for (int idx : indices) {
            if (idx < 0 || idx >= slideCount) {
                return error("Invalid slide index in slide_indices: " + idx);
            }
        }

        boolean keepAtLeastOne = args.path("keep_at_least_one").asBoolean(true);
        int uniqueCount = new HashSet<>(indices).size();
        if (keepAtLeastOne && slideCount - uniqueCount < 1) {
            return error("Cannot delete all slides when keep_at_least_one is true");
        }

        List<Integer> sorted = new ArrayList<>(new HashSet<>(indices));
        sorted.sort((a, b) -> Integer.compare(b, a));
        for (int idx : sorted) {
            show.removeSlide(idx);
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("deleted_count", sorted.size());
        payload.put("remaining_slide_count", show.getSlides().size());
        payload.put("message", "Slides deleted");
        return success(payload);
    }

    public ToolCallResult updateText(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        String oldText = requiredString(args, "old_text");
        String newText = requiredString(args, "new_text");
        int occurrence = args.path("occurrence").asInt(1);
        if (occurrence < 1) {
            return error("occurrence must be >= 1");
        }

        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        int seen = 0;
        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }
            String current = PptSlideBuilder.visibleText(textShape);
            if (current.isEmpty()) {
                continue;
            }
            int idx = current.indexOf(oldText);
            while (idx >= 0) {
                seen++;
                if (seen == occurrence) {
                    String replaced = current.substring(0, idx) + newText + current.substring(idx + oldText.length());
                    textShape.clearText();
                    textShape.setText(replaced);
                    session.touch(true);

                    ObjectNode payload = okPayload();
                    payload.put("document_id", session.getId());
                    payload.put("slide_index", slideIndex);
                    payload.put("message", "Text updated");
                    payload.put("replaced_occurrence", occurrence);
                    return success(payload);
                }
                idx = current.indexOf(oldText, idx + oldText.length());
            }
        }

        return error("Could not find the requested text occurrence");
    }

    public ToolCallResult replaceTextGlobally(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        String oldText = requiredString(args, "old_text");
        if (oldText.isEmpty()) {
            return error("old_text must not be empty");
        }
        String newText = requiredString(args, "new_text");
        boolean caseSensitive = args.path("case_sensitive").asBoolean(false);
        int maxReplacements = args.path("max_replacements").asInt(Integer.MAX_VALUE);

        int replacementsCount = 0;
        int slidesAffected = 0;
        for (XSLFSlide slide : session.getSlideShow().getSlides()) {
            boolean slideModified = false;
            for (XSLFShape shape : slide.getShapes()) {
                if (!(shape instanceof XSLFTextShape textShape)) {
                    continue;
                }
                String text = PptSlideBuilder.visibleText(textShape);
                if (text.isEmpty()) {
                    continue;
                }
                int remaining = maxReplacements - replacementsCount;
                if (remaining <= 0) {
                    break;
                }

                ReplacementResult result = replaceOccurrences(text, oldText, newText, caseSensitive, remaining);
                if (result.count > 0) {
                    textShape.clearText();
                    textShape.setText(result.value);
                    replacementsCount += result.count;
                    slideModified = true;
                }
            }

            if (slideModified) {
                slidesAffected++;
            }
            if (replacementsCount >= maxReplacements) {
                break;
            }
        }

        if (replacementsCount > 0) {
            session.touch(true);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("replacements_count", replacementsCount);
        payload.put("slides_affected", slidesAffected);
        payload.put("case_sensitive", caseSensitive);
        payload.put("max_replacements", maxReplacements == Integer.MAX_VALUE ? -1 : maxReplacements);
        return success(payload);
    }

    public ToolCallResult addTextBox(JsonNode args) {
        ToolCallResult limit = limits.enforceShapeLimit(args);
        if (limit != null) {
            return limit;
        }
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        String text = requiredString(args, "text");
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!argumentValidator.isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        XSLFTextShape box = slide.createTextBox();
        box.setAnchor(new Rectangle2D.Double(x, y, width, height));
        // Split on \n so multi-line text becomes one paragraph per line — matches user
        // intuition from Word/Docs where pressing Enter starts a new paragraph. Reuse the
        // first paragraph POI creates with the shape; addNewTextParagraph would otherwise
        // leave a leading empty paragraph.
        Double fontSize = args.has("font_size") ? args.path("font_size").asDouble() : null;
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            var paragraph = i == 0 && !box.getTextParagraphs().isEmpty()
                    ? box.getTextParagraphs().get(0)
                    : box.addNewTextParagraph();
            XSLFTextRun run = paragraph.addNewTextRun();
            run.setText(lines[i]);
            if (fontSize != null) {
                run.setFontSize(fontSize);
            }
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("message", "Text box added");
        return success(payload);
    }

    public ToolCallResult getSlideNotes(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        XSLFNotes notes = slide.getNotes();
        XSLFTextShape bodyShape = notes == null ? null : findNotesBody(notes);
        String notesText = bodyShape == null ? "" : bodyShape.getText();
        if (notesText == null) {
            notesText = "";
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("notes_text", notesText);
        return success(payload);
    }

    public ToolCallResult setSlideNotes(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        String notesText = requiredString(args, "notes_text");
        // getNotesSlide is get-or-create: it lazily materializes a notes part (and the
        // notes master on first use) when the slide doesn't already have one. Without
        // this, freshly-created slides — which carry no notes until authored — would
        // error on the very first set_slide_notes call.
        XSLFNotes notes = session.getSlideShow().getNotesSlide(slide);
        if (notes == null) {
            return error("Could not initialize notes section for this slide");
        }

        // Target the BODY placeholder specifically. Notes slides created from the master
        // carry date / slide-number / footer placeholders pre-populated with master
        // prompts — writing into "the first text shape" would clobber the date placeholder
        // and leave the master's prompts visible through get_slide_notes.
        XSLFTextShape targetShape = findNotesBody(notes);
        if (targetShape == null) {
            targetShape = notes.createTextBox();
            targetShape.setAnchor(new Rectangle2D.Double(30, 30, 900, 120));
        }
        targetShape.clearText();
        targetShape.setText(notesText);

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("notes_text", notesText);
        payload.put("message", "Slide notes updated");
        return success(payload);
    }

    private static XSLFTextShape findNotesBody(XSLFNotes notes) {
        for (XSLFShape shape : notes.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }
            org.apache.poi.sl.usermodel.Placeholder ph = textShape.getTextType();
            if (ph == org.apache.poi.sl.usermodel.Placeholder.BODY) {
                return textShape;
            }
        }
        return null;
    }

    private ReplacementResult replaceOccurrences(
            String source,
            String target,
            String replacement,
            boolean caseSensitive,
            int maxReplacements) {
        if (source.isEmpty() || target.isEmpty() || maxReplacements <= 0) {
            return new ReplacementResult(source, 0);
        }

        String haystack = caseSensitive ? source : source.toLowerCase(Locale.ROOT);
        String needle = caseSensitive ? target : target.toLowerCase(Locale.ROOT);

        int from = 0;
        int count = 0;
        StringBuilder out = new StringBuilder(source.length());

        while (count < maxReplacements) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                break;
            }

            out.append(source, from, idx);
            out.append(replacement);
            from = idx + target.length();
            count++;
        }

        if (count == 0) {
            return new ReplacementResult(source, 0);
        }

        out.append(source.substring(from));
        return new ReplacementResult(out.toString(), count);
    }

    private static final class ReplacementResult {
        private final String value;
        private final int count;

        private ReplacementResult(String value, int count) {
            this.value = value;
            this.count = count;
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
