package ai.skaile.mcpo.ppt.tooling.operations;

import com.fasterxml.jackson.databind.JsonNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;

public final class PptDocumentOperations {
    private final ToolHandler createDocumentHandler;
    private final ToolHandler openDocumentHandler;
    private final ToolHandler closeDocumentHandler;
    private final ToolHandler getDocumentInfoHandler;
    private final ToolHandler listSlidesHandler;
    private final ToolHandler reorderSlidesHandler;
    private final ToolHandler exportDocumentHandler;
    private final ToolHandler generatePresentationHandler;
    private final ToolHandler mergePresentationsHandler;
    private final ToolHandler transactionBeginHandler;
    private final ToolHandler transactionCommitHandler;
    private final ToolHandler transactionRollbackHandler;

    public PptDocumentOperations(
            ToolHandler createDocumentHandler,
            ToolHandler openDocumentHandler,
            ToolHandler closeDocumentHandler,
            ToolHandler getDocumentInfoHandler,
            ToolHandler listSlidesHandler,
            ToolHandler reorderSlidesHandler,
            ToolHandler exportDocumentHandler,
            ToolHandler generatePresentationHandler,
            ToolHandler mergePresentationsHandler,
            ToolHandler transactionBeginHandler,
            ToolHandler transactionCommitHandler,
            ToolHandler transactionRollbackHandler) {
        this.createDocumentHandler = createDocumentHandler;
        this.openDocumentHandler = openDocumentHandler;
        this.closeDocumentHandler = closeDocumentHandler;
        this.getDocumentInfoHandler = getDocumentInfoHandler;
        this.listSlidesHandler = listSlidesHandler;
        this.reorderSlidesHandler = reorderSlidesHandler;
        this.exportDocumentHandler = exportDocumentHandler;
        this.generatePresentationHandler = generatePresentationHandler;
        this.mergePresentationsHandler = mergePresentationsHandler;
        this.transactionBeginHandler = transactionBeginHandler;
        this.transactionCommitHandler = transactionCommitHandler;
        this.transactionRollbackHandler = transactionRollbackHandler;
    }

    public ToolCallResult createDocument(JsonNode args) throws Exception {
        return createDocumentHandler.handle(args);
    }

    public ToolCallResult openDocument(JsonNode args) throws Exception {
        return openDocumentHandler.handle(args);
    }

    public ToolCallResult closeDocument(JsonNode args) throws Exception {
        return closeDocumentHandler.handle(args);
    }

    public ToolCallResult getDocumentInfo(JsonNode args) throws Exception {
        return getDocumentInfoHandler.handle(args);
    }

    public ToolCallResult listSlides(JsonNode args) throws Exception {
        return listSlidesHandler.handle(args);
    }

    public ToolCallResult reorderSlides(JsonNode args) throws Exception {
        return reorderSlidesHandler.handle(args);
    }

    public ToolCallResult exportDocument(JsonNode args) throws Exception {
        return exportDocumentHandler.handle(args);
    }

    public ToolCallResult generatePresentation(JsonNode args) throws Exception {
        return generatePresentationHandler.handle(args);
    }

    public ToolCallResult mergePresentations(JsonNode args) throws Exception {
        return mergePresentationsHandler.handle(args);
    }

    public ToolCallResult transactionBegin(JsonNode args) throws Exception {
        return transactionBeginHandler.handle(args);
    }

    public ToolCallResult transactionCommit(JsonNode args) throws Exception {
        return transactionCommitHandler.handle(args);
    }

    public ToolCallResult transactionRollback(JsonNode args) throws Exception {
        return transactionRollbackHandler.handle(args);
    }
}
