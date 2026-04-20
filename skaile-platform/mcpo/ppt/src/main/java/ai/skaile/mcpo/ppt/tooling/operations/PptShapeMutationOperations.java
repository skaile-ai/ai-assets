package ai.skaile.mcpo.ppt.tooling.operations;

import ai.skaile.mcpo.ppt.session.PptDocumentSession;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;
import ai.skaile.mcpo.ppt.tooling.infra.ColorParser;
import ai.skaile.mcpo.ppt.tooling.infra.PptLimits;
import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptShapeFinder;
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
        return Map.of(
                "ppt.add_shape", this::addShape,
                "ppt.delete_shape", this::deleteShape,
                "ppt.move_shape", this::moveShape,
                "ppt.resize_shape", this::resizeShape,
                "ppt.clone_shape", this::cloneShape,
                "ppt.get_shape_properties", this::getShapeProperties,
                "ppt.set_shape_style", this::setShapeStyle,
                "ppt.set_shape_z_order", this::setShapeZOrder,
                "ppt.add_hyperlink", this::addHyperlink,
                "ppt.replace_image", this::replaceImage);
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
            payload.put("text", textShape.getText());
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

        if (args.has("fill_color")) {
            if (!(shape instanceof XSLFSimpleShape simpleShape)) {
                return error("fill_color is only supported for simple shapes");
            }
            simpleShape.setFillColor(ColorParser.parseHex(args.path("fill_color").asText("")));
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
