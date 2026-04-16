package ai.skaile.mcpo.ppt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PptToolServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void createAddSaveAndReopenFlow() throws Exception {
        PptToolService service = new PptToolService();

        ObjectNode createArgs = mapper.createObjectNode();
        createArgs.put("title", "Demo");
        ToolCallResult created = service.call("ppt.create_document", createArgs);
        assertTrue(created.success());
        String documentId = created.payload().path("document_id").asText();
        assertFalse(documentId.isBlank());

        ObjectNode addSlideArgs = mapper.createObjectNode();
        addSlideArgs.put("document_id", documentId);
        addSlideArgs.put("title", "Agenda");
        ToolCallResult added = service.call("ppt.add_slide", addSlideArgs);
        assertTrue(added.success());
        assertEquals(2, added.payload().path("slide_count").asInt());

        Path tempPptx = Files.createTempFile("ppt-mcp-test", ".pptx");
        ObjectNode saveArgs = mapper.createObjectNode();
        saveArgs.put("document_id", documentId);
        saveArgs.put("output_path", tempPptx.toString());
        ToolCallResult saved = service.call("ppt.save_document", saveArgs);
        assertTrue(saved.success());
        assertTrue(Files.exists(tempPptx));

        ObjectNode openArgs = mapper.createObjectNode();
        openArgs.put("path", tempPptx.toString());
        ToolCallResult reopened = service.call("ppt.open_document", openArgs);
        assertTrue(reopened.success());
        assertEquals(2, reopened.payload().path("slide_count").asInt());
    }

    @Test
    void updateTextAndRenderPng() throws Exception {
        PptToolService service = new PptToolService();

        ObjectNode createArgs = mapper.createObjectNode();
        ToolCallResult created = service.call("ppt.create_document", createArgs);
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Hello Q1");
        addTextbox.put("x", 50);
        addTextbox.put("y", 50);
        addTextbox.put("width", 300);
        addTextbox.put("height", 80);
        ToolCallResult textbox = service.call("ppt.add_textbox", addTextbox);
        assertTrue(textbox.success());

        ObjectNode updateArgs = mapper.createObjectNode();
        updateArgs.put("document_id", documentId);
        updateArgs.put("slide_index", 0);
        updateArgs.put("old_text", "Q1");
        updateArgs.put("new_text", "Q2");
        ToolCallResult updated = service.call("ppt.update_text", updateArgs);
        assertTrue(updated.success());

        Path out = Files.createTempFile("ppt-render", ".png");
        ObjectNode renderArgs = mapper.createObjectNode();
        renderArgs.put("document_id", documentId);
        renderArgs.put("slide_index", 0);
        renderArgs.put("output_path", out.toString());
        ToolCallResult rendered = service.call("ppt.render_slide_image", renderArgs);
        assertTrue(rendered.success());
        assertTrue(Files.size(out) > 0);
    }

    @Test
    void findTextAndRenderSelection() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Revenue growth 2026");
        addTextbox.put("x", 120);
        addTextbox.put("y", 120);
        addTextbox.put("width", 400);
        addTextbox.put("height", 80);
        ToolCallResult textbox = service.call("ppt.add_textbox", addTextbox);
        assertTrue(textbox.success());

        ObjectNode findArgs = mapper.createObjectNode();
        findArgs.put("document_id", documentId);
        findArgs.put("query", "growth");
        ToolCallResult findResult = service.call("ppt.find_text", findArgs);
        assertTrue(findResult.success());
        assertTrue(findResult.payload().path("count").asInt() > 0);

        ObjectNode slideContentArgs = mapper.createObjectNode();
        slideContentArgs.put("document_id", documentId);
        slideContentArgs.put("slide_index", 0);
        ToolCallResult slideContent = service.call("ppt.get_slide_content", slideContentArgs);
        assertTrue(slideContent.success());
        int shapeCount = slideContent.payload().path("shapes").size();
        assertTrue(shapeCount > 0);

        Path outPng = Files.createTempFile("ppt-selection", ".png");
        ObjectNode renderSelectionArgs = mapper.createObjectNode();
        renderSelectionArgs.put("document_id", documentId);
        renderSelectionArgs.put("slide_index", 0);
        renderSelectionArgs.put("output_path", outPng.toString());
        renderSelectionArgs.putArray("shape_indices").add(shapeCount - 1);
        ToolCallResult selectionRendered = service.call("ppt.render_selection_image", renderSelectionArgs);
        assertTrue(selectionRendered.success());
        assertTrue(Files.size(outPng) > 0);
    }

    @Test
    void invalidSaveFormatReturnsError() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();
        assertNotEquals("", documentId);

        ObjectNode saveArgs = mapper.createObjectNode();
        saveArgs.put("document_id", documentId);
        saveArgs.put("format", "docx");
        ToolCallResult result = service.call("ppt.save_document", saveArgs);
        assertFalse(result.success());
        assertEquals("error", result.payload().path("status").asText());
        assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
        assertFalse(result.payload().path("retriable").asBoolean(true));
    }

    @Test
    void uploadTemplateSetDefaultAndGenerateWithoutTemplatePath() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult base = service.call("ppt.create_document", mapper.createObjectNode());
        String baseId = base.payload().path("document_id").asText();

        Path templatePath = Files.createTempFile("mcpo-default-template", ".pptx");
        ObjectNode saveTemplateArgs = mapper.createObjectNode();
        saveTemplateArgs.put("document_id", baseId);
        saveTemplateArgs.put("output_path", templatePath.toString());
        ToolCallResult templateSaved = service.call("ppt.save_document", saveTemplateArgs);
        assertTrue(templateSaved.success());

        ObjectNode uploadArgs = mapper.createObjectNode();
        uploadArgs.put("source_path", templatePath.toString());
        uploadArgs.put("template_name", "team-default-template.pptx");
        uploadArgs.put("make_default", true);
        ToolCallResult uploaded = service.call("ppt.upload_template", uploadArgs);
        assertTrue(uploaded.success());
        assertFalse(uploaded.payload().path("default_template_path").asText().isBlank());

        ObjectNode getDefaultArgs = mapper.createObjectNode();
        ToolCallResult defaultTemplate = service.call("ppt.get_default_template", getDefaultArgs);
        assertTrue(defaultTemplate.success());
        assertTrue(defaultTemplate.payload().path("has_default_template").asBoolean());

        ObjectNode generateArgs = mapper.createObjectNode();
        generateArgs.putArray("slide_titles").add("Intro").add("Plan").add("Summary");
        ToolCallResult generated = service.call("ppt.generate_presentation", generateArgs);
        assertTrue(generated.success());
        assertEquals(3, generated.payload().path("slide_count").asInt());
        assertFalse(generated.payload().path("template_path").asText().isBlank());
    }

    @Test
    void uploadTemplateRejectsPathLikeTemplateName() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult base = service.call("ppt.create_document", mapper.createObjectNode());
        String baseId = base.payload().path("document_id").asText();

        Path templatePath = Files.createTempFile("mcpo-template-pathlike", ".pptx");
        ObjectNode saveTemplateArgs = mapper.createObjectNode();
        saveTemplateArgs.put("document_id", baseId);
        saveTemplateArgs.put("output_path", templatePath.toString());
        ToolCallResult templateSaved = service.call("ppt.save_document", saveTemplateArgs);
        assertTrue(templateSaved.success());

        ObjectNode uploadArgs = mapper.createObjectNode();
        uploadArgs.put("source_path", templatePath.toString());
        uploadArgs.put("template_name", "..\\outside-template");
        ToolCallResult uploaded = service.call("ppt.upload_template", uploadArgs);

        assertFalse(uploaded.success());
        assertEquals("error", uploaded.payload().path("status").asText());
        assertEquals("VALIDATION_ERROR", uploaded.payload().path("code").asText());
    }

    @Test
    void insertImageAddsPictureShape() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        Path imagePath = Files.createTempFile("mcpo-image", ".png");
        BufferedImage img = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        var g = img.createGraphics();
        g.setColor(Color.ORANGE);
        g.fillRect(0, 0, 40, 30);
        g.dispose();
        ImageIO.write(img, "png", imagePath.toFile());

        ObjectNode insertArgs = mapper.createObjectNode();
        insertArgs.put("document_id", documentId);
        insertArgs.put("slide_index", 0);
        insertArgs.put("image_path", imagePath.toString());
        insertArgs.put("x", 100);
        insertArgs.put("y", 100);
        insertArgs.put("width", 200);
        insertArgs.put("height", 100);
        ToolCallResult inserted = service.call("ppt.insert_image", insertArgs);
        assertTrue(inserted.success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult slideContent = service.call("ppt.get_slide_content", contentArgs);
        assertTrue(slideContent.success());
        assertTrue(slideContent.payload().path("shapes").size() > 0);
    }

    @Test
    void setAndGetSlideNotesRoundTrip() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode setNotesArgs = mapper.createObjectNode();
        setNotesArgs.put("document_id", documentId);
        setNotesArgs.put("slide_index", 0);
        setNotesArgs.put("notes_text", "Presenter note: focus on Q2 growth.");
        ToolCallResult setNotes = service.call("ppt.set_slide_notes", setNotesArgs);

        ObjectNode getNotesArgs = mapper.createObjectNode();
        getNotesArgs.put("document_id", documentId);
        getNotesArgs.put("slide_index", 0);
        ToolCallResult getNotes = service.call("ppt.get_slide_notes", getNotesArgs);

        if (setNotes.success()) {
            assertTrue(getNotes.success());
            assertEquals("Presenter note: focus on Q2 growth.", getNotes.payload().path("notes_text").asText());
        } else {
            assertEquals("error", setNotes.payload().path("status").asText());
        }
    }

    @Test
    void tableCellSetAndGetRoundTrip() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTableArgs = mapper.createObjectNode();
        addTableArgs.put("document_id", documentId);
        addTableArgs.put("slide_index", 0);
        addTableArgs.put("rows", 2);
        addTableArgs.put("cols", 2);
        addTableArgs.put("x", 50);
        addTableArgs.put("y", 150);
        addTableArgs.put("width", 400);
        addTableArgs.put("height", 120);
        ToolCallResult tableAdded = service.call("ppt.add_table", addTableArgs);
        assertTrue(tableAdded.success());

        int shapeIndex = tableAdded.payload().path("shape_index").asInt(-1);
        assertTrue(shapeIndex >= 0);

        ObjectNode setCellArgs = mapper.createObjectNode();
        setCellArgs.put("document_id", documentId);
        setCellArgs.put("slide_index", 0);
        setCellArgs.put("shape_index", shapeIndex);
        setCellArgs.put("row_index", 1);
        setCellArgs.put("col_index", 1);
        setCellArgs.put("text", "Quarterly target");
        ToolCallResult setCell = service.call("ppt.set_table_cell", setCellArgs);
        assertTrue(setCell.success());

        ObjectNode getCellArgs = mapper.createObjectNode();
        getCellArgs.put("document_id", documentId);
        getCellArgs.put("slide_index", 0);
        getCellArgs.put("shape_index", shapeIndex);
        getCellArgs.put("row_index", 1);
        getCellArgs.put("col_index", 1);
        ToolCallResult getCell = service.call("ppt.get_table_cell", getCellArgs);
        assertTrue(getCell.success());
        assertEquals("Quarterly target", getCell.payload().path("text").asText());
    }

    @Test
    void setTextStyleUpdatesTextShape() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Styled title");
        addTextbox.put("x", 60);
        addTextbox.put("y", 60);
        addTextbox.put("width", 280);
        addTextbox.put("height", 70);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult before = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = before.payload().path("shapes").size() - 1;

        ObjectNode styleArgs = mapper.createObjectNode();
        styleArgs.put("document_id", documentId);
        styleArgs.put("slide_index", 0);
        styleArgs.put("shape_index", shapeIndex);
        styleArgs.put("bold", true);
        styleArgs.put("italic", true);
        styleArgs.put("underline", false);
        styleArgs.put("font_size", 24);
        styleArgs.put("font_color", "#003366");
        ToolCallResult styled = service.call("ppt.set_text_style", styleArgs);
        assertTrue(styled.success());
    }

    @Test
    void schemaValidationRejectsUnexpectedArgument() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addSlideArgs = mapper.createObjectNode();
        addSlideArgs.put("document_id", documentId);
        addSlideArgs.put("title", "Agenda");
        addSlideArgs.put("unexpected", "not-allowed");
        ToolCallResult result = service.call("ppt.add_slide", addSlideArgs);

        assertFalse(result.success());
        assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
    }
}
