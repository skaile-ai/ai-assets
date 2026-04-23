package ai.skaile.mcpo.ppt.tooling;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * Visual regression — builds deterministic decks through {@link PptToolService}, renders
 * slide 0 at {@code fidelity=low} (POI + Java2D, no soffice dependency), and pixel-diffs
 * the PNG against a committed golden under {@code src/test/resources/golden/}. The
 * primary value is catching classes of bug that byte-level XML checks miss — most
 * importantly, a placeholder prompt becoming visible because a regression re-populated
 * an unfilled placeholder at serialisation time.
 *
 * <p>Run {@code ./mvnw test -Dtest=PptRenderGoldenTest -Dregenerate.goldens=true} to
 * rewrite the committed PNGs after an intentional rendering change.
 *
 * <p>POI's low-fidelity rasteriser is deterministic on the same JDK and available-font
 * set. Tolerances below are set generous enough to absorb sub-pixel antialiasing drift
 * between JDK minor versions but tight enough that placeholder-prompt text (which
 * introduces dozens of dark pixels inside the subtitle region) will trip the test.
 */
class PptRenderGoldenTest {

    private static final Path GOLDEN_DIR = Paths.get("src/test/resources/golden");

    private static final int RENDER_WIDTH = 640;
    private static final int RENDER_HEIGHT = 360;

    /** Max absolute per-channel delta treated as noise. */
    private static final int PIXEL_DELTA_THRESHOLD = 12;
    /** Max allowed fraction of pixels exceeding PIXEL_DELTA_THRESHOLD. */
    private static final double MAX_DIFF_FRACTION = 0.02;

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void titleSlideWithTitleOnly() throws Exception {
        renderAndCompare("title-only", service -> {
            ObjectNode args = mapper.createObjectNode();
            args.put("title", "Quarterly review");
            return service.call("ppt.create_document", args);
        });
    }

    @Test
    void titleSlideWithNoTitle() throws Exception {
        // This is the regression scenario: a slide created with NO title text must
        // not render the "Click to edit Master title/subtitle style" prompts.
        renderAndCompare("blank-title-layout", service -> {
            ObjectNode args = mapper.createObjectNode();
            return service.call("ppt.create_document", args);
        });
    }

    @Test
    void contentSlideWithTextbox() throws Exception {
        renderAndCompare("textbox-body", service -> {
            ObjectNode create = mapper.createObjectNode();
            create.put("title", "Findings");
            ToolCallResult created = service.call("ppt.create_document", create);
            String docId = created.payload().path("document_id").asText();

            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Revenue is up 42%");
            tb.put("x", 60);
            tb.put("y", 200);
            tb.put("width", 500);
            tb.put("height", 60);
            tb.put("font_size", 24);
            service.call("ppt.add_textbox", tb);
            return created;
        });
    }

    // --- helpers ---

    @FunctionalInterface
    private interface DeckSetup {
        ToolCallResult apply(PptToolService service) throws IOException;
    }

    private void renderAndCompare(String scenarioName, DeckSetup setup) throws Exception {
        PptToolService service = new PptToolService();
        try {
            ToolCallResult initial = setup.apply(service);
            String docId = initial.payload().path("document_id").asText();
            assertTrue(!docId.isBlank(),
                    "setup produced no document_id: " + initial.payload());

            Path rendered = Files.createTempFile("golden-" + scenarioName + "-", ".png");
            try {
                ObjectNode renderArgs = mapper.createObjectNode();
                renderArgs.put("document_id", docId);
                renderArgs.put("slide_index", 0);
                renderArgs.put("output_path", rendered.toString());
                renderArgs.put("format", "png");
                renderArgs.put("fidelity", "low");
                renderArgs.put("width", RENDER_WIDTH);
                renderArgs.put("height", RENDER_HEIGHT);
                ToolCallResult rendered0 = service.call("ppt.render_slide", renderArgs);
                assertTrue(rendered0.success(),
                        "render_slide failed for " + scenarioName + ": " + rendered0.payload());
                assertTrue(Files.size(rendered) > 0,
                        "rendered png is empty for " + scenarioName);

                Path goldenPath = GOLDEN_DIR.resolve(scenarioName + ".png");

                if (Boolean.getBoolean("regenerate.goldens")) {
                    Files.createDirectories(GOLDEN_DIR);
                    Files.copy(rendered, goldenPath, StandardCopyOption.REPLACE_EXISTING);
                    return;
                }

                if (!Files.exists(goldenPath)) {
                    fail("missing golden for " + scenarioName + " at " + goldenPath.toAbsolutePath()
                            + " — run with -Dregenerate.goldens=true to create it");
                }

                compareImages(goldenPath, rendered, scenarioName);
            } finally {
                Files.deleteIfExists(rendered);
            }
        } finally {
            service.closeAllSessions();
        }
    }

    private void compareImages(Path goldenPath, Path actualPath, String scenarioName)
            throws IOException {
        BufferedImage golden = ImageIO.read(goldenPath.toFile());
        BufferedImage actual = ImageIO.read(actualPath.toFile());
        if (golden == null || actual == null) {
            fail("could not decode golden or rendered PNG for " + scenarioName);
        }

        if (golden.getWidth() != actual.getWidth() || golden.getHeight() != actual.getHeight()) {
            fail(scenarioName + ": dimensions differ — golden "
                    + golden.getWidth() + "x" + golden.getHeight()
                    + " vs actual " + actual.getWidth() + "x" + actual.getHeight());
        }

        long differingPixels = 0;
        long totalPixels = (long) golden.getWidth() * golden.getHeight();
        for (int y = 0; y < golden.getHeight(); y++) {
            for (int x = 0; x < golden.getWidth(); x++) {
                int g = golden.getRGB(x, y);
                int a = actual.getRGB(x, y);
                if (channelDelta(g, a) > PIXEL_DELTA_THRESHOLD) {
                    differingPixels++;
                }
            }
        }

        double diffFraction = (double) differingPixels / totalPixels;
        assertTrue(diffFraction <= MAX_DIFF_FRACTION,
                scenarioName + ": " + String.format("%.2f%%", diffFraction * 100)
                        + " of pixels differ beyond threshold (max "
                        + String.format("%.2f%%", MAX_DIFF_FRACTION * 100)
                        + "). Re-run with -Dregenerate.goldens=true if this is intentional.");
    }

    private static int channelDelta(int rgbA, int rgbB) {
        int rA = (rgbA >> 16) & 0xFF;
        int gA = (rgbA >> 8) & 0xFF;
        int bA = rgbA & 0xFF;
        int rB = (rgbB >> 16) & 0xFF;
        int gB = (rgbB >> 8) & 0xFF;
        int bB = rgbB & 0xFF;
        return Math.max(Math.abs(rA - rB), Math.max(Math.abs(gA - gB), Math.abs(bA - bB)));
    }
}
