package ai.skaile.mcpo.ppt.tooling.operations;

import com.fasterxml.jackson.databind.JsonNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolHandler;

public final class PptRenderService {
    private final ToolHandler renderSlideImageHandler;
    private final ToolHandler renderSlideSvgHandler;
    private final ToolHandler findTextHandler;
    private final ToolHandler getSlideMetricsHandler;

    public PptRenderService(
            ToolHandler renderSlideImageHandler,
            ToolHandler renderSlideSvgHandler,
            ToolHandler findTextHandler,
            ToolHandler getSlideMetricsHandler) {
        this.renderSlideImageHandler = renderSlideImageHandler;
        this.renderSlideSvgHandler = renderSlideSvgHandler;
        this.findTextHandler = findTextHandler;
        this.getSlideMetricsHandler = getSlideMetricsHandler;
    }

    public ToolCallResult renderSlideImage(JsonNode args) throws Exception {
        return renderSlideImageHandler.handle(args);
    }

    public ToolCallResult renderSlideSvg(JsonNode args) throws Exception {
        return renderSlideSvgHandler.handle(args);
    }

    public ToolCallResult findText(JsonNode args) throws Exception {
        return findTextHandler.handle(args);
    }

    public ToolCallResult getSlideMetrics(JsonNode args) throws Exception {
        return getSlideMetricsHandler.handle(args);
    }
}
