package ai.skaile.mcpo.ppt.tooling.infra;

import java.awt.geom.Rectangle2D;
import java.util.regex.Pattern;
import org.apache.poi.sl.usermodel.Placeholder;
import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XSLFNotes;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xslf.usermodel.XMLSlideShow;

/**
 * Slide-construction helpers shared by document / template operations. Keeps the
 * title-placement, body-text, and "best-effort layout" conventions consistent across
 * {@code ppt.create_document}, {@code ppt.generate_presentation},
 * {@code ppt.import_markdown_outline}, and {@code ppt.add_slide}.
 */
public final class PptSlideBuilder {

    /**
     * Matches POI's layout/master prompt text ("Click to edit Master title style",
     * "Click to add text", etc.) emitted when a placeholder has no user content.
     * The outline export, list_slides preview and get_slide_metrics counters
     * all filter these so agents see real slide content only.
     */
    public static final Pattern PLACEHOLDER_PROMPT =
            Pattern.compile("^Click to (edit|add) [^\\n]+$");

    private PptSlideBuilder() {
    }

    /**
     * Returns the shape's user-visible text, filtering POI's inherited layout prompt
     * ("Click to edit Master title style", "Click to add text", ...) back to "". Call
     * this wherever raw {@code textShape.getText()} would otherwise leak the prompt to
     * a tool response (get_slide_content, get_shape_properties, find_text, set_text).
     */
    public static String visibleText(XSLFTextShape textShape) {
        if (textShape == null) {
            return "";
        }
        String text = textShape.getText();
        if (text == null || isPlaceholderPrompt(text)) {
            return "";
        }
        return text;
    }

    /**
     * True if {@code text} is POI's layout-inherited placeholder prompt rather than
     * authored user content. Multi-line strings are checked line-by-line and only
     * collapse to {@code true} when every non-blank line matches the prompt pattern.
     */
    public static boolean isPlaceholderPrompt(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.strip();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (String line : trimmed.split("\\r?\\n")) {
            String l = line.strip();
            if (l.isEmpty()) {
                continue;
            }
            if (!PLACEHOLDER_PROMPT.matcher(l).matches()) {
                return false;
            }
        }
        return true;
    }

    public static XSLFSlide ensureFirstSlide(XMLSlideShow show) {
        if (show.getSlides().isEmpty()) {
            return createDefaultSlide(show);
        }
        return show.getSlides().get(0);
    }

    public static XSLFSlide createDefaultSlide(XMLSlideShow show) {
        XSLFSlide slide;
        if (!show.getSlideMasters().isEmpty()) {
            XSLFSlideMaster master = show.getSlideMasters().get(0);
            SlideLayout preferred = bestEffortLayout(master);
            if (preferred != null) {
                XSLFSlideLayout layout = master.getLayout(preferred);
                if (layout != null) {
                    slide = show.createSlide(layout);
                    clearUnfilledPlaceholders(slide);
                    return slide;
                }
            }
        }
        slide = show.createSlide();
        clearUnfilledPlaceholders(slide);
        return slide;
    }

    /**
     * Replaces any layout-inherited placeholder prompt text ("Click to edit Master title style",
     * "Click to edit Master subtitle style", "Click to add text", ...) with a truly empty text
     * body so LibreOffice renders the slide without those prompts. POI's default behaviour is to
     * leave newly-created placeholders inheriting the layout's prompt, which PowerPoint hides in
     * slideshow mode but LibreOffice rasterises into PNG/PDF output.
     */
    public static void clearUnfilledPlaceholders(XSLFSlide slide) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && isPlaceholderPrompt(text)) {
                    textShape.clearText();
                }
            }
        }
    }

    public static void setSlideTitle(XSLFSlide slide, String title) {
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                Placeholder type = textShape.getTextType();
                if (type == Placeholder.TITLE || type == Placeholder.CENTERED_TITLE) {
                    textShape.clearText();
                    textShape.setText(title);
                    return;
                }
            }
        }

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

    public static void addBodyText(XSLFSlide slide, String text) {
        XSLFTextShape body = slide.createTextBox();
        body.setAnchor(new Rectangle2D.Double(50, 120, 860, 380));
        body.setText(text);
    }

    public static String collectSlideText(XSLFSlide slide) {
        StringBuilder parts = new StringBuilder();
        for (XSLFShape shape : slide.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank() && !isPlaceholderPrompt(text)) {
                    if (parts.length() > 0) {
                        parts.append('\n');
                    }
                    parts.append(text.strip());
                }
            }
        }
        return parts.toString();
    }

    public static String collectNotesText(XSLFNotes notes) {
        StringBuilder parts = new StringBuilder();
        for (XSLFShape shape : notes.getShapes()) {
            if (shape instanceof XSLFTextShape textShape) {
                String text = textShape.getText();
                if (text != null && !text.isBlank()) {
                    if (parts.length() > 0) {
                        parts.append('\n');
                    }
                    parts.append(text.strip());
                }
            }
        }
        return parts.toString();
    }

    private static SlideLayout bestEffortLayout(XSLFSlideMaster master) {
        SlideLayout[] preferred = new SlideLayout[] {
                SlideLayout.TITLE,
                SlideLayout.TITLE_AND_CONTENT,
                SlideLayout.BLANK
        };
        for (SlideLayout layout : preferred) {
            if (master.getLayout(layout) != null) {
                return layout;
            }
        }
        return null;
    }
}
