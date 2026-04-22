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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.geom.Rectangle2D;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Template persistence + image insertion + markdown-outline import. Also owns the
 * {@code defaultTemplatePath} mutable state — document-producing handlers resolve
 * their template through {@link #loadTemplateOrBlank(String)} so the default
 * template pointer lives in a single place.
 */
public final class PptTemplateOperations {
    private static final Logger LOG = LoggerFactory.getLogger(PptTemplateOperations.class);

    private final SessionStore store;
    private final ObjectMapper mapper;
    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptPathResolver pathResolver;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;
    private final Path templatesDir;
    private final Path defaultTemplateConfigPath;
    private final Path allowedRoot;
    private final int maxOpenDocs;

    private Path defaultTemplatePath;

    public PptTemplateOperations(
            SessionStore store,
            ObjectMapper mapper,
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptPathResolver pathResolver,
            PptShapeFinder shapeFinder,
            PptServerConfig config,
            PptLimits limits) {
        this.store = store;
        this.mapper = mapper;
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.pathResolver = pathResolver;
        this.shapeFinder = shapeFinder;
        this.limits = limits;
        this.templatesDir = config.templatesDir();
        this.defaultTemplateConfigPath = config.defaultTemplateConfigPath();
        this.allowedRoot = config.allowedRoot();
        this.maxOpenDocs = config.maxOpenDocs();
        this.defaultTemplatePath = loadDefaultTemplatePath();
    }

    public Map<String, ToolHandler> handlers() {
        return Map.of(
                "ppt.insert_image", this::insertImage,
                "ppt.upload_template", this::uploadTemplate,
                "ppt.set_default_template", this::setDefaultTemplate,
                "ppt.get_default_template", this::getDefaultTemplate,
                "ppt.import_markdown_outline", this::importMarkdownOutline);
    }

    public Path defaultTemplatePath() {
        return defaultTemplatePath;
    }

    /**
     * Resolve the effective template path for document creation. Returns a
     * freshly-opened {@link XMLSlideShow} — either loaded from the requested
     * template path, loaded from the default template, or a blank presentation.
     */
    public XMLSlideShow loadTemplateOrBlank(String requestedTemplatePath) {
        String candidate = requestedTemplatePath == null ? "" : requestedTemplatePath.strip();
        Path templateToUse = null;

        if (!candidate.isBlank()) {
            templateToUse = pathResolver.resolvePath(candidate, false);
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

    public String currentTemplatePathAsString(String requestedTemplatePath) {
        String candidate = requestedTemplatePath == null ? "" : requestedTemplatePath.strip();
        if (!candidate.isBlank()) {
            return pathResolver.resolvePath(candidate, false).toString();
        }
        return defaultTemplatePath == null ? "" : defaultTemplatePath.toString();
    }

    public ToolCallResult insertImage(JsonNode args) throws IOException {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        ToolCallResult shapeLimit = limits.enforceShapeLimitForSlide(slide);
        if (shapeLimit != null) {
            return shapeLimit;
        }

        Path imagePath = pathResolver.resolvePath(requiredString(args, "image_path"), false);
        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!argumentValidator.isValidRect(x, y, width, height)) {
            return error("x, y, width, height must be valid positive numbers");
        }

        long imageSize = Files.size(imagePath);
        ToolCallResult imageLimit = limits.enforceImageBytesLimit(imageSize);
        if (imageLimit != null) {
            return imageLimit;
        }

        PictureData.PictureType pictureType = pathResolver.inferPictureType(imagePath);
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

    public ToolCallResult uploadTemplate(JsonNode args) throws IOException {
        Path source = pathResolver.resolvePath(requiredString(args, "source_path"), false);
        String extension = pathResolver.extensionOf(source.getFileName().toString());
        if (!("pptx".equals(extension) || "potx".equals(extension))) {
            return error("Template must be a .pptx or .potx file");
        }

        Files.createDirectories(templatesDir);
        String preferredName = args.path("template_name").asText("").strip();
        if (preferredName.isBlank()) {
            preferredName = source.getFileName().toString();
        }
        String safeName = pathResolver.sanitizeTemplateName(preferredName, extension);

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

    public ToolCallResult setDefaultTemplate(JsonNode args) throws IOException {
        Path templatePath = pathResolver.resolvePath(requiredString(args, "template_path"), false);
        String extension = pathResolver.extensionOf(templatePath.getFileName().toString());
        if (!("pptx".equals(extension) || "potx".equals(extension))) {
            return error("Default template must be a .pptx or .potx file");
        }

        setDefaultTemplateInternal(templatePath);

        ObjectNode payload = okPayload();
        payload.put("default_template_path", defaultTemplatePath.toString());
        payload.put("message", "Default template updated");
        return success(payload);
    }

    public ToolCallResult getDefaultTemplate(JsonNode args) {
        ObjectNode payload = okPayload();
        payload.put("default_template_path", defaultTemplatePath == null ? "" : defaultTemplatePath.toString());
        payload.put("has_default_template", defaultTemplatePath != null);
        return success(payload);
    }

    public ToolCallResult importMarkdownOutline(JsonNode args) throws IOException {
        String markdown = requiredString(args, "markdown_text");
        if (markdown.isBlank()) {
            return error("markdown_text must not be blank");
        }

        if (store.size() >= maxOpenDocs) {
            return error("LIMIT_MAX_OPEN_DOCS",
                    "Open document limit reached (" + maxOpenDocs + ")", false);
        }

        XMLSlideShow show = loadTemplateOrBlank("");
        // Keep the template's masters/layouts but discard any pre-existing content slides
        // so N top-level `#` headings produce exactly N slides — callers expect the
        // template to supply styling, not a leading placeholder slide.
        for (int i = show.getSlides().size() - 1; i >= 0; i--) {
            show.removeSlide(i);
        }
        List<String> lines = Arrays.asList(markdown.split("\\r?\\n"));
        XSLFSlide current = null;
        StringBuilder body = new StringBuilder();
        int createdSlides = 0;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.startsWith("# ")) {
                if (current != null && body.length() > 0) {
                    PptSlideBuilder.addBodyText(current, body.toString());
                    body.setLength(0);
                }
                current = PptSlideBuilder.createDefaultSlide(show);
                PptSlideBuilder.setSlideTitle(current, line.substring(2).strip());
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
            PptSlideBuilder.addBodyText(current, body.toString());
        }
        if (createdSlides == 0) {
            current = PptSlideBuilder.ensureFirstSlide(show);
            PptSlideBuilder.setSlideTitle(current, "Imported Outline");
            PptSlideBuilder.addBodyText(current, markdown.strip());
            createdSlides = 1;
        }

        PptDocumentSession session = store.create(show);
        session.touch(true);

        if (args.has("output_path") && !args.path("output_path").asText().isBlank()) {
            Path outputPath = pathResolver.resolvePath(args.path("output_path").asText(), true);
            pathResolver.createParentDirectories(outputPath);
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
        pathResolver.createParentDirectories(defaultTemplateConfigPath);
        Files.writeString(defaultTemplateConfigPath, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root),
                StandardCharsets.UTF_8);
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
