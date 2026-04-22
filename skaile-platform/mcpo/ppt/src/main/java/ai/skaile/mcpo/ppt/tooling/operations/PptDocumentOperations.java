package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.session.SessionStore;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.PptSlideBuilder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

/**
 * Document-lifecycle tool handlers: open / close / create / list_slides / reorder,
 * plus metadata-free export, merge, transactions, and {@code generate_presentation}.
 * Template resolution is delegated to {@link PptTemplateOperations}.
 */
public final class PptDocumentOperations {

    /**
     * POI surfaces layout/master prompt text ("Click to edit Master title style",
     * "Click to add text", etc.) when a placeholder has no user content. The
     * outline export filters these so chat agents see slide content only.
     */
    private static final Pattern PLACEHOLDER_PROMPT =
            Pattern.compile("^Click to (edit|add) [^\\n]+$");

    private final SessionStore store;
    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptPathResolver pathResolver;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;
    private final PptTransactionManager transactions;
    private final PptTemplateOperations templates;
    private final SofficeRenderer sofficeRenderer;
    private final int maxOpenDocs;

    public PptDocumentOperations(
            SessionStore store,
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptPathResolver pathResolver,
            PptShapeFinder shapeFinder,
            PptLimits limits,
            PptTransactionManager transactions,
            PptTemplateOperations templates,
            SofficeRenderer sofficeRenderer,
            PptServerConfig config) {
        this.store = store;
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.pathResolver = pathResolver;
        this.shapeFinder = shapeFinder;
        this.limits = limits;
        this.transactions = transactions;
        this.templates = templates;
        this.sofficeRenderer = sofficeRenderer;
        this.maxOpenDocs = config.maxOpenDocs();
    }

    public Map<String, ToolHandler> handlers() {
        return Map.ofEntries(
                Map.entry("ppt.create_document", this::createDocument),
                Map.entry("ppt.open_document", this::openDocument),
                Map.entry("ppt.close_document", this::closeDocument),
                Map.entry("ppt.get_document_info", this::getDocumentInfo),
                Map.entry("ppt.list_slides", this::listSlides),
                Map.entry("ppt.reorder_slides", this::reorderSlides),
                Map.entry("ppt.export_document", this::exportDocument),
                Map.entry("ppt.generate_presentation", this::generatePresentation),
                Map.entry("ppt.merge_presentations", this::mergePresentations),
                Map.entry("ppt.transaction_begin", this::transactionBegin),
                Map.entry("ppt.transaction_commit", this::transactionCommit),
                Map.entry("ppt.transaction_rollback", this::transactionRollback));
    }

    public ToolCallResult createDocument(JsonNode args) {
        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        String title = args.path("title").asText("");
        XMLSlideShow show = templates.loadTemplateOrBlank(args.path("template_path").asText(""));
        XSLFSlide slide = PptSlideBuilder.ensureFirstSlide(show);
        if (!title.isBlank()) {
            PptSlideBuilder.setSlideTitle(slide, title);
        }

        PptDocumentSession session = store.create(show);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("template_path",
                templates.currentTemplatePathAsString(args.path("template_path").asText("")));
        payload.put("message", "Created new presentation in memory");
        return success(payload);
    }

    public ToolCallResult openDocument(JsonNode args) throws IOException {
        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        String pathRaw = requiredString(args, "path");
        Path path = pathResolver.resolvePath(pathRaw, false);
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

    public ToolCallResult closeDocument(JsonNode args) throws IOException {
        String id = requiredString(args, "document_id");
        boolean closed = store.close(id);
        if (!closed) {
            return error("Unknown document_id: " + id);
        }
        transactions.clear(id);

        ObjectNode payload = okPayload();
        payload.put("document_id", id);
        payload.put("message", "Document closed");
        return success(payload);
    }

    public ToolCallResult getDocumentInfo(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

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

    public ToolCallResult listSlides(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        ArrayNode slides = payload.putArray("slides");

        List<XSLFSlide> allSlides = session.getSlideShow().getSlides();
        for (int i = 0; i < allSlides.size(); i++) {
            XSLFSlide slide = allSlides.get(i);
            ObjectNode entry = slides.addObject();
            entry.put("slide_index", i);
            String text = PptSlideBuilder.collectSlideText(slide);
            entry.put("text_preview", text.length() > 240 ? text.substring(0, 240) + "..." : text);
            entry.put("shape_count", slide.getShapes().size());
        }

        return success(payload);
    }

    public ToolCallResult reorderSlides(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

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

    public ToolCallResult mergePresentations(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        Path mergePath = pathResolver.resolvePath(requiredString(args, "merge_path"), false);
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

    public ToolCallResult exportDocument(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        String format = args.path("format").asText("pptx").toLowerCase(Locale.ROOT);
        boolean isBatch = "png_batch".equals(format) || "jpg_batch".equals(format)
                || "svg_batch".equals(format);

        switch (format) {
            case "pptx":
            case "pdf":
            case "html":
            case "png_batch":
            case "jpg_batch":
            case "svg_batch":
            case "outline_text":
                break;
            default:
                return error("VALIDATION_ERROR",
                        "format must be one of: pptx, pdf, html, png_batch, jpg_batch, svg_batch, outline_text",
                        false);
        }

        Path outputPath = resolveExportOutputPath(args, session, format, isBatch);
        if (outputPath == null) {
            return error("output_path is required for unsaved documents");
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("format", format);

        try {
            switch (format) {
                case "pptx" -> exportPptx(session, outputPath);
                case "pdf" -> sofficeRenderer.exportWholeDeck(
                        session.getSlideShow(), outputPath, "pdf", "pdf");
                case "html" -> sofficeRenderer.exportWholeDeck(
                        session.getSlideShow(), outputPath, "html", "html");
                case "png_batch" -> exportBatch(session, payload, outputPath, "png", "png");
                case "jpg_batch" -> exportBatch(session, payload, outputPath, "jpg", "jpg");
                case "svg_batch" -> exportBatch(session, payload, outputPath, "svg", "svg");
                case "outline_text" -> exportOutlineText(session, outputPath);
                default -> throw new IllegalStateException("unreachable format=" + format);
            }
        } catch (SofficeRenderer.SofficeUnavailableException ex) {
            return error("SOFFICE_UNAVAILABLE", ex.getMessage(), false);
        }

        if (isBatch) {
            payload.put("output_dir", outputPath.toString());
        } else {
            payload.put("output_path", outputPath.toString());
        }
        payload.put("message", "Document exported");
        return success(payload);
    }

    private Path resolveExportOutputPath(JsonNode args, PptDocumentSession session,
            String format, boolean isBatch) {
        boolean hasOutputPath = args.has("output_path")
                && !args.path("output_path").asText().isBlank();
        if (hasOutputPath) {
            Path resolved = pathResolver.resolvePath(args.path("output_path").asText(), true);
            if (isBatch && Files.exists(resolved) && !Files.isDirectory(resolved)) {
                throw new IllegalArgumentException(
                        "For " + format + " output_path must be a directory, not a file: "
                                + resolved);
            }
            return resolved;
        }
        if ("pptx".equals(format) && session.getSourcePath() != null) {
            return session.getSourcePath();
        }
        return null;
    }

    private void exportPptx(PptDocumentSession session, Path outputPath) throws IOException {
        pathResolver.createParentDirectories(outputPath);
        try (FileOutputStream out = new FileOutputStream(outputPath.toFile())) {
            session.getSlideShow().write(out);
        }
        session.setSourcePath(outputPath);
        session.setDirty(false);
    }

    private void exportBatch(PptDocumentSession session, ObjectNode payload, Path outputDir,
            String sofficeFormat, String extension) throws IOException {
        List<Path> files = sofficeRenderer.renderAllSlides(
                session.getSlideShow(), outputDir, sofficeFormat, extension, "slide-%03d");
        ArrayNode filesNode = payload.putArray("files");
        for (Path file : files) {
            filesNode.add(file.toString());
        }
        payload.put("slide_count", files.size());
    }

    private void exportOutlineText(PptDocumentSession session, Path outputPath) throws IOException {
        pathResolver.createParentDirectories(outputPath);
        String content = buildOutlineText(session.getSlideShow());
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile());
                Writer writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
    }

    /**
     * Deterministic markdown-style outline: first text run of each slide becomes a
     * top-level heading; subsequent paragraphs become bullets. Designed for
     * chat-agent consumption, not round-tripping.
     *
     * <p>POI returns inherited layout/master prompt text ("Click to edit Master title
     * style", etc.) from empty placeholders via {@code paragraph.getText()}. We
     * filter those by requiring each emitted paragraph to carry at least one
     * direct text run — inherited-only paragraphs have empty {@code getTextRuns()}.
     */
    static String buildOutlineText(XMLSlideShow show) {
        StringBuilder buf = new StringBuilder();
        List<XSLFSlide> slides = show.getSlides();
        for (int i = 0; i < slides.size(); i++) {
            XSLFSlide slide = slides.get(i);
            String title = null;
            List<String> bullets = new ArrayList<>();
            for (XSLFShape shape : slide.getShapes()) {
                if (!(shape instanceof XSLFTextShape textShape)) {
                    continue;
                }
                for (XSLFTextParagraph paragraph : textShape.getTextParagraphs()) {
                    String text = paragraph.getText();
                    if (text == null) {
                        continue;
                    }
                    String trimmed = text.strip();
                    if (trimmed.isEmpty() || PLACEHOLDER_PROMPT.matcher(trimmed).matches()) {
                        continue;
                    }
                    if (title == null) {
                        title = trimmed;
                    } else {
                        bullets.add(trimmed);
                    }
                }
            }
            if (title == null && bullets.isEmpty()) {
                buf.append("# Slide ").append(i + 1).append('\n').append('\n');
                continue;
            }
            buf.append("# ").append(title == null ? "Slide " + (i + 1) : title).append('\n');
            buf.append('\n');
            for (String bullet : bullets) {
                buf.append("- ").append(bullet).append('\n');
            }
            buf.append('\n');
        }
        return buf.toString();
    }

    public ToolCallResult generatePresentation(JsonNode args) throws IOException {
        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        String templateArg = args.path("template_path").asText("");
        XMLSlideShow show = templates.loadTemplateOrBlank(templateArg);
        XSLFSlide firstSlide = PptSlideBuilder.ensureFirstSlide(show);

        String title = args.path("title").asText("").strip();
        if (!title.isBlank()) {
            PptSlideBuilder.setSlideTitle(firstSlide, title);
        }

        List<String> slideTitles = argumentValidator.parseSlideTitles(args.path("slide_titles"));
        if (!slideTitles.isEmpty()) {
            if (slideTitles.size() > PptLimits.MAX_SLIDES) {
                return error("LIMIT_MAX_SLIDES",
                        "slide_titles exceeds the " + PptLimits.MAX_SLIDES + "-slide limit",
                        false);
            }
            PptSlideBuilder.setSlideTitle(firstSlide, slideTitles.get(0));
            for (int i = 1; i < slideTitles.size(); i++) {
                XSLFSlide slide = PptSlideBuilder.createDefaultSlide(show);
                PptSlideBuilder.setSlideTitle(slide, slideTitles.get(i));
            }
        }

        PptDocumentSession session = store.create(show);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", show.getSlides().size());
        payload.put("template_path", templates.currentTemplatePathAsString(templateArg));

        if (args.has("output_path") && !args.path("output_path").asText("").isBlank()) {
            Path outputPath = pathResolver.resolvePath(args.path("output_path").asText(), true);
            pathResolver.createParentDirectories(outputPath);
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

    public ToolCallResult transactionBegin(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        transactions.begin(session);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("message", "Transaction snapshot created");
        return success(payload);
    }

    public ToolCallResult transactionCommit(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        transactions.commit(session);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("message", "Transaction committed");
        return success(payload);
    }

    public ToolCallResult transactionRollback(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        if (!transactions.rollback(session)) {
            return error("NO_ACTIVE_TRANSACTION",
                    "No active transaction snapshot for document", false);
        }

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_count", session.getSlideShow().getSlides().size());
        payload.put("message", "Transaction rolled back");
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

    private ToolCallResult error(String code, String message, boolean retriable) {
        return responseFactory.error(code, message, retriable);
    }
}
