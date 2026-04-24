package ai.skaile.mcpo.ppt.tooling;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/**
 * Structural invariants on the serialized .pptx XML. These run fast (no soffice, no
 * Docker) and catch the class of bug where authoring code leaves layout-inherited
 * content — placeholder prompts, orphan shapes — in the saved deck.
 *
 * <p>Paired with {@code PptRenderGoldenTest} (slower, pixel-level). This class is the
 * first line of defence: if the XML already contains "Click to edit Master", the
 * render is guaranteed to show it and there's no point booting soffice.
 */
class PptStructuralRegressionTest {

    private static final String PLACEHOLDER_PROMPT_MARKER = "Click to edit Master";
    private static final String PLACEHOLDER_ADD_TEXT_MARKER = "Click to add text";
    // <a:p> / <a:p/> / <a:p attr="..."> — NOT <a:pPr>, <a:pStyle>, etc.
    private static final Pattern PARAGRAPH_OPEN = Pattern.compile("<a:p[\\s/>]");

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createDocumentWithTitleLeavesNoPlaceholderPromptInSlideXml() throws Exception {
        Path pptx = exportDeck(service -> {
            ObjectNode args = mapper.createObjectNode();
            args.put("title", "Quarterly Review");
            return service.call("ppt.create_document", args);
        });

        assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_PROMPT_MARKER);
        assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_ADD_TEXT_MARKER);
    }

    @Test
    void addSlideLeavesNoPlaceholderPromptInSlideXml() throws Exception {
        Path pptx = exportDeck(service -> {
            ObjectNode createArgs = mapper.createObjectNode();
            ToolCallResult created = service.call("ppt.create_document", createArgs);
            String documentId = created.payload().path("document_id").asText();

            ObjectNode addArgs = mapper.createObjectNode();
            addArgs.put("document_id", documentId);
            return service.call("ppt.add_slide", addArgs);
        });

        assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_PROMPT_MARKER);
        assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_ADD_TEXT_MARKER);
    }

    @Test
    void generatePresentationLeavesNoPlaceholderPromptAcrossAllSlides() throws Exception {
        Path pptx = Files.createTempFile("ppt-mcp-gen", ".pptx");
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("title", "Deck title");
            ArrayNode titles = args.putArray("slide_titles");
            titles.add("Cover");
            titles.add("Agenda");
            titles.add("Findings");
            titles.add("Next steps");
            args.put("output_path", pptx.toString());

            ToolCallResult result = service.call("ppt.generate_presentation", args);
            assertTrue(result.success(),
                    "generate_presentation failed: " + result.payload());

            assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_PROMPT_MARKER);
            assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_ADD_TEXT_MARKER);
            assertSlideCountAtLeast(pptx, 4);
        } finally {
            Files.deleteIfExists(pptx);
        }
    }

    @Test
    void importMarkdownOutlineLeavesNoPlaceholderPromptAcrossAllSlides() throws Exception {
        Path pptx = Files.createTempFile("ppt-mcp-md", ".pptx");
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("markdown_text", "# Slide one\n- point A\n- point B\n\n# Slide two\n- point C\n");
            args.put("output_path", pptx.toString());

            ToolCallResult result = service.call("ppt.import_markdown_outline", args);
            assertTrue(result.success(),
                    "import_markdown_outline failed: " + result.payload());

            assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_PROMPT_MARKER);
            assertSlideXmlsDoNotContain(pptx, PLACEHOLDER_ADD_TEXT_MARKER);
            assertSlideCountAtLeast(pptx, 2);
        } finally {
            Files.deleteIfExists(pptx);
        }
    }

    @Test
    void exportedDeckContainsAtLeastOneSlideMaster() throws Exception {
        Path pptx = exportDeck(service -> {
            ObjectNode args = mapper.createObjectNode();
            return service.call("ppt.create_document", args);
        });

        List<String> entries = listZipEntries(pptx);
        boolean hasMaster = entries.stream()
                .anyMatch(name -> name.startsWith("ppt/slideMasters/slideMaster")
                        && name.endsWith(".xml"));
        assertTrue(hasMaster,
                "exported pptx has no slideMaster: entries=" + entries);
    }

    @Test
    void everyTxBodyInSlideXmlContainsAtLeastOneParagraph() throws Exception {
        // PowerPoint refuses to open a deck whose <p:txBody> has no <a:p/> child,
        // even though LibreOffice tolerates it. The placeholder-clearing path
        // (PptSlideBuilder.clearUnfilledPlaceholders) must preserve an empty
        // paragraph after removing the inherited prompt text. Assert on every
        // txBody element across a few authoring paths.
        Path pptx = Files.createTempFile("ppt-mcp-validity", ".pptx");
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("title", "deck");
            ArrayNode titles = args.putArray("slide_titles");
            titles.add("a");
            titles.add("b");
            args.put("output_path", pptx.toString());
            assertTrue(service.call("ppt.generate_presentation", args).success());

            try (ZipFile zip = new ZipFile(pptx.toFile())) {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith("ppt/slides/slide") || !name.endsWith(".xml")) {
                        continue;
                    }
                    String xml;
                    try (InputStream in = zip.getInputStream(entry)) {
                        xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    }
                    int txBodyStart = 0;
                    while ((txBodyStart = xml.indexOf("<p:txBody>", txBodyStart)) >= 0) {
                        int txBodyEnd = xml.indexOf("</p:txBody>", txBodyStart);
                        assertTrue(txBodyEnd > txBodyStart,
                                name + ": unterminated <p:txBody>");
                        String body = xml.substring(txBodyStart, txBodyEnd);
                        assertTrue(PARAGRAPH_OPEN.matcher(body).find(),
                                name + ": <p:txBody> has no <a:p/> paragraph — PowerPoint will reject this deck. Body was: " + body);
                        txBodyStart = txBodyEnd;
                    }
                }
            }
        } finally {
            Files.deleteIfExists(pptx);
        }
    }

    // --- helpers ---

    @FunctionalInterface
    private interface DeckAction {
        ToolCallResult apply(PptToolService service) throws IOException;
    }

    private Path exportDeck(DeckAction action) throws Exception {
        PptToolService service = new PptToolService();
        ToolCallResult result = action.apply(service);
        assertTrue(result.success(),
                "setup action failed: " + result.payload());

        String documentId = extractDocumentId(result);

        Path pptx = Files.createTempFile("ppt-mcp-struct", ".pptx");
        try {
            ObjectNode export = mapper.createObjectNode();
            export.put("document_id", documentId);
            export.put("output_path", pptx.toString());
            ToolCallResult saved = service.call("ppt.export_document", export);
            assertTrue(saved.success(),
                    "export failed: " + saved.payload());
            return pptx;
        } catch (Exception e) {
            Files.deleteIfExists(pptx);
            throw e;
        }
    }

    private String extractDocumentId(ToolCallResult result) {
        String id = result.payload().path("document_id").asText("");
        if (id.isBlank()) {
            throw new IllegalStateException(
                    "result has no document_id: " + result.payload());
        }
        return id;
    }

    private void assertSlideXmlsDoNotContain(Path pptx, String needle) throws IOException {
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith("ppt/slides/slide") || !name.endsWith(".xml")) {
                    continue;
                }
                String xml;
                try (InputStream in = zip.getInputStream(entry)) {
                    xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                assertFalse(xml.contains(needle),
                        "slide XML " + name + " contains forbidden marker '" + needle
                                + "' — placeholder prompt leaked into saved deck");
            }
        }
    }

    private void assertSlideCountAtLeast(Path pptx, int expected) throws IOException {
        int seen = 0;
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                    seen++;
                }
            }
        }
        assertTrue(seen >= expected,
                "expected at least " + expected + " slides, got " + seen);
    }

    private List<String> listZipEntries(Path pptx) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipFile zip = new ZipFile(pptx.toFile())) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                names.add(entries.nextElement().getName());
            }
        }
        return names;
    }
}
