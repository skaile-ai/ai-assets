package ai.skaile.mcpo.ppt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerPptMcpServerSmokeTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String IMAGE_TAG = "ppt-mcp-server:itest";
    private static final String CONTAINER_WORKSPACE_ROOT = "/workspace/resources";
    private static final Path MODULE_DIR = Path.of("").toAbsolutePath().normalize();
    private static final List<String> EXPECTED_TOOLS = List.of(
            "ppt.create_document",
            "ppt.open_document",
            "ppt.close_document",
            "ppt.get_document_info",
            "ppt.set_page_setup",
            "ppt.list_slides",
            "ppt.reorder_slides",
            "ppt.get_slide_content",
            "ppt.add_slide",
            "ppt.duplicate_slide",
            "ppt.delete_slides",
            "ppt.merge_presentations",
            "ppt.update_text",
            "ppt.replace_text_globally",
            "ppt.add_textbox",
            "ppt.add_shape",
            "ppt.insert_image",
            "ppt.replace_image",
            "ppt.get_slide_notes",
            "ppt.set_slide_notes",
            "ppt.add_table",
            "ppt.get_table",
            "ppt.edit_table",
            "ppt.set_text",
            "ppt.move_shape",
            "ppt.clone_shape",
            "ppt.resize_shape",
            "ppt.add_hyperlink",
            "ppt.set_slide_background",
            "ppt.import_markdown_outline",
            "ppt.transaction_begin",
            "ppt.transaction_commit",
            "ppt.transaction_rollback",
            "ppt.get_slide_metrics",
            "ppt.export_document",
            "ppt.render_slide",
            "ppt.render_all_slides",
            "ppt.find_text",
            "ppt.upload_template",
            "ppt.set_default_template",
            "ppt.get_default_template",
            "ppt.generate_presentation",
            "ppt.delete_shape",
            "ppt.get_shape_properties",
            "ppt.set_shape_style",
            "ppt.set_picture_effects",
            "ppt.set_document_metadata",
            "ppt.set_slide_layout",
            "ppt.set_shape_z_order",
            "ppt.list_charts",
            "ppt.update_chart_data",
            "ppt.capabilities");

    @BeforeAll
    static void buildDockerImage() throws Exception {
        Assumptions.assumeTrue(isDockerAvailable(), "Docker is not available in this environment");
        runCommand(List.of("docker", "build", "-t", IMAGE_TAG, "."), MODULE_DIR);
    }

    @AfterAll
    static void cleanupImage() throws Exception {
        if (!isDockerAvailable()) {
            return;
        }
        runCommand(List.of("docker", "image", "rm", "-f", IMAGE_TAG), MODULE_DIR, true);
    }

    @Test
    void dockerImageRespondsAndExposesCorePptWorkflows() throws Exception {
        Path workspace = Files.createTempDirectory("ppt-docker-smoke");
        makeWorldWritable(workspace);

        try (DockerSession session = DockerSession.start(IMAGE_TAG, workspace)) {
            JsonNode initializeResponse = session.request(request("1", "initialize", emptyObject()));
            assertEquals("2.0", initializeResponse.path("jsonrpc").asText());
            assertEquals("1", initializeResponse.path("id").asText());
            assertEquals("2024-11-05", initializeResponse.path("result").path("protocolVersion").asText());

            JsonNode toolsResponse = session.request(request("2", "tools/list", emptyObject()));
            assertEquals("2", toolsResponse.path("id").asText());
            Set<String> toolNames = new HashSet<>();
            for (JsonNode tool : toolsResponse.path("result").path("tools")) {
                toolNames.add(tool.path("name").asText());
            }
            assertEquals(Set.copyOf(EXPECTED_TOOLS), toolNames);

            JsonNode created = callTool(session, "ppt.create_document", objectNode("title", "Docker smoke test"));
            String documentId = created.path("document_id").asText();
            assertFalse(documentId.isBlank());

            JsonNode info = callTool(session, "ppt.get_document_info", objectNode("document_id", documentId));
            assertEquals(documentId, info.path("document_id").asText());
            assertEquals(1, info.path("slide_count").asInt());

            JsonNode addedSlide = callTool(session, "ppt.add_slide", objectNode("document_id", documentId, "title", "Agenda"));
            assertEquals(2, addedSlide.path("slide_count").asInt());

            JsonNode textbox = callTool(session, "ppt.add_textbox", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "text", "Quarterly growth is strong",
                    "x", 50,
                    "y", 50,
                    "width", 340,
                    "height", 80));
                assertEquals("Text box added", textbox.path("message").asText());
                JsonNode slideContent = callTool(session, "ppt.get_slide_content", objectNode(
                    "document_id", documentId,
                    "slide_index", 0));
                int textboxShapeIndex = slideContent.path("shapes").size() - 1;
            assertTrue(textboxShapeIndex >= 0);

            JsonNode updated = callTool(session, "ppt.update_text", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "old_text", "growth",
                    "new_text", "revenue"));
            assertEquals("success", updated.path("status").asText());

            JsonNode found = callTool(session, "ppt.find_text", objectNode("document_id", documentId, "query", "revenue"));
            assertTrue(found.path("count").asInt() > 0);

            JsonNode notesUpdated = callToolMaybeError(session, "ppt.set_slide_notes", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "notes_text", "Presenter note from Docker test."));
            if ("success".equals(notesUpdated.path("status").asText())) {
                JsonNode notes = callTool(session, "ppt.get_slide_notes", objectNode("document_id", documentId, "slide_index", 0));
                assertEquals("Presenter note from Docker test.", notes.path("notes_text").asText());
            } else {
                assertEquals("error", notesUpdated.path("status").asText());
            }

            JsonNode table = callTool(session, "ppt.add_table", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "rows", 2,
                    "cols", 2,
                    "x", 50,
                    "y", 160,
                    "width", 360,
                    "height", 120));
            int tableShapeIndex = table.path("shape_index").asInt(-1);
            assertTrue(tableShapeIndex >= 0);

            callTool(session, "ppt.edit_table", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", tableShapeIndex,
                    "operation", "set_cell",
                    "row", 1,
                    "col", 1,
                    "text", "Target met"));

            JsonNode tableSnapshot = callTool(session, "ppt.get_table", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", tableShapeIndex));
            assertEquals("Target met",
                    tableSnapshot.path("cells").path(1).path(1).path("text").asText());

            // Phase 4: merge a 2x2 anchored block + apply a per-cell border with dash style.
            JsonNode merged = callTool(session, "ppt.edit_table", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", tableShapeIndex,
                    "operation", "merge_cells",
                    "start_row", 0,
                    "start_col", 0,
                    "end_row", 1,
                    "end_col", 1));
            assertEquals(2, merged.path("row_span").asInt());
            assertEquals(2, merged.path("col_span").asInt());

            ObjectNode borderArgs = objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", tableShapeIndex,
                    "operation", "set_cell_border",
                    "row", 0,
                    "col", 0,
                    "color", "#226699",
                    "width", 2.0,
                    "dash_style", "dashdot");
            borderArgs.putArray("sides").add("all");
            JsonNode bordered = callTool(session, "ppt.edit_table", borderArgs);
            assertEquals(4, bordered.path("sides_applied").asInt());

            JsonNode style = callTool(session, "ppt.set_text", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", textboxShapeIndex,
                    "bold", true,
                    "italic", true,
                    "underline", false,
                    "font_size", 24,
                    "font_color", "#003366"));
            assertEquals("success", style.path("status").asText());

                Path imagePath = workspace.resolve("input/logo.png");
            Files.createDirectories(imagePath.getParent());
            writeSampleImage(imagePath);

            JsonNode insertedImage = callTool(session, "ppt.insert_image", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "image_path", containerPath("input/logo.png"),
                    "x", 420,
                    "y", 50,
                    "width", 120,
                    "height", 90));
            int imageShapeIndex = insertedImage.path("shape_index").asInt(-1);
            assertTrue(imageShapeIndex >= 0);

            // Phase 4: gradient fill on a primitive shape and picture effects on the inserted image.
            JsonNode rect = callTool(session, "ppt.add_shape", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_type", "rectangle",
                    "x", 50, "y", 290,
                    "width", 200, "height", 60));
            int rectShapeIndex = rect.path("shape_index").asInt(-1);
            ObjectNode gradientStyle = objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", rectShapeIndex,
                    "fill_type", "gradient");
            ObjectNode gradient = MAPPER.createObjectNode();
            gradient.put("type", "linear");
            gradient.put("angle", 45.0);
            ArrayNode stops = gradient.putArray("stops");
            ObjectNode stop1 = stops.addObject();
            stop1.put("color", "#FF6600"); stop1.put("position", 0.0);
            ObjectNode stop2 = stops.addObject();
            stop2.put("color", "#003366"); stop2.put("position", 1.0);
            gradientStyle.set("fill_gradient", gradient);
            JsonNode gradientResult = callTool(session, "ppt.set_shape_style", gradientStyle);
            assertEquals("success", gradientResult.path("status").asText());

            ObjectNode pictureFx = objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", imageShapeIndex,
                    "alpha", 0.7);
            ObjectNode crop = MAPPER.createObjectNode();
            crop.put("left", 0.05); crop.put("right", 0.05);
            crop.put("top", 0.05); crop.put("bottom", 0.05);
            pictureFx.set("crop", crop);
            ObjectNode recolor = MAPPER.createObjectNode();
            recolor.put("mode", "grayscale");
            pictureFx.set("recolor", recolor);
            JsonNode pictureFxResult = callTool(session, "ppt.set_picture_effects", pictureFx);
            assertTrue(pictureFxResult.path("crop_applied").asBoolean());
            assertTrue(pictureFxResult.path("alpha_applied").asBoolean());
            assertTrue(pictureFxResult.path("recolor_applied").asBoolean());

            // Phase 4: clone_shape now works on any shape, e.g. the table built earlier.
            JsonNode cloned = callTool(session, "ppt.clone_shape", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "shape_index", tableShapeIndex,
                    "offset_x", 30.0,
                    "offset_y", 30.0));
            assertTrue(cloned.path("shape_index").asInt(-1) >= 0);

                Path pptxPath = workspace.resolve("output/report.pptx");
            JsonNode savedPptx = callTool(session, "ppt.export_document", objectNode(
                    "document_id", documentId,
                    "output_path", containerPath("output/report.pptx")));
            assertEquals("pptx", savedPptx.path("format").asText());
            assertTrue(Files.exists(pptxPath));

                JsonNode reopened = callTool(session, "ppt.open_document", objectNode("path", containerPath("output/report.pptx")));
            String reopenedDocumentId = reopened.path("document_id").asText();
            assertFalse(reopenedDocumentId.isBlank());

            Path pdfPath = workspace.resolve("output/report.pdf");
            JsonNode savedPdf = callTool(session, "ppt.export_document", objectNode(
                    "document_id", documentId,
                    "output_path", containerPath("output/report.pdf"),
                    "format", "pdf"));
            assertEquals("pdf", savedPdf.path("format").asText());
            assertTrue(Files.exists(pdfPath));
            assertTrue(Files.size(pdfPath) > 0);

            // Phase 3: html export routes through the same soffice renderer.
            Path htmlPath = workspace.resolve("output/report.html");
            JsonNode savedHtml = callTool(session, "ppt.export_document", objectNode(
                    "document_id", documentId,
                    "output_path", containerPath("output/report.html"),
                    "format", "html"));
            assertEquals("html", savedHtml.path("format").asText());
            assertTrue(Files.exists(htmlPath));
            assertTrue(Files.size(htmlPath) > 0);

            // Phase 3: outline_text is pure POI traversal, no soffice needed.
            Path outlinePath = workspace.resolve("output/report.md");
            JsonNode savedOutline = callTool(session, "ppt.export_document", objectNode(
                    "document_id", documentId,
                    "output_path", containerPath("output/report.md"),
                    "format", "outline_text"));
            assertEquals("outline_text", savedOutline.path("format").asText());
            assertTrue(Files.exists(outlinePath));
            String outlineContent = Files.readString(outlinePath);
            assertTrue(outlineContent.contains("#"),
                    "outline must emit at least one markdown heading");

            // Phase 3: png_batch writes one image per slide into a directory.
            Path batchDir = workspace.resolve("output/png-batch");
            Files.createDirectories(batchDir);
            makeWorldWritable(batchDir);
            JsonNode savedBatch = callTool(session, "ppt.export_document", objectNode(
                    "document_id", documentId,
                    "output_path", containerPath("output/png-batch"),
                    "format", "png_batch"));
            assertEquals("png_batch", savedBatch.path("format").asText());
            assertTrue(savedBatch.path("slide_count").asInt() >= 2);
            try (var stream = Files.list(batchDir)) {
                long produced = stream
                        .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".png"))
                        .count();
                assertTrue(produced >= 2,
                        "png_batch must produce one png per slide; got " + produced);
            }

            // Phase 3: high-fidelity render through soffice. Build a CJK+emoji slide,
            // render it high-fidelity, then sample pixels to confirm the image isn't blank.
            JsonNode cjkSlide = callTool(session, "ppt.add_slide", objectNode(
                    "document_id", documentId,
                    "title", "こんにちは 世界 \uD83D\uDCCA"));
            int cjkIndex = cjkSlide.path("slide_count").asInt() - 1;
            Path hiFidelityPng = workspace.resolve("output/slide-cjk-hi.png");
            JsonNode hiFidelity = callTool(session, "ppt.render_slide", objectNode(
                    "document_id", documentId,
                    "slide_index", cjkIndex,
                    "output_path", containerPath("output/slide-cjk-hi.png"),
                    "format", "png",
                    "fidelity", "high"));
            assertEquals("high", hiFidelity.path("fidelity").asText());
            assertTrue(Files.exists(hiFidelityPng));
            assertTrue(Files.size(hiFidelityPng) > 0);
            java.awt.image.BufferedImage hiFidelityImage =
                    javax.imageio.ImageIO.read(hiFidelityPng.toFile());
            assertTrue(hiFidelityImage != null && hiFidelityImage.getWidth() > 0);
            boolean hasNonWhitePixel = false;
            int w = hiFidelityImage.getWidth();
            int h = hiFidelityImage.getHeight();
            for (int y = 0; y < h && !hasNonWhitePixel; y += Math.max(1, h / 20)) {
                for (int x = 0; x < w && !hasNonWhitePixel; x += Math.max(1, w / 20)) {
                    int rgb = hiFidelityImage.getRGB(x, y) & 0xFFFFFF;
                    if (rgb != 0xFFFFFF) {
                        hasNonWhitePixel = true;
                    }
                }
            }
            assertTrue(hasNonWhitePixel,
                    "high-fidelity CJK render must contain non-white pixels (tofu indicates missing fonts)");

            Path slidePng = workspace.resolve("output/slide.png");
            JsonNode slideImage = callTool(session, "ppt.render_slide", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "output_path", containerPath("output/slide.png"),
                    "width", 1280,
                    "height", 720));
            assertEquals("png", slideImage.path("format").asText());
            assertTrue(Files.exists(slidePng));

            Path slideSvg = workspace.resolve("output/slide.svg");
            JsonNode slideVector = callToolMaybeError(session, "ppt.render_slide", objectNode(
                    "document_id", documentId,
                    "slide_index", 0,
                    "output_path", containerPath("output/slide.svg"),
                    "format", "svg",
                    "width", 1280,
                    "height", 720));
            if ("success".equals(slideVector.path("status").asText())) {
                assertEquals("svg", slideVector.path("format").asText());
                assertTrue(Files.exists(slideSvg));
            } else {
                assertEquals("error", slideVector.path("status").asText());
            }

            // Phase 5: list_charts + update_chart_data. Build a chart-bearing fixture on the
            // host via ChartFixtureBuilder, drop it in the bind-mount, then open + list +
            // update + high-fidelity render. Compare pre-/post-update PNGs to confirm the
            // rendered chart actually changed.
            Path chartFixture = workspace.resolve("output/chart-sample.pptx");
            ai.skaile.mcpo.ppt.tooling.operations.ChartFixtureBuilder
                    .writeClusteredColumn(chartFixture);
            JsonNode chartOpened = callTool(session, "ppt.open_document", objectNode(
                    "path", containerPath("output/chart-sample.pptx")));
            String chartDocId = chartOpened.path("document_id").asText();
            assertFalse(chartDocId.isBlank());

            JsonNode listed = callTool(session, "ppt.list_charts", objectNode(
                    "document_id", chartDocId));
            assertEquals(1, listed.path("charts").size());
            JsonNode chartInfo = listed.path("charts").get(0);
            assertEquals("column", chartInfo.path("chart_type").asText());
            assertTrue(chartInfo.path("has_embedded_workbook").asBoolean());
            int chartSlide = chartInfo.path("slide_index").asInt();
            int chartShape = chartInfo.path("shape_index").asInt();

            Path beforePng = workspace.resolve("output/chart-before.png");
            JsonNode beforeRender = callTool(session, "ppt.render_slide", objectNode(
                    "document_id", chartDocId,
                    "slide_index", chartSlide,
                    "output_path", containerPath("output/chart-before.png"),
                    "format", "png",
                    "fidelity", "high"));
            assertEquals("success", beforeRender.path("status").asText());
            assertTrue(Files.exists(beforePng));
            long beforeSize = Files.size(beforePng);

            ObjectNode updateArgs = objectNode(
                    "document_id", chartDocId,
                    "slide_index", chartSlide,
                    "shape_index", chartShape);
            ArrayNode updatedSeries = updateArgs.putArray("series");
            for (int s = 0; s < 3; s++) {
                ObjectNode sNode = updatedSeries.addObject();
                sNode.put("name", "Updated " + (s + 1));
                ArrayNode vals = sNode.putArray("values");
                // Large values to force a visible change in the rendered chart.
                for (int i = 0; i < 4; i++) {
                    vals.add(1000.0 * (s + 1) + i * 100.0);
                }
            }
            JsonNode chartUpdated = callTool(session, "ppt.update_chart_data", updateArgs);
            assertEquals("success", chartUpdated.path("status").asText());
            assertEquals(3, chartUpdated.path("updated_series").asInt());

            Path afterPng = workspace.resolve("output/chart-after.png");
            JsonNode afterRender = callTool(session, "ppt.render_slide", objectNode(
                    "document_id", chartDocId,
                    "slide_index", chartSlide,
                    "output_path", containerPath("output/chart-after.png"),
                    "format", "png",
                    "fidelity", "high"));
            assertEquals("success", afterRender.path("status").asText());
            assertTrue(Files.exists(afterPng));
            long afterSize = Files.size(afterPng);
            // Identical PNGs are the failure we're guarding against: if the renderer
            // silently ignores the mutation, the two files would be byte-identical.
            // Tolerate minor JPEG-ish encoder drift via a byte-diff OR size-diff check.
            boolean bytesDiffer = !java.util.Arrays.equals(
                    Files.readAllBytes(beforePng), Files.readAllBytes(afterPng));
            assertTrue(bytesDiffer || afterSize != beforeSize,
                    "chart render before/after must differ after update_chart_data");

            JsonNode chartClosed = callTool(session, "ppt.close_document",
                    objectNode("document_id", chartDocId));
            assertEquals("success", chartClosed.path("status").asText());

            JsonNode templateUpload = callTool(session, "ppt.upload_template", objectNode(
                    "source_path", containerPath("output/report.pptx"),
                    "template_name", "docker-default-template.pptx",
                    "make_default", true));
            String templatePath = templateUpload.path("template_path").asText();
            assertFalse(templatePath.isBlank());

            JsonNode defaultTemplate = callTool(session, "ppt.get_default_template", emptyObject());
            assertTrue(defaultTemplate.path("has_default_template").asBoolean());
            assertEquals(templatePath, defaultTemplate.path("default_template_path").asText());

            JsonNode setDefaultTemplate = callTool(session, "ppt.set_default_template", objectNode("template_path", templatePath));
            assertEquals(templatePath, setDefaultTemplate.path("default_template_path").asText());

            JsonNode generated = callTool(session, "ppt.generate_presentation", objectNode(
                    "title", "Generated from Docker test",
                    "slide_titles", arrayNode("Intro", "Plan", "Summary"),
                    "output_path", containerPath("output/generated.pptx")));
                assertTrue(generated.path("slide_count").asInt() >= 3);
            assertTrue(Files.exists(workspace.resolve("output/generated.pptx")));

            JsonNode closed = callTool(session, "ppt.close_document", objectNode("document_id", reopenedDocumentId));
            assertEquals("success", closed.path("status").asText());
        } finally {
            deleteRecursively(workspace);
        }
    }

    private static JsonNode callTool(DockerSession session, String toolName, ObjectNode arguments) throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments);

        JsonNode response = session.request(request(session.nextId(), "tools/call", params));
        assertFalse(response.path("result").path("isError").asBoolean());
        assertEquals(toolName, response.path("result").path("structuredContent").path("tool_name").asText());
        return response.path("result").path("structuredContent");
    }

    private static JsonNode callToolMaybeError(DockerSession session, String toolName, ObjectNode arguments) throws Exception {
        ObjectNode params = MAPPER.createObjectNode();
        params.put("name", toolName);
        params.set("arguments", arguments);

        JsonNode response = session.request(request(session.nextId(), "tools/call", params));
        return response.path("result").path("structuredContent");
    }

    private static ObjectNode request(String id, String method, ObjectNode params) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("jsonrpc", "2.0");
        request.put("id", id);
        request.put("method", method);
        request.set("params", params);
        return request;
    }

    private static ObjectNode emptyObject() {
        return MAPPER.createObjectNode();
    }

    private static String containerPath(String relativePath) {
        return CONTAINER_WORKSPACE_ROOT + "/" + relativePath.replace('\\', '/');
    }

    private static ObjectNode objectNode(Object... entries) {
        if ((entries.length % 2) != 0) {
            throw new IllegalArgumentException("entries must contain key/value pairs");
        }
        ObjectNode node = MAPPER.createObjectNode();
        for (int i = 0; i < entries.length; i += 2) {
            String key = (String) entries[i];
            Object value = entries[i + 1];
            if (value instanceof String stringValue) {
                node.put(key, stringValue);
            } else if (value instanceof Integer integerValue) {
                node.put(key, integerValue);
            } else if (value instanceof Long longValue) {
                node.put(key, longValue);
            } else if (value instanceof Double doubleValue) {
                node.put(key, doubleValue);
            } else if (value instanceof Boolean booleanValue) {
                node.put(key, booleanValue);
            } else if (value instanceof JsonNode jsonNode) {
                node.set(key, jsonNode);
            } else {
                throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
            }
        }
        return node;
    }

    private static ArrayNode arrayNode(Object... values) {
        ArrayNode array = MAPPER.createArrayNode();
        for (Object value : values) {
            if (value instanceof String stringValue) {
                array.add(stringValue);
            } else if (value instanceof Integer integerValue) {
                array.add(integerValue);
            } else if (value instanceof Long longValue) {
                array.add(longValue);
            } else if (value instanceof Double doubleValue) {
                array.add(doubleValue);
            } else if (value instanceof Boolean booleanValue) {
                array.add(booleanValue);
            } else if (value instanceof JsonNode jsonNode) {
                array.add(jsonNode);
            } else {
                throw new IllegalArgumentException("Unsupported array value type: " + value.getClass());
            }
        }
        return array;
    }

    private static void writeSampleImage(Path imagePath) throws IOException {
        BufferedImage image = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, 64, 48);
        graphics.setColor(Color.BLACK);
        graphics.drawLine(0, 0, 63, 47);
        graphics.dispose();
        ImageIO.write(image, "png", imagePath.toFile());
    }

    private static void makeWorldWritable(Path path) {
        try {
            Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxrwxrwx");
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Best effort only.
        }
    }

    private static boolean isDockerAvailable() {
        try {
            Process process = new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            return process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0;
        } catch (IOException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void runCommand(List<String> command, Path workingDir) throws Exception {
        runCommand(command, workingDir, false);
    }

    private static void runCommand(List<String> command, Path workingDir, boolean ignoreFailure) throws Exception {
        Path output = Files.createTempFile("docker-command", ".log");
        try {
            Process process = new ProcessBuilder(command)
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .redirectOutput(output.toFile())
                    .start();
            boolean finished = process.waitFor(30, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new AssertionError("Timed out running command: " + String.join(" ", command));
            }
            if (process.exitValue() != 0 && !ignoreFailure) {
                throw new AssertionError("Command failed: " + String.join(" ", command) + System.lineSeparator()
                        + Files.readString(output));
            }
        } finally {
            Files.deleteIfExists(output);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static final class DockerSession implements AutoCloseable {
        private final Process process;
        private final BufferedWriter stdin;
        private final BlockingQueue<String> stdoutLines = new LinkedBlockingQueue<>();
        private final StringBuilder stderr = new StringBuilder();
        private final Thread stdoutPump;
        private final Thread stderrPump;
        private int counter = 3;

        private DockerSession(Process process) {
            this.process = process;
            this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8));

            this.stdoutPump = new Thread(() -> pumpLines(stdoutReader, stdoutLines));
            this.stdoutPump.setDaemon(true);
            this.stdoutPump.start();

            this.stderrPump = new Thread(() -> pumpLines(stderrReader, stderr));
            this.stderrPump.setDaemon(true);
            this.stderrPump.start();
        }

        static DockerSession start(String imageTag, Path workspace) throws Exception {
            Process process = new ProcessBuilder(
                    "docker",
                    "run",
                    "--rm",
                    "-i",
                    "-v",
                    workspace.toAbsolutePath().normalize() + ":/workspace/resources",
                    imageTag)
                    .directory(MODULE_DIR.toFile())
                    .start();

            DockerSession session = new DockerSession(process);
            session.assertRunning();
            return session;
        }

        String nextId() {
            return String.valueOf(counter++);
        }

        JsonNode request(ObjectNode request) throws Exception {
            String requestId = request.path("id").asText("");
            synchronized (stdin) {
                stdin.write(MAPPER.writeValueAsString(request));
                stdin.newLine();
                stdin.flush();
            }

            String line = stdoutLines.poll(5, TimeUnit.MINUTES);
            if (line == null) {
                throw new AssertionError("Timed out waiting for Docker MCP response. Stderr:\n" + currentStderr());
            }

            JsonNode response = MAPPER.readTree(line);
            assertEquals(requestId, response.path("id").asText(""));
            if (response.hasNonNull("error")) {
                throw new AssertionError("Server returned error: " + response.toPrettyString() + System.lineSeparator() + currentStderr());
            }
            return response;
        }

        private void assertRunning() {
            if (!process.isAlive()) {
                throw new AssertionError("Docker container exited early: " + currentStderr());
            }
        }

        @Override
        public void close() throws Exception {
            try {
                synchronized (stdin) {
                    stdin.close();
                }
            } catch (IOException ignored) {
                // Ignore shutdown errors.
            }

            process.destroy();
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                process.waitFor(30, TimeUnit.SECONDS);
            }

            stdoutPump.join(TimeUnit.SECONDS.toMillis(5));
            stderrPump.join(TimeUnit.SECONDS.toMillis(5));
        }

        private void pumpLines(BufferedReader reader, BlockingQueue<String> queue) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    queue.offer(line);
                }
            } catch (IOException ignored) {
                // Reader closed during shutdown.
            }
        }

        private void pumpLines(BufferedReader reader, StringBuilder buffer) {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (buffer) {
                        buffer.append(line).append(System.lineSeparator());
                    }
                }
            } catch (IOException ignored) {
                // Reader closed during shutdown.
            }
        }

        private String currentStderr() {
            synchronized (stderr) {
                return stderr.toString();
            }
        }
    }
}