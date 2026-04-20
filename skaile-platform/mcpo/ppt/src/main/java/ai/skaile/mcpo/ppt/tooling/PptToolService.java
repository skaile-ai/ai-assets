package ai.skaile.mcpo.ppt.tooling;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.session.SessionStore;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolDefinition;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import ai.skaile.mcpo.ppt.tooling.operations.PptDocumentOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptAdvancedMutationOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptRenderService;
import ai.skaile.mcpo.ppt.tooling.operations.PptSlideOperations;
import ai.skaile.mcpo.ppt.tooling.operations.PptTemplateService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

public final class PptToolService {
    private static final Logger LOG = LoggerFactory.getLogger(PptToolService.class);
    private static final String SERVER_VERSION = "0.1.0";
    private static final String SVG_NS = "http://www.w3.org/2000/svg";
    private static final int DEFAULT_MAX_OPEN_DOCS = 100;
    private static final long SOFFICE_TIMEOUT_SECONDS = 90;
    private static final String DEFAULT_TEMPLATE_CONFIG = ".mcpo-ppt-default-template.json";

    // Safety limits surfaced via ppt.capabilities and enforced in Phase 1.6.
    static final int LIMIT_MAX_SLIDES = 2000;
    static final int LIMIT_MAX_SHAPES_PER_SLIDE = 500;
    static final long LIMIT_MAX_IMAGE_BYTES = 50L * 1024 * 1024;
    static final int LIMIT_MAX_RENDER_DIMENSION = 10_000;
    private final ObjectMapper mapper = new ObjectMapper();
    private final SessionStore store = new SessionStore();
    private final List<ToolDefinition> tools;
    private final Map<String, ToolDefinition> toolsByName;
    private final Map<String, ToolHandler> toolHandlers;
    private final int maxOpenDocs;
    private final Path allowedRoot;
    private final Path templatesDir;
    private final Path defaultTemplateConfigPath;
    private final ToolResponseFactory responseFactory;
    private final ToolArgumentValidator argumentValidator;
    private final PptPathResolver pathResolver;
    private final PptDocumentOperations documentOperations;
    private final PptSlideOperations slideOperations;
    private final PptAdvancedMutationOperations advancedMutationOperations;
    private final PptRenderService renderService;
    private final PptTemplateService templateService;
    private final Map<String, TransactionSnapshot> transactionSnapshots = new HashMap<>();
    private Path defaultTemplatePath;

    public PptToolService() {
        this.maxOpenDocs = parseMaxOpenDocs();
        this.allowedRoot = parseAllowedRoot();
        this.pathResolver = new PptPathResolver(this.allowedRoot);
        this.responseFactory = new ToolResponseFactory(mapper);
        this.argumentValidator = new ToolArgumentValidator(mapper);
        this.templatesDir = parseTemplatesDir();
        this.defaultTemplateConfigPath = parseDefaultTemplateConfigPath();
        this.defaultTemplatePath = loadDefaultTemplatePath();
        tools = PptToolDefinitions.create(mapper);
        this.toolsByName = new HashMap<>();
        for (ToolDefinition definition : tools) {
            toolsByName.put(definition.name(), definition);
        }
        this.documentOperations = new PptDocumentOperations(
            this::createDocument,
            this::openDocument,
            this::closeDocument,
            this::getDocumentInfo,
            this::listSlides,
            this::reorderSlides,
            this::exportDocument,
            this::generatePresentation,
            this::mergePresentations,
            this::transactionBegin,
            this::transactionCommit,
            this::transactionRollback);
        this.slideOperations = new PptSlideOperations(
            this::getSlideContent,
            this::addSlide,
            this::duplicateSlide,
            this::deleteSlides,
            this::updateText,
            this::replaceTextGlobally,
            this::addTextBox,
            this::getSlideNotes,
            this::setSlideNotes,
            this::addTable,
            this::getTable,
            this::editTable,
            this::setText,
            this::moveShape,
            this::cloneShape,
            this::resizeShape,
            this::addHyperlink,
            this::setSlideBackground);
        this.advancedMutationOperations = new PptAdvancedMutationOperations(
            store,
            mapper,
            argumentValidator,
            responseFactory,
            pathResolver);
        this.renderService = new PptRenderService(
            this::renderSlide,
            this::renderAllSlides,
            this::findText,
            this::getSlideMetrics);
        this.templateService = new PptTemplateService(
            this::insertImage,
            this::uploadTemplate,
            this::setDefaultTemplate,
            this::getDefaultTemplate,
            this::importMarkdownOutline);
        this.toolHandlers = createToolHandlers();
    }

    public List<ToolDefinition> listTools() {
        return tools;
    }

    public int closeAllSessions() {
        int closed = store.closeAll();
        try {
            pathResolver.cleanSandboxTmpDir();
        } catch (IOException ex) {
            LOG.warn("Failed to clean sandbox tmp dir on shutdown: {}", ex.toString(), ex);
        }
        return closed;
    }

    public ToolCallResult call(String name, JsonNode arguments) {
        try {
            ToolDefinition definition = toolsByName.get(name);
            if (definition == null) {
                return error("Unknown tool: " + name);
            }
            JsonNode safeArguments = normalizeArguments(arguments);
            validateArguments(definition, safeArguments);
            ToolHandler handler = toolHandlers.get(name);
            if (handler == null) {
                return error("Unknown tool: " + name);
            }
            return invokeWithSessionLock(safeArguments, handler);
        } catch (IllegalArgumentException e) {
            return error("VALIDATION_ERROR", e.getMessage(), false);
        } catch (Exception e) {
            return error("TOOL_EXECUTION_ERROR", "Tool execution failed: " + e.getMessage(), false);
        }
    }

    /**
     * POI's {@code XMLSlideShow} DOM is not thread-safe even for read-only traversal, so every
     * tool invocation that targets an existing session must serialize against that session's
     * lock. Tools without a {@code document_id} argument (document creation, templating, etc.)
     * bypass the lock — they either create a new session (already serialized inside the store)
     * or touch only process-wide template state.
     */
    private ToolCallResult invokeWithSessionLock(JsonNode arguments, ToolHandler handler)
            throws Exception {
        JsonNode idNode = arguments.path("document_id");
        if (!idNode.isTextual() || idNode.asText().isBlank()) {
            return handler.handle(arguments);
        }
        Optional<PptDocumentSession> maybe = store.get(idNode.asText());
        if (maybe.isEmpty()) {
            // Let the handler surface the "unknown document_id" error in its own format.
            return handler.handle(arguments);
        }
        ReentrantLock lock = maybe.get().getLock();
        lock.lock();
        try {
            return handler.handle(arguments);
        } finally {
            lock.unlock();
        }
    }

    private Map<String, ToolHandler> createToolHandlers() {
        Map<String, ToolHandler> handlers = new HashMap<>();
        handlers.put("ppt.create_document", documentOperations::createDocument);
        handlers.put("ppt.open_document", documentOperations::openDocument);
        handlers.put("ppt.close_document", documentOperations::closeDocument);
        handlers.put("ppt.get_document_info", documentOperations::getDocumentInfo);
        handlers.put("ppt.set_page_setup", this::setPageSetup);
        handlers.put("ppt.list_slides", documentOperations::listSlides);
        handlers.put("ppt.reorder_slides", documentOperations::reorderSlides);
        handlers.put("ppt.export_document", documentOperations::exportDocument);
        handlers.put("ppt.generate_presentation", documentOperations::generatePresentation);
        handlers.put("ppt.merge_presentations", documentOperations::mergePresentations);
        handlers.put("ppt.transaction_begin", documentOperations::transactionBegin);
        handlers.put("ppt.transaction_commit", documentOperations::transactionCommit);
        handlers.put("ppt.transaction_rollback", documentOperations::transactionRollback);

        handlers.put("ppt.get_slide_content", slideOperations::getSlideContent);
        handlers.put("ppt.add_slide", slideOperations::addSlide);
        handlers.put("ppt.duplicate_slide", slideOperations::duplicateSlide);
        handlers.put("ppt.delete_slides", slideOperations::deleteSlides);
        handlers.put("ppt.update_text", slideOperations::updateText);
        handlers.put("ppt.replace_text_globally", slideOperations::replaceTextGlobally);
        handlers.put("ppt.add_textbox", slideOperations::addTextBox);
        handlers.put("ppt.add_shape", this::addShape);
        handlers.put("ppt.get_slide_notes", slideOperations::getSlideNotes);
        handlers.put("ppt.set_slide_notes", slideOperations::setSlideNotes);
        handlers.put("ppt.add_table", slideOperations::addTable);
        handlers.put("ppt.get_table", slideOperations::getTable);
        handlers.put("ppt.edit_table", slideOperations::editTable);
        handlers.put("ppt.set_text", slideOperations::setText);
        handlers.put("ppt.move_shape", slideOperations::moveShape);
        handlers.put("ppt.clone_shape", slideOperations::cloneShape);
        handlers.put("ppt.resize_shape", slideOperations::resizeShape);
        handlers.put("ppt.add_hyperlink", slideOperations::addHyperlink);
        handlers.put("ppt.set_slide_background", slideOperations::setSlideBackground);

        handlers.put("ppt.render_slide", renderService::renderSlide);
        handlers.put("ppt.render_all_slides", renderService::renderAllSlides);
        handlers.put("ppt.find_text", renderService::findText);
        handlers.put("ppt.get_slide_metrics", renderService::getSlideMetrics);

        handlers.put("ppt.insert_image", templateService::insertImage);
        handlers.put("ppt.replace_image", this::replaceImage);
        handlers.put("ppt.upload_template", templateService::uploadTemplate);
        handlers.put("ppt.set_default_template", templateService::setDefaultTemplate);
        handlers.put("ppt.get_default_template", templateService::getDefaultTemplate);
        handlers.put("ppt.import_markdown_outline", templateService::importMarkdownOutline);

        handlers.put("ppt.delete_shape", this::deleteShape);
        handlers.put("ppt.get_shape_properties", this::getShapeProperties);
        handlers.put("ppt.set_shape_style", this::setShapeStyle);
        handlers.put("ppt.set_document_metadata", this::setDocumentMetadata);
        handlers.put("ppt.set_slide_layout", this::setSlideLayout);
        handlers.put("ppt.set_shape_z_order", this::setShapeZOrder);
        handlers.put("ppt.capabilities", this::capabilities);
        return handlers;
    }

    private ToolCallResult createDocument(JsonNode args) {
        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        String title = args.path("title").asText("");
        XMLSlideShow show = loadTemplateOrBlank(args.path("template_path").asText(""));
        XSLFSlide slide = ensureFirstSlide(show);
        if (!title.isBlank()) {
            setSlideTitle(slide, title);
        }

        PptDocumentSession session = store.create(show);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("template_path", currentTemplatePathAsString(args.path("template_path").asText("")));
        payload.put("message", "Created new presentation in memory");
        return success(payload);
    }

    private ToolCallResult openDocument(JsonNode args) throws IOException {
        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        String pathRaw = requiredString(args, "path");
        Path path = resolvePath(pathRaw, false);
        if (!Files.exists(path)) {
            return error("File does not exist: " + path);
        }

        XMLSlideShow show;
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            show = new XMLSlideShow(in);
        }

        PptDocumentSession session = store.create(show);
        session.setSourcePath(path);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("source_path", path.toString());
        payload.put("slide_count", show.getSlides().size());
        payload.put("message", "Opened presentation in memory");
        return success(payload);
    }

    private ToolCallResult closeDocument(JsonNode args) throws IOException {
        String id = requiredString(args, "document_id");
        boolean closed = store.close(id);
        if (!closed) {
            return error("Unknown document_id: " + id);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", id);
        payload.put("message", "Document closed");
        return success(payload);
    }

    private ToolCallResult getDocumentInfo(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        XMLSlideShow show = session.getSlideShow();
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("dirty", session.isDirty());
        payload.put("opened_at", session.getOpenedAt().toString());
        payload.put("updated_at", session.getUpdatedAt().toString());
        payload.put("source_path", session.getSourcePath() == null ? "" : session.getSourcePath().toString());
        Dimension pageSize = show.getPageSize();
        payload.put("page_width", pageSize.width);
        payload.put("page_height", pageSize.height);
        return success(payload);
    }

    private ToolCallResult setPageSetup(JsonNode args) {
        return advancedMutationOperations.setPageSetup(args);
    }

    private ToolCallResult listSlides(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        ArrayNode slides = payload.putArray("slides");

        List<XSLFSlide> allSlides = session.getSlideShow().getSlides();
        for (int i = 0; i < allSlides.size(); i++) {
            XSLFSlide slide = allSlides.get(i);
            ObjectNode entry = slides.addObject();
            entry.put("slide_index", i);
            String text = collectSlideText(slide);
            entry.put("text_preview", text.length() > 240 ? text.substring(0, 240) + "..." : text);
            entry.put("shape_count", slide.getShapes().size());
        }

        return success(payload);
    }

    private ToolCallResult getSlideContent(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("text", collectSlideText(slide));

        ArrayNode shapes = payload.putArray("shapes");
        int i = 0;
        for (XSLFShape shape : slide.getShapes()) {
            ObjectNode shapeNode = shapes.addObject();
            shapeNode.put("shape_index", i++);
            shapeNode.put("shape_type", shape.getClass().getSimpleName());
            if (shape instanceof XSLFTextShape textShape) {
                shapeNode.put("text", textShape.getText());
            }
            if (shape instanceof XSLFTable table) {
                shapeNode.put("rows", table.getNumberOfRows());
                shapeNode.put("cols", table.getNumberOfColumns());
            }
        }

        return success(payload);
    }

    private ToolCallResult reorderSlides(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        JsonNode newOrderNode = args.path("new_order");
        if (!newOrderNode.isArray() || newOrderNode.isEmpty()) {
            return error("new_order must be a non-empty array");
        }

        XMLSlideShow show = session.getSlideShow();
        int slideCount = show.getSlides().size();
        if (newOrderNode.size() != slideCount) {
            return error("new_order must contain exactly " + slideCount + " indices");
        }

        List<XSLFSlide> orderedSlides = new ArrayList<>(slideCount);
        HashSet<Integer> seen = new HashSet<>();
        for (JsonNode indexNode : newOrderNode) {
            if (!indexNode.isInt()) {
                return error("new_order must only contain integer indices");
            }

            int idx = indexNode.asInt(-1);
            if (idx < 0 || idx >= slideCount) {
                return error("new_order contains invalid slide index: " + idx);
            }
            if (!seen.add(idx)) {
                return error("new_order contains duplicate slide index: " + idx);
            }

            orderedSlides.add(show.getSlides().get(idx));
        }

        for (int i = 0; i < orderedSlides.size(); i++) {
            show.setSlideOrder(orderedSlides.get(i), i);
        }
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", slideCount);
        payload.set("new_order", newOrderNode.deepCopy());
        payload.put("message", "Slides reordered");
        return success(payload);
    }

    private ToolCallResult addSlide(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        ToolCallResult limit = enforceSlideLimit(session);
        if (limit != null) {
            return limit;
        }

        String title = args.path("title").asText("");
        XSLFSlide slide = createDefaultSlide(session.getSlideShow());
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

    private ToolCallResult duplicateSlide(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        ToolCallResult limit = enforceSlideLimit(session);
        if (limit != null) {
            return limit;
        }

        XMLSlideShow show = session.getSlideShow();
        int sourceSlideIndex = args.path("source_slide_index").asInt(-1);
        XSLFSlide source = getSlideByIndex(show, sourceSlideIndex);
        if (source == null) {
            return error("Invalid source_slide_index: " + sourceSlideIndex);
        }

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

    private ToolCallResult deleteSlides(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        XMLSlideShow show = session.getSlideShow();
        List<XSLFSlide> slides = show.getSlides();
        List<Integer> indices = parseShapeIndices(args.path("slide_indices"));
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

    private ToolCallResult mergePresentations(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        Path mergePath = resolvePath(requiredString(args, "merge_path"), false);
        if (!Files.exists(mergePath)) {
            return error("merge_path does not exist: " + mergePath);
        }

        XMLSlideShow target = session.getSlideShow();
        int initialCount = target.getSlides().size();
        int insertAt = args.has("insert_at_index")
                ? args.path("insert_at_index").asInt(target.getSlides().size())
                : target.getSlides().size();
        if (insertAt < 0 || insertAt > target.getSlides().size()) {
            return error("Invalid insert_at_index: " + insertAt);
        }

        int merged = 0;
        try (FileInputStream in = new FileInputStream(mergePath.toFile()); XMLSlideShow source = new XMLSlideShow(in)) {
            for (XSLFSlide sourceSlide : source.getSlides()) {
                XSLFSlide newSlide = target.createSlide();
                newSlide.importContent(sourceSlide);
                target.setSlideOrder(newSlide, insertAt + merged);
                merged++;
            }
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("merged_slide_count", merged);
        payload.put("slide_count", initialCount + merged);
        payload.put("insert_at_index", insertAt);
        payload.put("merge_path", mergePath.toString());
        payload.put("message", "Presentations merged");
        return success(payload);
    }

    private ToolCallResult updateText(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        String oldText = requiredString(args, "old_text");
        String newText = requiredString(args, "new_text");
        int occurrence = args.path("occurrence").asInt(1);
        if (occurrence < 1) {
            return error("occurrence must be >= 1");
        }

        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        int seen = 0;
        for (XSLFShape shape : slide.getShapes()) {
            if (!(shape instanceof XSLFTextShape textShape)) {
                continue;
            }
            String current = textShape.getText();
            if (current == null || current.isEmpty()) {
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

    private ToolCallResult replaceTextGlobally(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

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
                String text = textShape.getText();
                if (text == null || text.isEmpty()) {
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

    private ToolCallResult addTextBox(JsonNode args) {
        ToolCallResult limit = enforceShapeLimit(args);
        if (limit != null) {
            return limit;
        }
        return advancedMutationOperations.addTextBox(args);
    }

    private ToolCallResult addShape(JsonNode args) {
        ToolCallResult limit = enforceShapeLimit(args);
        if (limit != null) {
            return limit;
        }
        return advancedMutationOperations.addShape(args);
    }

    private ToolCallResult insertImage(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        ToolCallResult shapeLimit = enforceShapeLimitForSlide(slide);
        if (shapeLimit != null) {
            return shapeLimit;
        }

        Path imagePath = resolvePath(requiredString(args, "image_path"), false);
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        long imageSize = Files.size(imagePath);
        ToolCallResult imageLimit = enforceImageBytesLimit(imageSize);
        if (imageLimit != null) {
            return imageLimit;
        }

        PictureData.PictureType pictureType = inferPictureType(imagePath);
        byte[] imageBytes = Files.readAllBytes(imagePath);
        XSLFPictureData pictureData = session.getSlideShow().addPicture(imageBytes, pictureType);
        XSLFPictureShape picture = slide.createPicture(pictureData);
        picture.setAnchor(new Rectangle2D.Double(x, y, width, height));

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", slide.getShapes().size() - 1);
        payload.put("image_path", imagePath.toString());
        payload.put("message", "Image inserted");
        return success(payload);
    }

    private ToolCallResult replaceImage(JsonNode args) throws IOException {
        // replace_image swaps an existing picture without changing shape count, so no shape
        // limit check. Image-byte limit still applies.
        JsonNode raw = args.path("image_path");
        if (raw.isTextual() && !raw.asText().isBlank()) {
            Path imagePath = resolvePath(raw.asText(), false);
            ToolCallResult imageLimit = enforceImageBytesLimit(Files.size(imagePath));
            if (imageLimit != null) {
                return imageLimit;
            }
        }
        return advancedMutationOperations.replaceImage(args);
    }

    private ToolCallResult getSlideNotes(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        XSLFNotes notes = slide.getNotes();
        String notesText = notes == null ? "" : collectSlideText(notes);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("notes_text", notesText);
        return success(payload);
    }

    private ToolCallResult setSlideNotes(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        String notesText = requiredString(args, "notes_text");
        XSLFNotes notes = slide.getNotes();
        if (notes == null) {
            return error("Slide does not have a notes section");
        }

        XSLFTextShape targetShape = null;
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                targetShape = textShape;
                break;
            }
        }
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

    private ToolCallResult addTable(JsonNode args) {
        ToolCallResult limit = enforceShapeLimit(args);
        if (limit != null) {
            return limit;
        }
        return advancedMutationOperations.addTable(args);
    }

    private ToolCallResult getTable(JsonNode args) {
        return advancedMutationOperations.getTable(args);
    }

    private ToolCallResult editTable(JsonNode args) {
        return advancedMutationOperations.editTable(args);
    }

    private ToolCallResult setText(JsonNode args) {
        return advancedMutationOperations.setText(args);
    }

    private ToolCallResult moveShape(JsonNode args) {
        return advancedMutationOperations.moveShape(args);
    }

    private ToolCallResult cloneShape(JsonNode args) {
        ToolCallResult limit = enforceShapeLimit(args);
        if (limit != null) {
            return limit;
        }
        return advancedMutationOperations.cloneShape(args);
    }

    private ToolCallResult resizeShape(JsonNode args) {
        return advancedMutationOperations.resizeShape(args);
    }

    private ToolCallResult addHyperlink(JsonNode args) {
        return advancedMutationOperations.addHyperlink(args);
    }

    private ToolCallResult setSlideBackground(JsonNode args) {
        return advancedMutationOperations.setSlideBackground(args);
    }

    private ToolCallResult importMarkdownOutline(JsonNode args) throws IOException {
        String markdown = requiredString(args, "markdown_text");
        if (markdown.isBlank()) {
            return error("markdown_text must not be blank");
        }

        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        XMLSlideShow show = loadTemplateOrBlank("");
        List<String> lines = Arrays.asList(markdown.split("\\r?\\n"));
        XSLFSlide current = null;
        StringBuilder body = new StringBuilder();
        int createdSlides = 0;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.startsWith("# ")) {
                if (current != null && body.length() > 0) {
                    addBodyText(current, body.toString());
                    body.setLength(0);
                }
                current = createDefaultSlide(show);
                setSlideTitle(current, line.substring(2).strip());
                createdSlides++;
            } else if (line.startsWith("- ") || line.startsWith("* ")) {
                if (body.length() > 0) {
                    body.append("\n");
                }
                body.append("• ").append(line.substring(2).strip());
            } else if (!line.isBlank()) {
                if (body.length() > 0) {
                    body.append("\n");
                }
                body.append(line);
            }
        }

        if (current != null && body.length() > 0) {
            addBodyText(current, body.toString());
        }
        if (createdSlides == 0) {
            current = ensureFirstSlide(show);
            setSlideTitle(current, "Imported Outline");
            addBodyText(current, markdown.strip());
            createdSlides = 1;
        }

        PptDocumentSession session = store.create(show);
        session.touch(true);

        if (args.has("output_path") && !args.path("output_path").asText().isBlank()) {
            Path outputPath = resolvePath(args.path("output_path").asText(), true);
            createParentDirectories(outputPath);
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                show.write(out);
            }
            session.setSourcePath(outputPath);
            session.setDirty(false);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("message", "Presentation imported from markdown outline");
        return success(payload);
    }

    private ToolCallResult transactionBegin(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        byte[] snapshot = serializeShow(session.getSlideShow());
        transactionSnapshots.put(session.getId(), new TransactionSnapshot(snapshot, session.isDirty(), session.getSourcePath()));

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("message", "Transaction snapshot created");
        return success(payload);
    }

    private ToolCallResult transactionCommit(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        transactionSnapshots.remove(session.getId());
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("message", "Transaction committed");
        return success(payload);
    }

    private ToolCallResult transactionRollback(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        TransactionSnapshot snapshot = transactionSnapshots.get(session.getId());
        if (snapshot == null) {
            return error("No active transaction snapshot for document");
        }

        try (XMLSlideShow rollbackShow = new XMLSlideShow(new ByteArrayInputStream(snapshot.data))) {
            restoreShow(session.getSlideShow(), rollbackShow);
        }
        session.setDirty(snapshot.dirty);
        session.setSourcePath(snapshot.sourcePath);
        transactionSnapshots.remove(session.getId());

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", session.getSlideShow().getSlides().size());
        payload.put("message", "Transaction rolled back");
        return success(payload);
    }

    private ToolCallResult getSlideMetrics(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        int shapeCount = slide.getShapes().size();
        int imageCount = 0;
        int tableCount = 0;
        int tableCellCount = 0;
        int textChars = 0;
        int wordCount = 0;
        int textShapeCount = 0;

        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFPictureShape) {
                imageCount++;
            }
            if (shape instanceof XSLFTable table) {
                tableCount++;
                for (XSLFTableRow row : table.getRows()) {
                    tableCellCount += row.getCells().size();
                }
            }
            if (shape instanceof XSLFTextShape textShape) {
                textShapeCount++;
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    String normalized = text.trim().replaceAll("\\s+", " ");
                    textChars += normalized.length();
                    wordCount += normalized.split(" ").length;
                }
            }
        }

        double avgWordLength = wordCount == 0 ? 0.0 : ((double) textChars / wordCount);
        int complexityScore = (int) Math.max(1, Math.min(10,
                Math.round((shapeCount * 0.4) + (wordCount * 0.05) + (tableCount * 1.5) + (imageCount * 0.8))));

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_count", shapeCount);
        payload.put("text_shape_count", textShapeCount);
        payload.put("image_count", imageCount);
        payload.put("table_count", tableCount);
        payload.put("table_cells", tableCellCount);
        payload.put("text_chars", textChars);
        payload.put("word_count", wordCount);
        payload.put("avg_word_length", avgWordLength);
        payload.put("complexity_score", complexityScore);
        return success(payload);
    }

    private ToolCallResult exportDocument(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String format = args.path("format").asText("pptx").toLowerCase(Locale.ROOT);
        switch (format) {
            case "pptx":
            case "pdf":
                break;
            case "html":
            case "png_batch":
            case "jpg_batch":
            case "svg_batch":
            case "outline_text":
                return error("FORMAT_NOT_YET_IMPLEMENTED",
                        "Export format '" + format + "' lands in Phase 2.",
                        false);
            default:
                return error("VALIDATION_ERROR",
                        "format must be one of: pptx, pdf, html, png_batch, jpg_batch, svg_batch, outline_text",
                        false);
        }

        Path outputPath;
        if (args.has("output_path") && !args.path("output_path").asText().isBlank()) {
            outputPath = resolvePath(args.path("output_path").asText(), true);
        } else if (session.getSourcePath() != null && "pptx".equals(format)) {
            outputPath = session.getSourcePath();
        } else {
            return error("output_path is required for unsaved documents");
        }

        createParentDirectories(outputPath);

        if ("pptx".equals(format)) {
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                session.getSlideShow().write(out);
            }
            session.setSourcePath(outputPath);
            session.setDirty(false);
        } else {
            SofficeAvailability soffice = SofficeAvailability.get();
            if (!soffice.available()) {
                return error("SOFFICE_UNAVAILABLE",
                        "LibreOffice (soffice) is not available on this host: " + soffice.error(),
                        false);
            }
            Path tempPptx = pathResolver.createSandboxTempFile("mcpo-export-", ".pptx");
            try {
                try (FileOutputStream out = new FileOutputStream(tempPptx.toFile())) {
                    session.getSlideShow().write(out);
                }
                exportPdfWithSoffice(tempPptx, outputPath);
            } finally {
                Files.deleteIfExists(tempPptx);
            }
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("output_path", outputPath.toString());
        payload.put("format", format);
        payload.put("message", "Document exported");
        return success(payload);
    }

    private ToolCallResult renderSlide(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return error("Invalid slide_index: " + slideIndex);
        }

        String format = normalizeRenderFormat(args.path("format").asText("png"));
        if (format == null) {
            return error("VALIDATION_ERROR", "format must be one of: png, jpg, svg", false);
        }
        String fidelity = args.path("fidelity").asText("low").toLowerCase(Locale.ROOT);
        if (!"low".equals(fidelity) && !"high".equals(fidelity)) {
            return error("VALIDATION_ERROR", "fidelity must be one of: low, high", false);
        }
        if ("high".equals(fidelity)) {
            return error("FORMAT_NOT_YET_IMPLEMENTED",
                    "High-fidelity render lands in Phase 2.", false);
        }

        Path outputPath = resolvePath(requiredString(args, "output_path"), true);
        createParentDirectories(outputPath);

        Dimension pageSize = session.getSlideShow().getPageSize();
        int width = args.path("width").asInt(pageSize.width);
        int height = args.path("height").asInt(pageSize.height);
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }
        ToolCallResult renderLimit = enforceRenderDimensionLimit(width, height);
        if (renderLimit != null) {
            return renderLimit;
        }

        if ("svg".equals(format)) {
            renderSlideToSvg(slide, outputPath, width, height, pageSize);
        } else {
            renderSlideToRaster(slide, outputPath, width, height, pageSize, format);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("output_path", outputPath.toString());
        payload.put("format", format);
        payload.put("fidelity", fidelity);
        payload.put("message", "Slide rendered");
        return success(payload);
    }

    private ToolCallResult renderAllSlides(JsonNode args) throws IOException {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String format = normalizeRenderFormat(args.path("format").asText("png"));
        if (format == null) {
            return error("VALIDATION_ERROR", "format must be one of: png, jpg, svg", false);
        }
        String fidelity = args.path("fidelity").asText("low").toLowerCase(Locale.ROOT);
        if (!"low".equals(fidelity) && !"high".equals(fidelity)) {
            return error("VALIDATION_ERROR", "fidelity must be one of: low, high", false);
        }
        if ("high".equals(fidelity)) {
            return error("FORMAT_NOT_YET_IMPLEMENTED",
                    "High-fidelity render lands in Phase 2.", false);
        }

        Path outputDir = resolvePath(requiredString(args, "output_dir"), true);
        Files.createDirectories(outputDir);

        String pattern = args.path("file_name_pattern").asText("slide-%03d");
        Dimension pageSize = session.getSlideShow().getPageSize();
        int width = args.path("width").asInt(pageSize.width);
        int height = args.path("height").asInt(pageSize.height);
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }
        ToolCallResult renderLimit = enforceRenderDimensionLimit(width, height);
        if (renderLimit != null) {
            return renderLimit;
        }

        ArrayNode files = mapper.createArrayNode();
        List<XSLFSlide> slides = session.getSlideShow().getSlides();
        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            String fileName = String.format(Locale.ROOT, pattern, i + 1);
            if (!fileName.toLowerCase(Locale.ROOT).endsWith("." + format)) {
                fileName = fileName + "." + format;
            }
            Path outputPath = outputDir.resolve(fileName).toAbsolutePath().normalize();
            if (allowedRoot != null && !outputPath.startsWith(allowedRoot)) {
                return error("Output path is outside allowed root: " + outputPath);
            }
            if ("svg".equals(format)) {
                renderSlideToSvg(slide, outputPath, width, height, pageSize);
            } else {
                renderSlideToRaster(slide, outputPath, width, height, pageSize, format);
            }
            files.add(outputPath.toString());
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", slides.size());
        payload.put("format", format);
        payload.put("fidelity", fidelity);
        payload.set("files", files);
        payload.put("message", "All slides rendered");
        return success(payload);
    }

    private String normalizeRenderFormat(String raw) {
        String lower = raw == null ? "" : raw.toLowerCase(Locale.ROOT);
        if ("jpeg".equals(lower)) {
            return "jpg";
        }
        if ("png".equals(lower) || "jpg".equals(lower) || "svg".equals(lower)) {
            return lower;
        }
        return null;
    }

    private void renderSlideToRaster(XSLFSlide slide, Path outputPath, int width, int height,
            Dimension pageSize, String format) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.scale(width / (double) pageSize.width, height / (double) pageSize.height);
        slide.draw(graphics);
        graphics.dispose();
        javax.imageio.ImageIO.write(image, format, outputPath.toFile());
    }

    private void renderSlideToSvg(XSLFSlide slide, Path outputPath, int width, int height,
            Dimension pageSize) throws IOException {
        DOMImplementation domImplementation = GenericDOMImplementation.getDOMImplementation();
        Document document = domImplementation.createDocument(SVG_NS, "svg", null);
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
        svgGenerator.setSVGCanvasSize(new Dimension(width, height));
        svgGenerator.scale(width / (double) pageSize.width, height / (double) pageSize.height);
        slide.draw(svgGenerator);
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(outputPath.toFile()),
                StandardCharsets.UTF_8)) {
            svgGenerator.stream(writer, true);
        }
    }

    private ToolCallResult findText(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return error("Unknown document_id");
        }

        String query = requiredString(args, "query");
        boolean caseSensitive = args.path("case_sensitive").asBoolean(false);
        String normalizedQuery = caseSensitive ? query : query.toLowerCase(Locale.ROOT);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("query", query);
        ArrayNode matches = payload.putArray("matches");

        List<XSLFSlide> slides = session.getSlideShow().getSlides();
        for (int slideIndex = 0; slideIndex < slides.size(); slideIndex++) {
            XSLFSlide slide = slides.get(slideIndex);
            List<XSLFShape> shapes = slide.getShapes();
            for (int shapeIndex = 0; shapeIndex < shapes.size(); shapeIndex++) {
                XSLFShape shape = shapes.get(shapeIndex);
                if (!(shape instanceof XSLFTextShape textShape)) {
                    continue;
                }
                String text = textShape.getText();
                if (text == null || text.isBlank()) {
                    continue;
                }

                String haystack = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
                int from = 0;
                while (true) {
                    int idx = haystack.indexOf(normalizedQuery, from);
                    if (idx < 0) {
                        break;
                    }
                    ObjectNode match = matches.addObject();
                    match.put("slide_index", slideIndex);
                    match.put("shape_index", shapeIndex);
                    match.put("start", idx);
                    match.put("end", idx + query.length());
                    match.put("text", text);
                    from = idx + Math.max(1, query.length());
                }
            }
        }

        payload.put("count", matches.size());
        return success(payload);
    }

    private ToolCallResult uploadTemplate(JsonNode args) throws IOException {
        Path source = resolvePath(requiredString(args, "source_path"), false);
        String extension = extensionOf(source.getFileName().toString());
        if (!("pptx".equals(extension) || "potx".equals(extension))) {
            return error("Template must be a .pptx or .potx file");
        }

        Files.createDirectories(templatesDir);
        String preferredName = args.path("template_name").asText("").strip();
        if (preferredName.isBlank()) {
            preferredName = source.getFileName().toString();
        }
        String safeName = sanitizeTemplateName(preferredName, extension);

        Path target = templatesDir.resolve(safeName).toAbsolutePath().normalize();
        if (!target.startsWith(templatesDir)) {
            return error("Template target path escapes template directory");
        }
        if (allowedRoot != null && !target.startsWith(allowedRoot)) {
            return error("Template target path is outside allowed root: " + target);
        }

        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        boolean makeDefault = args.path("make_default").asBoolean(true);
        if (makeDefault) {
            setDefaultTemplateInternal(target);
        }

        ObjectNode payload = okPayload();
        payload.put("template_path", target.toString());
        payload.put("default_template_path", defaultTemplatePath == null ? "" : defaultTemplatePath.toString());
        payload.put("message", makeDefault ? "Template uploaded and set as default" : "Template uploaded");
        return success(payload);
    }

    private ToolCallResult setDefaultTemplate(JsonNode args) throws IOException {
        Path templatePath = resolvePath(requiredString(args, "template_path"), false);
        String extension = extensionOf(templatePath.getFileName().toString());
        if (!("pptx".equals(extension) || "potx".equals(extension))) {
            return error("Default template must be a .pptx or .potx file");
        }

        setDefaultTemplateInternal(templatePath);

        ObjectNode payload = okPayload();
        payload.put("default_template_path", defaultTemplatePath.toString());
        payload.put("message", "Default template updated");
        return success(payload);
    }

    private ToolCallResult getDefaultTemplate(JsonNode args) {
        ObjectNode payload = okPayload();
        payload.put("default_template_path", defaultTemplatePath == null ? "" : defaultTemplatePath.toString());
        payload.put("has_default_template", defaultTemplatePath != null);
        return success(payload);
    }

    private ToolCallResult generatePresentation(JsonNode args) throws IOException {
        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        String templateArg = args.path("template_path").asText("");
        XMLSlideShow show = loadTemplateOrBlank(templateArg);
        XSLFSlide firstSlide = ensureFirstSlide(show);

        String title = args.path("title").asText("").strip();
        if (!title.isBlank()) {
            setSlideTitle(firstSlide, title);
        }

        List<String> slideTitles = parseSlideTitles(args.path("slide_titles"));
        if (!slideTitles.isEmpty()) {
            if (slideTitles.size() > LIMIT_MAX_SLIDES) {
                return error("LIMIT_MAX_SLIDES",
                        "slide_titles exceeds the " + LIMIT_MAX_SLIDES + "-slide limit",
                        false);
            }
            setSlideTitle(firstSlide, slideTitles.get(0));
            for (int i = 1; i < slideTitles.size(); i++) {
                XSLFSlide slide = createDefaultSlide(show);
                setSlideTitle(slide, slideTitles.get(i));
            }
        }

        PptDocumentSession session = store.create(show);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("template_path", currentTemplatePathAsString(templateArg));

        if (args.has("output_path") && !args.path("output_path").asText("").isBlank()) {
            Path outputPath = resolvePath(args.path("output_path").asText(), true);
            createParentDirectories(outputPath);
            try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
                show.write(out);
            }
            session.setSourcePath(outputPath);
            session.setDirty(false);
            payload.put("output_path", outputPath.toString());
        }

        payload.put("message", "Presentation generated");
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

    private JsonNode normalizeArguments(JsonNode arguments) {
        return argumentValidator.normalizeArguments(arguments);
    }

    private void validateArguments(ToolDefinition definition, JsonNode arguments) {
        argumentValidator.validateArguments(definition, arguments);
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

    private String inferImageFormat(Path path) {
        return pathResolver.inferImageFormat(path);
    }

    private PictureData.PictureType inferPictureType(Path imagePath) {
        return pathResolver.inferPictureType(imagePath);
    }

    private Path resolvePath(String rawPath, boolean forWrite) {
        return pathResolver.resolvePath(rawPath, forWrite);
    }

    private void createParentDirectories(Path path) throws IOException {
        pathResolver.createParentDirectories(path);
    }

    private XMLSlideShow loadTemplateOrBlank(String requestedTemplatePath) {
        String candidate = requestedTemplatePath == null ? "" : requestedTemplatePath.strip();
        Path templateToUse = null;

        if (!candidate.isBlank()) {
            templateToUse = resolvePath(candidate, false);
        } else if (defaultTemplatePath != null) {
            templateToUse = defaultTemplatePath;
        }

        if (templateToUse == null) {
            return new XMLSlideShow();
        }

        try (FileInputStream in = new FileInputStream(templateToUse.toFile())) {
            return new XMLSlideShow(in);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Failed to load template: " + templateToUse + " (" + e.getMessage() + ")", e);
        }
    }

    private XSLFSlide ensureFirstSlide(XMLSlideShow show) {
        if (show.getSlides().isEmpty()) {
            return createDefaultSlide(show);
        }
        return show.getSlides().get(0);
    }

    private void setSlideTitle(XSLFSlide slide, String title) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String existing = textShape.getText();
                if (existing != null && !existing.isBlank()) {
                    textShape.clearText();
                    textShape.setText(title);
                    return;
                }
            }
        }

        XSLFTextShape titleShape = slide.createTextBox();
        titleShape.setAnchor(new Rectangle2D.Double(40, 30, 840, 80));
        titleShape.setText(title);
    }

    private void addBodyText(XSLFSlide slide, String text) {
        XSLFTextShape body = slide.createTextBox();
        body.setAnchor(new Rectangle2D.Double(50, 120, 860, 380));
        body.setText(text);
    }

    private List<String> parseSlideTitles(JsonNode node) {
        return argumentValidator.parseSlideTitles(node);
    }

    private String extensionOf(String fileName) {
        return pathResolver.extensionOf(fileName);
    }

    private String currentTemplatePathAsString(String requestedTemplatePath) {
        String candidate = requestedTemplatePath == null ? "" : requestedTemplatePath.strip();
        if (!candidate.isBlank()) {
            return resolvePath(candidate, false).toString();
        }
        return defaultTemplatePath == null ? "" : defaultTemplatePath.toString();
    }

    private Path parseTemplatesDir() {
        String raw = System.getenv("MCPO_TEMPLATE_DIR");
        Path root;
        if (raw == null || raw.isBlank()) {
            root = Path.of(System.getProperty("user.home"), ".mcpo-ppt", "templates");
        } else {
            root = Path.of(raw);
        }
        Path normalized = root.toAbsolutePath().normalize();
        if (allowedRoot != null && !normalized.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Template directory is outside allowed root: " + normalized);
        }
        return normalized;
    }

    private Path parseDefaultTemplateConfigPath() {
        String raw = System.getenv("MCPO_DEFAULT_TEMPLATE_CONFIG");
        Path resolved;
        if (raw == null || raw.isBlank()) {
            if (allowedRoot != null) {
                resolved = allowedRoot.resolve(DEFAULT_TEMPLATE_CONFIG).toAbsolutePath().normalize();
            } else {
                resolved = Path.of(System.getProperty("user.home"), DEFAULT_TEMPLATE_CONFIG).toAbsolutePath()
                        .normalize();
            }
        } else {
            resolved = Path.of(raw).toAbsolutePath().normalize();
        }
        if (allowedRoot != null && !resolved.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Default template config path is outside allowed root: " + resolved);
        }
        return resolved;
    }

    private Path loadDefaultTemplatePath() {
        try {
            if (!Files.exists(defaultTemplateConfigPath)) {
                return null;
            }
            JsonNode root = mapper.readTree(Files.readString(defaultTemplateConfigPath, StandardCharsets.UTF_8));
            String value = root.path("default_template_path").asText("").strip();
            if (value.isBlank()) {
                return null;
            }
            Path template = Path.of(value).toAbsolutePath().normalize();
            if (!Files.exists(template)) {
                LOG.warn("Default template config at {} points to missing file {}",
                        defaultTemplateConfigPath, template);
                return null;
            }
            if (allowedRoot != null && !template.startsWith(allowedRoot)) {
                LOG.warn("Default template {} lies outside allowed root {}", template, allowedRoot);
                return null;
            }
            return template;
        } catch (Exception ex) {
            LOG.warn("Failed to load default template config at {}: {}",
                    defaultTemplateConfigPath, ex.toString(), ex);
            return null;
        }
    }

    private void setDefaultTemplateInternal(Path templatePath) throws IOException {
        Path normalized = templatePath.toAbsolutePath().normalize();
        if (!Files.exists(normalized)) {
            throw new IllegalArgumentException("Template path does not exist: " + normalized);
        }
        if (allowedRoot != null && !normalized.startsWith(allowedRoot)) {
            throw new IllegalArgumentException("Template path is outside allowed root: " + normalized);
        }
        this.defaultTemplatePath = normalized;

        ObjectNode root = mapper.createObjectNode();
        root.put("default_template_path", normalized.toString());
        createParentDirectories(defaultTemplateConfigPath);
        Files.writeString(defaultTemplateConfigPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);
    }

    private int parseMaxOpenDocs() {
        String raw = System.getenv("MCPO_MAX_OPEN_DOCS");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_MAX_OPEN_DOCS;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : DEFAULT_MAX_OPEN_DOCS;
        } catch (NumberFormatException ex) {
            return DEFAULT_MAX_OPEN_DOCS;
        }
    }

    private Path parseAllowedRoot() {
        String raw = System.getenv("MCPO_ALLOWED_ROOT");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Path.of(raw).toAbsolutePath().normalize();
    }

    private void exportPdfWithSoffice(Path inputPptx, Path outputPdf) throws IOException {
        String sofficeExecutable = System.getenv("SOFFICE_PATH");
        if (sofficeExecutable == null || sofficeExecutable.isBlank()) {
            sofficeExecutable = "soffice";
        }

        Path outputDir = outputPdf.getParent();
        if (outputDir == null) {
            outputDir = Path.of(".").toAbsolutePath().normalize();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        sofficeExecutable,
                        "--headless",
                        "--convert-to",
                        "pdf",
                        "--outdir",
                        outputDir.toString(),
                        inputPptx.toString()));
        // Capture both streams so stderr is surfaced on failure instead of being discarded.
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Drain combined stdout+stderr concurrently so the child never blocks on a full pipe
        // buffer, and so we can include the tail in an error message if the conversion fails.
        StringBuilder capturedOutput = new StringBuilder();
        Thread drain = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (capturedOutput) {
                        if (capturedOutput.length() < 8192) {
                            if (capturedOutput.length() > 0) {
                                capturedOutput.append('\n');
                            }
                            capturedOutput.append(line);
                        }
                    }
                }
            } catch (IOException ignored) {
                // Process exit races with stream close; ignore.
            }
        }, "soffice-output-drain");
        drain.setDaemon(true);
        drain.start();

        boolean finished;
        try {
            finished = process.waitFor(SOFFICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("PDF export interrupted", e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new IOException("PDF export timed out waiting for LibreOffice after "
                    + SOFFICE_TIMEOUT_SECONDS + "s");
        }

        try {
            drain.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if (process.exitValue() != 0) {
            String output;
            synchronized (capturedOutput) {
                output = capturedOutput.toString().strip();
            }
            throw new IOException("LibreOffice PDF export failed with exit code "
                    + process.exitValue()
                    + (output.isEmpty() ? "" : ": " + output));
        }

        String baseName = inputPptx.getFileName().toString();
        int dot = baseName.lastIndexOf('.');
        String pdfName = (dot > 0 ? baseName.substring(0, dot) : baseName) + ".pdf";
        Path generatedPdf = outputDir.resolve(pdfName).toAbsolutePath().normalize();
        if (!Files.exists(generatedPdf)) {
            throw new IOException("Expected PDF output was not created: " + generatedPdf);
        }

        if (!generatedPdf.equals(outputPdf)) {
            Files.move(generatedPdf, outputPdf, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private List<Integer> parseShapeIndices(JsonNode node) {
        return argumentValidator.parseShapeIndices(node);
    }

    private String sanitizeTemplateName(String templateName, String extension) {
        return pathResolver.sanitizeTemplateName(templateName, extension);
    }

    private String collectSlideText(XSLFSlide slide) {
        List<String> textParts = new ArrayList<>();
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    textParts.add(text.strip());
                }
            }
        }
        return String.join("\n", textParts);
    }

    private String collectSlideText(XSLFNotes notes) {
        List<String> textParts = new ArrayList<>();
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    textParts.add(text.strip());
                }
            }
        }
        return String.join("\n", textParts);
    }

    private XSLFSlide createDefaultSlide(XMLSlideShow show) {
        if (!show.getSlideMasters().isEmpty()) {
            XSLFSlideMaster master = show.getSlideMasters().get(0);
            XSLFSlideLayout layout = master.getLayout(SlideLayoutResolver.bestEffortLayout(master));
            if (layout != null) {
                return show.createSlide(layout);
            }
        }
        return show.createSlide();
    }

    private boolean isValidRect(double x, double y, double w, double h) {
        return argumentValidator.isValidRect(x, y, w, h);
    }

    private byte[] serializeShow(XMLSlideShow show) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        show.write(out);
        return out.toByteArray();
    }

    private ToolCallResult deleteShape(JsonNode args) {
        return advancedMutationOperations.deleteShape(args);
    }

    private ToolCallResult getShapeProperties(JsonNode args) {
        return advancedMutationOperations.getShapeProperties(args);
    }

    private ToolCallResult setShapeStyle(JsonNode args) {
        return advancedMutationOperations.setShapeStyle(args);
    }

    private ToolCallResult setDocumentMetadata(JsonNode args) {
        return advancedMutationOperations.setDocumentMetadata(args);
    }

    private ToolCallResult setSlideLayout(JsonNode args) {
        return advancedMutationOperations.setSlideLayout(args);
    }

    private ToolCallResult setShapeZOrder(JsonNode args) {
        return advancedMutationOperations.setShapeZOrder(args);
    }

    private ToolCallResult capabilities(JsonNode args) {
        SofficeAvailability soffice = SofficeAvailability.get();
        ObjectNode payload = okPayload();
        payload.put("server_version", SERVER_VERSION);
        payload.put("poi_version", resolvePoiVersion());
        payload.put("soffice_available", soffice.available());
        if (soffice.available() && soffice.version() != null && !soffice.version().isBlank()) {
            payload.put("soffice_version", soffice.version());
        }
        payload.put("java_version", System.getProperty("java.version", "unknown"));

        ArrayNode inputFormats = payload.putArray("supported_input_formats");
        inputFormats.add("pptx");
        inputFormats.add("pptm");

        ArrayNode exportFormats = payload.putArray("supported_export_formats");
        for (String fmt : new String[] {
                "pptx", "pdf", "html", "png_batch", "jpg_batch", "svg_batch", "outline_text"}) {
            exportFormats.add(fmt);
        }

        ArrayNode renderFormats = payload.putArray("supported_render_formats");
        renderFormats.add("png");
        renderFormats.add("jpg");
        renderFormats.add("svg");

        ArrayNode fonts = payload.putArray("installed_fonts");
        for (String family : getInstalledFontFamilies()) {
            fonts.add(family);
        }

        ObjectNode flags = payload.putObject("feature_flags");
        // Phase 1: every advanced feature flag is still false. They flip in Phases 2–4.
        flags.put("charts_update", false);
        flags.put("high_fidelity_render", false);
        flags.put("gradients", false);
        flags.put("picture_effects", false);
        flags.put("table_borders", false);
        flags.put("table_merge", false);

        ObjectNode limits = payload.putObject("limits");
        limits.put("max_open_docs", maxOpenDocs);
        limits.put("max_slides_per_deck", LIMIT_MAX_SLIDES);
        limits.put("max_shapes_per_slide", LIMIT_MAX_SHAPES_PER_SLIDE);
        limits.put("max_image_bytes", LIMIT_MAX_IMAGE_BYTES);
        limits.put("max_render_dimension", LIMIT_MAX_RENDER_DIMENSION);

        return success(payload);
    }

    private ToolCallResult enforceSlideLimit(PptDocumentSession session) {
        int current = session.getSlideShow().getSlides().size();
        if (current >= LIMIT_MAX_SLIDES) {
            return error("LIMIT_MAX_SLIDES",
                    "Slide count limit reached (" + LIMIT_MAX_SLIDES + ")", false);
        }
        return null;
    }

    private ToolCallResult enforceShapeLimit(JsonNode args) {
        PptDocumentSession session = requireSession(args);
        if (session == null) {
            return null;
        }
        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = getSlideByIndex(session.getSlideShow(), slideIndex);
        if (slide == null) {
            return null;
        }
        return enforceShapeLimitForSlide(slide);
    }

    private ToolCallResult enforceShapeLimitForSlide(XSLFSlide slide) {
        if (slide.getShapes().size() >= LIMIT_MAX_SHAPES_PER_SLIDE) {
            return error("LIMIT_MAX_SHAPES",
                    "Slide has reached the shape limit (" + LIMIT_MAX_SHAPES_PER_SLIDE + ")",
                    false);
        }
        return null;
    }

    private ToolCallResult enforceImageBytesLimit(long imageBytes) {
        if (imageBytes > LIMIT_MAX_IMAGE_BYTES) {
            return error("LIMIT_MAX_IMAGE_BYTES",
                    "Image exceeds the " + LIMIT_MAX_IMAGE_BYTES + "-byte limit: " + imageBytes,
                    false);
        }
        return null;
    }

    private ToolCallResult enforceRenderDimensionLimit(int width, int height) {
        if (width > LIMIT_MAX_RENDER_DIMENSION || height > LIMIT_MAX_RENDER_DIMENSION) {
            return error("LIMIT_MAX_RENDER_DIMENSION",
                    "Render dimensions exceed " + LIMIT_MAX_RENDER_DIMENSION + "px: "
                            + width + "x" + height,
                    false);
        }
        return null;
    }

    private String resolvePoiVersion() {
        String v = org.apache.poi.Version.getVersion();
        return v == null || v.isBlank() ? "unknown" : v;
    }

    private static volatile List<String> cachedFontFamilies;

    private List<String> getInstalledFontFamilies() {
        List<String> snapshot = cachedFontFamilies;
        if (snapshot != null) {
            return snapshot;
        }
        synchronized (PptToolService.class) {
            if (cachedFontFamilies != null) {
                return cachedFontFamilies;
            }
            cachedFontFamilies = probeFontFamilies();
            return cachedFontFamilies;
        }
    }

    private List<String> probeFontFamilies() {
        // fc-list is the lowest-dependency way to learn what fonts the runtime has available.
        // On hosts without fontconfig (macOS dev machines that didn't install it) we fall
        // back to the JDK's local graphics environment — less accurate but never throws.
        try {
            Process process = new ProcessBuilder("fc-list", ":family")
                    .redirectErrorStream(true)
                    .start();
            List<String> families = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && families.size() < 50) {
                    String first = line.split(",", 2)[0].strip();
                    if (!first.isBlank() && !families.contains(first)) {
                        families.add(first);
                    }
                }
            }
            boolean finished = process.waitFor(3, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
            } else if (process.exitValue() == 0 && !families.isEmpty()) {
                return List.copyOf(families);
            }
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOG.debug("fc-list probe failed, falling back to AWT: {}", ex.toString());
        }
        try {
            String[] awt = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getAvailableFontFamilyNames();
            List<String> families = new ArrayList<>();
            for (int i = 0; i < awt.length && families.size() < 50; i++) {
                families.add(awt[i]);
            }
            return List.copyOf(families);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void restoreShow(XMLSlideShow target, XMLSlideShow source) {
        for (int i = target.getSlides().size() - 1; i >= 0; i--) {
            target.removeSlide(i);
        }

        target.setPageSize(source.getPageSize());
        for (XSLFSlide sourceSlide : source.getSlides()) {
            XSLFSlide newSlide = target.createSlide();
            newSlide.importContent(sourceSlide);
        }
    }

    private static final class SlideLayoutResolver {
        private SlideLayoutResolver() {
        }

        static org.apache.poi.xslf.usermodel.SlideLayout bestEffortLayout(XSLFSlideMaster master) {
            org.apache.poi.xslf.usermodel.SlideLayout[] preferred = new org.apache.poi.xslf.usermodel.SlideLayout[] {
                    org.apache.poi.xslf.usermodel.SlideLayout.TITLE,
                    org.apache.poi.xslf.usermodel.SlideLayout.TITLE_AND_CONTENT,
                    org.apache.poi.xslf.usermodel.SlideLayout.BLANK
            };
            for (org.apache.poi.xslf.usermodel.SlideLayout layout : preferred) {
                if (master.getLayout(layout) != null) {
                    return layout;
                }
            }
            return null;
        }
    }

    private static final class TransactionSnapshot {
        private final byte[] data;
        private final boolean dirty;
        private final Path sourcePath;

        private TransactionSnapshot(byte[] data, boolean dirty, Path sourcePath) {
            this.data = data;
            this.dirty = dirty;
            this.sourcePath = sourcePath;
        }
    }
}
