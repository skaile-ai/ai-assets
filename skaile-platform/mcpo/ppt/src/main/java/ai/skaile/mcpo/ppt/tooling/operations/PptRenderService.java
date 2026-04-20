package ai.skaile.mcpo.ppt.tooling.operations;

import com.fasterxml.jackson.databind.JsonNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;

public final class PptRenderService {
    private final ToolHandler renderSlideHandler;
    private final ToolHandler renderAllSlidesHandler;
    private final ToolHandler findTextHandler;
    private final ToolHandler getSlideMetricsHandler;

    public PptRenderService(
            ToolHandler renderSlideHandler,
            ToolHandler renderAllSlidesHandler,
            ToolHandler findTextHandler,
            ToolHandler getSlideMetricsHandler) {
        this.renderSlideHandler = renderSlideHandler;
        this.renderAllSlidesHandler = renderAllSlidesHandler;
        this.findTextHandler = findTextHandler;
        this.getSlideMetricsHandler = getSlideMetricsHandler;
    }

    public ToolCallResult renderSlide(JsonNode args) throws Exception {
        return renderSlideHandler.handle(args);
    }

    public ToolCallResult renderAllSlides(JsonNode args) throws Exception {
        return renderAllSlidesHandler.handle(args);
    }

    public ToolCallResult findText(JsonNode args) throws Exception {
        return findTextHandler.handle(args);
    }

    public ToolCallResult getSlideMetrics(JsonNode args) throws Exception {
        return getSlideMetricsHandler.handle(args);
    }
}
