package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.Test;

class PptSlideBuilderTest {

    @Test
    void isPlaceholderPromptMatchesTitleSubtitleAndBodyPrompts() {
        assertTrue(PptSlideBuilder.isPlaceholderPrompt("Click to edit Master title style"));
        assertTrue(PptSlideBuilder.isPlaceholderPrompt("Click to edit Master subtitle style"));
        assertTrue(PptSlideBuilder.isPlaceholderPrompt("Click to add text"));
        assertTrue(PptSlideBuilder.isPlaceholderPrompt("Click to add title"));
    }

    @Test
    void isPlaceholderPromptRejectsAuthoredContent() {
        assertFalse(PptSlideBuilder.isPlaceholderPrompt("Quarterly review"));
        assertFalse(PptSlideBuilder.isPlaceholderPrompt(""));
        assertFalse(PptSlideBuilder.isPlaceholderPrompt(null));
    }

    @Test
    void createDefaultSlideLeavesNoPlaceholderPromptText() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = PptSlideBuilder.createDefaultSlide(show);

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    String text = textShape.getText();
                    assertFalse(PptSlideBuilder.isPlaceholderPrompt(text),
                            "placeholder prompt leaked through: " + text);
                }
            }
        }
    }

    @Test
    void setSlideTitleFindsTitlePlaceholderEvenAfterClearing() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = PptSlideBuilder.createDefaultSlide(show);
            PptSlideBuilder.setSlideTitle(slide, "Q1 Review");

            String collected = PptSlideBuilder.collectSlideText(slide);
            assertTrue(collected.contains("Q1 Review"),
                    "title text missing from slide: '" + collected + "'");
            assertFalse(collected.contains("Click to edit"),
                    "placeholder prompt leaked after setSlideTitle: '" + collected + "'");
        }
    }

    @Test
    void setSlideTitleDoesNotDuplicateTitleShape() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = PptSlideBuilder.createDefaultSlide(show);
            int shapeCountBefore = slide.getShapes().size();
            PptSlideBuilder.setSlideTitle(slide, "Hello");
            int shapeCountAfter = slide.getShapes().size();

            assertEquals(shapeCountBefore, shapeCountAfter,
                    "setSlideTitle should reuse an existing placeholder, not add a new textbox");
        }
    }

    @Test
    void clearUnfilledPlaceholdersIsIdempotent() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = PptSlideBuilder.createDefaultSlide(show);
            PptSlideBuilder.clearUnfilledPlaceholders(slide);
            PptSlideBuilder.clearUnfilledPlaceholders(slide);

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    assertFalse(PptSlideBuilder.isPlaceholderPrompt(textShape.getText()));
                }
            }
        }
    }

    @Test
    void clearUnfilledPlaceholdersPreservesAuthoredText() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = PptSlideBuilder.createDefaultSlide(show);
            PptSlideBuilder.setSlideTitle(slide, "Real title");

            PptSlideBuilder.clearUnfilledPlaceholders(slide);

            boolean found = false;
            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape textShape
                        && "Real title".equals(textShape.getText())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "authored title was wiped by clearUnfilledPlaceholders");
        }
    }

    @Test
    void ensureFirstSlideOnEmptyDeckCleansPlaceholders() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            XSLFSlide slide = PptSlideBuilder.ensureFirstSlide(show);
            assertNotNull(slide);

            for (XSLFShape shape : slide.getShapes()) {
                if (shape instanceof XSLFTextShape textShape) {
                    assertFalse(PptSlideBuilder.isPlaceholderPrompt(textShape.getText()));
                }
            }
        }
    }
}
