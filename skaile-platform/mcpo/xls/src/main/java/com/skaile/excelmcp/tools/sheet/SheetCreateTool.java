package com.skaile.excelmcp.tools.sheet;

import static com.skaile.excelmcp.server.ToolInputs.object;
import static com.skaile.excelmcp.server.ToolInputs.requireHandle;
import static com.skaile.excelmcp.server.ToolInputs.requireString;
import static com.skaile.excelmcp.server.ToolInputs.stringProp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.skaile.excelmcp.engine.WorkbookEngine;
import com.skaile.excelmcp.error.McpException;
import com.skaile.excelmcp.handles.HandleId;
import com.skaile.excelmcp.server.ToolDefinition;
import com.skaile.excelmcp.shape.SheetShape;
import java.util.OptionalInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code sheet.create(handle, name, index?)} — add a new empty sheet. */
public final class SheetCreateTool implements ToolDefinition {

  private static final Logger log = LoggerFactory.getLogger(SheetCreateTool.class);

  private final WorkbookEngine engine;

  public SheetCreateTool(WorkbookEngine engine) {
    this.engine = engine;
  }

  @Override
  public String name() {
    return "sheet.create";
  }

  @Override
  public String description() {
    return "Adds a new empty sheet to the workbook. Requires an open workbook handle; the sheet"
        + " can be populated immediately via range.set but the change is in-memory until"
        + " workbook.save. Sheet names are case-insensitive for uniqueness and capped at 31"
        + " characters with the usual forbidden characters (: \\ / ? * [ ]); a duplicate name"
        + " returns SHEET_ALREADY_EXISTS.";
  }

  @Override
  public JsonNode inputSchema() {
    ObjectNode props = object();
    props.set(
        "handle",
        stringProp(
            "Workbook handle previously returned by workbook.open or workbook.create; opaque"
                + " \"wb-\" prefixed string, e.g. \"wb-3f9a1c4d\"."));
    props.set(
        "name",
        stringProp(
            "New sheet name; 1–31 chars, must not contain : \\ / ? * [ ] or start/end with an"
                + " apostrophe. Case-insensitive uniqueness — \"Summary\" clashes with"
                + " \"summary\"."));
    ObjectNode indexProp = object();
    indexProp.put("type", "integer");
    indexProp.put(
        "description",
        "Optional zero-based insert position in the sheet order; omit to append at the end. Must"
            + " be between 0 and the current sheet count (inclusive).");
    indexProp.put("minimum", 0);
    props.set("index", indexProp);
    ObjectNode schema = object();
    schema.put("type", "object");
    schema.set("properties", props);
    schema.putArray("required").add("handle").add("name");
    return schema;
  }

  @Override
  public Object execute(JsonNode input) throws McpException {
    HandleId id = requireHandle(input);
    String name = requireString(input, "name");
    OptionalInt index = OptionalInt.empty();
    JsonNode idx = input.get("index");
    if (idx != null && !idx.isNull() && idx.canConvertToInt()) {
      index = OptionalInt.of(idx.intValue());
    }
    SheetShape created = engine.createSheet(id, name, index);
    log.info(
        "sheet.create handle={} name={} index={}", id.value(), created.name(), created.index());
    return created;
  }
}
