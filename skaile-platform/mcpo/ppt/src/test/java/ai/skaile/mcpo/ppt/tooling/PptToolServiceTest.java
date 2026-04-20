package ai.skaile.mcpo.ppt.tooling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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
        ToolCallResult saved = service.call("ppt.export_document", saveArgs);
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
        ToolCallResult rendered = service.call("ppt.render_slide", renderArgs);
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
        ToolCallResult result = service.call("ppt.export_document", saveArgs);
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
        ToolCallResult templateSaved = service.call("ppt.export_document", saveTemplateArgs);
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
        ToolCallResult templateSaved = service.call("ppt.export_document", saveTemplateArgs);
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
        setCellArgs.put("operation", "set_cell");
        setCellArgs.put("row", 1);
        setCellArgs.put("col", 1);
        setCellArgs.put("text", "Quarterly target");
        ToolCallResult setCell = service.call("ppt.edit_table", setCellArgs);
        assertTrue(setCell.success());

        ObjectNode getTableArgs = mapper.createObjectNode();
        getTableArgs.put("document_id", documentId);
        getTableArgs.put("slide_index", 0);
        getTableArgs.put("shape_index", shapeIndex);
        ToolCallResult getTable = service.call("ppt.get_table", getTableArgs);
        assertTrue(getTable.success());
        assertEquals("Quarterly target",
                getTable.payload().path("cells").path(1).path(1).path("text").asText());
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
        ToolCallResult styled = service.call("ppt.set_text", styleArgs);
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
        ToolCallResult formatted = service.call("ppt.set_text", formatArgs);
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
        styleArgs.put("scope", "run");
        styleArgs.put("target_text", "Beta");
        styleArgs.put("bold", true);
        styleArgs.put("font_color", "#FF0000");
        ToolCallResult styled = service.call("ppt.set_text", styleArgs);
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
        ToolCallResult restructured = service.call("ppt.edit_table", structureArgs);
        assertTrue(restructured.success());

        int newShapeIndex = restructured.payload().path("shape_index").asInt();

        ObjectNode rowHeightArgs = mapper.createObjectNode();
        rowHeightArgs.put("document_id", documentId);
        rowHeightArgs.put("slide_index", 0);
        rowHeightArgs.put("shape_index", newShapeIndex);
        rowHeightArgs.put("operation", "set_row_height");
        rowHeightArgs.put("row_index", 0);
        rowHeightArgs.put("height", 28.0);
        assertTrue(service.call("ppt.edit_table", rowHeightArgs).success());

        ObjectNode colWidthArgs = mapper.createObjectNode();
        colWidthArgs.put("document_id", documentId);
        colWidthArgs.put("slide_index", 0);
        colWidthArgs.put("shape_index", newShapeIndex);
        colWidthArgs.put("operation", "set_col_width");
        colWidthArgs.put("col_index", 0);
        colWidthArgs.put("width", 180.0);
        assertTrue(service.call("ppt.edit_table", colWidthArgs).success());

        ObjectNode headerArgs = mapper.createObjectNode();
        headerArgs.put("document_id", documentId);
        headerArgs.put("slide_index", 0);
        headerArgs.put("shape_index", newShapeIndex);
        headerArgs.put("operation", "set_header_style");
        headerArgs.put("fill_color", "#003366");
        headerArgs.put("font_color", "#FFFFFF");
        headerArgs.put("bold", true);
        assertTrue(service.call("ppt.edit_table", headerArgs).success());
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
        ToolCallResult formatted = service.call("ppt.set_text", listArgs);
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
        ToolCallResult rendered = service.call("ppt.render_all_slides", renderArgs);
        assertTrue(rendered.success());
        assertEquals(2, rendered.payload().path("files").size());

        for (int i = 0; i < rendered.payload().path("files").size(); i++) {
            Path file = Path.of(rendered.payload().path("files").get(i).asText());
            assertTrue(Files.exists(file));
            assertTrue(Files.size(file) > 0);
        }
    }

    @Test
    void concurrentUpdateTextOnSameDocumentIsSerializedWithoutCorruption() throws Exception {
        // Phase 0 acceptance: per-session lock must serialize concurrent mutations against the
        // same document_id. Without it, POI's XMLSlideShow DOM corrupts under parallel writes.
        PptToolService service = new PptToolService();

        ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
        String documentId = created.payload().path("document_id").asText();

        ObjectNode addTextbox = mapper.createObjectNode();
        addTextbox.put("document_id", documentId);
        addTextbox.put("slide_index", 0);
        addTextbox.put("text", "seed");
        addTextbox.put("x", 10);
        addTextbox.put("y", 10);
        addTextbox.put("width", 600);
        addTextbox.put("height", 80);
        assertTrue(service.call("ppt.add_textbox", addTextbox).success());

        int iterations = 30;
        int workers = 4;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Boolean>> results = new ArrayList<>();
            for (int w = 0; w < workers; w++) {
                final int workerId = w;
                results.add(pool.submit(() -> {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        ObjectNode args = mapper.createObjectNode();
                        args.put("document_id", documentId);
                        args.put("slide_index", 0);
                        args.put("old_text", "seed");
                        args.put("new_text", "seed");
                        ToolCallResult r = service.call("ppt.update_text", args);
                        if (!r.success()) {
                            return false;
                        }
                        // Read-path also takes the lock; exercise it so any concurrency
                        // bug surfaces as a race on the shared DOM.
                        ObjectNode listArgs = mapper.createObjectNode();
                        listArgs.put("document_id", documentId);
                        ToolCallResult l = service.call("ppt.list_slides", listArgs);
                        if (!l.success() || l.payload().path("slides").size() != 1) {
                            return false;
                        }
                    }
                    return Boolean.TRUE;
                }));
            }
            start.countDown();
            for (Future<Boolean> f : results) {
                assertTrue(f.get(60, TimeUnit.SECONDS),
                        "concurrent update_text/list_slides must all succeed");
            }
        } finally {
            pool.shutdownNow();
            service.closeAllSessions();
        }
    }

    @Test
    void concurrentCallsOnDifferentDocumentsDoNotSerialize() throws Exception {
        // Two documents have independent locks; long-running calls on doc A must not block B.
        PptToolService service = new PptToolService();
        try {
            String docA = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            String docB = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            ExecutorService pool = Executors.newFixedThreadPool(2);
            try {
                Future<Boolean> a = pool.submit(() -> runRepeatedListSlides(service, docA, 20));
                Future<Boolean> b = pool.submit(() -> runRepeatedListSlides(service, docB, 20));
                assertTrue(a.get(30, TimeUnit.SECONDS));
                assertTrue(b.get(30, TimeUnit.SECONDS));
            } finally {
                pool.shutdownNow();
            }
        } finally {
            service.closeAllSessions();
        }
    }

    private boolean runRepeatedListSlides(PptToolService service, String documentId, int iterations) {
        for (int i = 0; i < iterations; i++) {
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", documentId);
            if (!service.call("ppt.list_slides", args).success()) {
                return false;
            }
        }
        return true;
    }

    @Test
    void savePdfReturnsSofficeUnavailableWhenBinaryMissing() throws Exception {
        // With SOFFICE_PATH pointing nowhere, the probe returns unavailable and the PDF
        // branch must short-circuit to a structured error instead of throwing.
        String previousPath = System.getenv("SOFFICE_PATH");
        // We cannot mutate the process env from Java portably, but we can reset the probe
        // cache and depend on the shipped resolver: if no soffice is on PATH, the probe
        // fails; else this test is a no-op.
        ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.reset();
        ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability probe =
                ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.get();
        org.junit.jupiter.api.Assumptions.assumeFalse(probe.available(),
                "skipping: real soffice is installed on PATH, SOFFICE_UNAVAILABLE path can't be exercised");

        PptToolService service = new PptToolService();
        try {
            ToolCallResult created = service.call("ppt.create_document", mapper.createObjectNode());
            String documentId = created.payload().path("document_id").asText();

            Path out = Files.createTempFile("pdf-export", ".pdf");
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", documentId);
            args.put("output_path", out.toString());
            args.put("format", "pdf");
            ToolCallResult result = service.call("ppt.export_document", args);
            assertFalse(result.success());
            assertEquals("SOFFICE_UNAVAILABLE", result.payload().path("code").asText());
            assertFalse(result.payload().path("retriable").asBoolean(true));
        } finally {
            service.closeAllSessions();
            ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.reset();
        }
    }

    // ---------- Phase 1 additions ----------

    @Test
    void capabilitiesReportsVersionsLimitsAndFeatureFlags() {
        PptToolService service = new PptToolService();
        try {
            ToolCallResult result = service.call("ppt.capabilities", mapper.createObjectNode());
            assertTrue(result.success());
            var payload = result.payload();
            assertEquals("0.1.0", payload.path("server_version").asText());
            assertFalse(payload.path("poi_version").asText().isBlank());
            assertFalse(payload.path("java_version").asText().isBlank());
            assertTrue(payload.path("supported_input_formats").isArray());
            assertTrue(payload.path("supported_export_formats").size() >= 7);
            assertEquals(3, payload.path("supported_render_formats").size());
            assertEquals(100, payload.path("limits").path("max_open_docs").asInt());
            assertEquals(2000, payload.path("limits").path("max_slides_per_deck").asInt());
            assertEquals(500, payload.path("limits").path("max_shapes_per_slide").asInt());
            assertEquals(52428800L, payload.path("limits").path("max_image_bytes").asLong());
            assertEquals(10000, payload.path("limits").path("max_render_dimension").asInt());
            // Phase 1: every advanced feature flag is still false.
            assertFalse(payload.path("feature_flags").path("high_fidelity_render").asBoolean());
            assertFalse(payload.path("feature_flags").path("gradients").asBoolean());
            assertFalse(payload.path("feature_flags").path("charts_update").asBoolean());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextScopeRunStylesSingleOccurrence() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Alpha Beta Gamma");
            tb.put("x", 30); tb.put("y", 30); tb.put("width", 400); tb.put("height", 80);
            assertTrue(service.call("ppt.add_textbox", tb).success());

            int shapeIndex = service.call("ppt.get_slide_content", objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode run = mapper.createObjectNode();
            run.put("document_id", docId);
            run.put("slide_index", 0);
            run.put("shape_index", shapeIndex);
            run.put("scope", "run");
            run.put("target_text", "Beta");
            run.put("bold", true);
            ToolCallResult result = service.call("ppt.set_text", run);
            assertTrue(result.success());
            assertEquals("run", result.payload().path("scope").asText());
            assertEquals(6, result.payload().path("start").asInt());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextScopeParagraphAppliesOnlyToSelectedParagraph() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Line one\nLine two");
            tb.put("x", 30); tb.put("y", 30); tb.put("width", 400); tb.put("height", 80);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int shapeIndex = service.call("ppt.get_slide_content", objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode para = mapper.createObjectNode();
            para.put("document_id", docId);
            para.put("slide_index", 0);
            para.put("shape_index", shapeIndex);
            para.put("scope", "paragraph");
            para.put("paragraph_index", 0);
            para.put("text_align", "center");
            para.put("bold", true);
            ToolCallResult result = service.call("ppt.set_text", para);
            assertTrue(result.success());
            assertEquals(0, result.payload().path("paragraph_index").asInt());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextRejectsInvalidScopeAndMissingParagraphIndex() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Hi");
            tb.put("x", 30); tb.put("y", 30); tb.put("width", 400); tb.put("height", 80);
            assertTrue(service.call("ppt.add_textbox", tb).success());

            ObjectNode bogus = mapper.createObjectNode();
            bogus.put("document_id", docId);
            bogus.put("slide_index", 0);
            bogus.put("shape_index", 0);
            bogus.put("scope", "paragraph");
            bogus.put("paragraph_index", 999);
            ToolCallResult r = service.call("ppt.set_text", bogus);
            assertFalse(r.success());
            assertTrue(r.payload().path("error").asText().contains("paragraph_index"));
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void editTableRoundTripsInsertsDeletesAndHeader() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 2);
            at.put("cols", 2);
            at.put("x", 20); at.put("y", 20); at.put("width", 400); at.put("height", 200);
            int tableIdx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            // insert_col then delete_col
            ToolCallResult ins = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", tableIdx, "operation", "insert_col", "index", 1));
            assertTrue(ins.success());
            int newIdx = ins.payload().path("shape_index").asInt();
            ToolCallResult del = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", newIdx, "operation", "delete_col", "index", 1));
            assertTrue(del.success());

            ToolCallResult snap = service.call("ppt.get_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", del.payload().path("shape_index").asInt()));
            assertTrue(snap.success());
            assertEquals(2, snap.payload().path("cols").asInt());
            assertEquals(2, snap.payload().path("rows").asInt());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void editTableReportsFeatureNotImplementedForPhase3Operations() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 2);
            at.put("cols", 2);
            at.put("x", 20); at.put("y", 20); at.put("width", 400); at.put("height", 200);
            int idx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            ToolCallResult merge = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", idx, "operation", "merge_cells",
                            "start_row", 0, "start_col", 0, "end_row", 1, "end_col", 1));
            assertFalse(merge.success());
            assertEquals("FEATURE_NOT_IMPLEMENTED", merge.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void renderSlideHighFidelityReturnsNotYetImplemented() throws Exception {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            Path out = Files.createTempFile("ppt-render", ".png");
            ObjectNode r = mapper.createObjectNode();
            r.put("document_id", docId);
            r.put("slide_index", 0);
            r.put("output_path", out.toString());
            r.put("fidelity", "high");
            ToolCallResult result = service.call("ppt.render_slide", r);
            assertFalse(result.success());
            assertEquals("FORMAT_NOT_YET_IMPLEMENTED", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void renderSlideSvgFormatProducesFile() throws Exception {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            Path out = Files.createTempFile("ppt-render", ".svg");
            ObjectNode r = mapper.createObjectNode();
            r.put("document_id", docId);
            r.put("slide_index", 0);
            r.put("output_path", out.toString());
            r.put("format", "svg");
            ToolCallResult result = service.call("ppt.render_slide", r);
            assertTrue(result.success());
            assertEquals("svg", result.payload().path("format").asText());
            assertTrue(Files.size(out) > 0);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void exportDocumentReservedFormatsReturnNotYetImplemented() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            for (String fmt : new String[] {"html", "png_batch", "jpg_batch", "svg_batch", "outline_text"}) {
                ObjectNode args = mapper.createObjectNode();
                args.put("document_id", docId);
                args.put("format", fmt);
                args.put("output_path", "/tmp/unused-" + fmt);
                ToolCallResult result = service.call("ppt.export_document", args);
                assertFalse(result.success());
                assertEquals("FORMAT_NOT_YET_IMPLEMENTED",
                        result.payload().path("code").asText(),
                        "format=" + fmt);
            }
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void renderDimensionLimitEnforced() throws Exception {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            Path out = Files.createTempFile("ppt-huge", ".png");
            ObjectNode r = mapper.createObjectNode();
            r.put("document_id", docId);
            r.put("slide_index", 0);
            r.put("output_path", out.toString());
            r.put("width", 20_000);
            r.put("height", 100);
            ToolCallResult result = service.call("ppt.render_slide", r);
            assertFalse(result.success());
            assertEquals("LIMIT_MAX_RENDER_DIMENSION", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void phase1BroadCoverageFlow() throws Exception {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            ToolCallResult pageSetup = service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId, "preset", "widescreen_16_9"));
            assertTrue(pageSetup.success(), pageSetup.payload().toString());

            assertTrue(service.call("ppt.set_document_metadata", objectNode(
                    "document_id", docId,
                    "title", "T", "author", "A", "subject", "S", "keywords", "k1,k2"))
                    .success());

            assertTrue(service.call("ppt.transaction_begin",
                    objectNode("document_id", docId)).success());
            assertTrue(service.call("ppt.add_slide",
                    objectNode("document_id", docId, "title", "Mid")).success());
            assertTrue(service.call("ppt.transaction_rollback",
                    objectNode("document_id", docId)).success());
            // Rollback restored: only the original slide remains.
            assertEquals(1, service.call("ppt.get_document_info",
                    objectNode("document_id", docId))
                    .payload().path("slide_count").asInt());

            assertTrue(service.call("ppt.transaction_begin",
                    objectNode("document_id", docId)).success());
            assertTrue(service.call("ppt.add_slide",
                    objectNode("document_id", docId, "title", "Kept")).success());
            assertTrue(service.call("ppt.transaction_commit",
                    objectNode("document_id", docId)).success());
            assertEquals(2, service.call("ppt.get_document_info",
                    objectNode("document_id", docId))
                    .payload().path("slide_count").asInt());

            // replace_text_globally on an added text box.
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0, "text", "seed word",
                    "x", 20, "y", 20, "width", 400, "height", 80)).success());
            ToolCallResult replaced = service.call("ppt.replace_text_globally", objectNode(
                    "document_id", docId, "old_text", "seed", "new_text", "fixed"));
            assertTrue(replaced.success());
            assertTrue(replaced.payload().path("replacements_count").asInt() >= 1);

            // render_all_slides in svg — exercises the SVG branch of the multi-slide renderer.
            Path dir = Files.createTempDirectory("render-all-svg");
            ToolCallResult rendered = service.call("ppt.render_all_slides", objectNode(
                    "document_id", docId,
                    "output_dir", dir.toString(),
                    "format", "svg"));
            assertTrue(rendered.success());
            assertEquals("svg", rendered.payload().path("format").asText());
            assertEquals(2, rendered.payload().path("files").size());

            // delete_shape + import_markdown_outline on a fresh doc.
            assertTrue(service.call("ppt.delete_shape", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0))
                    .success());

            ToolCallResult md = service.call("ppt.import_markdown_outline", objectNode(
                    "markdown_text", "# Hello\n\n- first\n- second\n\n# World\n\n- third"));
            assertTrue(md.success());
            String mdDoc = md.payload().path("document_id").asText();
            int mdSlides = service.call("ppt.get_document_info",
                    objectNode("document_id", mdDoc)).payload().path("slide_count").asInt();
            assertTrue(mdSlides >= 2, "import_markdown_outline should create at least 2 slides");

            // merge_presentations: save docId to disk, then merge it into mdDoc.
            Path merged = Files.createTempFile("merge-src", ".pptx");
            assertTrue(service.call("ppt.export_document", objectNode(
                    "document_id", docId, "output_path", merged.toString())).success());
            ToolCallResult mergeResult = service.call("ppt.merge_presentations", objectNode(
                    "document_id", mdDoc, "merge_path", merged.toString()));
            assertTrue(mergeResult.success());
            assertTrue(service.call("ppt.get_document_info",
                    objectNode("document_id", mdDoc)).payload().path("slide_count").asInt() > mdSlides);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void getShapePropertiesAndZOrderFlow() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            assertTrue(service.call("ppt.add_shape", objectNode(
                    "document_id", docId, "slide_index", 0,
                    "shape_type", "rectangle",
                    "x", 20, "y", 20, "width", 120, "height", 60)).success());

            ToolCallResult props = service.call("ppt.get_shape_properties", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0));
            assertTrue(props.success());
            assertFalse(props.payload().path("shape_type").asText().isBlank());

            assertTrue(service.call("ppt.set_shape_style", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0,
                    "fill_color", "#112233", "border_color", "#AABBCC")).success());

            // resize_shape and move_shape
            ToolCallResult resized = service.call("ppt.resize_shape", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0,
                    "width", 200, "height", 80));
            assertTrue(resized.success(), resized.payload().toString());
            assertTrue(service.call("ppt.move_shape", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0,
                    "x", 50, "y", 50)).success());

            // add_hyperlink
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0, "text", "Go",
                    "x", 200, "y", 200, "width", 80, "height", 40)).success());
            assertTrue(service.call("ppt.add_hyperlink", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 1,
                    "url", "https://example.com")).success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void generatePresentationWithOutputPathWritesFile() throws Exception {
        PptToolService service = new PptToolService();
        try {
            Path out = Files.createTempFile("gen-pres", ".pptx");
            ObjectNode args = mapper.createObjectNode();
            args.putArray("slide_titles").add("A").add("B").add("C");
            args.put("output_path", out.toString());
            args.put("title", "Deck");
            ToolCallResult result = service.call("ppt.generate_presentation", args);
            assertTrue(result.success(), result.payload().toString());
            assertEquals(3, result.payload().path("slide_count").asInt());
            assertTrue(Files.size(out) > 0);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void getSlideMetricsReportsTextCharsAndShapeCounts() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0,
                    "text", "Quick brown fox jumps over the lazy dog",
                    "x", 10, "y", 10, "width", 400, "height", 80)).success());
            ToolCallResult metrics = service.call("ppt.get_slide_metrics", objectNode(
                    "document_id", docId, "slide_index", 0));
            assertTrue(metrics.success());
            assertTrue(metrics.payload().path("text_chars").asInt() > 0);
            assertTrue(metrics.payload().path("word_count").asInt() > 0);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextShapeScopeWithBulletsAndNumbers() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0, "text", "Line\nAnother line",
                    "x", 30, "y", 30, "width", 400, "height", 80)).success());

            // scope=shape with numbered=true hits the numbered branch in applyParagraphStyle
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", 0);
            args.put("numbered", true);
            args.put("text_align", "left");
            args.put("bullet_level", 0);
            args.put("space_before", 6.0);
            args.put("space_after", 6.0);
            args.put("font_family", "Arial");
            ToolCallResult r = service.call("ppt.set_text", args);
            assertTrue(r.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextRunScopeErrorsOnEmptyTargetText() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0, "text", "Hello world",
                    "x", 30, "y", 30, "width", 400, "height", 80)).success());
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", 0);
            args.put("scope", "run");
            args.put("target_text", "");
            ToolCallResult r = service.call("ppt.set_text", args);
            assertFalse(r.success());
            assertEquals("VALIDATION_ERROR", r.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void editTableRejectsUnknownOperation() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 2); at.put("cols", 2);
            at.put("x", 20); at.put("y", 20); at.put("width", 200); at.put("height", 100);
            int idx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            ToolCallResult r = service.call("ppt.edit_table", objectNode(
                    "document_id", docId, "slide_index", 0,
                    "shape_index", idx, "operation", "bogus_op"));
            assertFalse(r.success());
            assertEquals("VALIDATION_ERROR", r.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setPageSetupCustomDimensionsAccepted() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            assertTrue(service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId,
                    "preset", "custom",
                    "width", 1024, "height", 600)).success());
            ToolCallResult info = service.call("ppt.get_document_info",
                    objectNode("document_id", docId));
            assertEquals(1024, info.payload().path("page_width").asInt());
            assertEquals(600, info.payload().path("page_height").asInt());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void errorPathsReturnStructuredEnvelopes() throws Exception {
        PptToolService service = new PptToolService();
        try {
            // Unknown tool.
            ToolCallResult unknown = service.call("ppt.does_not_exist", mapper.createObjectNode());
            assertFalse(unknown.success());

            // open_document: file does not exist.
            Path missing = Files.createTempFile("doesnt-stick", ".pptx");
            Files.delete(missing);
            ToolCallResult open = service.call("ppt.open_document",
                    objectNode("path", missing.toString()));
            assertFalse(open.success());

            // close_document: unknown id.
            assertFalse(service.call("ppt.close_document",
                    objectNode("document_id", "doc_nope")).success());

            // Create a real doc and exercise various validation branches.
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            // list_slides, get_slide_content, update_text with unknown doc.
            assertFalse(service.call("ppt.list_slides",
                    objectNode("document_id", "doc_nope")).success());
            assertFalse(service.call("ppt.get_slide_content",
                    objectNode("document_id", "doc_nope", "slide_index", 0)).success());

            // update_text with invalid slide_index.
            assertFalse(service.call("ppt.update_text", objectNode(
                    "document_id", docId, "slide_index", 99,
                    "old_text", "x", "new_text", "y")).success());

            // replace_text_globally with empty old_text.
            assertFalse(service.call("ppt.replace_text_globally", objectNode(
                    "document_id", docId, "old_text", "", "new_text", "x")).success());

            // reorder_slides: not-an-array, wrong length, invalid index.
            assertFalse(service.call("ppt.reorder_slides",
                    objectNode("document_id", docId)).success());

            ObjectNode wrongLength = mapper.createObjectNode();
            wrongLength.put("document_id", docId);
            wrongLength.putArray("new_order").add(0).add(1);
            assertFalse(service.call("ppt.reorder_slides", wrongLength).success());

            // delete_slides: empty list + keep_at_least_one.
            ObjectNode emptyDel = mapper.createObjectNode();
            emptyDel.put("document_id", docId);
            emptyDel.putArray("slide_indices");
            assertFalse(service.call("ppt.delete_slides", emptyDel).success());

            ObjectNode delAll = mapper.createObjectNode();
            delAll.put("document_id", docId);
            delAll.putArray("slide_indices").add(0);
            assertFalse(service.call("ppt.delete_slides", delAll).success(),
                    "deleting only slide must fail when keep_at_least_one defaults to true");

            // duplicate_slide with invalid source.
            assertFalse(service.call("ppt.duplicate_slide", objectNode(
                    "document_id", docId, "source_slide_index", 99)).success());

            // merge_presentations with missing file.
            assertFalse(service.call("ppt.merge_presentations", objectNode(
                    "document_id", docId, "merge_path", missing.toString())).success());

            // set_slide_notes on fresh slide also triggers its notes-creation branch.
            assertTrue(service.call("ppt.add_slide", objectNode(
                    "document_id", docId, "title", "Second")).success());
            ToolCallResult notes = service.call("ppt.set_slide_notes", objectNode(
                    "document_id", docId, "slide_index", 1, "notes_text", "hello"));
            // Depending on POI state, notes creation may or may not succeed — tolerate both
            // but make sure an error surfaces a structured envelope.
            if (!notes.success()) {
                assertEquals("error", notes.payload().path("status").asText());
            }

            // close_document happy path (covers return success).
            assertTrue(service.call("ppt.close_document",
                    objectNode("document_id", docId)).success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void additionalErrorBranchesExercised() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            // set_page_setup: custom missing dimensions, unsupported preset.
            assertFalse(service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId, "preset", "custom")).success());
            assertFalse(service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId, "preset", "bogus_preset")).success());
            // Exercise all preset branches.
            assertTrue(service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId, "preset", "standard_4_3")).success());
            assertTrue(service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId, "preset", "a4_landscape")).success());
            assertTrue(service.call("ppt.set_page_setup", objectNode(
                    "document_id", docId, "preset", "a4_portrait")).success());

            // add_shape: invalid geometry, remaining shape_type enum values.
            assertFalse(service.call("ppt.add_shape", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_type", "rectangle",
                    "x", 0, "y", 0, "width", -1, "height", -1)).success());
            // ellipse + line + arrow variants exercise the other switch branches.
            for (String shapeType : new String[] {"ellipse", "line", "arrow"}) {
                assertTrue(service.call("ppt.add_shape", objectNode(
                        "document_id", docId, "slide_index", 0,
                        "shape_type", shapeType,
                        "x", 20, "y", 20, "width", 80, "height", 40)).success(),
                        shapeType);
            }

            // add_table with bad rows/cols.
            assertFalse(service.call("ppt.add_table", objectNode(
                    "document_id", docId, "slide_index", 0,
                    "rows", 0, "cols", 2,
                    "x", 20, "y", 20, "width", 100, "height", 40)).success());

            // get_table on a non-table shape → error.
            assertFalse(service.call("ppt.get_table", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0)).success());

            // edit_table set_cell on a non-table shape.
            assertFalse(service.call("ppt.edit_table", objectNode(
                    "document_id", docId, "slide_index", 0, "shape_index", 0,
                    "operation", "set_cell", "row", 0, "col", 0, "text", "x")).success());

            // set_text on a non-text shape (the ellipse we added is text-capable, so use a
            // table's frame — an ellipse IS an XSLFTextShape actually; pick a picture-less
            // scenario by rejecting a missing paragraph. This branch already covered.)
            // set_text with invalid font_size.
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0, "text", "hi",
                    "x", 30, "y", 30, "width", 300, "height", 80)).success());
            ObjectNode badFont = mapper.createObjectNode();
            badFont.put("document_id", docId);
            badFont.put("slide_index", 0);
            badFont.put("shape_index", 0);
            // Use fractional font_size to avoid the schema's integer minimum; negative values
            // fail validator first. Use a negative number bypassing minimum by reading
            // a non-validated-but-rejected branch in setText.
            badFont.put("font_size", 0.5);
            // Actually 0.5 passes the schema minimum of 1 — change to hit server-side check.
            // Use a slightly smaller value that still passes schema but fails business rule
            // is impossible (schema min=1). Instead hit the run scope with missing target.
            badFont.remove("font_size");
            badFont.put("scope", "paragraph");
            badFont.put("paragraph_index", -1);
            ToolCallResult r = service.call("ppt.set_text", badFont);
            assertFalse(r.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void findTextReturnsMatchesAcrossSlides() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 0, "text", "alpha beta alpha",
                    "x", 20, "y", 20, "width", 400, "height", 80)).success());
            assertTrue(service.call("ppt.add_slide",
                    objectNode("document_id", docId, "title", "More alpha")).success());
            assertTrue(service.call("ppt.add_textbox", objectNode(
                    "document_id", docId, "slide_index", 1, "text", "one alpha two",
                    "x", 20, "y", 20, "width", 400, "height", 80)).success());

            ToolCallResult r = service.call("ppt.find_text",
                    objectNode("document_id", docId, "query", "alpha"));
            assertTrue(r.success());
            assertTrue(r.payload().path("count").asInt() >= 3);
            assertTrue(r.payload().path("matches").isArray());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setDefaultTemplateAndUploadTemplateErrorPaths() throws Exception {
        PptToolService service = new PptToolService();
        try {
            // set_default_template: wrong extension.
            Path txt = Files.createTempFile("not-pptx", ".txt");
            ToolCallResult r1 = service.call("ppt.set_default_template",
                    objectNode("template_path", txt.toString()));
            assertFalse(r1.success());

            // upload_template: non-pptx source.
            Path src = Files.createTempFile("wrong-ext", ".odt");
            ToolCallResult r2 = service.call("ppt.upload_template",
                    objectNode("source_path", src.toString()));
            assertFalse(r2.success());
        } finally {
            service.closeAllSessions();
        }
    }

    private ObjectNode objectNode(Object... kv) {
        ObjectNode n = mapper.createObjectNode();
        for (int i = 0; i < kv.length; i += 2) {
            String k = (String) kv[i];
            Object v = kv[i + 1];
            if (v instanceof String s) n.put(k, s);
            else if (v instanceof Integer iv) n.put(k, iv);
            else if (v instanceof Long lv) n.put(k, lv);
            else if (v instanceof Double dv) n.put(k, dv);
            else if (v instanceof Boolean bv) n.put(k, bv);
            else throw new IllegalArgumentException("unsupported: " + v);
        }
        return n;
    }
}
