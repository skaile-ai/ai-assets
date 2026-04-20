package ai.skaile.mcpo.ppt.tooling.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.skaile.mcpo.ppt.tooling.contracts.ToolDefinition;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class ToolArgumentValidatorTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ToolArgumentValidator validator = new ToolArgumentValidator(mapper);

    @Test
    void normalizeArgumentsReturnsEmptyObjectForNullOrMissing() {
        assertTrue(validator.normalizeArguments(null).isObject());
        assertTrue(validator.normalizeArguments(mapper.nullNode()).isObject());
        assertTrue(validator.normalizeArguments(mapper.missingNode()).isObject());
    }

    @Test
    void requiredStringThrowsOnBlankValue() {
        ObjectNode args = mapper.createObjectNode();
        args.put("k", "");
        assertThrows(IllegalArgumentException.class, () -> validator.requiredString(args, "k"));
        assertThrows(IllegalArgumentException.class, () -> validator.requiredString(args, "missing"));
        args.put("k", "value");
        assertEquals("value", validator.requiredString(args, "k"));
    }

    @Test
    void validateArgumentsFlagsMissingAndExtraFields() throws Exception {
        JsonNode schema = mapper.readTree(
                "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},"
                        + "\"required\":[\"a\"],\"additionalProperties\":false}");
        ToolDefinition def = new ToolDefinition("t", "d", schema);

        ObjectNode missing = mapper.createObjectNode();
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateArguments(def, missing));

        ObjectNode bad = mapper.createObjectNode();
        bad.put("a", "ok");
        bad.put("x", "nope");
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateArguments(def, bad));

        ObjectNode ok = mapper.createObjectNode();
        ok.put("a", "ok");
        validator.validateArguments(def, ok);
    }

    @Test
    void validatePropertyChecksTypesEnumsAndMinimums() throws Exception {
        JsonNode schema = mapper.readTree(
                "{\"type\":\"object\",\"properties\":{"
                        + "\"n\":{\"type\":\"number\",\"minimum\":0},"
                        + "\"choice\":{\"type\":\"string\",\"enum\":[\"a\",\"b\"]},"
                        + "\"list\":{\"type\":\"array\",\"minItems\":1,\"items\":{\"type\":\"integer\"}}"
                        + "}}");
        ToolDefinition def = new ToolDefinition("t", "d", schema);

        ObjectNode wrongType = mapper.createObjectNode();
        wrongType.put("n", "not a number");
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateArguments(def, wrongType));

        ObjectNode belowMin = mapper.createObjectNode();
        belowMin.put("n", -1);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateArguments(def, belowMin));

        ObjectNode badEnum = mapper.createObjectNode();
        badEnum.put("choice", "c");
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateArguments(def, badEnum));

        ObjectNode tooShort = mapper.createObjectNode();
        tooShort.putArray("list");
        assertThrows(IllegalArgumentException.class,
                () -> validator.validateArguments(def, tooShort));

        ObjectNode ok = mapper.createObjectNode();
        ok.put("n", 5);
        ok.put("choice", "a");
        ArrayNode list = ok.putArray("list");
        list.add(1).add(2);
        validator.validateArguments(def, ok);
    }

    @Test
    void parseShapeIndicesIgnoresNonArrayAndNegative() {
        ObjectNode args = mapper.createObjectNode();
        assertTrue(validator.parseShapeIndices(args).isEmpty());
        ArrayNode arr = args.putArray("idx");
        arr.add(0).add(-1).add(5);
        assertEquals(java.util.List.of(0, 5), validator.parseShapeIndices(arr));
    }

    @Test
    void parseSlideTitlesTrimsAndSkipsBlanks() {
        ObjectNode args = mapper.createObjectNode();
        assertTrue(validator.parseSlideTitles(args).isEmpty());
        ArrayNode arr = args.putArray("t");
        arr.add("A").add("  ").add("  B  ");
        assertEquals(java.util.List.of("A", "B"), validator.parseSlideTitles(arr));
    }

    @Test
    void isValidRectAcceptsPositiveFiniteNumbers() {
        assertTrue(validator.isValidRect(0, 0, 10, 10));
        assertFalse(validator.isValidRect(Double.NaN, 0, 10, 10));
        assertFalse(validator.isValidRect(0, 0, 0, 10));
        assertFalse(validator.isValidRect(0, 0, -1, 10));
    }
}
