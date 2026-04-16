package com.portfolex.excelmcp.handles;

import com.portfolex.excelmcp.error.ErrorCode;
import com.portfolex.excelmcp.error.McpException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * In-process registry of open workbooks (§5.2). Single-threaded by design — the MCP stdio loop
 * processes one tool call at a time. No locking, no idle-TTL eviction (process death evicts).
 */
public final class HandleRegistry {

  private final Map<HandleId, OpenWorkbook> entries = new LinkedHashMap<>();

  public synchronized HandleId register(OpenWorkbook entry) {
    entries.put(entry.id(), entry);
    return entry.id();
  }

  public synchronized Optional<OpenWorkbook> lookup(HandleId id) {
    return Optional.ofNullable(entries.get(id));
  }

  public synchronized OpenWorkbook require(HandleId id) throws McpException {
    OpenWorkbook found = entries.get(id);
    if (found == null) {
      throw new McpException(
          ErrorCode.HANDLE_UNKNOWN, "handle not found: " + id, Map.of("handle", id.value()));
    }
    return found;
  }

  public synchronized Optional<OpenWorkbook> remove(HandleId id) {
    return Optional.ofNullable(entries.remove(id));
  }

  public synchronized int size() {
    return entries.size();
  }
}
