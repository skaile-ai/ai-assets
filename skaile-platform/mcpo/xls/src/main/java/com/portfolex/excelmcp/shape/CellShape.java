package com.portfolex.excelmcp.shape;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Map;

/**
 * Single-cell wire shape (§7.1). {@code type} is the discriminator for {@code value}. {@code
 * formatting} is present only when a reading tool was called with {@code include_formatting=true};
 * otherwise it is null and omitted by the serialiser.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"a1", "type", "value", "formula", "formatting"})
public record CellShape(
    String a1, String type, Object value, String formula, Map<String, Object> formatting) {

  public enum Type {
    STRING("string"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    DATE("date"),
    ERROR("error"),
    BLANK("blank"),
    FORMULA_UNCOMPUTED("formula_uncomputed");

    private final String wire;

    Type(String wire) {
      this.wire = wire;
    }

    public String wire() {
      return wire;
    }
  }

  public static CellShape blank(String a1) {
    return new CellShape(a1, Type.BLANK.wire(), null, null, null);
  }

  public static CellShape value(String a1, Type type, Object value) {
    return new CellShape(a1, type.wire(), value, null, null);
  }

  public static CellShape formula(String a1, Type cachedType, Object cachedValue, String formula) {
    return new CellShape(a1, cachedType.wire(), cachedValue, formula, null);
  }

  public static CellShape uncomputedFormula(String a1, String formula) {
    return new CellShape(a1, Type.FORMULA_UNCOMPUTED.wire(), null, formula, null);
  }
}
