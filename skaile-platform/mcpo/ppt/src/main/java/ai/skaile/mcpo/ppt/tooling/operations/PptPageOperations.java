package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.ColorParser;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Dimension;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;

/**
 * Deck-wide and slide-wide metadata tools: page setup, slide layout swap,
 * slide background, and document metadata. All handlers mutate presentation
 * properties outside the individual shape graph.
 */
public final class PptPageOperations {

    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptShapeFinder shapeFinder;

    public PptPageOperations(
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptShapeFinder shapeFinder) {
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.shapeFinder = shapeFinder;
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of(
                "ppt.set_page_setup", this::setPageSetup,
                "ppt.set_slide_background", this::setSlideBackground,
                "ppt.set_slide_layout", this::setSlideLayout,
                "ppt.set_document_metadata", this::setDocumentMetadata);
    }

    ToolCallResult setPageSetup(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

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

    ToolCallResult setSlideBackground(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        String raw = requiredString(args, "color");
        Color color = ColorParser.parseHex(raw);
        slide.getBackground().setFillColor(color);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("color", raw);
        payload.put("message", "Slide background updated");
        return success(payload);
    }

    ToolCallResult setSlideLayout(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        String layoutType = args.path("layout_type").asText("");

        XMLSlideShow show = session.getSlideShow();
        XSLFSlide source = shapeFinder.requireSlide(session, slideIndex);
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

    ToolCallResult setDocumentMetadata(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

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
