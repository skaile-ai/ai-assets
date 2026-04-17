package ai.skaile.mcpo.ppt.tooling.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import ai.skaile.mcpo.ppt.tooling.contracts.ToolDefinition;

public final class ToolArgumentValidator {
    private final ObjectMapper mapper;

    public ToolArgumentValidator(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public JsonNode normalizeArguments(JsonNode arguments) {
        if (arguments == null || arguments.isNull() || arguments.isMissingNode()) {
            return mapper.createObjectNode();
        }
        return arguments;
    }

    public String requiredString(JsonNode args, String key) {
        String value = args.path(key).asText("");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument: " + key);
        }
        return value;
    }

    public void validateArguments(ToolDefinition definition, JsonNode arguments) {
        JsonNode schema = definition.inputSchema();
        if (!arguments.isObject()) {
            throw new IllegalArgumentException("arguments must be a JSON object");
        }

        JsonNode required = schema.path("required");
        if (required.isArray()) {
            for (JsonNode requiredKey : required) {
                String key = requiredKey.asText();
                if (!arguments.has(key) || arguments.path(key).isNull()) {
                    throw new IllegalArgumentException("Missing required argument: " + key);
                }
            }
        }

        JsonNode properties = schema.path("properties");
        boolean allowAdditional = schema.path("additionalProperties").asBoolean(true);

        Iterator<String> fieldNames = arguments.fieldNames();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (!allowAdditional && !properties.has(field)) {
                throw new IllegalArgumentException("Unknown argument: " + field);
            }
            if (properties.has(field)) {
                validateProperty(field, arguments.path(field), properties.path(field));
            }
        }
    }

    public List<Integer> parseShapeIndices(JsonNode node) {
        List<Integer> indices = new ArrayList<>();
        if (!node.isArray()) {
            return indices;
        }
        for (JsonNode item : node) {
            int idx = item.asInt(-1);
            if (idx >= 0) {
                indices.add(idx);
            }
        }
        return indices;
    }

    public List<String> parseSlideTitles(JsonNode node) {
        List<String> titles = new ArrayList<>();
        if (!node.isArray()) {
            return titles;
        }
        for (JsonNode item : node) {
            String value = item.asText("").strip();
            if (!value.isBlank()) {
                titles.add(value);
            }
        }
        return titles;
    }

    public boolean isValidRect(double x, double y, double w, double h) {
        return Double.isFinite(x)
                && Double.isFinite(y)
                && Double.isFinite(w)
                && Double.isFinite(h)
                && w > 0
                && h > 0;
    }

    private void validateProperty(String fieldName, JsonNode value, JsonNode propertySchema) {
        if (value == null || value.isNull()) {
            return;
        }

        String type = propertySchema.path("type").asText("");
        if (!type.isBlank()) {
            boolean typeValid = switch (type) {
                case "string" -> value.isTextual();
                case "integer" -> value.isIntegralNumber();
                case "number" -> value.isNumber();
                case "boolean" -> value.isBoolean();
                case "array" -> value.isArray();
                case "object" -> value.isObject();
                default -> true;
            };
            if (!typeValid) {
                throw new IllegalArgumentException("Invalid type for argument " + fieldName + ": expected " + type);
            }
        }

        JsonNode enumNode = propertySchema.path("enum");
        if (enumNode.isArray() && enumNode.size() > 0) {
            boolean matched = false;
            for (JsonNode enumValue : enumNode) {
                if (enumValue.equals(value)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new IllegalArgumentException("Invalid value for argument " + fieldName + ": " + value);
            }
        }

        if (value.isNumber() && propertySchema.has("minimum")) {
            double minimum = propertySchema.path("minimum").asDouble();
            if (value.asDouble() < minimum) {
                throw new IllegalArgumentException(
                        "Invalid value for argument " + fieldName + ": must be >= " + minimum);
            }
        }

        if (value.isArray()) {
            int minItems = propertySchema.path("minItems").asInt(-1);
            if (minItems >= 0 && value.size() < minItems) {
                throw new IllegalArgumentException(
                        "Invalid value for argument " + fieldName + ": must contain at least " + minItems
                                + " items");
            }
            JsonNode itemSchema = propertySchema.path("items");
            for (JsonNode item : value) {
                validateProperty(fieldName + "[]", item, itemSchema);
            }
        }
    }
}
