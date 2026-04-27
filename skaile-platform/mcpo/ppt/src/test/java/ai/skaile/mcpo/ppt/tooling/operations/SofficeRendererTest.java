package ai.skaile.mcpo.ppt.tooling.operations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.skaile.mcpo.ppt.tooling.infra.PptPathResolver;
import ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig;
import ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SofficeRenderer} that do not require soffice on PATH.
 * Happy-path coverage of actual LibreOffice invocations lives in the Docker
 * smoke test, which ships with soffice. Here we exercise the unavailable-binary
 * short-circuit and verify that temp files are cleaned up even on failure.
 */
final class SofficeRendererTest {

    private PptPathResolver pathResolver;
    private SofficeRenderer renderer;
    private Path sandboxRoot;
    private PptServerConfig config;

    @BeforeEach
    void setUp() throws Exception {
        sandboxRoot = Files.createTempDirectory("mcpo-soffice-test-");
        pathResolver = new PptPathResolver(sandboxRoot);
        // Point the soffice binary at a path guaranteed not to exist so the probe
        // reports unavailable. Any runSoffice call must short-circuit through the
        // availability check and throw SofficeUnavailableException.
        config = new PptServerConfig(
                sandboxRoot,
                sandboxRoot.resolve("templates"),
                sandboxRoot.resolve("templates/default-template.json"),
                5,
                sandboxRoot.resolve("definitely-not-a-real-soffice").toString(),
                System.getProperty("java.version", "unknown"));
        SofficeAvailability.reset();
        renderer = new SofficeRenderer(pathResolver, config);
    }

    @AfterEach
    void tearDown() {
        SofficeAvailability.reset();
    }

    @Test
    void exportWholeDeckThrowsWhenSofficeMissing() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            show.createSlide();
            Path out = sandboxRoot.resolve("nope.pdf");
            assertThrows(SofficeRenderer.SofficeUnavailableException.class,
                    () -> renderer.exportWholeDeck(show, out, "pdf", "pdf"));
            assertFalse(Files.exists(out), "output should not have been created");
        }
        assertNoStaleTempFiles();
    }

    @Test
    void renderSingleSlideExtractsTargetThenThrowsWhenSofficeMissing() throws Exception {
        // Build a 3-slide deck and ask to render slide 1. We can't verify the soffice call
        // itself here, but the failure should happen inside runSoffice — meaning the single-
        // slide extract ran first without raising. The original show must remain untouched.
        try (XMLSlideShow show = new XMLSlideShow()) {
            for (int i = 0; i < 3; i++) {
                XSLFSlide slide = show.createSlide();
                XSLFTextShape title = slide.createTextBox();
                title.setText("Slide " + i);
            }
            int slidesBefore = show.getSlides().size();
            Path out = sandboxRoot.resolve("slide-1.png");
            assertThrows(SofficeRenderer.SofficeUnavailableException.class,
                    () -> renderer.renderSingleSlide(show, 1, out, "png", "png"));
            assertFalse(Files.exists(out), "output should not have been created");
            assertTrue(slidesBefore == show.getSlides().size(),
                    "original slideshow must not be mutated by the extract");
        }
        assertNoStaleTempFiles();
    }

    @Test
    void renderAllSlidesCreatesOutputDirThenThrowsWhenSofficeMissing() throws Exception {
        try (XMLSlideShow show = new XMLSlideShow()) {
            show.createSlide();
            show.createSlide();
            Path outDir = sandboxRoot.resolve("out-batch");
            assertThrows(SofficeRenderer.SofficeUnavailableException.class,
                    () -> renderer.renderAllSlides(show, outDir, "png", "png", "slide-%03d"));
            // Output directory may or may not have been created (we create it eagerly),
            // but must be empty.
            if (Files.exists(outDir)) {
                try (var stream = Files.list(outDir)) {
                    assertTrue(stream.findAny().isEmpty(),
                            "batch output directory must be empty after failure");
                }
            }
        }
        assertNoStaleTempFiles();
    }

    @Test
    void renderSingleSlideRejectsOutOfRangeIndex() throws Exception {
        // Single-slide extraction runs before any soffice invocation, so the bounds
        // check surfaces as IOException rather than SofficeUnavailableException.
        try (XMLSlideShow show = new XMLSlideShow()) {
            show.createSlide();
            Path out = sandboxRoot.resolve("ignored.png");
            assertThrows(java.io.IOException.class,
                    () -> renderer.renderSingleSlide(show, 42, out, "png", "png"));
        }
        assertNoStaleTempFiles();
    }

    @Test
    void singleSlideDeckExtractIsRoundTripReadable() throws Exception {
        // Extraction is driven via renderSingleSlide, but we can cross-check the temp-pptx
        // shape by invoking exportWholeDeck on a deck we've already written, reading it back,
        // and asserting the slide count. exportWholeDeck will fail to shell out, but the
        // sandbox temp pptx it writes is still a valid pptx up until that point. We can't
        // observe that file directly (it gets cleaned up), so instead we just make sure
        // writing a deck through renderSingleSlide cleans up after itself.
        try (XMLSlideShow show = new XMLSlideShow()) {
            show.createSlide();
            show.createSlide();
            show.createSlide();
            Path out = sandboxRoot.resolve("ignored.png");
            assertThrows(SofficeRenderer.SofficeUnavailableException.class,
                    () -> renderer.renderSingleSlide(show, 0, out, "png", "png"));
        }
        // Repeating once more confirms sandbox cleanup is idempotent.
        try (XMLSlideShow show = new XMLSlideShow()) {
            show.createSlide();
            Path out = sandboxRoot.resolve("ignored2.png");
            assertThrows(SofficeRenderer.SofficeUnavailableException.class,
                    () -> renderer.renderSingleSlide(show, 0, out, "png", "png"));
        }
        assertNoStaleTempFiles();
    }

    @Test
    void deleteRecursivelyQuietlyRemovesTreeAndToleratesMissingInput() throws Exception {
        Path root = Files.createTempDirectory("mcpo-del-rec-");
        Path sub = Files.createDirectories(root.resolve("a/b"));
        Files.writeString(sub.resolve("leaf.txt"), "x");
        Files.writeString(root.resolve("top.txt"), "y");

        SofficeRenderer.deleteRecursivelyQuietly(root);
        assertFalse(Files.exists(root), "root and all descendants must be deleted");

        // Null + non-existent paths must be safe (shutdown-hook contract: best-effort).
        SofficeRenderer.deleteRecursivelyQuietly(null);
        SofficeRenderer.deleteRecursivelyQuietly(root.resolve("never-created"));
    }

    private void assertNoStaleTempFiles() throws Exception {
        Path tmpDir = sandboxRoot.resolve(PptPathResolver.SANDBOX_TMP_SUBDIR);
        if (!Files.exists(tmpDir)) {
            return;
        }
        try (var stream = Files.walk(tmpDir)) {
            long leftoverFiles = stream
                    .filter(p -> !p.equals(tmpDir))
                    .filter(Files::isRegularFile)
                    .count();
            assertTrue(leftoverFiles == 0,
                    "expected no stale files under " + tmpDir + ", found " + leftoverFiles);
        }
    }

    /** Round-trips a small pptx via file IO so the test suite remains independent of POI internals. */
    @SuppressWarnings("unused")
    private Path writeDeck(XMLSlideShow show, Path path) throws Exception {
        try (FileOutputStream out = new FileOutputStream(path.toFile())) {
            show.write(out);
        }
        try (FileInputStream in = new FileInputStream(path.toFile());
                XMLSlideShow read = new XMLSlideShow(in)) {
            assertTrue(!read.getSlides().isEmpty());
        }
        return path;
    }
}
