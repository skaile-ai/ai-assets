package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.ColorParser;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptSlideBuilder;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeXml;
import ai.skaile.mcpo.ppt.tooling.infra.ToolArgumentValidator;
import ai.skaile.mcpo.ppt.tooling.infra.ToolResponseFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.sl.usermodel.PictureData;
import org.apache.poi.sl.usermodel.ShapeType;
import org.apache.poi.sl.usermodel.TextParagraph;
import org.apache.poi.xslf.usermodel.XSLFAutoShape;
import org.apache.poi.xslf.usermodel.XSLFPictureData;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.w3c.dom.Node;

/**
 * Mutations that target a single shape: add / delete / move / resize / clone,
 * property inspection + styling, hyperlinks, z-order, and image replacement.
 */
public final class PptShapeMutationOperations {

    private final ToolArgumentValidator argumentValidator;
    private final ToolResponseFactory responseFactory;
    private final PptPathResolver pathResolver;
    private final PptShapeFinder shapeFinder;
    private final PptLimits limits;

    public PptShapeMutationOperations(
            ToolArgumentValidator argumentValidator,
            ToolResponseFactory responseFactory,
            PptPathResolver pathResolver,
            PptShapeFinder shapeFinder,
            PptLimits limits) {
        this.argumentValidator = argumentValidator;
        this.responseFactory = responseFactory;
        this.pathResolver = pathResolver;
        this.shapeFinder = shapeFinder;
        this.limits = limits;
    }

    public Map<String, ToolHandler> handlers() {
        Map<String, ToolHandler> map = new java.util.HashMap<>();
        map.put("ppt.add_shape", this::addShape);
        map.put("ppt.delete_shape", this::deleteShape);
        map.put("ppt.move_shape", this::moveShape);
        map.put("ppt.resize_shape", this::resizeShape);
        map.put("ppt.clone_shape", this::cloneShape);
        map.put("ppt.get_shape_properties", this::getShapeProperties);
        map.put("ppt.set_shape_style", this::setShapeStyle);
        map.put("ppt.set_picture_effects", this::setPictureEffects);
        map.put("ppt.set_shape_z_order", this::setShapeZOrder);
        map.put("ppt.add_hyperlink", this::addHyperlink);
        map.put("ppt.replace_image", this::replaceImage);
        return Map.copyOf(map);
    }

    public ToolCallResult addShape(JsonNode args) {
        ToolCallResult shapeLimit = limits.enforceShapeLimit(args);
        if (shapeLimit != null) {
            return shapeLimit;
        }

        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);

        double x = args.path("x").asDouble(Double.NaN);
        double y = args.path("y").asDouble(Double.NaN);
        double width = args.path("width").asDouble(Double.NaN);
        double height = args.path("height").asDouble(Double.NaN);
        if (!argumentValidator.isValidRect(x, y, width, height)) {
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
            shape.setFillColor(ColorParser.parseHex(args.path("fill_color").asText("")));
        }
        if (args.has("border_color")) {
            shape.setLineColor(ColorParser.parseHex(args.path("border_color").asText("")));
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

    public ToolCallResult deleteShape(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        slide.removeShape(shape);
        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("message", "Shape deleted");
        return success(payload);
    }

    public ToolCallResult moveShape(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
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

    public ToolCallResult resizeShape(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
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

    public ToolCallResult cloneShape(JsonNode args) {
        ToolCallResult shapeLimit = limits.enforceShapeLimit(args);
        if (shapeLimit != null) {
            return shapeLimit;
        }

        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape original = shapeFinder.requireShape(slide, shapeIndex);

        Rectangle2D anchor = original.getAnchor();
        double offsetX = args.path("offset_x").asDouble(20);
        double offsetY = args.path("offset_y").asDouble(20);

        // Add a typed child element to the slide's spTree, then deep-copy the original shape's
        // XML into it. This handles every shape kind uniformly (autoShape, picture,
        // table/graphicFrame, connector, group) and produces an element POI's buildShapes()
        // recognizes when reparsing.
        var spTree = slide.getXmlObject().getCSld().getSpTree();
        org.apache.xmlbeans.XmlObject originalXml = original.getXmlObject();
        org.apache.xmlbeans.XmlObject newChild;
        if (originalXml instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTShape) {
            newChild = spTree.addNewSp();
        } else if (originalXml instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTPicture) {
            newChild = spTree.addNewPic();
        } else if (originalXml instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTGroupShape) {
            newChild = spTree.addNewGrpSp();
        } else if (originalXml instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTGraphicalObjectFrame) {
            newChild = spTree.addNewGraphicFrame();
        } else if (originalXml instanceof org.openxmlformats.schemas.presentationml.x2006.main.CTConnector) {
            newChild = spTree.addNewCxnSp();
        } else {
            return responseFactory.error("VALIDATION_ERROR",
                    "Unsupported shape type for clone: " + originalXml.getClass().getSimpleName(),
                    false);
        }
        newChild.set(originalXml);

        // POI caches XSLFShape instances on XSLFSheet._shapes. Direct XML manipulation does not
        // invalidate the cache, so getShapes() would return the stale list. Reset it.
        invalidateShapeCache(slide);

        List<XSLFShape> shapesAfter = slide.getShapes();
        XSLFShape clone = shapesAfter.get(shapesAfter.size() - 1);

        long newId = ai.skaile.mcpo.ppt.tooling.infra.PptShapeXml.nextShapeId(clone);
        ai.skaile.mcpo.ppt.tooling.infra.PptShapeXml.setShapeCnvPrId(clone.getXmlObject(), newId);

        if (anchor != null) {
            Rectangle2D shifted = new Rectangle2D.Double(
                    anchor.getX() + offsetX,
                    anchor.getY() + offsetY,
                    anchor.getWidth(),
                    anchor.getHeight());
            applyAnchor(clone, shifted);
        }

        session.touch(true);

        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("source_shape_index", shapeIndex);
        payload.put("shape_index", shapesAfter.size() - 1);
        payload.put("message", "Shape cloned");
        return success(payload);
    }

    private static void invalidateShapeCache(XSLFSlide slide) {
        try {
            java.lang.reflect.Field field =
                    org.apache.poi.xslf.usermodel.XSLFSheet.class.getDeclaredField("_shapes");
            field.setAccessible(true);
            field.set(slide, null);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // POI version drift will leave the cache populated; getShapes() returns stale data.
        }
    }

    private static void applyAnchor(XSLFShape shape, Rectangle2D anchor) {
        if (shape instanceof XSLFSimpleShape simple) {
            simple.setAnchor(anchor);
            return;
        }
        if (shape instanceof org.apache.poi.xslf.usermodel.XSLFGraphicFrame frame) {
            frame.setAnchor(anchor);
            return;
        }
        if (shape instanceof org.apache.poi.xslf.usermodel.XSLFGroupShape group) {
            group.setAnchor(anchor);
            return;
        }
        // Unknown shape type — leave the anchor as-is rather than fail the clone.
    }

    public ToolCallResult getShapeProperties(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
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
            payload.put("text", PptSlideBuilder.visibleText(textShape));
        }
        if (shape instanceof XSLFSimpleShape simpleShape) {
            Color fill = simpleShape.getFillColor();
            if (fill != null) {
                payload.put("fill_color", ColorParser.toHex(fill));
            }
            Color line = simpleShape.getLineColor();
            if (line != null) {
                payload.put("border_color", ColorParser.toHex(line));
            }
            payload.put("border_width", simpleShape.getLineWidth());
        }

        return success(payload);
    }

    public ToolCallResult setShapeStyle(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);

        ToolCallResult fillError = applyFill(shape, args);
        if (fillError != null) {
            return fillError;
        }

        if (args.has("border_color")) {
            if (!(shape instanceof XSLFSimpleShape simpleShape)) {
                return error("border_color is only supported for simple shapes");
            }
            simpleShape.setLineColor(ColorParser.parseHex(args.path("border_color").asText("")));
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

    /**
     * Routes the {@code fill_type} branch. Returns {@code null} on success, otherwise an
     * error result. {@code fill_color} without {@code fill_type} is treated as the
     * legacy {@code fill_type=solid} shorthand for backward compatibility.
     */
    private ToolCallResult applyFill(XSLFShape shape, JsonNode args) {
        boolean hasFillType = args.has("fill_type");
        boolean hasFillColor = args.has("fill_color");
        boolean hasGradient = args.has("fill_gradient");
        boolean hasPattern = args.has("fill_pattern");
        if (!hasFillType && !hasFillColor && !hasGradient && !hasPattern) {
            return null;
        }
        if (!(shape instanceof XSLFSimpleShape simpleShape)) {
            return error("fill is only supported for simple shapes");
        }
        String fillType = hasFillType
                ? args.path("fill_type").asText("").toLowerCase(Locale.ROOT)
                : "solid";
        switch (fillType) {
            case "solid":
                if (hasFillColor) {
                    simpleShape.setFillColor(ColorParser.parseHex(args.path("fill_color").asText("")));
                }
                return null;
            case "gradient":
                PptShapeXml.applyGradientFill(PptShapeXml.getShapeProperties(simpleShape),
                        args.path("fill_gradient"));
                return null;
            case "pattern":
                PptShapeXml.applyPatternFill(PptShapeXml.getShapeProperties(simpleShape),
                        args.path("fill_pattern"));
                return null;
            case "none":
                PptShapeXml.applyNoFill(PptShapeXml.getShapeProperties(simpleShape));
                return null;
            default:
                return responseFactory.error("VALIDATION_ERROR",
                        "fill_type must be one of: solid, gradient, pattern, none", false);
        }
    }

    public ToolCallResult setPictureEffects(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);
        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFPictureShape picture)) {
            return responseFactory.error("SHAPE_NOT_PICTURE",
                    "shape_index does not point to a picture shape", false);
        }

        boolean hasCrop = args.hasNonNull("crop");
        boolean hasAlpha = args.hasNonNull("alpha");
        boolean hasRecolor = args.hasNonNull("recolor");
        if (!hasCrop && !hasAlpha && !hasRecolor) {
            return responseFactory.error("VALIDATION_ERROR",
                    "set_picture_effects requires at least one of: crop, alpha, recolor", false);
        }

        var blipFill = PptShapeXml.getBlipFill(picture);
        if (hasCrop) {
            PptShapeXml.applyCrop(blipFill, args.path("crop"));
        }
        if (hasAlpha) {
            PptShapeXml.applyAlpha(blipFill, args.path("alpha").asDouble());
        }
        if (hasRecolor) {
            PptShapeXml.applyRecolor(blipFill, args.path("recolor"));
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("crop_applied", hasCrop);
        payload.put("alpha_applied", hasAlpha);
        payload.put("recolor_applied", hasRecolor);
        payload.put("message", "Picture effects applied");
        return success(payload);
    }

    public ToolCallResult setShapeZOrder(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        String position = requiredString(args, "position").toLowerCase(Locale.ROOT);

        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
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
            // Raw DOM reordering bypasses POI's facade, so XSLFSheet._shapes keeps the
            // pre-reorder XSLFShape list. Without invalidation, a subsequent delete_shape
            // at a given shape_index removes the wrong shape and later getShapes() calls
            // NPE on the now-detached XSLFShape wrappers.
            invalidateShapeCache(slide);
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

    public ToolCallResult addHyperlink(JsonNode args) {
        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        String url = requiredString(args, "url");
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape shape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(shape instanceof XSLFTextShape textShape)) {
            return error("shape_index does not point to a text-capable shape");
        }

        List<XSLFTextRun> runs = collectTextRuns(textShape);
        if (runs.isEmpty()) {
            return error("Selected text shape has no text runs to hyperlink");
        }

        // XSLFAutoShape.setText(...) produces runs whose underlying CTRegularTextRun has no
        // rPr element yet; POI's createHyperlink() calls getRPr(true).addNewHlinkClick() on
        // the run and NPEs when the run subclass can't supply an rPr. Guard each run so a
        // hostile subclass skips rather than poisons the whole call.
        int applied = 0;
        for (XSLFTextRun run : runs) {
            try {
                org.apache.poi.xslf.usermodel.XSLFHyperlink link = run.createHyperlink();
                if (link == null) {
                    continue;
                }
                link.setAddress(url);
                applied++;
            } catch (RuntimeException ignored) {
                // best-effort — continue with the remaining runs
            }
        }
        if (applied == 0) {
            return error("No text run in this shape accepted the hyperlink");
        }

        session.touch(true);
        ObjectNode payload = okPayload();
        payload.put("document_id", session.getId());
        payload.put("slide_index", slideIndex);
        payload.put("shape_index", shapeIndex);
        payload.put("url", url);
        payload.put("runs_updated", applied);
        payload.put("message", "Hyperlink added");
        return success(payload);
    }

    public ToolCallResult replaceImage(JsonNode args) throws IOException {
        // replace_image swaps an existing picture without changing shape count, so no shape
        // limit check. Image-byte limit still applies.
        JsonNode raw = args.path("image_path");
        if (raw.isTextual() && !raw.asText().isBlank()) {
            Path preCheck = pathResolver.resolvePath(raw.asText(), false);
            ToolCallResult imageLimit = limits.enforceImageBytesLimit(Files.size(preCheck));
            if (imageLimit != null) {
                return imageLimit;
            }
        }

        PptDocumentSession session = shapeFinder.requireSession(args);

        int slideIndex = args.path("slide_index").asInt(-1);
        int shapeIndex = args.path("shape_index").asInt(-1);
        XSLFSlide slide = shapeFinder.requireSlide(session, slideIndex);
        XSLFShape originalShape = shapeFinder.requireShape(slide, shapeIndex);
        if (!(originalShape instanceof XSLFPictureShape originalPicture)) {
            return error("shape_index does not point to a picture shape");
        }

        Rectangle2D anchor = originalPicture.getAnchor();
        if (anchor == null) {
            return error("Selected picture shape has no anchor");
        }

        Path imagePath = pathResolver.resolvePath(requiredString(args, "image_path"), false);
        PictureData.PictureType pictureType = pathResolver.inferPictureType(imagePath);
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

    // ---------- Helpers ----------

    static List<XSLFTextRun> collectTextRuns(XSLFTextShape textShape) {
        List<XSLFTextRun> runs = new ArrayList<>();
        textShape.getTextParagraphs().forEach(paragraph -> runs.addAll(paragraph.getTextRuns()));
        return runs;
    }

    static TextParagraph.TextAlign parseTextAlign(String raw) {
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

    static int indexOfShape(XSLFSlide slide, XSLFShape shape) {
        List<XSLFShape> shapes = slide.getShapes();
        for (int i = 0; i < shapes.size(); i++) {
            if (shapes.get(i) == shape) {
                return i;
            }
        }
        return -1;
    }

    private static boolean setShapeAnchor(XSLFShape shape, double x, double y, double w, double h) {
        if (shape instanceof XSLFSimpleShape simpleShape) {
            simpleShape.setAnchor(new Rectangle2D.Double(x, y, w, h));
            return true;
        }
        return false;
    }

    private static List<Node> collectZOrderNodes(Node parent) {
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
