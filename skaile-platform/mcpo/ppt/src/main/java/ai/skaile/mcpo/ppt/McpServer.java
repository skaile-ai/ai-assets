package ai.skaile.mcpo.ppt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class McpServer {
    private static final String SERVER_NAME = "skaile-ppt-poi-mcp";
    private static final String SERVER_VERSION = "0.1.0";
    private final ObjectMapper mapper = new ObjectMapper();
    private final PptToolService toolService = new PptToolService();

    public void run() {
        try {
            while (true) {
                byte[] raw = JsonRpcIO.readMessage(System.in);
                if (raw == null) {
                    return;
                }

                JsonNode request = mapper.readTree(raw);
                JsonNode id = request.get("id");
                String method = request.path("method").asText();
                JsonNode params = request.path("params");

                if (id == null || id.isMissingNode()) {
                    if ("notifications/initialized".equals(method)) {
                        continue;
                    }
                    continue;
                }

                ObjectNode response = handleRequest(id, method, params);
                byte[] body = mapper.writeValueAsString(response).getBytes(StandardCharsets.UTF_8);
                JsonRpcIO.writeMessage(System.out, body);
            }
        } catch (Exception e) {
            try {
                ObjectNode fatal = mapper.createObjectNode();
                fatal.put("jsonrpc", "2.0");
                fatal.putNull("id");
                ObjectNode error = fatal.putObject("error");
                error.put("code", -32603);
                error.put("message", "Fatal server error: " + e.getMessage());
                byte[] body = mapper.writeValueAsString(fatal).getBytes(StandardCharsets.UTF_8);
                JsonRpcIO.writeMessage(System.out, body);
            } catch (IOException ignored) {
                // Last-resort failure path.
            }
        } finally {
            toolService.closeAllSessions();
        }
    }

    private ObjectNode handleRequest(JsonNode id, String method, JsonNode params) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        response.set("id", id);

        try {
            switch (method) {
                case "initialize" -> response.set("result", initializeResult());
                case "ping" -> response.set("result", mapper.createObjectNode());
                case "tools/list" -> response.set("result", toolsListResult());
                case "tools/call" -> response.set("result", toolsCallResult(id, params));
                default -> response.set("error", methodNotFound(method));
            }
        } catch (Exception e) {
            ObjectNode error = mapper.createObjectNode();
            error.put("code", -32603);
            error.put("message", e.getMessage());
            response.set("error", error);
        }

        return response;
    }

    private ObjectNode initializeResult() {
        ObjectNode result = mapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");

        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools");

        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", SERVER_NAME);
        serverInfo.put("version", SERVER_VERSION);
        return result;
    }

    private ObjectNode toolsListResult() {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode tools = result.putArray("tools");
        for (ToolDefinition definition : toolService.listTools()) {
            ObjectNode tool = tools.addObject();
            tool.put("name", definition.name());
            tool.put("description", definition.description());
            tool.set("inputSchema", definition.inputSchema());
        }
        return result;
    }

    private ObjectNode toolsCallResult(JsonNode requestId, JsonNode params) {
        String name = params.path("name").asText();
        JsonNode arguments = params.path("arguments");
        ToolCallResult toolResult = toolService.call(name, arguments);
        if (toolResult.payload() != null) {
            toolResult.payload().put("correlation_id", requestId.asText());
            toolResult.payload().put("tool_name", name);
        }

        ObjectNode result = mapper.createObjectNode();
        result.put("isError", !toolResult.success());
        result.set("structuredContent", toolResult.payload());
        ArrayNode content = result.putArray("content");
        ObjectNode textNode = content.addObject();
        textNode.put("type", "text");
        try {
            textNode.put("text", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toolResult.payload()));
        } catch (Exception e) {
            textNode.put("text", toolResult.payload().toString());
        }
        return result;
    }

    private ObjectNode methodNotFound(String method) {
        ObjectNode error = mapper.createObjectNode();
        error.put("code", -32601);
        error.put("message", "Method not found: " + method);
        return error;
    }
}
