package com.skaile.excelmcp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import org.junit.jupiter.api.Test;

/** Coverage for the input-validation routing fixed in review B3 / B5. */
class ToolInputsTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static JsonNode parse(String json) throws Exception {
    return MAPPER.readTree(json);
  }

  @Test
  void missingRequiredStringRaisesValidationError() throws Exception {
    JsonNode in = parse("{\"handle\": \"wb-1\"}");
    assertThatThrownBy(() -> ToolInputs.requireString(in, "sheet"))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void wrongTypeStringFieldRaisesValidationError() throws Exception {
    JsonNode in = parse("{\"sheet\": 42}");
    assertThatThrownBy(() -> ToolInputs.requireString(in, "sheet"))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void presentStringPassesThrough() throws Exception {
    JsonNode in = parse("{\"sheet\": \"Data\"}");
    assertThat(ToolInputs.requireString(in, "sheet")).isEqualTo("Data");
  }

  @Test
  void missingHandleStaysAsHandleUnknownNotValidationError() throws Exception {
    JsonNode in = parse("{}");
    assertThatThrownBy(() -> ToolInputs.requireHandle(in))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.HANDLE_UNKNOWN);
  }

  @Test
  void malformedHandleStaysAsHandleUnknown() throws Exception {
    JsonNode in = parse("{\"handle\": \"not-a-handle\"}");
    assertThatThrownBy(() -> ToolInputs.requireHandle(in))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.HANDLE_UNKNOWN);
  }

  @Test
  void requireIntThrowsValidationErrorWhenMissing() throws Exception {
    JsonNode in = parse("{}");
    assertThatThrownBy(() -> ToolInputs.requireInt(in, "start_row"))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void requireIntThrowsValidationErrorWhenWrongType() throws Exception {
    JsonNode in = parse("{\"start_row\": \"five\"}");
    assertThatThrownBy(() -> ToolInputs.requireInt(in, "start_row"))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.VALIDATION_ERROR);
  }

  @Test
  void requireIntReturnsValueWhenPresent() throws Exception {
    JsonNode in = parse("{\"start_row\": 5}");
    assertThat(ToolInputs.requireInt(in, "start_row")).isEqualTo(5);
  }
}
