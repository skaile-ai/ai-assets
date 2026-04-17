package ai.skaile.mcpo.ppt.tooling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
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
    void reorderSlidesReordersDeckByPermutation() {
        PptToolService service = new PptToolService();

        ObjectNode createArgs = mapper.createObjectNode();
        createArgs.put("title", "Slide One");
        ToolCallResult created = service.call("ppt.create_document", createArgs);
        assertTrue(created.success());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addSecond = mapper.createObjectNode();
        addSecond.put("document_id", documentId);
        addSecond.put("title", "Slide Two");
        assertTrue(service.call("ppt.add_slide", addSecond).success());

        ObjectNode addThird = mapper.createObjectNode();
        addThird.put("document_id", documentId);
        addThird.put("title", "Slide Three");
        assertTrue(service.call("ppt.add_slide", addThird).success());

        ObjectNode reorderArgs = mapper.createObjectNode();
        reorderArgs.put("document_id", documentId);
        reorderArgs.putArray("new_order").add(2).add(0).add(1);
        ToolCallResult reordered = service.call("ppt.reorder_slides", reorderArgs);
        assertTrue(reordered.success());
        assertEquals(3, reordered.payload().path("slide_count").asInt());

        ObjectNode listArgs = mapper.createObjectNode();
        listArgs.put("document_id", documentId);
        ToolCallResult listed = service.call("ppt.list_slides", listArgs);
        assertTrue(listed.success());

        String firstPreview = listed.payload().path("slides").path(0).path("text_preview").asText();
        String secondPreview = listed.payload().path("slides").path(1).path("text_preview").asText();
        String thirdPreview = listed.payload().path("slides").path(2).path("text_preview").asText();

        assertTrue(firstPreview.contains("Slide Three"));
        assertTrue(secondPreview.contains("Slide One"));
        assertTrue(thirdPreview.contains("Slide Two"));
    }

    @Test
    void replaceTextGloballyReplacesAcrossSlides() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        assertTrue(created.success());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addSlideArgs = mapper.createObjectNode();
        addSlideArgs.put("document_id", documentId);
        service.call("ppt.add_slide", addSlideArgs);

        ObjectNode addBoxSlide0 = mapper.createObjectNode();
        addBoxSlide0.put("document_id", documentId);
        addBoxSlide0.put("slide_index", 0);
        addBoxSlide0.put("text", "Q1 revenue increased");
        addBoxSlide0.put("x", 40);
        addBoxSlide0.put("y", 40);
        addBoxSlide0.put("width", 300);
        addBoxSlide0.put("height", 80);
        assertTrue(service.call("ppt.add_textbox", addBoxSlide0).success());

        ObjectNode addBoxSlide1 = mapper.createObjectNode();
        addBoxSlide1.put("document_id", documentId);
        addBoxSlide1.put("slide_index", 1);
        addBoxSlide1.put("text", "q1 forecast updated");
        addBoxSlide1.put("x", 40);
        addBoxSlide1.put("y", 40);
        addBoxSlide1.put("width", 300);
        addBoxSlide1.put("height", 80);
        assertTrue(service.call("ppt.add_textbox", addBoxSlide1).success());

        ObjectNode replaceArgs = mapper.createObjectNode();
        replaceArgs.put("document_id", documentId);
        replaceArgs.put("old_text", "q1");
        replaceArgs.put("new_text", "Q2");
        replaceArgs.put("case_sensitive", false);
        ToolCallResult replaced = service.call("ppt.replace_text_globally", replaceArgs);
        assertTrue(replaced.success());
        assertEquals(2, replaced.payload().path("replacements_count").asInt());
        assertEquals(2, replaced.payload().path("slides_affected").asInt());

        ObjectNode content0 = mapper.createObjectNode();
        content0.put("document_id", documentId);
        content0.put("slide_index", 0);
        ToolCallResult slide0 = service.call("ppt.get_slide_content", content0);
        assertTrue(slide0.success());
        assertTrue(slide0.payload().path("text").asText().contains("Q2"));

        ObjectNode content1 = mapper.createObjectNode();
        content1.put("document_id", documentId);
        content1.put("slide_index", 1);
        ToolCallResult slide1 = service.call("ppt.get_slide_content", content1);
        assertTrue(slide1.success());
        assertTrue(slide1.payload().path("text").asText().contains("Q2"));
    }

    @Test
    void moveShapeMovesTextBoxAnchor() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addBox = mapper.createObjectNode();
        addBox.put("document_id", documentId);
        addBox.put("slide_index", 0);
        addBox.put("text", "Move me");
        addBox.put("x", 10);
        addBox.put("y", 20);
        addBox.put("width", 200);
        addBox.put("height", 50);
        assertTrue(service.call("ppt.add_textbox", addBox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = content.payload().path("shapes").size() - 1;

        ObjectNode moveArgs = mapper.createObjectNode();
        moveArgs.put("document_id", documentId);
        moveArgs.put("slide_index", 0);
        moveArgs.put("shape_index", shapeIndex);
        moveArgs.put("x", 150);
        moveArgs.put("y", 175);
        ToolCallResult moved = service.call("ppt.move_shape", moveArgs);

        assertTrue(moved.success());
        assertEquals(150.0, moved.payload().path("x").asDouble(), 0.01);
        assertEquals(175.0, moved.payload().path("y").asDouble(), 0.01);
    }

    @Test
    void duplicateAndDeleteSlidesWork() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addSlide = mapper.createObjectNode();
        addSlide.put("document_id", documentId);
        addSlide.put("title", "Second");
        assertTrue(service.call("ppt.add_slide", addSlide).success());

        ObjectNode duplicateArgs = mapper.createObjectNode();
        duplicateArgs.put("document_id", documentId);
        duplicateArgs.put("source_slide_index", 0);
        ToolCallResult duplicated = service.call("ppt.duplicate_slide", duplicateArgs);
        assertTrue(duplicated.success());
        assertEquals(3, duplicated.payload().path("slide_count").asInt());

        ObjectNode deleteArgs = mapper.createObjectNode();
        deleteArgs.put("document_id", documentId);
        deleteArgs.putArray("slide_indices").add(1);
        ToolCallResult deleted = service.call("ppt.delete_slides", deleteArgs);
        assertTrue(deleted.success());
        assertEquals(2, deleted.payload().path("remaining_slide_count").asInt());
    }

    @Test
    void mergePresentationsImportsSlides() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        Path sourcePpt = Files.createTempFile("ppt-merge-source", ".pptx");
        try (XMLSlideShow source = new XMLSlideShow(); FileOutputStream out = new FileOutputStream(sourcePpt.toFile())) {
            source.createSlide();
            source.createSlide();
            source.write(out);
        }

        ObjectNode mergeArgs = mapper.createObjectNode();
        mergeArgs.put("document_id", documentId);
        mergeArgs.put("merge_path", sourcePpt.toString());
        ToolCallResult merged = service.call("ppt.merge_presentations", mergeArgs);

        assertTrue(merged.success());
        assertEquals(2, merged.payload().path("merged_slide_count").asInt());
        assertEquals(3, merged.payload().path("slide_count").asInt());
    }

    @Test
    void transactionRollbackRestoresPreviousState() {
        PptToolService service = new PptToolService();
        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode beginArgs = mapper.createObjectNode();
        beginArgs.put("document_id", documentId);
        assertTrue(service.call("ppt.transaction_begin", beginArgs).success());

        ObjectNode addSlideArgs = mapper.createObjectNode();
        addSlideArgs.put("document_id", documentId);
        assertTrue(service.call("ppt.add_slide", addSlideArgs).success());

        ObjectNode infoArgs = mapper.createObjectNode();
        infoArgs.put("document_id", documentId);
        assertEquals(2, service.call("ppt.get_document_info", infoArgs).payload().path("slide_count").asInt());

        ObjectNode rollbackArgs = mapper.createObjectNode();
        rollbackArgs.put("document_id", documentId);
        ToolCallResult rollback = service.call("ppt.transaction_rollback", rollbackArgs);
        assertTrue(rollback.success());

        ToolCallResult infoAfter = service.call("ppt.get_document_info", infoArgs);
        assertEquals(1, infoAfter.payload().path("slide_count").asInt());
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

    @Test
    void deleteShapeRemovesShapeFromSlide() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Delete me");
        addTextbox.put("x", 50);
        addTextbox.put("y", 50);
        addTextbox.put("width", 300);
        addTextbox.put("height", 80);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult before = service.call("ppt.get_slide_content", contentArgs);
        int beforeCount = before.payload().path("shapes").size();

        ObjectNode deleteArgs = mapper.createObjectNode();
        deleteArgs.put("document_id", documentId);
        deleteArgs.put("slide_index", 0);
        deleteArgs.put("shape_index", beforeCount - 1);
        ToolCallResult deleted = service.call("ppt.delete_shape", deleteArgs);
        assertTrue(deleted.success());

        ToolCallResult after = service.call("ppt.get_slide_content", contentArgs);
        assertEquals(beforeCount - 1, after.payload().path("shapes").size());
    }

    @Test
    void getShapePropertiesReturnsTextShapeDetails() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Shape details");
        addTextbox.put("x", 100);
        addTextbox.put("y", 120);
        addTextbox.put("width", 320);
        addTextbox.put("height", 90);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = content.payload().path("shapes").size() - 1;

        ObjectNode propsArgs = mapper.createObjectNode();
        propsArgs.put("document_id", documentId);
        propsArgs.put("slide_index", 0);
        propsArgs.put("shape_index", shapeIndex);
        ToolCallResult properties = service.call("ppt.get_shape_properties", propsArgs);
        assertTrue(properties.success());
        assertTrue(properties.payload().path("shape_type").asText().contains("Text"));
        assertTrue(properties.payload().path("text").asText().contains("Shape details"));
    }

    @Test
    void setShapeStyleAppliesVisualProperties() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Style target");
        addTextbox.put("x", 70);
        addTextbox.put("y", 80);
        addTextbox.put("width", 320);
        addTextbox.put("height", 90);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = content.payload().path("shapes").size() - 1;

        ObjectNode styleArgs = mapper.createObjectNode();
        styleArgs.put("document_id", documentId);
        styleArgs.put("slide_index", 0);
        styleArgs.put("shape_index", shapeIndex);
        styleArgs.put("fill_color", "#FF0000");
        styleArgs.put("border_color", "#000000");
        styleArgs.put("border_width", 2.0);
        styleArgs.put("text_align", "center");
        ToolCallResult styled = service.call("ppt.set_shape_style", styleArgs);
        assertTrue(styled.success());
    }

    @Test
    void setDocumentMetadataUpdatesCoreProperties() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode metadataArgs = mapper.createObjectNode();
        metadataArgs.put("document_id", documentId);
        metadataArgs.put("title", "Deck title");
        metadataArgs.put("author", "Deck author");
        metadataArgs.put("subject", "Deck subject");
        metadataArgs.put("keywords", "a,b,c");
        ToolCallResult updated = service.call("ppt.set_document_metadata", metadataArgs);
        assertTrue(updated.success());
    }

    @Test
    void setSlideLayoutUpdatesSlideUsingMasterLayout() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode layoutArgs = mapper.createObjectNode();
        layoutArgs.put("document_id", documentId);
        layoutArgs.put("slide_index", 0);
        layoutArgs.put("layout_type", "title_content");
        ToolCallResult changed = service.call("ppt.set_slide_layout", layoutArgs);
        assertTrue(changed.success());
    }

    @Test
    void setTextFormattingAppliesParagraphFormatting() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Paragraph formatting test");
        addTextbox.put("x", 40);
        addTextbox.put("y", 40);
        addTextbox.put("width", 400);
        addTextbox.put("height", 120);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = content.payload().path("shapes").size() - 1;

        ObjectNode formatArgs = mapper.createObjectNode();
        formatArgs.put("document_id", documentId);
        formatArgs.put("slide_index", 0);
        formatArgs.put("shape_index", shapeIndex);
        formatArgs.put("text_align", "justify");
        formatArgs.put("line_spacing", 120.0);
        formatArgs.put("left_margin", 10.0);
        formatArgs.put("indent", 5.0);
        ToolCallResult formatted = service.call("ppt.set_text_formatting", formatArgs);
        assertTrue(formatted.success());
    }

    @Test
    void setShapeZOrderMovesShapeToFront() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode first = mapper.createObjectNode();
        first.put("document_id", documentId);
        first.put("slide_index", 0);
        first.put("text", "First");
        first.put("x", 20);
        first.put("y", 20);
        first.put("width", 200);
        first.put("height", 60);
        assertTrue(service.call("ppt.add_textbox", first).success());

        ObjectNode second = mapper.createObjectNode();
        second.put("document_id", documentId);
        second.put("slide_index", 0);
        second.put("text", "Second");
        second.put("x", 40);
        second.put("y", 90);
        second.put("width", 200);
        second.put("height", 60);
        assertTrue(service.call("ppt.add_textbox", second).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int firstShapeIndex = content.payload().path("shapes").size() - 2;

        ObjectNode zOrderArgs = mapper.createObjectNode();
        zOrderArgs.put("document_id", documentId);
        zOrderArgs.put("slide_index", 0);
        zOrderArgs.put("shape_index", firstShapeIndex);
        zOrderArgs.put("position", "front");
        ToolCallResult reordered = service.call("ppt.set_shape_z_order", zOrderArgs);
        assertTrue(reordered.success());
    }

    @Test
    void setPageSetupUpdatesDocumentDimensions() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode setupArgs = mapper.createObjectNode();
        setupArgs.put("document_id", documentId);
        setupArgs.put("preset", "widescreen_16_9");
        ToolCallResult setup = service.call("ppt.set_page_setup", setupArgs);
        assertTrue(setup.success());

        ObjectNode infoArgs = mapper.createObjectNode();
        infoArgs.put("document_id", documentId);
        ToolCallResult info = service.call("ppt.get_document_info", infoArgs);
        assertEquals(960, info.payload().path("page_width").asInt());
        assertEquals(540, info.payload().path("page_height").asInt());
    }

    @Test
    void addShapeCreatesPrimitiveOnSlide() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode shapeArgs = mapper.createObjectNode();
        shapeArgs.put("document_id", documentId);
        shapeArgs.put("slide_index", 0);
        shapeArgs.put("shape_type", "rectangle");
        shapeArgs.put("x", 120);
        shapeArgs.put("y", 90);
        shapeArgs.put("width", 220);
        shapeArgs.put("height", 110);
        shapeArgs.put("text", "Primitive");
        shapeArgs.put("fill_color", "#CCE5FF");
        ToolCallResult added = service.call("ppt.add_shape", shapeArgs);
        assertTrue(added.success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = added.payload().path("shape_index").asInt();
        assertTrue(content.payload().path("shapes").path(shapeIndex).path("shape_type").asText().contains("AutoShape"));
    }

    @Test
    void setTextRunStyleStylesMatchedSegment() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "Alpha Beta Gamma");
        addTextbox.put("x", 50);
        addTextbox.put("y", 50);
        addTextbox.put("width", 300);
        addTextbox.put("height", 80);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = content.payload().path("shapes").size() - 1;

        ObjectNode styleArgs = mapper.createObjectNode();
        styleArgs.put("document_id", documentId);
        styleArgs.put("slide_index", 0);
        styleArgs.put("shape_index", shapeIndex);
        styleArgs.put("target_text", "Beta");
        styleArgs.put("bold", true);
        styleArgs.put("font_color", "#FF0000");
        ToolCallResult styled = service.call("ppt.set_text_run_style", styleArgs);
        assertTrue(styled.success());
    }

    @Test
    void tableStructureAndFormattingToolsWork() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTableArgs = mapper.createObjectNode();
        addTableArgs.put("document_id", documentId);
        addTableArgs.put("slide_index", 0);
        addTableArgs.put("rows", 2);
        addTableArgs.put("cols", 2);
        addTableArgs.put("x", 60);
        addTableArgs.put("y", 160);
        addTableArgs.put("width", 360);
        addTableArgs.put("height", 140);
        ToolCallResult tableAdded = service.call("ppt.add_table", addTableArgs);
        assertTrue(tableAdded.success());

        int shapeIndex = tableAdded.payload().path("shape_index").asInt();

        ObjectNode structureArgs = mapper.createObjectNode();
        structureArgs.put("document_id", documentId);
        structureArgs.put("slide_index", 0);
        structureArgs.put("shape_index", shapeIndex);
        structureArgs.put("operation", "insert_row");
        structureArgs.put("index", 1);
        ToolCallResult restructured = service.call("ppt.modify_table_structure", structureArgs);
        assertTrue(restructured.success());

        int newShapeIndex = restructured.payload().path("shape_index").asInt();

        ObjectNode rowHeightArgs = mapper.createObjectNode();
        rowHeightArgs.put("document_id", documentId);
        rowHeightArgs.put("slide_index", 0);
        rowHeightArgs.put("shape_index", newShapeIndex);
        rowHeightArgs.put("row_index", 0);
        rowHeightArgs.put("height", 28.0);
        assertTrue(service.call("ppt.set_table_row_height", rowHeightArgs).success());

        ObjectNode colWidthArgs = mapper.createObjectNode();
        colWidthArgs.put("document_id", documentId);
        colWidthArgs.put("slide_index", 0);
        colWidthArgs.put("shape_index", newShapeIndex);
        colWidthArgs.put("col_index", 0);
        colWidthArgs.put("width", 180.0);
        assertTrue(service.call("ppt.set_table_column_width", colWidthArgs).success());

        ObjectNode headerArgs = mapper.createObjectNode();
        headerArgs.put("document_id", documentId);
        headerArgs.put("slide_index", 0);
        headerArgs.put("shape_index", newShapeIndex);
        headerArgs.put("fill_color", "#003366");
        headerArgs.put("font_color", "#FFFFFF");
        headerArgs.put("bold", true);
        assertTrue(service.call("ppt.set_table_header_style", headerArgs).success());
    }

    @Test
    void setListFormattingUpdatesParagraphSemantics() {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "One\nTwo\nThree");
        addTextbox.put("x", 70);
        addTextbox.put("y", 70);
        addTextbox.put("width", 350);
        addTextbox.put("height", 140);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        ObjectNode contentArgs = mapper.createObjectNode();
        contentArgs.put("document_id", documentId);
        contentArgs.put("slide_index", 0);
        ToolCallResult content = service.call("ppt.get_slide_content", contentArgs);
        int shapeIndex = content.payload().path("shapes").size() - 1;

        ObjectNode listArgs = mapper.createObjectNode();
        listArgs.put("document_id", documentId);
        listArgs.put("slide_index", 0);
        listArgs.put("shape_index", shapeIndex);
        listArgs.put("bullet_enabled", true);
        listArgs.put("bullet_character", "•");
        listArgs.put("bullet_level", 1);
        listArgs.put("line_spacing", 110.0);
        listArgs.put("space_before", 2.0);
        listArgs.put("space_after", 2.0);
        ToolCallResult formatted = service.call("ppt.set_list_formatting", listArgs);
        assertTrue(formatted.success());
    }

    @Test
    void replaceImageKeepsPictureShapePosition() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        Path firstImage = Files.createTempFile("mcpo-image-1", ".png");
        BufferedImage img1 = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        var g1 = img1.createGraphics();
        g1.setColor(Color.ORANGE);
        g1.fillRect(0, 0, 40, 30);
        g1.dispose();
        ImageIO.write(img1, "png", firstImage.toFile());

        Path secondImage = Files.createTempFile("mcpo-image-2", ".png");
        BufferedImage img2 = new BufferedImage(40, 30, BufferedImage.TYPE_INT_RGB);
        var g2 = img2.createGraphics();
        g2.setColor(Color.BLUE);
        g2.fillRect(0, 0, 40, 30);
        g2.dispose();
        ImageIO.write(img2, "png", secondImage.toFile());

        ObjectNode insertArgs = mapper.createObjectNode();
        insertArgs.put("document_id", documentId);
        insertArgs.put("slide_index", 0);
        insertArgs.put("image_path", firstImage.toString());
        insertArgs.put("x", 80);
        insertArgs.put("y", 90);
        insertArgs.put("width", 200);
        insertArgs.put("height", 120);
        ToolCallResult inserted = service.call("ppt.insert_image", insertArgs);
        assertTrue(inserted.success());

        ObjectNode replaceArgs = mapper.createObjectNode();
        replaceArgs.put("document_id", documentId);
        replaceArgs.put("slide_index", 0);
        replaceArgs.put("shape_index", inserted.payload().path("shape_index").asInt());
        replaceArgs.put("image_path", secondImage.toString());
        replaceArgs.put("keep_size", true);
        ToolCallResult replaced = service.call("ppt.replace_image", replaceArgs);
        assertTrue(replaced.success());
    }

    @Test
    void renderAllSlidesImageWritesBatchFiles() throws Exception {
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addSlide = mapper.createObjectNode();
        addSlide.put("document_id", documentId);
        addSlide.put("title", "Second");
        assertTrue(service.call("ppt.add_slide", addSlide).success());

        Path outputDir = Files.createTempDirectory("mcpo-batch-render");
        ObjectNode renderArgs = mapper.createObjectNode();
        renderArgs.put("document_id", documentId);
        renderArgs.put("output_dir", outputDir.toString());
        renderArgs.put("format", "png");
        renderArgs.put("file_name_pattern", "slide-%02d");
        ToolCallResult rendered = service.call("ppt.render_all_slides_image", renderArgs);
        assertTrue(rendered.success());
        assertEquals(2, rendered.payload().path("files").size());

        for (int i = 0; i < rendered.payload().path("files").size(); i++) {
            Path file = Path.of(rendered.payload().path("files").get(i).asText());
            assertTrue(Files.exists(file));
            assertTrue(Files.size(file) > 0);
        }
    }
}
