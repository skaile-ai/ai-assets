package ai.skaile.mcpo.ppt.tooling.operations;

import com.fasterxml.jackson.databind.JsonNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;

public final class PptSlideOperations {
    private final ToolHandler getSlideContentHandler;
    private final ToolHandler addSlideHandler;
    private final ToolHandler duplicateSlideHandler;
    private final ToolHandler deleteSlidesHandler;
    private final ToolHandler updateTextHandler;
    private final ToolHandler replaceTextGloballyHandler;
    private final ToolHandler addTextBoxHandler;
    private final ToolHandler getSlideNotesHandler;
    private final ToolHandler setSlideNotesHandler;
    private final ToolHandler addTableHandler;
    private final ToolHandler getTableHandler;
    private final ToolHandler editTableHandler;
    private final ToolHandler setTextHandler;
    private final ToolHandler moveShapeHandler;
    private final ToolHandler cloneShapeHandler;
    private final ToolHandler resizeShapeHandler;
    private final ToolHandler addHyperlinkHandler;
    private final ToolHandler setSlideBackgroundHandler;

    public PptSlideOperations(
            ToolHandler getSlideContentHandler,
            ToolHandler addSlideHandler,
            ToolHandler duplicateSlideHandler,
            ToolHandler deleteSlidesHandler,
            ToolHandler updateTextHandler,
            ToolHandler replaceTextGloballyHandler,
            ToolHandler addTextBoxHandler,
            ToolHandler getSlideNotesHandler,
            ToolHandler setSlideNotesHandler,
            ToolHandler addTableHandler,
            ToolHandler getTableHandler,
            ToolHandler editTableHandler,
            ToolHandler setTextHandler,
            ToolHandler moveShapeHandler,
            ToolHandler cloneShapeHandler,
            ToolHandler resizeShapeHandler,
            ToolHandler addHyperlinkHandler,
            ToolHandler setSlideBackgroundHandler) {
        this.getSlideContentHandler = getSlideContentHandler;
        this.addSlideHandler = addSlideHandler;
        this.duplicateSlideHandler = duplicateSlideHandler;
        this.deleteSlidesHandler = deleteSlidesHandler;
        this.updateTextHandler = updateTextHandler;
        this.replaceTextGloballyHandler = replaceTextGloballyHandler;
        this.addTextBoxHandler = addTextBoxHandler;
        this.getSlideNotesHandler = getSlideNotesHandler;
        this.setSlideNotesHandler = setSlideNotesHandler;
        this.addTableHandler = addTableHandler;
        this.getTableHandler = getTableHandler;
        this.editTableHandler = editTableHandler;
        this.setTextHandler = setTextHandler;
        this.moveShapeHandler = moveShapeHandler;
        this.cloneShapeHandler = cloneShapeHandler;
        this.resizeShapeHandler = resizeShapeHandler;
        this.addHyperlinkHandler = addHyperlinkHandler;
        this.setSlideBackgroundHandler = setSlideBackgroundHandler;
    }

    public ToolCallResult getSlideContent(JsonNode args) throws Exception {
        return getSlideContentHandler.handle(args);
    }

    public ToolCallResult addSlide(JsonNode args) throws Exception {
        return addSlideHandler.handle(args);
    }

    public ToolCallResult duplicateSlide(JsonNode args) throws Exception {
        return duplicateSlideHandler.handle(args);
    }

    public ToolCallResult deleteSlides(JsonNode args) throws Exception {
        return deleteSlidesHandler.handle(args);
    }

    public ToolCallResult updateText(JsonNode args) throws Exception {
        return updateTextHandler.handle(args);
    }

    public ToolCallResult replaceTextGlobally(JsonNode args) throws Exception {
        return replaceTextGloballyHandler.handle(args);
    }

    public ToolCallResult addTextBox(JsonNode args) throws Exception {
        return addTextBoxHandler.handle(args);
    }

    public ToolCallResult getSlideNotes(JsonNode args) throws Exception {
        return getSlideNotesHandler.handle(args);
    }

    public ToolCallResult setSlideNotes(JsonNode args) throws Exception {
        return setSlideNotesHandler.handle(args);
    }

    public ToolCallResult addTable(JsonNode args) throws Exception {
        return addTableHandler.handle(args);
    }

    public ToolCallResult getTable(JsonNode args) throws Exception {
        return getTableHandler.handle(args);
    }

    public ToolCallResult editTable(JsonNode args) throws Exception {
        return editTableHandler.handle(args);
    }

    public ToolCallResult setText(JsonNode args) throws Exception {
        return setTextHandler.handle(args);
    }

    public ToolCallResult moveShape(JsonNode args) throws Exception {
        return moveShapeHandler.handle(args);
    }

    public ToolCallResult cloneShape(JsonNode args) throws Exception {
        return cloneShapeHandler.handle(args);
    }

    public ToolCallResult resizeShape(JsonNode args) throws Exception {
        return resizeShapeHandler.handle(args);
    }

    public ToolCallResult addHyperlink(JsonNode args) throws Exception {
        return addHyperlinkHandler.handle(args);
    }

    public ToolCallResult setSlideBackground(JsonNode args) throws Exception {
        return setSlideBackgroundHandler.handle(args);
    }
}
