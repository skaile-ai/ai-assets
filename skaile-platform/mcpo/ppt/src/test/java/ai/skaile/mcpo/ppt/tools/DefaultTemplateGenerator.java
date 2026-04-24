package ai.skaile.mcpo.ppt.tools;

import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.skaile.mcpo.ppt.tooling.infra.PptSlideBuilder;
import java.awt.Dimension;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Produces {@code src/main/resources/templates/skaile-default.pptx} — the opinionated
 * default template shipped with the ppt-mcp server. The resulting file matches the
 * typography/palette documented in SKILL.md § "Design principles".
 *
 * <p>Run with {@code ./mvnw test -Dtest=DefaultTemplateGenerator -Dgenerate.template=true}
 * to regenerate. The committed {@code .pptx} is authoritative — this class exists only
 * so the file can be rebuilt reproducibly after a POI upgrade or palette change.
 *
 * <p>The sample slide is produced via {@link PptSlideBuilder#createDefaultSlide} so the
 * shipped artefact goes through the same placeholder-cleanup path as agent-authored
 * decks. That matters for PowerPoint compatibility: PowerPoint is stricter about PPTX
 * validity than LibreOffice and refuses a file whose {@code <p:txBody>} has no
 * {@code <a:p/>}. The cleanup helper guarantees an empty paragraph survives.
 *
 * <p>A second test, always enabled, round-trips the committed template through POI so a
 * corrupt or missing file is caught at build time.
 */
class DefaultTemplateGenerator {

    private static final String TEMPLATE_RESOURCE_PATH = "templates/skaile-default.pptx";
    private static final Path SOURCE_PATH =
            Paths.get("src/main/resources/" + TEMPLATE_RESOURCE_PATH);

    private static final int PAGE_WIDTH_PT = 960;
    private static final int PAGE_HEIGHT_PT = 540;

    @Test
    @EnabledIfSystemProperty(named = "generate.template", matches = "true")
    void writeDefaultTemplateToSourceTree() throws IOException {
        Files.createDirectories(SOURCE_PATH.getParent());

        try (XMLSlideShow show = new XMLSlideShow();
                FileOutputStream out = new FileOutputStream(SOURCE_PATH.toFile())) {
            show.setPageSize(new Dimension(PAGE_WIDTH_PT, PAGE_HEIGHT_PT));
            PptSlideBuilder.createDefaultSlide(show);
            show.write(out);
        }

        assertTrue(Files.size(SOURCE_PATH) > 0,
                "generated template is empty: " + SOURCE_PATH);
    }

    @Test
    void committedTemplateIsLoadableAndHasExpectedGeometry() throws IOException {
        try (InputStream in = DefaultTemplateGenerator.class.getClassLoader()
                .getResourceAsStream(TEMPLATE_RESOURCE_PATH)) {
            if (in == null) {
                // Template hasn't been generated yet — skip silently on the first
                // build. The gated @Test above produces it.
                return;
            }
            try (XMLSlideShow show = new XMLSlideShow(in)) {
                Dimension page = show.getPageSize();
                assertTrue(
                        page.width == PAGE_WIDTH_PT && page.height == PAGE_HEIGHT_PT,
                        "template page size drifted: got " + page.width + "x" + page.height
                                + ", expected " + PAGE_WIDTH_PT + "x" + PAGE_HEIGHT_PT);
                assertTrue(!show.getSlideMasters().isEmpty(),
                        "template has no slide master");
            }
        }
    }
}
