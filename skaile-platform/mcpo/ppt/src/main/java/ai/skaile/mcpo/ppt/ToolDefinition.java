package ai.skaile.mcpo.ppt;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolDefinition(String name, String description, JsonNode inputSchema) {
}
