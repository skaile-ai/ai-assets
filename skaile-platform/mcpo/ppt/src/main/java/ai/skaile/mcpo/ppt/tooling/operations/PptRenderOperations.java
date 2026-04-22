package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.PptSlideBuilder;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * Slide rendering, text search, and deck-metric tools. Rasterizes via Java2D +
 * Batik SVG for {@code fidelity=low}; delegates to {@link SofficeRenderer} for
 * {@code fidelity=high}.
 */
public final class PptRenderOperations {

    private static final String SVG_NS = "http://www.w3.org/2000/svg";

    private final ObjectMapper mapper;
    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptPathResolver pathResolver;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;
    private final SofficeRenderer sofficeRenderer;
    private final Path allowedRoot;

    public PptRenderOperations(
            ObjectMapper mapper,
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptPathResolver pathResolver,
            PptShapeFinder shapeFinder,
            PptLimits limits,
            SofficeRenderer sofficeRenderer) {
        this.mapper = mapper;
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.pathResolver = pathResolver;
        this.shapeFinder = shapeFinder;
        this.limits = limits;
        this.sofficeRenderer = sofficeRenderer;
        this.allowedRoot = pathResolver.allowedRoot();
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of(
                "ppt.render_slide", this::renderSlide,
                "ppt.render_all_slides", this::renderAllSlides,
                "ppt.find_text", this::findText,
                "ppt.get_slide_metrics", this::getSlideMetrics);
    }

    public ToolCallResult renderSlide(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        String format = normalizeRenderFormat(args.path("format").asText("png"));
        if (format == null) {
            return error("VALIDATION_ERROR", "format must be one of: png, jpg, svg", false);
        }
        String fidelity = args.path("fidelity").asText("low").toLowerCase(Locale.ROOT);
        if (!"low".equals(fidelity) && !"high".equals(fidelity)) {
            return error("VALIDATION_ERROR", "fidelity must be one of: low, high", false);
        }

        Path outputPath = pathResolver.resolvePath(requiredString(args, "output_path"), true);
        pathResolver.createParentDirectories(outputPath);

        Dimension pageSize = session.getSlideShow().getPageSize();
        int width = args.path("width").asInt(pageSize.width);
        int height = args.path("height").asInt(pageSize.height);
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }
        ToolCallResult renderLimit = limits.enforceRenderDimensionLimit(width, height);
        if (renderLimit != null) {
            return renderLimit;
        }

        if ("high".equals(fidelity)) {
            try {
                sofficeRenderer.renderSingleSlide(
                        session.getSlideShow(), slideIndex, outputPath, format, format);
            } catch (SofficeRenderer.SofficeUnavailableException ex) {
                return error("SOFFICE_UNAVAILABLE", ex.getMessage(), false);
            }
        } else if ("svg".equals(format)) {
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

    public ToolCallResult renderAllSlides(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        String format = normalizeRenderFormat(args.path("format").asText("png"));
        if (format == null) {
            return error("VALIDATION_ERROR", "format must be one of: png, jpg, svg", false);
        }
        String fidelity = args.path("fidelity").asText("low").toLowerCase(Locale.ROOT);
        if (!"low".equals(fidelity) && !"high".equals(fidelity)) {
            return error("VALIDATION_ERROR", "fidelity must be one of: low, high", false);
        }

        Path outputDir = pathResolver.resolvePath(requiredString(args, "output_dir"), true);
        java.nio.file.Files.createDirectories(outputDir);

        String pattern = args.path("file_name_pattern").asText("slide-%03d");
        Dimension pageSize = session.getSlideShow().getPageSize();
        int width = args.path("width").asInt(pageSize.width);
        int height = args.path("height").asInt(pageSize.height);
        if (width < 1 || height < 1) {
            return error("width and height must be >= 1");
        }
        ToolCallResult renderLimit = limits.enforceRenderDimensionLimit(width, height);
        if (renderLimit != null) {
            return renderLimit;
        }

        List<XSLFSlide> slides = session.getSlideShow().getSlides();
        ArrayNode files = mapper.createArrayNode();

        if ("high".equals(fidelity)) {
            try {
                List<Path> produced = sofficeRenderer.renderAllSlides(
                        session.getSlideShow(), outputDir, format, format, pattern);
                for (Path p : produced) {
                    if (allowedRoot != null && !p.startsWith(allowedRoot)) {
                        return error("Output path is outside allowed root: " + p);
                    }
                    files.add(p.toString());
                }
            } catch (SofficeRenderer.SofficeUnavailableException ex) {
                return error("SOFFICE_UNAVAILABLE", ex.getMessage(), false);
            }
        } else {
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

    public ToolCallResult findText(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

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
                String text = PptSlideBuilder.visibleText(textShape);
                if (text.isBlank()) {
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

    public ToolCallResult getSlideMetrics(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

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
                // Count text-bearing shapes regardless of placeholder status (an empty
                // title placeholder still counts as a text shape), but exclude POI's
                // inherited "Click to edit..." prompts from the character / word stats
                // so get_slide_metrics measures authored content, not master prompts.
                textShapeCount++;
                String text = textShape.getText();
                if (text != null && !text.isBlank() && !PptSlideBuilder.isPlaceholderPrompt(text)) {
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
        // JPEG has no alpha channel — ImageIO.write silently returns false for an ARGB
        // BufferedImage and produces a zero-byte file. Use TYPE_INT_RGB for jpg/jpeg so
        // the write path picks up a real JPEGImageWriter.
        boolean opaque = "jpg".equalsIgnoreCase(format) || "jpeg".equalsIgnoreCase(format);
        BufferedImage image = new BufferedImage(width, height,
                opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.scale(width / (double) pageSize.width, height / (double) pageSize.height);
        slide.draw(graphics);
        graphics.dispose();
        if (!javax.imageio.ImageIO.write(image, format, outputPath.toFile())) {
            throw new IOException("No ImageIO writer available for format: " + format);
        }
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
