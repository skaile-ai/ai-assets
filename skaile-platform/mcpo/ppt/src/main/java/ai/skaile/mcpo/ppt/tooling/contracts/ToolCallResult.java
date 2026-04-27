package ai.skaile.mcpo.ppt.tooling.contracts;

import com.fasterxml.jackson.databind.node.ObjectNode;

public record ToolCallResult(boolean success, ObjectNode payload) {
}
