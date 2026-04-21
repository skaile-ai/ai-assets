package ai.skaile.mcpo.ppt.tooling.infra;

import com.fasterxml.jackson.databind.JsonNode;
import java.awt.Color;
import java.util.Locale;
import org.apache.poi.xslf.usermodel.XSLFPictureShape;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSimpleShape;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlip;
import org.openxmlformats.schemas.drawingml.x2006.main.CTBlipFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientFillProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStop;
import org.openxmlformats.schemas.drawingml.x2006.main.CTGradientStopList;
import org.openxmlformats.schemas.drawingml.x2006.main.CTLinearShadeProperties;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRelativeRect;
import org.openxmlformats.schemas.drawingml.x2006.main.CTSRgbColor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTShapeProperties;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

/**
 * XML-level helpers for PPT shape effects that the high-level POI {@code XSLF*}
 * facades don't expose: gradient/pattern fills on {@link CTShapeProperties},
 * crop / alpha / recolor on {@link CTBlipFillProperties} for pictures.
 *
 * <p>All methods throw {@link IllegalArgumentException} on invalid input —
 * callers translate that to {@code VALIDATION_ERROR} via the standard
 * dispatcher path. Color parsing uses {@link ColorParser}, so malformed
 * hex strings surface as {@code INVALID_COLOR}.
 */
public final class PptShapeXml {

    private PptShapeXml() {
    }

    // ---------- Shape properties access ----------

    /** Returns the {@code <a:spPr>} for a simple shape (auto, picture, connector). */
    public static CTShapeProperties getShapeProperties(XSLFSimpleShape shape) {
        XmlObject xml = shape.getXmlObject();
        if (xml instanceof CTShape ctShape) {
            return ctShape.getSpPr();
        }
        if (xml instanceof CTPicture ctPicture) {
            return ctPicture.getSpPr();
        }
        // Fallback: walk the XML for any <a:spPr> child.
        XmlObject[] result = xml.selectPath(
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main'; "
                        + "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main'; "
                        + "./*/p:spPr | ./p:spPr | ./*/a:spPr | ./a:spPr");
        if (result.length > 0 && result[0] instanceof CTShapeProperties spPr) {
            return spPr;
        }
        throw new IllegalArgumentException(
                "Unsupported shape type for fill manipulation: " + shape.getClass().getSimpleName());
    }

    /** Strips any existing {@code solidFill / gradFill / pattFill / blipFill / noFill} on the shape. */
    public static void clearFill(CTShapeProperties spPr) {
        if (spPr.isSetSolidFill()) {
            spPr.unsetSolidFill();
        }
        if (spPr.isSetGradFill()) {
            spPr.unsetGradFill();
        }
        if (spPr.isSetPattFill()) {
            spPr.unsetPattFill();
        }
        if (spPr.isSetBlipFill()) {
            spPr.unsetBlipFill();
        }
        if (spPr.isSetNoFill()) {
            spPr.unsetNoFill();
        }
    }

    // ---------- Gradient + pattern ----------

    public static void applyGradientFill(CTShapeProperties spPr, JsonNode gradientNode) {
        if (gradientNode == null || gradientNode.isMissingNode() || gradientNode.isNull()) {
            throw new IllegalArgumentException("fill_gradient is required when fill_type=gradient");
        }
        String type = gradientNode.path("type").asText("").toLowerCase(Locale.ROOT);
        boolean linear = "linear".equals(type);
        boolean radial = "radial".equals(type);
        if (!linear && !radial) {
            throw new IllegalArgumentException("fill_gradient.type must be 'linear' or 'radial'");
        }
        JsonNode stops = gradientNode.path("stops");
        if (!stops.isArray() || stops.size() < 2) {
            throw new IllegalArgumentException("fill_gradient.stops must be an array of >=2 entries");
        }

        clearFill(spPr);
        CTGradientFillProperties grad = spPr.addNewGradFill();
        CTGradientStopList stopList = grad.addNewGsLst();
        for (JsonNode stopNode : stops) {
            double position = stopNode.path("position").asDouble(-1);
            if (position < 0 || position > 1) {
                throw new IllegalArgumentException("fill_gradient stop.position must be in [0, 1]");
            }
            Color color = ColorParser.parseHex(stopNode.path("color").asText(""));
            CTGradientStop stop = stopList.addNewGs();
            // STPositiveFixedPercentage: thousandths of a percent (100% = 100000).
            stop.setPos((int) Math.round(position * 100000));
            CTSRgbColor srgb = stop.addNewSrgbClr();
            srgb.setVal(rgbBytes(color));
        }

        if (linear) {
            CTLinearShadeProperties lin = grad.addNewLin();
            double angleDeg = gradientNode.path("angle").asDouble(0);
            lin.setAng((int) Math.round(angleDeg * 60000));
            lin.setScaled(false);
        } else {
            // Radial: simple centered circle; POI surfaces this as <a:path path="circle"/>.
            grad.addNewPath().setPath(
                    org.openxmlformats.schemas.drawingml.x2006.main.STPathShadeType.CIRCLE);
        }
    }

    public static void applyPatternFill(CTShapeProperties spPr, JsonNode patternNode) {
        if (patternNode == null || patternNode.isMissingNode() || patternNode.isNull()) {
            throw new IllegalArgumentException("fill_pattern is required when fill_type=pattern");
        }
        String preset = patternNode.path("preset").asText("").toLowerCase(Locale.ROOT);
        String prstVal;
        switch (preset) {
            case "horizontal":
                prstVal = "horz";
                break;
            case "vertical":
                prstVal = "vert";
                break;
            case "diagonal_up":
                prstVal = "upDiag";
                break;
            case "diagonal_down":
                prstVal = "dnDiag";
                break;
            case "cross":
                prstVal = "cross";
                break;
            case "dotted":
                prstVal = "pct50";
                break;
            default:
                throw new IllegalArgumentException(
                        "fill_pattern.preset must be one of: horizontal, vertical, diagonal_up, "
                                + "diagonal_down, cross, dotted");
        }
        Color fg = ColorParser.parseHex(patternNode.path("fg_color").asText(""));
        Color bg = ColorParser.parseHex(patternNode.path("bg_color").asText(""));

        clearFill(spPr);
        // CTPatternFillProperties is not in poi-ooxml-lite's schema impl, so addNewPattFill
        // returns the wrong runtime type and casting fails. Author the XML directly via cursor.
        String aNs = "http://schemas.openxmlformats.org/drawingml/2006/main";
        try (var cursor = spPr.newCursor()) {
            cursor.toEndToken();
            cursor.beginElement(new javax.xml.namespace.QName(aNs, "pattFill"));
            cursor.insertAttributeWithValue("prst", prstVal);
            cursor.beginElement(new javax.xml.namespace.QName(aNs, "fgClr"));
            cursor.beginElement(new javax.xml.namespace.QName(aNs, "srgbClr"));
            cursor.insertAttributeWithValue("val", hexString(fg));
            cursor.toNextToken();
            cursor.toNextToken();
            cursor.beginElement(new javax.xml.namespace.QName(aNs, "bgClr"));
            cursor.beginElement(new javax.xml.namespace.QName(aNs, "srgbClr"));
            cursor.insertAttributeWithValue("val", hexString(bg));
        }
    }

    private static String hexString(Color c) {
        return String.format("%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    public static void applyNoFill(CTShapeProperties spPr) {
        clearFill(spPr);
        spPr.addNewNoFill();
    }

    // ---------- Picture effects ----------

    /** Returns the picture's {@code <p:blipFill>}. Throws if it has no blip (degenerate). */
    public static CTBlipFillProperties getBlipFill(XSLFPictureShape picture) {
        XmlObject xml = picture.getXmlObject();
        if (!(xml instanceof CTPicture ctPic)) {
            throw new IllegalArgumentException(
                    "Expected p:pic XML object for picture shape, got " + xml.getClass().getSimpleName());
        }
        CTBlipFillProperties blipFill = ctPic.getBlipFill();
        if (blipFill == null) {
            throw new IllegalArgumentException("Picture shape has no blipFill element");
        }
        return blipFill;
    }

    /**
     * Crop fractions are PowerPoint-style insets relative to the source rect.
     * {@code left + right < 1} and {@code top + bottom < 1} — otherwise nothing is left.
     */
    public static void applyCrop(CTBlipFillProperties blipFill, JsonNode cropNode) {
        if (cropNode == null || cropNode.isMissingNode() || cropNode.isNull()) {
            return;
        }
        double left = cropNode.path("left").asDouble(0);
        double top = cropNode.path("top").asDouble(0);
        double right = cropNode.path("right").asDouble(0);
        double bottom = cropNode.path("bottom").asDouble(0);
        if (left < 0 || left > 1 || right < 0 || right > 1 || top < 0 || top > 1 || bottom < 0 || bottom > 1) {
            throw new IllegalArgumentException("crop values must be fractions in [0, 1]");
        }
        if (left + right >= 1 || top + bottom >= 1) {
            throw new IllegalArgumentException("crop values on each axis must sum to < 1");
        }
        if (blipFill.isSetSrcRect()) {
            blipFill.unsetSrcRect();
        }
        CTRelativeRect rect = blipFill.addNewSrcRect();
        rect.setL((int) Math.round(left * 100000));
        rect.setT((int) Math.round(top * 100000));
        rect.setR((int) Math.round(right * 100000));
        rect.setB((int) Math.round(bottom * 100000));
    }

    /** Adds {@code <a:alphaModFix val="..."/>} to the picture's blip. {@code alpha=1} is fully opaque. */
    public static void applyAlpha(CTBlipFillProperties blipFill, double alpha) {
        if (alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in [0, 1]");
        }
        CTBlip blip = blipFill.getBlip();
        if (blip == null) {
            throw new IllegalArgumentException("Picture has no blip to apply alpha to");
        }
        // Clear any prior alphaModFix so repeated calls remain idempotent.
        XmlObject[] existing = blip.selectPath(
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main'; ./a:alphaModFix");
        for (XmlObject child : existing) {
            child.newCursor().removeXml();
        }
        // STPositiveFixedPercentage: 1000ths of a percent, 100% = 100000.
        int valuePct = (int) Math.round(alpha * 100000);
        try (var cursor = blip.newCursor()) {
            cursor.toEndToken();
            cursor.beginElement(new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/drawingml/2006/main", "alphaModFix"));
            cursor.insertAttributeWithValue("amt", String.valueOf(valuePct));
        }
    }

    /**
     * Recolor effects via blip-effect children:
     * grayscale → {@code <a:lum sat="0"/>}; sepia → duotone with sepia palette;
     * duotone → arbitrary two-color via {@code <a:duotone>}; washout → {@code <a:lum bright="70%"/>}.
     */
    public static void applyRecolor(CTBlipFillProperties blipFill, JsonNode recolorNode) {
        if (recolorNode == null || recolorNode.isMissingNode() || recolorNode.isNull()) {
            return;
        }
        String mode = recolorNode.path("mode").asText("").toLowerCase(Locale.ROOT);
        CTBlip blip = blipFill.getBlip();
        if (blip == null) {
            throw new IllegalArgumentException("Picture has no blip to recolor");
        }
        // Reset any previously-applied recolor effects so the new mode replaces, not accumulates.
        XmlObject[] previous = blip.selectPath(
                "declare namespace a='http://schemas.openxmlformats.org/drawingml/2006/main'; "
                        + "./a:lum | ./a:duotone | ./a:grayscl | ./a:biLevel");
        for (XmlObject child : previous) {
            child.newCursor().removeXml();
        }
        switch (mode) {
            case "grayscale":
                appendBlipChild(blip, "grayscl");
                break;
            case "sepia":
                appendDuotone(blip, new Color(112, 66, 20), new Color(255, 240, 192));
                break;
            case "duotone": {
                String hex = recolorNode.path("color").asText("");
                if (hex.isBlank()) {
                    throw new IllegalArgumentException("recolor.mode=duotone requires color");
                }
                Color second = ColorParser.parseHex(hex);
                appendDuotone(blip, Color.BLACK, second);
                break;
            }
            case "washout":
                try (var cursor = blip.newCursor()) {
                    cursor.toEndToken();
                    cursor.beginElement(new javax.xml.namespace.QName(
                            "http://schemas.openxmlformats.org/drawingml/2006/main", "lum"));
                    cursor.insertAttributeWithValue("bright", "70000");
                    cursor.insertAttributeWithValue("contrast", "-70000");
                }
                break;
            default:
                throw new IllegalArgumentException(
                        "recolor.mode must be one of: grayscale, sepia, duotone, washout");
        }
    }

    // ---------- Shared private helpers ----------

    private static void appendBlipChild(XmlObject blip, String localName) {
        try (var cursor = blip.newCursor()) {
            cursor.toEndToken();
            cursor.beginElement(new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/drawingml/2006/main", localName));
        }
    }

    private static void appendDuotone(CTBlip blip, Color first, Color second) {
        try (var cursor = blip.newCursor()) {
            cursor.toEndToken();
            cursor.beginElement(new javax.xml.namespace.QName(
                    "http://schemas.openxmlformats.org/drawingml/2006/main", "duotone"));
            insertSrgb(cursor, first);
            insertSrgb(cursor, second);
        }
    }

    private static void insertSrgb(org.apache.xmlbeans.XmlCursor cursor, Color color) {
        cursor.beginElement(new javax.xml.namespace.QName(
                "http://schemas.openxmlformats.org/drawingml/2006/main", "srgbClr"));
        cursor.insertAttributeWithValue("val",
                String.format("%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue()));
        cursor.toNextToken();
    }

    // ---------- Cloning ----------

    /**
     * Walks the slide collecting every {@code <p:cNvPr>} id attribute and returns one greater
     * than the highest. Shape ids must be unique within a slide. Pass any shape on the slide.
     */
    public static long nextShapeId(XSLFShape anyShapeOnSheet) {
        XmlObject xml = anyShapeOnSheet.getSheet().getXmlObject();
        XmlObject[] cnvPrs = xml.selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main'; "
                        + ".//p:cNvPr");
        long max = 0;
        for (XmlObject node : cnvPrs) {
            try (var cursor = node.newCursor()) {
                String idAttr = cursor.getAttributeText(new javax.xml.namespace.QName("id"));
                if (idAttr == null) {
                    continue;
                }
                try {
                    max = Math.max(max, Long.parseLong(idAttr.strip()));
                } catch (NumberFormatException ignored) {
                    // skip malformed
                }
            }
        }
        return max + 1;
    }

    /**
     * Rewrites the shape's own {@code <p:cNvPr>} id attribute. Any nested children (e.g. a group's
     * member shapes) are left alone — callers typically clone single shapes, and groups should run
     * this recursively if they need globally unique ids.
     */
    public static void setShapeCnvPrId(XmlObject shapeXml, long id) {
        XmlObject[] cnvPrs = shapeXml.selectPath(
                "declare namespace p='http://schemas.openxmlformats.org/presentationml/2006/main'; "
                        + "./p:nvSpPr/p:cNvPr | ./p:nvPicPr/p:cNvPr | ./p:nvCxnSpPr/p:cNvPr "
                        + "| ./p:nvGrpSpPr/p:cNvPr | ./p:nvGraphicFramePr/p:cNvPr");
        if (cnvPrs.length == 0) {
            return;
        }
        try (var cursor = cnvPrs[0].newCursor()) {
            cursor.setAttributeText(new javax.xml.namespace.QName("id"), String.valueOf(id));
        }
    }

    private static byte[] rgbBytes(Color c) {
        return new byte[] { (byte) c.getRed(), (byte) c.getGreen(), (byte) c.getBlue() };
    }
}
