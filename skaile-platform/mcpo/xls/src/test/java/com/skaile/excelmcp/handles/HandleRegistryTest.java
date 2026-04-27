package com.skaile.excelmcp.handles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skaile.excelmcp.error.ErrorCode;
import com.skaile.excelmcp.error.McpException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Coverage for the in-memory handle registry contract. */
class HandleRegistryTest {

  private static OpenWorkbook entry(HandleId id) {
    return new OpenWorkbook(id, Path.of("/tmp/wb.xlsx"), "xlsx", Instant.EPOCH, Optional.empty());
  }

  @Test
  void registerThenLookupReturnsTheEntry() {
    HandleRegistry r = new HandleRegistry();
    HandleId id = HandleId.newRandom();
    r.register(entry(id));

    assertThat(r.lookup(id)).isPresent().get().extracting(OpenWorkbook::id).isEqualTo(id);
    assertThat(r.size()).isEqualTo(1);
  }

  @Test
  void requireOnUnknownHandleThrowsHandleUnknown() {
    HandleRegistry r = new HandleRegistry();

    assertThatThrownBy(() -> r.require(HandleId.newRandom()))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.HANDLE_UNKNOWN);
  }

  @Test
  void removeThenLookupReturnsEmpty() {
    HandleRegistry r = new HandleRegistry();
    HandleId id = HandleId.newRandom();
    r.register(entry(id));

    assertThat(r.remove(id)).isPresent();
    assertThat(r.lookup(id)).isEmpty();
    assertThat(r.size()).isZero();
  }

  @Test
  void removeIsIdempotent() {
    HandleRegistry r = new HandleRegistry();
    HandleId id = HandleId.newRandom();
    r.register(entry(id));

    assertThat(r.remove(id)).isPresent();
    assertThat(r.remove(id)).isEmpty();
  }

  @Test
  void removeThenRequireThrowsHandleUnknown() {
    HandleRegistry r = new HandleRegistry();
    HandleId id = HandleId.newRandom();
    r.register(entry(id));
    r.remove(id);

    assertThatThrownBy(() -> r.require(id))
        .isInstanceOf(McpException.class)
        .extracting(ex -> ((McpException) ex).code())
        .isEqualTo(ErrorCode.HANDLE_UNKNOWN);
  }

  @Test
  void allReturnsImmutableSnapshotInInsertionOrder() {
    HandleRegistry r = new HandleRegistry();
    HandleId a = HandleId.newRandom();
    HandleId b = HandleId.newRandom();
    HandleId c = HandleId.newRandom();
    r.register(entry(a));
    r.register(entry(b));
    r.register(entry(c));

    assertThat(r.all()).extracting(OpenWorkbook::id).containsExactly(a, b, c);
  }
}
