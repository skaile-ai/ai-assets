package ai.skaile.mcpo.ppt.tooling.operations;

import com.fasterxml.jackson.databind.JsonNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;

public final class PptTemplateService {
    private final ToolHandler insertImageHandler;
    private final ToolHandler uploadTemplateHandler;
    private final ToolHandler setDefaultTemplateHandler;
    private final ToolHandler getDefaultTemplateHandler;
    private final ToolHandler importMarkdownOutlineHandler;

    public PptTemplateService(
            ToolHandler insertImageHandler,
            ToolHandler uploadTemplateHandler,
            ToolHandler setDefaultTemplateHandler,
            ToolHandler getDefaultTemplateHandler,
            ToolHandler importMarkdownOutlineHandler) {
        this.insertImageHandler = insertImageHandler;
        this.uploadTemplateHandler = uploadTemplateHandler;
        this.setDefaultTemplateHandler = setDefaultTemplateHandler;
        this.getDefaultTemplateHandler = getDefaultTemplateHandler;
        this.importMarkdownOutlineHandler = importMarkdownOutlineHandler;
    }

    public ToolCallResult insertImage(JsonNode args) throws Exception {
        return insertImageHandler.handle(args);
    }

    public ToolCallResult uploadTemplate(JsonNode args) throws Exception {
        return uploadTemplateHandler.handle(args);
    }

    public ToolCallResult setDefaultTemplate(JsonNode args) throws Exception {
        return setDefaultTemplateHandler.handle(args);
    }

    public ToolCallResult getDefaultTemplate(JsonNode args) throws Exception {
        return getDefaultTemplateHandler.handle(args);
    }

    public ToolCallResult importMarkdownOutline(JsonNode args) throws Exception {
        return importMarkdownOutlineHandler.handle(args);
    }
}
