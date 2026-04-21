package com.skaile.excelmcp.handles;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-process registry of open workbooks (§5.2). Single-threaded by design — the MCP Java SDK's sync
 * stdio dispatch already serialises tool calls, and {@link
 * com.skaile.excelmcp.engine.XssfInMemoryEngine}'s state (the {@code workbooks} and {@code
 * evaluators} {@link java.util.HashMap}s) makes the same assumption. No locking, no idle-TTL
 * eviction (process death evicts).
 *
 * <p>Callers must serialise access. The previous {@code synchronized} methods on this class were a
 * contradictory belt-and-braces against an invariant the rest of the engine doesn't honour, so
 * they're dropped to keep one story.
 */
public final class HandleRegistry {

  private final Map<HandleId, OpenWorkbook> entries = new LinkedHashMap<>();

  public HandleId register(OpenWorkbook entry) {
    entries.put(entry.id(), entry);
    return entry.id();
  }

  public Optional<OpenWorkbook> lookup(HandleId id) {
    return Optional.ofNullable(entries.get(id));
  }

  public OpenWorkbook require(HandleId id) throws McpException {
    OpenWorkbook found = entries.get(id);
    if (found == null) {
      throw new McpException(
          ErrorCode.HANDLE_UNKNOWN, "handle not found: " + id, Map.of("handle", id.value()));
    }
    return found;
  }

  public Optional<OpenWorkbook> remove(HandleId id) {
    return Optional.ofNullable(entries.remove(id));
  }

  public int size() {
    return entries.size();
  }

  /**
   * Snapshot of every currently-registered workbook entry, in insertion order (LinkedHashMap).
   * Returned as an immutable copy so callers can iterate without tripping over a concurrent open or
   * close on the (single) caller thread.
   */
  public Collection<OpenWorkbook> all() {
    return List.copyOf(new ArrayList<>(entries.values()));
  }
}
