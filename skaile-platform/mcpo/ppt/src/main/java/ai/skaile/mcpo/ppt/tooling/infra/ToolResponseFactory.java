package ai.skaile.mcpo.ppt.tooling.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolCallResult;

public final class ToolResponseFactory {
    private final ObjectMapper mapper;

    public ToolResponseFactory(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ObjectNode okPayload() {
        ObjectNode node = mapper.createObjectNode();
        node.put("status", "success");
        return node;
    }

    public ToolCallResult success(ObjectNode payload) {
        return new ToolCallResult(true, payload);
    }

    public ToolCallResult error(String message) {
        return error("TOOL_ERROR", message, false);
    }

    public ToolCallResult error(String code, String message, boolean retriable) {
        ObjectNode payload = mapper.createObjectNode();
        payload.put("status", "error");
        payload.put("code", code);
        payload.put("error", message);
        payload.put("retriable", retriable);
        return new ToolCallResult(false, payload);
    }
}
