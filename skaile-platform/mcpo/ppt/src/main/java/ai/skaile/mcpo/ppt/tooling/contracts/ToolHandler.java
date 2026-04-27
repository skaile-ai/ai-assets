package ai.skaile.mcpo.ppt.tooling.contracts;

import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface ToolHandler {
    ToolCallResult handle(JsonNode args) throws Exception;
}
