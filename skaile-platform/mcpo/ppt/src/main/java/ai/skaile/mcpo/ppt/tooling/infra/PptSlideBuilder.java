package ai.skaile.mcpo.ppt.tooling.infra;

import java.awt.geom.Rectangle2D;
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

    private PptSlideBuilder() {
    }

    public static XSLFSlide ensureFirstSlide(XMLSlideShow show) {
        if (show.getSlides().isEmpty()) {
            return createDefaultSlide(show);
        }
        return show.getSlides().get(0);
    }

    public static XSLFSlide createDefaultSlide(XMLSlideShow show) {
        if (!show.getSlideMasters().isEmpty()) {
            XSLFSlideMaster master = show.getSlideMasters().get(0);
            SlideLayout preferred = bestEffortLayout(master);
            if (preferred != null) {
                XSLFSlideLayout layout = master.getLayout(preferred);
                if (layout != null) {
                    return show.createSlide(layout);
                }
            }
        }
        return show.createSlide();
    }

    public static void setSlideTitle(XSLFSlide slide, String title) {
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
