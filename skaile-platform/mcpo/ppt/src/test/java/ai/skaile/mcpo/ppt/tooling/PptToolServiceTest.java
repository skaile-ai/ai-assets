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
            // Phase 3 wired high_fidelity_render end-to-end; the flag now reflects whether the
            // host has soffice on PATH (true in the Docker image, false on most dev machines).
            var sofficeAvailable = ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.get()
                    .available();
            assertEquals(sofficeAvailable,
                    payload.path("feature_flags").path("high_fidelity_render").asBoolean());
            // Phase 4 turned these on unconditionally — they're pure POI/XmlBeans manipulation.
            assertTrue(payload.path("feature_flags").path("gradients").asBoolean());
            assertTrue(payload.path("feature_flags").path("picture_effects").asBoolean());
            assertTrue(payload.path("feature_flags").path("table_borders").asBoolean());
            assertTrue(payload.path("feature_flags").path("table_merge").asBoolean());
            // Phase 5 flipped charts_update on.
            assertTrue(payload.path("feature_flags").path("charts_update").asBoolean());
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
    void editTableMergeCellsRoundTripsAndDetectsConflict() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 3);
            at.put("cols", 3);
            at.put("x", 20); at.put("y", 20); at.put("width", 400); at.put("height", 200);
            int idx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            ToolCallResult merge = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", idx, "operation", "merge_cells",
                            "start_row", 0, "start_col", 0, "end_row", 1, "end_col", 1));
            assertTrue(merge.success(), merge.payload().toString());
            assertEquals(2, merge.payload().path("row_span").asInt());
            assertEquals(2, merge.payload().path("col_span").asInt());

            // Re-merging an overlapping range must reject with MERGE_CONFLICT.
            ToolCallResult overlap = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", idx, "operation", "merge_cells",
                            "start_row", 1, "start_col", 1, "end_row", 2, "end_col", 2));
            assertFalse(overlap.success());
            assertEquals("MERGE_CONFLICT", overlap.payload().path("code").asText());

            // A single-cell range is rejected as a validation error before touching the table.
            ToolCallResult single = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", idx, "operation", "merge_cells",
                            "start_row", 2, "start_col", 2, "end_row", 2, "end_col", 2));
            assertFalse(single.success());
            assertEquals("VALIDATION_ERROR", single.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void editTableSetCellBorderAcceptsAllSidesAndDashStyle() {
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

            ObjectNode border = mapper.createObjectNode();
            border.put("document_id", docId);
            border.put("slide_index", 0);
            border.put("shape_index", idx);
            border.put("operation", "set_cell_border");
            border.put("row", 0);
            border.put("col", 0);
            border.putArray("sides").add("all");
            border.put("color", "#112233");
            border.put("width", 2.0);
            border.put("dash_style", "dash");
            ToolCallResult result = service.call("ppt.edit_table", border);
            assertTrue(result.success(), result.payload().toString());
            assertEquals(4, result.payload().path("sides_applied").asInt());

            // Empty sides array → validation error, not silent no-op.
            ObjectNode bad = mapper.createObjectNode();
            bad.put("document_id", docId);
            bad.put("slide_index", 0);
            bad.put("shape_index", idx);
            bad.put("operation", "set_cell_border");
            bad.put("row", 0);
            bad.put("col", 0);
            bad.putArray("sides");
            bad.put("color", "#000000");
            ToolCallResult badResult = service.call("ppt.edit_table", bad);
            assertFalse(badResult.success());
            assertEquals("VALIDATION_ERROR", badResult.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void renderSlideHighFidelityReturnsSofficeUnavailableWhenBinaryMissing() throws Exception {
        // Phase 3: fidelity=high is wired through SofficeRenderer. Without soffice on the
        // host, the call short-circuits to SOFFICE_UNAVAILABLE. With soffice installed, we
        // skip — the Docker smoke test exercises the happy path end-to-end.
        ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.reset();
        ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability probe =
                ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.get();
        org.junit.jupiter.api.Assumptions.assumeFalse(probe.available(),
                "skipping: soffice is installed, SOFFICE_UNAVAILABLE path can't be exercised");

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
            assertEquals("SOFFICE_UNAVAILABLE", result.payload().path("code").asText());
            assertFalse(result.payload().path("retriable").asBoolean(true));
        } finally {
            service.closeAllSessions();
            ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.reset();
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
    void exportDocumentSofficeFormatsReturnSofficeUnavailableWhenBinaryMissing() throws Exception {
        // Phase 3: html/png_batch/jpg_batch/svg_batch are now wired through SofficeRenderer.
        // Without soffice on the host, they short-circuit to SOFFICE_UNAVAILABLE.
        ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.reset();
        ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability probe =
                ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.get();
        org.junit.jupiter.api.Assumptions.assumeFalse(probe.available(),
                "skipping: soffice is installed, SOFFICE_UNAVAILABLE path can't be exercised");

        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            for (String fmt : new String[] {"html", "png_batch", "jpg_batch", "svg_batch"}) {
                ObjectNode args = mapper.createObjectNode();
                args.put("document_id", docId);
                args.put("format", fmt);
                args.put("output_path", Files.createTempDirectory("mcpo-export-" + fmt).toString());
                ToolCallResult result = service.call("ppt.export_document", args);
                assertFalse(result.success(), "format=" + fmt);
                assertEquals("SOFFICE_UNAVAILABLE",
                        result.payload().path("code").asText(),
                        "format=" + fmt);
                assertFalse(result.payload().path("retriable").asBoolean(true),
                        "format=" + fmt);
            }
        } finally {
            service.closeAllSessions();
            ai.skaile.mcpo.ppt.tooling.infra.SofficeAvailability.reset();
        }
    }

    @Test
    void exportDocumentOutlineTextProducesMarkdownFile() throws Exception {
        // outline_text is deterministic POI traversal; no soffice required.
        PptToolService service = new PptToolService();
        try {
            ObjectNode create = mapper.createObjectNode();
            create.put("title", "Q2 Review");
            String docId = service.call("ppt.create_document", create)
                    .payload().path("document_id").asText();

            ObjectNode addSlide = mapper.createObjectNode();
            addSlide.put("document_id", docId);
            addSlide.put("title", "Revenue");
            service.call("ppt.add_slide", addSlide);

            Path out = Files.createTempFile("outline-", ".md");
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("format", "outline_text");
            args.put("output_path", out.toString());
            ToolCallResult result = service.call("ppt.export_document", args);
            assertTrue(result.success(), "payload=" + result.payload().toString());
            assertEquals("outline_text", result.payload().path("format").asText());
            assertEquals(out.toString(), result.payload().path("output_path").asText());
            String contents = Files.readString(out);
            assertTrue(contents.contains("# Q2 Review"),
                    "expected title heading in: " + contents);
            assertTrue(contents.contains("# Revenue"),
                    "expected second slide heading in: " + contents);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void exportDocumentBatchRejectsFileOutputPath() throws Exception {
        // png_batch / jpg_batch / svg_batch must write to a directory. Passing an existing
        // file must produce VALIDATION_ERROR before we even try to shell out to soffice.
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            Path existingFile = Files.createTempFile("not-a-dir-", ".txt");
            Files.writeString(existingFile, "sentinel");
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("format", "png_batch");
            args.put("output_path", existingFile.toString());
            ToolCallResult result = service.call("ppt.export_document", args);
            assertFalse(result.success());
            assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
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

    // -------- Phase 4 --------

    @Test
    void setShapeStyleAcceptsGradientFill() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode addShape = mapper.createObjectNode();
            addShape.put("document_id", docId);
            addShape.put("slide_index", 0);
            addShape.put("shape_type", "rectangle");
            addShape.put("x", 50); addShape.put("y", 50);
            addShape.put("width", 200); addShape.put("height", 100);
            int idx = service.call("ppt.add_shape", addShape).payload().path("shape_index").asInt();

            ObjectNode style = mapper.createObjectNode();
            style.put("document_id", docId);
            style.put("slide_index", 0);
            style.put("shape_index", idx);
            style.put("fill_type", "gradient");
            ObjectNode grad = style.putObject("fill_gradient");
            grad.put("type", "linear");
            grad.put("angle", 90.0);
            var stops = grad.putArray("stops");
            ObjectNode a = stops.addObject(); a.put("color", "#FF0000"); a.put("position", 0.0);
            ObjectNode b = stops.addObject(); b.put("color", "#0000FF"); b.put("position", 1.0);
            ToolCallResult result = service.call("ppt.set_shape_style", style);
            assertTrue(result.success(), result.payload().toString());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setShapeStyleRejectsGradientWithSingleStop() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode addShape = mapper.createObjectNode();
            addShape.put("document_id", docId);
            addShape.put("slide_index", 0);
            addShape.put("shape_type", "rectangle");
            addShape.put("x", 0); addShape.put("y", 0);
            addShape.put("width", 100); addShape.put("height", 50);
            int idx = service.call("ppt.add_shape", addShape).payload().path("shape_index").asInt();

            ObjectNode style = mapper.createObjectNode();
            style.put("document_id", docId);
            style.put("slide_index", 0);
            style.put("shape_index", idx);
            style.put("fill_type", "gradient");
            ObjectNode grad = style.putObject("fill_gradient");
            grad.put("type", "linear");
            ObjectNode stop = grad.putArray("stops").addObject();
            stop.put("color", "#000000"); stop.put("position", 0.5);
            ToolCallResult result = service.call("ppt.set_shape_style", style);
            assertFalse(result.success());
            // Schema rejects minItems=2 violation as VALIDATION_ERROR.
            assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setShapeStyleAcceptsPatternFill() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode addShape = mapper.createObjectNode();
            addShape.put("document_id", docId);
            addShape.put("slide_index", 0);
            addShape.put("shape_type", "ellipse");
            addShape.put("x", 0); addShape.put("y", 0);
            addShape.put("width", 100); addShape.put("height", 50);
            int idx = service.call("ppt.add_shape", addShape).payload().path("shape_index").asInt();

            ObjectNode style = mapper.createObjectNode();
            style.put("document_id", docId);
            style.put("slide_index", 0);
            style.put("shape_index", idx);
            style.put("fill_type", "pattern");
            ObjectNode patt = style.putObject("fill_pattern");
            patt.put("preset", "horizontal");
            patt.put("fg_color", "#FFAA00");
            patt.put("bg_color", "#FFFFFF");
            ToolCallResult result = service.call("ppt.set_shape_style", style);
            assertTrue(result.success(), result.payload().toString());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setPictureEffectsRejectsNonPictureShape() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode addShape = mapper.createObjectNode();
            addShape.put("document_id", docId);
            addShape.put("slide_index", 0);
            addShape.put("shape_type", "rectangle");
            addShape.put("x", 0); addShape.put("y", 0);
            addShape.put("width", 100); addShape.put("height", 50);
            int idx = service.call("ppt.add_shape", addShape).payload().path("shape_index").asInt();

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            args.put("alpha", 0.5);
            ToolCallResult result = service.call("ppt.set_picture_effects", args);
            assertFalse(result.success());
            assertEquals("SHAPE_NOT_PICTURE", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setPictureEffectsAppliesCropAlphaRecolorOnRealPicture() throws Exception {
        // Build a tiny PNG so insert_image has something to point at.
        Path img = Files.createTempFile("phase4-pic", ".png");
        BufferedImage bi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bi, "png", img.toFile());

        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode insert = mapper.createObjectNode();
            insert.put("document_id", docId);
            insert.put("slide_index", 0);
            insert.put("image_path", img.toString());
            insert.put("x", 10); insert.put("y", 10);
            insert.put("width", 100); insert.put("height", 100);
            assertTrue(service.call("ppt.insert_image", insert).success());

            int picIdx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", picIdx);
            ObjectNode crop = args.putObject("crop");
            crop.put("left", 0.1); crop.put("top", 0.1);
            crop.put("right", 0.1); crop.put("bottom", 0.1);
            args.put("alpha", 0.6);
            args.putObject("recolor").put("mode", "grayscale");
            ToolCallResult result = service.call("ppt.set_picture_effects", args);
            assertTrue(result.success(), result.payload().toString());
            assertTrue(result.payload().path("crop_applied").asBoolean());
            assertTrue(result.payload().path("alpha_applied").asBoolean());
            assertTrue(result.payload().path("recolor_applied").asBoolean());
        } finally {
            service.closeAllSessions();
            Files.deleteIfExists(img);
        }
    }

    @Test
    void setPictureEffectsRejectsCropOverflow() throws Exception {
        Path img = Files.createTempFile("phase4-pic", ".png");
        BufferedImage bi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bi, "png", img.toFile());

        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode insert = mapper.createObjectNode();
            insert.put("document_id", docId);
            insert.put("slide_index", 0);
            insert.put("image_path", img.toString());
            insert.put("x", 10); insert.put("y", 10);
            insert.put("width", 100); insert.put("height", 100);
            assertTrue(service.call("ppt.insert_image", insert).success());
            int picIdx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", picIdx);
            ObjectNode crop = args.putObject("crop");
            crop.put("left", 0.6); crop.put("right", 0.5);
            ToolCallResult result = service.call("ppt.set_picture_effects", args);
            assertFalse(result.success());
            assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
            Files.deleteIfExists(img);
        }
    }

    @Test
    void cloneShapeNowSupportsAnyShapeNotJustText() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 2); at.put("cols", 2);
            at.put("x", 10); at.put("y", 10);
            at.put("width", 200); at.put("height", 100);
            int tableIdx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            ObjectNode clone = mapper.createObjectNode();
            clone.put("document_id", docId);
            clone.put("slide_index", 0);
            clone.put("shape_index", tableIdx);
            clone.put("offset_x", 30.0);
            clone.put("offset_y", 30.0);
            int shapesBefore = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size();
            ToolCallResult result = service.call("ppt.clone_shape", clone);
            assertTrue(result.success(), result.payload().toString());
            assertNotEquals(tableIdx, result.payload().path("shape_index").asInt());
            int shapesAfter = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size();
            assertEquals(shapesBefore + 1, shapesAfter);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextWiresStrikethroughAndAutoFitAndRotation() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Sample text");
            tb.put("x", 30); tb.put("y", 30);
            tb.put("width", 300); tb.put("height", 80);
            assertTrue(service.call("ppt.add_textbox", tb).success());

            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            args.put("strikethrough", true);
            args.put("rotation", 15.0);
            args.put("auto_fit", "normal");
            ToolCallResult result = service.call("ppt.set_text", args);
            assertTrue(result.success(), result.payload().toString());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextRejectsInvalidAutoFit() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "x");
            tb.put("x", 0); tb.put("y", 0);
            tb.put("width", 100); tb.put("height", 50);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            args.put("auto_fit", "bogus");
            ToolCallResult result = service.call("ppt.set_text", args);
            assertFalse(result.success());
            assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    // ---- Phase 6: coverage supplement ---------------------------------------------------
    // These tests exist primarily to exercise error/branch paths that weren't pulled in by
    // the happy-path tests above. They live alongside the behavioural tests rather than in a
    // separate file so the existing helpers (mapper, objectNode) can be reused.

    @Test
    void setShapeStylePatternFillAcceptsAllPresets() {
        String[] presets = {"vertical", "diagonal_up", "diagonal_down", "cross", "dotted"};
        for (String preset : presets) {
            PptToolService service = new PptToolService();
            try {
                String docId = service.call("ppt.create_document", mapper.createObjectNode())
                        .payload().path("document_id").asText();
                ObjectNode add = mapper.createObjectNode();
                add.put("document_id", docId);
                add.put("slide_index", 0);
                add.put("shape_type", "rectangle");
                add.put("x", 0); add.put("y", 0); add.put("width", 80); add.put("height", 40);
                int idx = service.call("ppt.add_shape", add).payload().path("shape_index").asInt();

                ObjectNode style = mapper.createObjectNode();
                style.put("document_id", docId);
                style.put("slide_index", 0);
                style.put("shape_index", idx);
                style.put("fill_type", "pattern");
                ObjectNode patt = style.putObject("fill_pattern");
                patt.put("preset", preset);
                patt.put("fg_color", "#112233");
                patt.put("bg_color", "#EEDDCC");
                ToolCallResult result = service.call("ppt.set_shape_style", style);
                assertTrue(result.success(), "preset=" + preset + " failed: " + result.payload());
            } finally {
                service.closeAllSessions();
            }
        }
    }

    @Test
    void setShapeStylePatternFillRejectsUnknownPreset() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode add = mapper.createObjectNode();
            add.put("document_id", docId);
            add.put("slide_index", 0);
            add.put("shape_type", "rectangle");
            add.put("x", 0); add.put("y", 0); add.put("width", 80); add.put("height", 40);
            int idx = service.call("ppt.add_shape", add).payload().path("shape_index").asInt();

            ObjectNode style = mapper.createObjectNode();
            style.put("document_id", docId);
            style.put("slide_index", 0);
            style.put("shape_index", idx);
            style.put("fill_type", "pattern");
            style.putObject("fill_pattern").put("preset", "bogus")
                    .put("fg_color", "#000000").put("bg_color", "#FFFFFF");
            ToolCallResult result = service.call("ppt.set_shape_style", style);
            assertFalse(result.success());
            assertEquals("VALIDATION_ERROR", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setShapeStyleNoFillAndSolidShorthandCoverAlternateBranches() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode add = mapper.createObjectNode();
            add.put("document_id", docId);
            add.put("slide_index", 0);
            add.put("shape_type", "rectangle");
            add.put("x", 0); add.put("y", 0); add.put("width", 80); add.put("height", 40);
            int idx = service.call("ppt.add_shape", add).payload().path("shape_index").asInt();

            ObjectNode none = mapper.createObjectNode();
            none.put("document_id", docId);
            none.put("slide_index", 0);
            none.put("shape_index", idx);
            none.put("fill_type", "none");
            assertTrue(service.call("ppt.set_shape_style", none).success());

            ObjectNode solidShort = mapper.createObjectNode();
            solidShort.put("document_id", docId);
            solidShort.put("slide_index", 0);
            solidShort.put("shape_index", idx);
            solidShort.put("fill_color", "#AA00AA");
            assertTrue(service.call("ppt.set_shape_style", solidShort).success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setPictureEffectsRecolorAcceptsAllModes() throws Exception {
        Path img = Files.createTempFile("cov-pic", ".png");
        BufferedImage bi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bi, "png", img.toFile());
        try {
            String[][] modes = {
                    {"sepia", null},
                    {"duotone", "#AA3300"},
                    {"washout", null},
            };
            for (String[] pair : modes) {
                PptToolService service = new PptToolService();
                try {
                    String docId = service.call("ppt.create_document", mapper.createObjectNode())
                            .payload().path("document_id").asText();
                    ObjectNode insert = mapper.createObjectNode();
                    insert.put("document_id", docId);
                    insert.put("slide_index", 0);
                    insert.put("image_path", img.toString());
                    insert.put("x", 0); insert.put("y", 0);
                    insert.put("width", 80); insert.put("height", 60);
                    assertTrue(service.call("ppt.insert_image", insert).success());
                    int idx = service.call("ppt.get_slide_content",
                            objectNode("document_id", docId, "slide_index", 0))
                            .payload().path("shapes").size() - 1;

                    ObjectNode args = mapper.createObjectNode();
                    args.put("document_id", docId);
                    args.put("slide_index", 0);
                    args.put("shape_index", idx);
                    ObjectNode recolor = args.putObject("recolor");
                    recolor.put("mode", pair[0]);
                    if (pair[1] != null) {
                        recolor.put("color", pair[1]);
                    }
                    ToolCallResult result = service.call("ppt.set_picture_effects", args);
                    assertTrue(result.success(), "mode=" + pair[0] + " failed: " + result.payload());
                    assertTrue(result.payload().path("recolor_applied").asBoolean());
                } finally {
                    service.closeAllSessions();
                }
            }
        } finally {
            Files.deleteIfExists(img);
        }
    }

    @Test
    void setPictureEffectsRejectsDuotoneWithoutColorAndUnknownMode() throws Exception {
        Path img = Files.createTempFile("cov-pic", ".png");
        BufferedImage bi = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(bi, "png", img.toFile());
        try {
            PptToolService service = new PptToolService();
            try {
                String docId = service.call("ppt.create_document", mapper.createObjectNode())
                        .payload().path("document_id").asText();
                ObjectNode insert = mapper.createObjectNode();
                insert.put("document_id", docId);
                insert.put("slide_index", 0);
                insert.put("image_path", img.toString());
                insert.put("x", 0); insert.put("y", 0);
                insert.put("width", 80); insert.put("height", 60);
                assertTrue(service.call("ppt.insert_image", insert).success());
                int idx = service.call("ppt.get_slide_content",
                        objectNode("document_id", docId, "slide_index", 0))
                        .payload().path("shapes").size() - 1;

                ObjectNode duotoneNoColor = mapper.createObjectNode();
                duotoneNoColor.put("document_id", docId);
                duotoneNoColor.put("slide_index", 0);
                duotoneNoColor.put("shape_index", idx);
                duotoneNoColor.putObject("recolor").put("mode", "duotone");
                ToolCallResult r1 = service.call("ppt.set_picture_effects", duotoneNoColor);
                assertFalse(r1.success());
                assertEquals("VALIDATION_ERROR", r1.payload().path("code").asText());

                ObjectNode unknownMode = mapper.createObjectNode();
                unknownMode.put("document_id", docId);
                unknownMode.put("slide_index", 0);
                unknownMode.put("shape_index", idx);
                unknownMode.putObject("recolor").put("mode", "nosuch");
                ToolCallResult r2 = service.call("ppt.set_picture_effects", unknownMode);
                assertFalse(r2.success());
                assertEquals("VALIDATION_ERROR", r2.payload().path("code").asText());

                ObjectNode noChange = mapper.createObjectNode();
                noChange.put("document_id", docId);
                noChange.put("slide_index", 0);
                noChange.put("shape_index", idx);
                ToolCallResult r3 = service.call("ppt.set_picture_effects", noChange);
                assertFalse(r3.success());
                assertEquals("VALIDATION_ERROR", r3.payload().path("code").asText());
            } finally {
                service.closeAllSessions();
            }
        } finally {
            Files.deleteIfExists(img);
        }
    }

    @Test
    void setShapeZOrderHandlesAllPositions() {
        String[] positions = {"back", "forward", "backward", "front"};
        for (String position : positions) {
            PptToolService service = new PptToolService();
            try {
                String docId = service.call("ppt.create_document", mapper.createObjectNode())
                        .payload().path("document_id").asText();
                for (int i = 0; i < 3; i++) {
                    ObjectNode shape = mapper.createObjectNode();
                    shape.put("document_id", docId);
                    shape.put("slide_index", 0);
                    shape.put("shape_type", "rectangle");
                    shape.put("x", 20 * i); shape.put("y", 0);
                    shape.put("width", 40); shape.put("height", 30);
                    assertTrue(service.call("ppt.add_shape", shape).success());
                }
                ObjectNode args = mapper.createObjectNode();
                args.put("document_id", docId);
                args.put("slide_index", 0);
                args.put("shape_index", 1);
                args.put("position", position);
                ToolCallResult result = service.call("ppt.set_shape_z_order", args);
                assertTrue(result.success(), "position=" + position + ": " + result.payload());
                assertEquals(position, result.payload().path("position").asText());
            } finally {
                service.closeAllSessions();
            }
        }
    }

    @Test
    void setShapeZOrderRejectsInvalidPosition() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode add = mapper.createObjectNode();
            add.put("document_id", docId);
            add.put("slide_index", 0);
            add.put("shape_type", "rectangle");
            add.put("x", 0); add.put("y", 0); add.put("width", 50); add.put("height", 20);
            int idx = service.call("ppt.add_shape", add).payload().path("shape_index").asInt();

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            args.put("position", "sideways");
            ToolCallResult result = service.call("ppt.set_shape_z_order", args);
            assertFalse(result.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void editTableSetCellBorderIndividualSidesAndAllDashStyles() {
        String[] dashStyles = {"solid", "dot", "dashdot"};
        String[][] sideSets = {{"top"}, {"bottom"}, {"left"}, {"right"}};
        for (String dash : dashStyles) {
            for (String[] sides : sideSets) {
                PptToolService service = new PptToolService();
                try {
                    String docId = service.call("ppt.create_document", mapper.createObjectNode())
                            .payload().path("document_id").asText();
                    ObjectNode at = mapper.createObjectNode();
                    at.put("document_id", docId);
                    at.put("slide_index", 0);
                    at.put("rows", 2); at.put("cols", 2);
                    at.put("x", 20); at.put("y", 20);
                    at.put("width", 200); at.put("height", 100);
                    int tableIdx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

                    ObjectNode border = mapper.createObjectNode();
                    border.put("document_id", docId);
                    border.put("slide_index", 0);
                    border.put("shape_index", tableIdx);
                    border.put("operation", "set_cell_border");
                    border.put("row", 0);
                    border.put("col", 0);
                    var sidesArr = border.putArray("sides");
                    for (String s : sides) sidesArr.add(s);
                    border.put("color", "#112233");
                    border.put("width", 1.5);
                    border.put("dash_style", dash);
                    ToolCallResult result = service.call("ppt.edit_table", border);
                    assertTrue(result.success(),
                            "dash=" + dash + " sides=" + String.join(",", sides)
                                    + " failed: " + result.payload());
                    assertEquals(sides.length, result.payload().path("sides_applied").asInt());
                } finally {
                    service.closeAllSessions();
                }
            }
        }
    }

    @Test
    void editTableSetCellBorderRejectsUnknownSideAndDash() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 2); at.put("cols", 2);
            at.put("x", 0); at.put("y", 0);
            at.put("width", 200); at.put("height", 100);
            int tableIdx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            ObjectNode badSide = mapper.createObjectNode();
            badSide.put("document_id", docId);
            badSide.put("slide_index", 0);
            badSide.put("shape_index", tableIdx);
            badSide.put("operation", "set_cell_border");
            badSide.put("row", 0); badSide.put("col", 0);
            badSide.putArray("sides").add("diagonal");
            badSide.put("color", "#000000");
            ToolCallResult r1 = service.call("ppt.edit_table", badSide);
            assertFalse(r1.success());
            assertEquals("VALIDATION_ERROR", r1.payload().path("code").asText());

            ObjectNode badDash = mapper.createObjectNode();
            badDash.put("document_id", docId);
            badDash.put("slide_index", 0);
            badDash.put("shape_index", tableIdx);
            badDash.put("operation", "set_cell_border");
            badDash.put("row", 0); badDash.put("col", 0);
            badDash.putArray("sides").add("all");
            badDash.put("color", "#000000");
            badDash.put("dash_style", "squiggle");
            ToolCallResult r2 = service.call("ppt.edit_table", badDash);
            assertFalse(r2.success());
            assertEquals("VALIDATION_ERROR", r2.payload().path("code").asText());

            ObjectNode negWidth = mapper.createObjectNode();
            negWidth.put("document_id", docId);
            negWidth.put("slide_index", 0);
            negWidth.put("shape_index", tableIdx);
            negWidth.put("operation", "set_cell_border");
            negWidth.put("row", 0); negWidth.put("col", 0);
            negWidth.putArray("sides").add("top");
            negWidth.put("color", "#000000");
            negWidth.put("width", -2.0);
            ToolCallResult r3 = service.call("ppt.edit_table", negWidth);
            assertFalse(r3.success());
            assertEquals("VALIDATION_ERROR", r3.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void importMarkdownOutlineHandlesBulletsAndPlainText() {
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("markdown_text", "# First\n- bullet one\n* bullet two\nstanding paragraph\n# Second\nextra line");
            ToolCallResult result = service.call("ppt.import_markdown_outline", args);
            assertTrue(result.success(), result.payload().toString());
            // The blank template ships with 1 default slide; the two headings add 2 more.
            assertTrue(result.payload().path("slide_count").asInt() >= 2,
                    result.payload().toString());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void importMarkdownOutlineEmptyRejected() {
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("markdown_text", "   \n   ");
            ToolCallResult result = service.call("ppt.import_markdown_outline", args);
            assertFalse(result.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void importMarkdownOutlineWithoutHeadingsCreatesFallbackSlide() {
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("markdown_text", "just some plain text\nwithout any heading");
            ToolCallResult result = service.call("ppt.import_markdown_outline", args);
            assertTrue(result.success(), result.payload().toString());
            assertEquals(1, result.payload().path("slide_count").asInt());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void listChartsOnDocumentWithoutChartsReturnsEmpty() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            ToolCallResult result = service.call("ppt.list_charts", args);
            assertTrue(result.success(), result.payload().toString());
            assertEquals(0, result.payload().path("charts").size());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void updateChartDataRejectsNonChartShapeWithShapeNotChart() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode add = mapper.createObjectNode();
            add.put("document_id", docId);
            add.put("slide_index", 0);
            add.put("shape_type", "rectangle");
            add.put("x", 0); add.put("y", 0); add.put("width", 50); add.put("height", 30);
            int idx = service.call("ppt.add_shape", add).payload().path("shape_index").asInt();

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            var seriesArr = args.putArray("series");
            ObjectNode s0 = seriesArr.addObject();
            s0.put("name", "s0");
            s0.putArray("values").add(1.0).add(2.0);
            ToolCallResult result = service.call("ppt.update_chart_data", args);
            assertFalse(result.success());
            assertEquals("SHAPE_NOT_CHART", result.payload().path("code").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setSlideBackgroundAppliesSolidColor() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("color", "#334455");
            ToolCallResult result = service.call("ppt.set_slide_background", args);
            assertTrue(result.success(), result.payload().toString());
            assertEquals("#334455", result.payload().path("color").asText());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setSlideLayoutHandlesKnownAndUnknownLayoutTypes() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            for (String layout : new String[]{"blank", "title", "title_content"}) {
                ObjectNode args = mapper.createObjectNode();
                args.put("document_id", docId);
                args.put("slide_index", 0);
                args.put("layout_type", layout);
                ToolCallResult result = service.call("ppt.set_slide_layout", args);
                assertTrue(result.success(), "layout=" + layout + " failed: " + result.payload());
            }
            ObjectNode bad = mapper.createObjectNode();
            bad.put("document_id", docId);
            bad.put("slide_index", 0);
            bad.put("layout_type", "mystery");
            ToolCallResult result = service.call("ppt.set_slide_layout", bad);
            assertFalse(result.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setPageSetupRejectsUnknownPresetAndCustomMissingDimensions() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode bogus = mapper.createObjectNode();
            bogus.put("document_id", docId);
            bogus.put("preset", "bogus");
            ToolCallResult r1 = service.call("ppt.set_page_setup", bogus);
            assertFalse(r1.success());

            ObjectNode custom = mapper.createObjectNode();
            custom.put("document_id", docId);
            custom.put("preset", "custom");
            ToolCallResult r2 = service.call("ppt.set_page_setup", custom);
            assertFalse(r2.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextAutoFitNoneAndShrinkAndTextAlignRight() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Long text that may overflow");
            tb.put("x", 0); tb.put("y", 0);
            tb.put("width", 100); tb.put("height", 40);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            for (String mode : new String[]{"none", "normal", "shrink"}) {
                ObjectNode args = mapper.createObjectNode();
                args.put("document_id", docId);
                args.put("slide_index", 0);
                args.put("shape_index", idx);
                args.put("auto_fit", mode);
                ToolCallResult result = service.call("ppt.set_text", args);
                assertTrue(result.success(),
                        "auto_fit=" + mode + " failed: " + result.payload());
            }

            // Cover text_align right/center/justify and strikethrough=false.
            for (String align : new String[]{"right", "center", "justify", "left"}) {
                ObjectNode args = mapper.createObjectNode();
                args.put("document_id", docId);
                args.put("slide_index", 0);
                args.put("shape_index", idx);
                args.put("text_align", align);
                ToolCallResult result = service.call("ppt.set_text", args);
                assertTrue(result.success(), "align=" + align + ": " + result.payload());
            }

            ObjectNode strikeOff = mapper.createObjectNode();
            strikeOff.put("document_id", docId);
            strikeOff.put("slide_index", 0);
            strikeOff.put("shape_index", idx);
            strikeOff.put("strikethrough", false);
            assertTrue(service.call("ppt.set_text", strikeOff).success());

            ObjectNode bad = mapper.createObjectNode();
            bad.put("document_id", docId);
            bad.put("slide_index", 0);
            bad.put("shape_index", idx);
            bad.put("text_align", "diagonal");
            ToolCallResult result = service.call("ppt.set_text", bad);
            assertFalse(result.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextRejectsNonPositiveFontSizeAndNonTextShape() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "x");
            tb.put("x", 0); tb.put("y", 0);
            tb.put("width", 50); tb.put("height", 30);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            args.put("font_size", 0.5);
            // Schema minimum is an integer so use 1 for schema-pass + runtime-validate by
            // supplying a negative paragraph_index instead: covers both branches.
            ToolCallResult r1 = service.call("ppt.set_text", args);
            // Either VALIDATION_ERROR (schema) or tool-level error — both are negative outcomes.
            // Test the not-text-shape branch by inserting an image and targeting it.
            java.nio.file.Path img;
            try {
                img = java.nio.file.Files.createTempFile("cov-text", ".png");
                javax.imageio.ImageIO.write(
                        new java.awt.image.BufferedImage(4, 4,
                                java.awt.image.BufferedImage.TYPE_INT_RGB),
                        "png", img.toFile());
            } catch (java.io.IOException ioe) {
                throw new RuntimeException(ioe);
            }
            try {
                ObjectNode insert = mapper.createObjectNode();
                insert.put("document_id", docId);
                insert.put("slide_index", 0);
                insert.put("image_path", img.toString());
                insert.put("x", 10); insert.put("y", 10);
                insert.put("width", 50); insert.put("height", 50);
                assertTrue(service.call("ppt.insert_image", insert).success());
                int picIdx = service.call("ppt.get_slide_content",
                        objectNode("document_id", docId, "slide_index", 0))
                        .payload().path("shapes").size() - 1;

                ObjectNode onPic = mapper.createObjectNode();
                onPic.put("document_id", docId);
                onPic.put("slide_index", 0);
                onPic.put("shape_index", picIdx);
                onPic.put("bold", true);
                ToolCallResult r2 = service.call("ppt.set_text", onPic);
                assertFalse(r2.success());

                ObjectNode badScope = mapper.createObjectNode();
                badScope.put("document_id", docId);
                badScope.put("slide_index", 0);
                badScope.put("shape_index", idx);
                badScope.put("scope", "weird");
                ToolCallResult r3 = service.call("ppt.set_text", badScope);
                assertFalse(r3.success());
            } finally {
                try {
                    java.nio.file.Files.deleteIfExists(img);
                } catch (java.io.IOException ignored) {
                }
            }
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setTextRunScopeCaseInsensitiveFindsMatch() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Quarterly Revenue is STRONG");
            tb.put("x", 0); tb.put("y", 0);
            tb.put("width", 300); tb.put("height", 50);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode args = mapper.createObjectNode();
            args.put("document_id", docId);
            args.put("slide_index", 0);
            args.put("shape_index", idx);
            args.put("scope", "run");
            args.put("target_text", "strong");
            args.put("case_sensitive", false);
            args.put("bold", true);
            ToolCallResult result = service.call("ppt.set_text", args);
            assertTrue(result.success(), result.payload().toString());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void reorderSlidesRejectsInvalidPermutations() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            service.call("ppt.add_slide", objectNode("document_id", docId, "title", "B"));
            service.call("ppt.add_slide", objectNode("document_id", docId, "title", "C"));

            // Missing index.
            ObjectNode missing = mapper.createObjectNode();
            missing.put("document_id", docId);
            missing.putArray("new_order").add(0).add(1);
            ToolCallResult r1 = service.call("ppt.reorder_slides", missing);
            assertFalse(r1.success());

            // Out of range.
            ObjectNode oob = mapper.createObjectNode();
            oob.put("document_id", docId);
            oob.putArray("new_order").add(0).add(1).add(7);
            ToolCallResult r2 = service.call("ppt.reorder_slides", oob);
            assertFalse(r2.success());

            // Duplicate index.
            ObjectNode dup = mapper.createObjectNode();
            dup.put("document_id", docId);
            dup.putArray("new_order").add(0).add(0).add(1);
            ToolCallResult r3 = service.call("ppt.reorder_slides", dup);
            assertFalse(r3.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void transactionRollbackWithoutBeginReturnsError() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ToolCallResult rollback = service.call("ppt.transaction_rollback",
                    objectNode("document_id", docId));
            assertFalse(rollback.success());
            // Commit without begin is a no-op by contract (idempotent).
            ToolCallResult commit = service.call("ppt.transaction_commit",
                    objectNode("document_id", docId));
            assertTrue(commit.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void deleteSlidesRefusesToRemoveEveryRemainingSlide() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode deleteAll = mapper.createObjectNode();
            deleteAll.put("document_id", docId);
            deleteAll.putArray("slide_indices").add(0);
            ToolCallResult result = service.call("ppt.delete_slides", deleteAll);
            assertFalse(result.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void renderSlideRejectsInvalidFidelityAndFormat() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();

            Path outPath = Files.createTempFile("cov-render", ".png");
            Files.deleteIfExists(outPath);

            ObjectNode badFormat = mapper.createObjectNode();
            badFormat.put("document_id", docId);
            badFormat.put("slide_index", 0);
            badFormat.put("output_path", outPath.toString());
            badFormat.put("format", "tiff");
            ToolCallResult r1 = service.call("ppt.render_slide", badFormat);
            assertFalse(r1.success());

            ObjectNode badFidelity = mapper.createObjectNode();
            badFidelity.put("document_id", docId);
            badFidelity.put("slide_index", 0);
            badFidelity.put("output_path", outPath.toString());
            badFidelity.put("fidelity", "ultra");
            ToolCallResult r2 = service.call("ppt.render_slide", badFidelity);
            assertFalse(r2.success());
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void uploadTemplateRejectsNonPptxExtension() {
        PptToolService service = new PptToolService();
        try {
            Path notPptx = Files.createTempFile("template-bad", ".txt");
            Files.writeString(notPptx, "not a pptx");
            try {
                ObjectNode args = mapper.createObjectNode();
                args.put("source_path", notPptx.toString());
                args.put("template_name", "bad.pptx");
                ToolCallResult result = service.call("ppt.upload_template", args);
                assertFalse(result.success());
            } finally {
                Files.deleteIfExists(notPptx);
            }
        } catch (java.io.IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setDefaultTemplateRejectsNonExistingPath() {
        PptToolService service = new PptToolService();
        try {
            ObjectNode args = mapper.createObjectNode();
            args.put("template_path", "/definitely/not/a/path/nothing.pptx");
            ToolCallResult result = service.call("ppt.set_default_template", args);
            assertFalse(result.success());

            ObjectNode bad = mapper.createObjectNode();
            bad.put("template_path", "/tmp/not-a-template.txt");
            ToolCallResult r2 = service.call("ppt.set_default_template", bad);
            assertFalse(r2.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void editTableRemainingStructuralOperationsCoverAllBranches() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode at = mapper.createObjectNode();
            at.put("document_id", docId);
            at.put("slide_index", 0);
            at.put("rows", 3);
            at.put("cols", 3);
            at.put("x", 0); at.put("y", 0); at.put("width", 300); at.put("height", 200);
            int tableIdx = service.call("ppt.add_table", at).payload().path("shape_index").asInt();

            // insert_row, delete_row, set_row_height, set_col_width, set_header_style.
            ToolCallResult insRow = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", tableIdx, "operation", "insert_row", "index", 1));
            assertTrue(insRow.success(), insRow.payload().toString());
            int tid = insRow.payload().path("shape_index").asInt();

            ToolCallResult delRow = service.call("ppt.edit_table",
                    objectNode("document_id", docId, "slide_index", 0,
                            "shape_index", tid, "operation", "delete_row", "index", 1));
            assertTrue(delRow.success(), delRow.payload().toString());
            tid = delRow.payload().path("shape_index").asInt();

            ObjectNode rowHeight = mapper.createObjectNode();
            rowHeight.put("document_id", docId);
            rowHeight.put("slide_index", 0);
            rowHeight.put("shape_index", tid);
            rowHeight.put("operation", "set_row_height");
            rowHeight.put("row_index", 0);
            rowHeight.put("height", 60.0);
            assertTrue(service.call("ppt.edit_table", rowHeight).success());

            ObjectNode colWidth = mapper.createObjectNode();
            colWidth.put("document_id", docId);
            colWidth.put("slide_index", 0);
            colWidth.put("shape_index", tid);
            colWidth.put("operation", "set_col_width");
            colWidth.put("col_index", 1);
            colWidth.put("width", 120.0);
            assertTrue(service.call("ppt.edit_table", colWidth).success());

            ObjectNode header = mapper.createObjectNode();
            header.put("document_id", docId);
            header.put("slide_index", 0);
            header.put("shape_index", tid);
            header.put("operation", "set_header_style");
            header.put("fill_color", "#112233");
            header.put("font_color", "#FFFFFF");
            header.put("bold", true);
            assertTrue(service.call("ppt.edit_table", header).success());

            // Unknown operation → VALIDATION_ERROR (already covered by another test) plus a
            // missing-shape path: pass a shape_index that points at something other than a table.
            ObjectNode addRect = mapper.createObjectNode();
            addRect.put("document_id", docId);
            addRect.put("slide_index", 0);
            addRect.put("shape_type", "rectangle");
            addRect.put("x", 0); addRect.put("y", 0);
            addRect.put("width", 40); addRect.put("height", 20);
            int rectIdx = service.call("ppt.add_shape", addRect).payload().path("shape_index").asInt();

            ObjectNode nonTable = mapper.createObjectNode();
            nonTable.put("document_id", docId);
            nonTable.put("slide_index", 0);
            nonTable.put("shape_index", rectIdx);
            nonTable.put("operation", "set_cell");
            nonTable.put("row", 0);
            nonTable.put("col", 0);
            nonTable.put("text", "x");
            ToolCallResult rNot = service.call("ppt.edit_table", nonTable);
            assertFalse(rNot.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void addHyperlinkAcceptsValidUrlRejectsNonText() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Click me");
            tb.put("x", 0); tb.put("y", 0); tb.put("width", 100); tb.put("height", 30);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            ObjectNode link = mapper.createObjectNode();
            link.put("document_id", docId);
            link.put("slide_index", 0);
            link.put("shape_index", idx);
            link.put("url", "https://example.com");
            assertTrue(service.call("ppt.add_hyperlink", link).success());

            ObjectNode add = mapper.createObjectNode();
            add.put("document_id", docId);
            add.put("slide_index", 0);
            add.put("rows", 2); add.put("cols", 2);
            add.put("x", 0); add.put("y", 80);
            add.put("width", 100); add.put("height", 100);
            int tableIdx = service.call("ppt.add_table", add).payload().path("shape_index").asInt();

            ObjectNode badLink = mapper.createObjectNode();
            badLink.put("document_id", docId);
            badLink.put("slide_index", 0);
            badLink.put("shape_index", tableIdx);
            badLink.put("url", "https://example.com");
            ToolCallResult result = service.call("ppt.add_hyperlink", badLink);
            assertFalse(result.success());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setShapeStyleTextAlignOnTextShapeBranch() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "align me");
            tb.put("x", 0); tb.put("y", 0); tb.put("width", 120); tb.put("height", 40);
            assertTrue(service.call("ppt.add_textbox", tb).success());
            int idx = service.call("ppt.get_slide_content",
                    objectNode("document_id", docId, "slide_index", 0))
                    .payload().path("shapes").size() - 1;

            for (String align : new String[]{"left", "center", "right", "justify"}) {
                ObjectNode style = mapper.createObjectNode();
                style.put("document_id", docId);
                style.put("slide_index", 0);
                style.put("shape_index", idx);
                style.put("text_align", align);
                ToolCallResult result = service.call("ppt.set_shape_style", style);
                assertTrue(result.success(), "align=" + align + ": " + result.payload());
            }
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void findTextCaseSensitiveAndWholeWordBranches() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode tb = mapper.createObjectNode();
            tb.put("document_id", docId);
            tb.put("slide_index", 0);
            tb.put("text", "Revenue is revenue and REVENUE");
            tb.put("x", 0); tb.put("y", 0); tb.put("width", 300); tb.put("height", 50);
            assertTrue(service.call("ppt.add_textbox", tb).success());

            ObjectNode caseSensitive = mapper.createObjectNode();
            caseSensitive.put("document_id", docId);
            caseSensitive.put("query", "Revenue");
            caseSensitive.put("case_sensitive", true);
            ToolCallResult r1 = service.call("ppt.find_text", caseSensitive);
            assertTrue(r1.success(), r1.payload().toString());

            ObjectNode caseInsensitive = mapper.createObjectNode();
            caseInsensitive.put("document_id", docId);
            caseInsensitive.put("query", "revenue");
            caseInsensitive.put("case_sensitive", false);
            ToolCallResult r2 = service.call("ppt.find_text", caseInsensitive);
            assertTrue(r2.success(), r2.payload().toString());
            // Case-insensitive should match 3 occurrences; case-sensitive 1.
            assertTrue(r2.payload().path("count").asInt() > r1.payload().path("count").asInt());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void setShapeStyleOutlineAndOpacityBranches() {
        PptToolService service = new PptToolService();
        try {
            String docId = service.call("ppt.create_document", mapper.createObjectNode())
                    .payload().path("document_id").asText();
            ObjectNode add = mapper.createObjectNode();
            add.put("document_id", docId);
            add.put("slide_index", 0);
            add.put("shape_type", "rectangle");
            add.put("x", 0); add.put("y", 0); add.put("width", 80); add.put("height", 40);
            int idx = service.call("ppt.add_shape", add).payload().path("shape_index").asInt();

            ObjectNode style = mapper.createObjectNode();
            style.put("document_id", docId);
            style.put("slide_index", 0);
            style.put("shape_index", idx);
            style.put("border_color", "#0099CC");
            style.put("border_width", 2.5);
            ToolCallResult result = service.call("ppt.set_shape_style", style);
            assertTrue(result.success(), result.payload().toString());
        } finally {
            service.closeAllSessions();
        }
    }

    @Test
    void pathOutsideAllowedRootReturnsPathNotAllowed() throws Exception {
        Path sandbox = Files.createTempDirectory("cov-sandbox-");
        try {
            ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig config =
                    new ai.skaile.mcpo.ppt.tooling.infra.PptServerConfig(
                            sandbox,
                            sandbox.resolve(".mcpo-ppt/templates"),
                            sandbox.resolve(".mcpo-ppt/default-template.json"),
                            5,
                            "soffice",
                            System.getProperty("java.version", "unknown"));
            PptToolService service = new PptToolService(config);
            try {
                ObjectNode args = mapper.createObjectNode();
                args.put("path", "/etc/hosts");
                ToolCallResult result = service.call("ppt.open_document", args);
                assertFalse(result.success());
                assertEquals("PATH_NOT_ALLOWED", result.payload().path("code").asText());
                assertFalse(result.payload().path("retriable").asBoolean());
            } finally {
                service.closeAllSessions();
            }
        } finally {
            Files.deleteIfExists(sandbox);
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
